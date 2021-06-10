package org.openinfralabs.caerus.ndpService.service;

import io.minio.*;
import io.minio.messages.Item;
import org.apache.commons.io.IOUtils;
import org.openinfralabs.caerus.ndpService.model.UdfInvocationMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.TimeZone;




@Service
//@Qualifier("storageAdapterMinioImpl")
public class StorageAdapterMinioImpl implements StorageAdapter {

    // adding conditional compiling flag
    public static final boolean SERVERLESS = true;

    Logger logger = LoggerFactory.getLogger(StorageAdapterMinioImpl.class);

    final String udfService_uri = "http://localhost:8002/";
    final String invocation_event_put = "put";
    final String invocation_event_access = "get";
    final String invocation_event_delete = "delete";
    final String bucketNamePath = "bucketName";
    final String objectKeyPath = "objectKey";
    final String DEFAULT_UDF_KEY = "udfKey";
    final String DEFAULT_INPUT_PARAMETERS_KEY = "inputParameters";

    @Autowired
    MinioClient minioClient;


    @Override
    public void uploadFile(String bucket, String filename, InputStream inputStream, UdfInvocationMetadata metadata, String optionalParametersJson) {
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

                RestTemplate udfServiceTemplate = new RestTemplate();

                String path = udfService_uri + bucket + "/" + filename;

                Map<String, String> params = new HashMap<String, String>();
                params.put(DEFAULT_UDF_KEY, metadata.getName());

                if (!userMetadata.isEmpty() && userMetadata.containsKey(DEFAULT_INPUT_PARAMETERS_KEY)) {
                    String inputParametersCommaSeparated = userMetadata.get(DEFAULT_INPUT_PARAMETERS_KEY);
                    params.put(DEFAULT_INPUT_PARAMETERS_KEY, inputParametersCommaSeparated);
                }

                UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(path);
                MultiValueMap<String, String> paramsMVMap = new LinkedMultiValueMap<>();
                paramsMVMap.setAll(params);
                if (!paramsMVMap.isEmpty()) {
                    uriBuilder.queryParams(paramsMVMap);
                }

                ResponseEntity<String> responseEntity = udfServiceTemplate.getForEntity(uriBuilder.build().encode().toUri(), String.class);

                boolean isOK = responseEntity.getStatusCode().equals(HttpStatus.OK);
                if (isOK)
                    logger.info("UDF invoked successfully: " + filename);
                else
                    logger.error("UDF invoked failed: " + filename);

            }

        } catch (Exception e) {
            logger.error("Error occurred: " + e);
        }

    }

    @Override
    public byte[] getFile(String bucket, String key, UdfInvocationMetadata metadata, Map<String, String> headersMap) {
        // metadata is for future use

        /*
            HTTP/1.1 200 OK
            x-amz-id-2: eftixk72aD6Ap51TnqcoF8eFidJG9Z/2mkiDFu8yU9AS1ed4OpIszj7UDNEHGran
            x-amz-request-id: 318BC8BC148832E5
            Date: Wed, 28 Oct 2009 22:32:00 GMT
            Last-Modified: Wed, 12 Oct 2009 17:50:00 GMT
            x-amz-expiration: expiry-date="Fri, 23 Dec 2012 00:00:00 GMT", rule-id="picture-deletion-rule"
            ETag: "fba9dede5f27731c9771645a39863328"
            Content-Length: 434234
            Content-Type: text/plain

            [434234 bytes of object data]
         */
        try {

            // get content-length etc first, also valdiate if the file is there
            StatObjectResponse objectStat =
                    minioClient.statObject(
                            StatObjectArgs.builder().bucket(bucket).object(key).build());
            headersMap.put("Last-Modified", objectStat.lastModified().toString());
            headersMap.put("ETag", objectStat.etag());
            headersMap.put("Content-Length", Long.toString(objectStat.size()));
            headersMap.put("Content-Type", objectStat.contentType());

            DateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date now = new Date();
            headersMap.put("Date", df.format(now));


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
    public boolean deleteFile(String bucket, String key, UdfInvocationMetadata metadata) {
        // metadata is for future use
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    @Override
    public boolean copyObject(String fromBucket, String toBucket, String sourceObjKey, String targetObjKey, UdfInvocationMetadata metadata, StringBuilder sb) {
        // metadata is for future use
        try {

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(toBucket)
                            .object(targetObjKey)
                            .source(
                                    CopySource.builder()
                                            .bucket(fromBucket)
                                            .object(sourceObjKey)
                                            .build())
                            .build());

            /*<?xml version="1.0" encoding="UTF-8"?>
                <CopyObjectResult>
                  <LastModified>2009-10-28T22:32:00</LastModified>
                  <ETag>"9b2cf535f27731c974343645a3985328"</ETag>
               <CopyObjectResult>*/

            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder().bucket(toBucket).object(targetObjKey).build());
            String lastModifiedStr = stat.lastModified().toString();
            String etagStr = stat.etag();

            String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
            sb.append(content);
            sb.append("<CopyObjectResult>\n");
            String lastModified = "<LastModified>" + lastModifiedStr + "</LastModified>\n";
            sb.append(lastModified);
            String etag = "<ETag>" + etagStr + "</ETag>\n";
            sb.append(etag);
            sb.append("</CopyObjectResult>\n");


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean listObjects(String bucket, StringBuilder sb) {
        /*
        <?xml version="1.0" encoding="UTF-8"?>
            <ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                <Name>bucket</Name>
                <Prefix/>
                <KeyCount>205</KeyCount>
                <MaxKeys>1000</MaxKeys>
                <IsTruncated>false</IsTruncated>
                <Contents>
                    <Key>my-image.jpg</Key>
                    <LastModified>2009-10-12T17:50:30.000Z</LastModified>
                    <ETag>"fba9dede5f27731c9771645a39863328"</ETag>
                    <Size>434234</Size>
                    <StorageClass>STANDARD</StorageClass>
                </Contents>
                <Contents>
                   ...
                </Contents>
                ...
            </ListBucketResult>
         */


        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">");

        sb.append("<Name>" + bucket + "</Name>\n");
        sb.append("<Prefix/>\n");

        List<String> objectsInfo = new ArrayList<String>();

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucket).maxKeys(100).build());


        int count = 0;
        StringBuilder contentsBuilder = new StringBuilder();
        for (Result<Item> result : results) {
            try {
                String objName = result.get().objectName();
                String objSize = Long.toString(result.get().size());
                String lastModified = result.get().lastModified().toString();
                String etag = result.get().etag();
                String storageClass = result.get().storageClass();

                contentsBuilder.append("<Contents>\n");
                contentsBuilder.append("<Key>" + objName + "</Key>\n");
                contentsBuilder.append("<LastModified>" + lastModified + "</LastModified>\n");
                contentsBuilder.append("<ETag>" + etag + "</ETag>\n");
                contentsBuilder.append("<Size>" + objSize + "</Size>\n");
                contentsBuilder.append("<StorageClass>" + storageClass + "</StorageClass>\n");
                contentsBuilder.append("</Contents>\n");

                count++;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        sb.append("<KeyCount>" + count + "</KeyCount>\n");
        sb.append("<MaxKeys>100</MaxKeys>\n");
        boolean isTruncated = false;
        if (count <= 100)
            sb.append("<IsTruncated>false</IsTruncated>\n");
        else
            sb.append("<IsTruncated>true</IsTruncated>\n");

        sb.append(contentsBuilder.toString());
        sb.append("</ListBucketResult>\n");

        return true;
    }
}
