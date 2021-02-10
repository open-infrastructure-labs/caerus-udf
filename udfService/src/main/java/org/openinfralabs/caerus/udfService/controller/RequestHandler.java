package org.openinfralabs.caerus.udfService.controller;

import org.openinfralabs.caerus.udfService.model.Udf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;


import java.io.*;
import java.util.*;

// SERVERLESS related imports
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;


import io.swagger.client.ApiException;
import io.swagger.client.api.DefaultApi;
import io.swagger.client.model.FunctionListEntry;

@RestController
public class RequestHandler {
    Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    final String udf_registry_service_uri = "http://localhost:8080/udf/";
    final String udf_docker_uri = "http://localhost:8090/";
    final String bucketNamePath = "bucketName";
    final String objectKeyPath = "objectKey";
    final String DEFAULT_UDF_KEY = "udfKey";
    final String DEFAULT_INPUT_PARAMETERS_KEY = "inputParameters";

    // TODO: we can implement a service interface, with serverless (Q1 item) and non-serverless (Q2 item) implementations, use user input or config file to read in option
    // for now, just use a simple approach by adding conditional compiling flag
    public static final boolean SERVERLESS = true;

    @GetMapping("/{bucketName}/{objectKey}")
    public ResponseEntity<String> handleRequest(@PathVariable("bucketName") String bucket, @PathVariable("objectKey") String key, @RequestParam Map<String, String> metadata) {

        String udfKey = metadata.get(DEFAULT_UDF_KEY);

        if (!SERVERLESS) {
            RestTemplate restTemplate = new RestTemplate();


            Udf udf = restTemplate.getForObject(udf_registry_service_uri + udfKey, Udf.class);
            if (udf != null) {
                logger.info("found udf in the registry: " + udfKey);
            } else {
                logger.error("no udf found in the registry.");
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("can't find the UDF in registry with this UDF key: " + udfKey);
            }

            //2. TODO: need automate the deploy of udf

            //3. invoke udf via udf service

            RestTemplate udfRestTemplate = new RestTemplate();
            Map<String, String> params = new HashMap<String, String>();
            params.put(bucketNamePath, bucket);
            params.put(objectKeyPath, key);

            String path = udf_docker_uri + bucket + "/" + key;


            if (!metadata.isEmpty() && metadata.containsKey(DEFAULT_INPUT_PARAMETERS_KEY)) {
                String inputParametersCommaSeparated = metadata.get(DEFAULT_INPUT_PARAMETERS_KEY);
                path = path + "/" + "?" + DEFAULT_INPUT_PARAMETERS_KEY + "=" + inputParametersCommaSeparated;
            }
            ResponseEntity<String> responseEntity = udfRestTemplate.getForEntity(path, String.class, params);
            boolean isOK = responseEntity.getStatusCode().equals(HttpStatus.OK);
            if (isOK)
                logger.info("UDF " + udfKey + " invoked successfully on object: " + key);
            else
                logger.error("UDF " + udfKey + " invoked failed on object: " + key);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Invocation failed for this UDF: " + udfKey);

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
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("can't find the UDF in registry with this UDF key: " + udfKey);
            }


            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty(bucketNamePath, bucket);
            jsonObject.addProperty(objectKeyPath, key);


            byte[] inputBytes = new byte[]{};

            if (!metadata.isEmpty() && metadata.containsKey(DEFAULT_INPUT_PARAMETERS_KEY)) {
                String inputParametersCommaSeparated = metadata.get(DEFAULT_INPUT_PARAMETERS_KEY);
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
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Invocation failed for this UDF: " + udfKey);

            }
        }

        logger.info("UDF invoked successfully");
        return ResponseEntity.ok("Udf invoked successfully.");
    }

}



