package org.openinfralabs.caerus.clientService.service;

import com.amazonaws.services.s3.event.S3EventNotification;
import io.minio.*;
import io.minio.messages.Bucket;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.openinfralabs.caerus.clientService.model.UdfInvocationMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URI;
import java.util.*;

import org.openinfralabs.caerus.clientService.model.Udf;
import org.springframework.web.util.UriComponentsBuilder;


// SERVERLESS related imports
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;


import io.swagger.client.ApiException;
import io.swagger.client.api.DefaultApi;
import io.swagger.client.model.FunctionListEntry;

@Service
public class StorageAdapterMinioImpl implements StorageAdapter {

    // adding conditional compiling flag
    public static final boolean SERVERLESS = true;

    Logger logger = LoggerFactory.getLogger(StorageAdapterMinioImpl.class);

    final String udf_registry_service_uri = "http://localhost:8080/udf/";
    final String udf_docker_uri = "http://localhost:8090/";
    final String invocation_event_put = "put";
    final String invocation_event_access = "get";
    final String invocation_event_delete = "delete";
    final String bucketNamePath = "bucketName";
    final String objectKeyPath = "objectKey";
    final String DEFAULT_INPUT_PARAMETERS_KEY = "inputParameters";

    @Autowired
    MinioClient minioClient;


    @Override
    public void uploadFile(String bucket, String filename, InputStream inputStream, UdfInvocationMetadata metadata) {
        try {
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            } else {
                logger.info("Bucket already exists: " + bucket);
            }

            // Upload input stream with headers and user metadata.

            // see if there is udf metadata
            Map<String, String> userMetadata = new HashMap<>();
            if (metadata == null) {
                minioClient.putObject(
                        PutObjectArgs.builder().bucket(bucket).object(filename).stream(
                                inputStream, inputStream.available(), -1)
                                .build());
            } else {
                // TODO: user metadata eventually can be used in storage to capture, and then add business logic in storage system. For now it is just a recording on storage side
                userMetadata.put("udfName", metadata.getName());

                Optional<List<String>> extraResources = metadata.getExtraResources();
                if (extraResources.isPresent()) {
                    String extraResourcesCommaSeparated = String.join(",", extraResources.get());
                    userMetadata.put("extraResources", extraResourcesCommaSeparated);
                }
                Optional<List<String>> inputParameters = metadata.getInputParameters();
                if (inputParameters.isPresent()) {
                    String inputParametersCommaSeparated = String.join(",", inputParameters.get());
                    userMetadata.put(DEFAULT_INPUT_PARAMETERS_KEY, inputParametersCommaSeparated);
                }
                minioClient.putObject(
                        PutObjectArgs.builder().bucket(bucket).object(filename).stream(
                                inputStream, inputStream.available(), -1)
                                .userMetadata(userMetadata)
                                .build());

            }

            // Make sure to close inputstream, otherwiase, it might cause Connection Reset error
            inputStream.close();

            logger.info("File uploaded: " + filename);

            // see if there is udf metadata
            if (metadata != null) {
                //1. validate via registry service

                String udfKey = metadata.getName();

                if (!SERVERLESS) {
                    RestTemplate restTemplate = new RestTemplate();


                    Udf udf = restTemplate.getForObject(udf_registry_service_uri + udfKey, Udf.class);
                    if (udf != null) {
                        logger.info("found udf in the registry: " + udfKey);
                    } else {
                        logger.error("no udf found in the registry, so ignore udf invocation.");
                        return;
                    }

                    //2. TODO: need automate the deploy of udf

                    //3. invoke udf via udf service

                    RestTemplate udfRestTemplate = new RestTemplate();
                    Map<String, String> params = new HashMap<String, String>();
                    params.put(bucketNamePath, bucket);
                    params.put(objectKeyPath, filename);

                    String path = udf_docker_uri + bucket + "/" + filename;

                    if (!userMetadata.isEmpty() && userMetadata.containsKey(DEFAULT_INPUT_PARAMETERS_KEY)) {
                        String inputParametersCommaSeparated = userMetadata.get(DEFAULT_INPUT_PARAMETERS_KEY);
                        path = path + "/" + "?" + DEFAULT_INPUT_PARAMETERS_KEY + "=" + inputParametersCommaSeparated;
                    }
                    ResponseEntity<String> responseEntity = udfRestTemplate.getForEntity(path, String.class, params);
                    boolean isOK = responseEntity.getStatusCode().equals(HttpStatus.OK);
                    if (isOK)
                        logger.info("UDF invoked successfully: " + filename);
                    else
                        logger.error("UDF invoked failed: " + filename);

                } else { //SERVERLESS MODE

                    DefaultApi apiInstance = new DefaultApi();

                    // check if the function name is in the openfaas deployed function list
                    // don't re-throw exception here, just log the error, since we did main task of storage operations like put, delete...

                    try {
                        FunctionListEntry result = apiInstance.systemFunctionFunctionNameGet(udfKey);
                        //System.out.println(result);
                    } catch (ApiException e) {
                        System.err.println("Exception when calling DefaultApi#systemFunctionFunctionNameGet");
                        e.printStackTrace();
                        logger.error("no function found in the Openfaas deployed function list, so ignore invocation: udfKey = " + udfKey);
                        return;
                    }


                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty(bucketNamePath, bucket);
                    jsonObject.addProperty(objectKeyPath, filename);


                    byte[] inputBytes = new byte[]{};

                    if (!userMetadata.isEmpty() && userMetadata.containsKey(DEFAULT_INPUT_PARAMETERS_KEY)) {
                        String inputParametersCommaSeparated = userMetadata.get(DEFAULT_INPUT_PARAMETERS_KEY);
                        List<String> list = Arrays.asList(inputParametersCommaSeparated.split(","));
                        String innerObjStr = new Gson().toJson(list);
                        JsonElement jsonElement = new JsonParser().parse(innerObjStr);

                        jsonObject.add(DEFAULT_INPUT_PARAMETERS_KEY, jsonElement);


                        String jsonStr = jsonObject.toString();
                        inputBytes = jsonStr.getBytes();
                    }


                    try {
                        apiInstance.functionFunctionNamePost(udfKey, inputBytes);
                    } catch (ApiException e) {
                        System.err.println("Exception when calling DefaultApi#functionFunctionNamePost");
                        logger.error("UDF invoked failed");
                        e.printStackTrace();
                        return;

                    }
                }

                logger.info("UDF invoked successfully");

            }


        } catch (Exception e) {
            logger.error("Error occurred: " + e);
        }

    }

    @Override
    public byte[] getFile(String bucket, String key, UdfInvocationMetadata metadata) {
        try {
            InputStream obj = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build());
            // Read data from stream

            byte[] content = IOUtils.toByteArray(obj);
            obj.close();
            return content;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public void deleteFile(String bucket, String key, UdfInvocationMetadata metadata) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
