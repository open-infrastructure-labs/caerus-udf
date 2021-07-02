package org.openinfralabs.caerus.eventListenerService.receiver;

import org.joda.time.DateTime;
import org.openinfralabs.caerus.eventListenerService.config.RedisConfig;
import org.openinfralabs.caerus.eventListenerService.model.HDFSiNotifyEvent;
import org.openinfralabs.caerus.eventListenerService.model.Udf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

//TODO: aws s3 should be in separate class, to support more cloud storages
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;

import org.springframework.web.client.RestTemplate;


// SERVERLESS related imports
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;


import org.openinfralabs.caerus.openfaasClient.ApiException;
import org.openinfralabs.caerus.openfaasClient.api.DefaultApi;
import org.openinfralabs.caerus.openfaasClient.model.FunctionListEntry;

@Component
public class KeySpaceNotificationMessageListener implements MessageListener {
    // adding conditional compiling flag
    public static final boolean SERVERLESS = true;

    Logger logger = LoggerFactory.getLogger(KeySpaceNotificationMessageListener.class);

    final String udf_registry_service_uri = "http://localhost:8004/udf";
    final String udf_docker_uri = "http://localhost:8090/";
    //final String udf_docker_uri = "http://172.17.0.2:8090/";
    final String invocation_event_put = "put";
    final String invocation_event_access = "get";
    final String invocation_event_delete = "delete";
    final String invocation_event_copy = "copy";
    //Six hdfs event types: append, close, create, metadata, rename, and unlink.
    final String invocation_event_put_hdfs = "create";
    // for HDFS, Note that no events are necessarily sent when a file is opened for read (although a MetadataUpdateEvent will be sent if the atime is updated).
    //Sent when a file is closed after append or create.
    final String invocation_event_close_hdfs = "close";
    final String invocation_event_delete_hdfs = "unlink";
    final String invocation_event_copy_hdfs = "create";


    final String bucketNamePath = "bucketName";
    final String objectKeyPath = "objectKey";
    final String DEFAULT_INPUT_PARAMETERS_KEY = "inputParameters";
    final String LEN = "200";
    final String WID = "400";
    String INPUT_PARAMETERS_COMMA_SEPARATED = "200,200";

    final String CAERUS_HDFS_EVENTS = "caerus_hdfs_events";

    @Autowired
    RedisCacheManager redisCacheManager;

    @Autowired
    RedisConfig redisConfig;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostConstruct
    public void init() {
        redisConfig.setKeySpaceNotificationMessageListener(this);
    }

    @Override
    public void onMessage(org.springframework.data.redis.connection.Message message, byte[] bytes) {

        String key = new String(message.getChannel());
        if (key.toLowerCase(Locale.ROOT).contains(CAERUS_HDFS_EVENTS.toLowerCase(Locale.ROOT))) {
            // hdfs
            onMessage_process_hdfs_events(message, bytes);
        } else {
            // object storage (use MinIO as an example)
            onMessage_process_object_storage_events(message, bytes);
        }
    }

    private void onMessage_process_hdfs_events(org.springframework.data.redis.connection.Message message, byte[] bytes) {
        /* for debug only
        String action = new String(message.getBody());
        String key = new String(message.getChannel());
        String bytesStr = new String(bytes);
        String messageToStr = message.toString();

        logger.info("Recieved action = " + action + " " +
                " key info = " + key
                + " bytes = " + bytesStr
                + "message string = " + messageToStr);


        Set<byte[]> keys = redisTemplate.getConnectionFactory().getConnection().keys("*".getBytes());

        Iterator<byte[]> it = keys.iterator();

        while(it.hasNext()){

            byte[] data = (byte[])it.next();

            logger.info(new String(data, 0, data.length));
        }*/

        // only the last event can be obtained, verified via redis cli
        // localhost:6379> get caerus_hdfs_events
        //    "{\"eventType\":\"CLOSE\",\"path\":\"/input/sample.jpg\",\"fileSize\":82245,\"timestamp\":1624930693634}"
        //  localhost:6379>
        String jsonRecord = (String) redisTemplate.opsForValue().get(CAERUS_HDFS_EVENTS);

        //String jsonRecords = (String) redisTemplate.opsForHash().get(key);
        if (jsonRecord == null) {
            logger.warn("no hdfs inotify event found, so ignore the folder notification.");
            return;
        }

        // construct event object from json
        HDFSiNotifyEvent event = new Gson().fromJson(jsonRecord, HDFSiNotifyEvent.class);

        if (SERVERLESS) {
            DefaultApi apiInstance = new DefaultApi();
            List<FunctionListEntry> response = new ArrayList();
            try {
                response = apiInstance.systemFunctionsGet();


            } catch (ApiException e) {
                System.err.println("Exception when calling DefaultApi#asyncFunctionFunctionNamePost");
                e.printStackTrace();
            }

            System.out.println(response);

            List<FunctionListEntry> udfPutList = new ArrayList<FunctionListEntry>();
            List<FunctionListEntry> udfDeleteList = new ArrayList<FunctionListEntry>();
            List<FunctionListEntry> udfAccessList = new ArrayList<FunctionListEntry>();
            List<FunctionListEntry> udfCopyList = new ArrayList<FunctionListEntry>();


            for (int i = 0; i < response.size(); i++) {
                FunctionListEntry functionListEntry = response.get(i);
                Map<String, String> annotations = functionListEntry.getAnnotations();
                String invocation_event_types = annotations.get("invocation_event_types");

                if (invocation_event_types != null) {
                    Gson gson = new Gson();
                    JsonElement jsonElement = new JsonParser().parse(invocation_event_types);
                    JsonArray jsonArray = jsonElement.getAsJsonArray();
                    List<String> list = gson.fromJson(jsonArray, new TypeToken<List<String>>() {
                    }.getType());
                    for (int j = 0; j < list.size(); j++) {
                        String type = list.get(j);
                        if (type.compareToIgnoreCase("put") == 0)
                            udfPutList.add(functionListEntry);
                        else if (type.compareToIgnoreCase("delete") == 0)
                            udfDeleteList.add(functionListEntry);
                        else if (type.compareToIgnoreCase("get") == 0)
                            udfAccessList.add(functionListEntry);
                        else if (type.compareToIgnoreCase("copy") == 0)
                            udfCopyList.add(functionListEntry);
                    }
                }

            }

            if (event.getEventType().toLowerCase().contains(invocation_event_put_hdfs) || event.getEventType().toLowerCase().contains(invocation_event_close_hdfs)) {


                if (false == udfPutList.isEmpty()) {
                    for (int i = 0; i < udfPutList.size(); i++) {

                        String functionName = udfPutList.get(i).getName(); // String | Function name

                        // separate path and object name
                        String pathStr = event.getPath();

                        // No subfolder support: nifi folder notification doesn't support changes inside child folders
                        Path path = Paths.get(pathStr);
                        String bucketName = path.getParent().getName(0).toString();


                        String objkey = path.getFileName().toString();


                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty(bucketNamePath, bucketName);
                        jsonObject.addProperty(objectKeyPath, objkey);

                        List<String> list = Arrays.asList(LEN, WID);
                        String innerObjStr = new Gson().toJson(list);
                        JsonElement jsonElement = new JsonParser().parse(innerObjStr);

                        jsonObject.add(DEFAULT_INPUT_PARAMETERS_KEY, jsonElement);


                        String jsonStr = jsonObject.toString();
                        byte[] inputBytes = jsonStr.getBytes();

                        try {
                            apiInstance.functionFunctionNamePost(functionName, inputBytes);
                        } catch (ApiException e) {
                            System.err.println("Exception when calling DefaultApi#functionFunctionNamePost");
                            logger.error("UDF invoked failed on HDFS");
                            e.printStackTrace();
                            return;
                        }

                        logger.info("UDF invoked successfully on HDFS");


                    }

                }

            } else if (event.getEventType().toLowerCase().contains(invocation_event_delete_hdfs)) {
                //TODO
            } else if (event.getEventType().toLowerCase().contains(invocation_event_copy_hdfs)) {
                //TODO
            }

        } else {
            // TODO: to be implemented for non-serverless mode
        }
    }

    private void onMessage_process_object_storage_events(org.springframework.data.redis.connection.Message message, byte[] bytes) {

        String action = new String(message.getBody());
        String key = new String(message.getChannel());
        String bytesStr = new String(bytes);
        String messageToStr = message.toString();

        logger.info("Recieved action = " + action + " " +
                " key info = " + key
                + " bytes = " + bytesStr
                + "message string = " + messageToStr);

        // get invocationEvents types from UdfRegistryService
        RestTemplate restTemplate = new RestTemplate();

        if (!SERVERLESS) {

            // TODO: need to handle this in a batch way, e.g., get a list of UDFs that are all on "PUT"
            // get udf notification event
            Udf[] udfArray = restTemplate.getForObject(udf_registry_service_uri, Udf[].class);
            List<Udf> udfPutList = new ArrayList<Udf>();
            List<Udf> udfDeleteList = new ArrayList<Udf>();
            List<Udf> udfAccessList = new ArrayList<Udf>();
            if (udfArray != null) {
                for (Udf tmpUdf : udfArray) {
                    List<String> listInvocationEvents = tmpUdf.getInvocationEvents();
                    for (String tmpStr : listInvocationEvents) {
                        if (tmpStr.toLowerCase().contains(invocation_event_access)) {
                            udfAccessList.add(tmpUdf);
                        } else if (tmpStr.toLowerCase().contains(invocation_event_put)) {
                            udfPutList.add(tmpUdf);
                        } else if (tmpStr.toLowerCase().contains(invocation_event_delete)) {
                            udfDeleteList.add(tmpUdf);
                        }
                    }
                }
            } else {
                // if there is no such bucket, it might not be error, the bucket might be deleted after the event, log warning and move on
                logger.warn("no udf found, so ignore the bucket notification.");
                return;
            }


            // TODO: separate this into aws s3 (minio use this one), azure bob storage, gcs google cloud storage etc. they all have notification and own APIs
            List<String> jsonRecords = redisTemplate.opsForHash().values(action);

            List<S3EventNotificationRecord> records = new ArrayList<S3EventNotificationRecord>();
            for (String payload : jsonRecords) {
                S3EventNotification s3EventNotification = S3EventNotification.parseJson(payload);
                records.addAll(s3EventNotification.getRecords());
            }


            for (S3EventNotificationRecord currRecord : records) {

                // TODO: Need to add a mechanism to only process the "new" event, based on eventtime
                DateTime eventtime = currRecord.getEventTime();
                // eventName s3:ObjectAccessed:Get, :Put, and :DeletetmpStr.
                String eventName = currRecord.getEventName();
                if (
                        (eventName.toLowerCase().contains(invocation_event_put) && !udfPutList.isEmpty()) ||
                                (eventName.toLowerCase().contains(invocation_event_access) && !udfAccessList.isEmpty()) ||
                                (eventName.toLowerCase().contains(invocation_event_delete) && !udfDeleteList.isEmpty())
                ) {
                    S3EventNotification.S3Entity s3 = currRecord.getS3();
                    S3EventNotification.S3BucketEntity bucket = s3.getBucket();
                    // bucket name: imagesbucket
                    String bucketName = bucket.getName();


                    S3EventNotification.S3ObjectEntity s3obj = s3.getObject();
                    // objkey: sample.jpg
                    String objkey = s3obj.getKey();


                    // get invocationEvents types from UdfRegistryService
                    RestTemplate udfRestTemplate = new RestTemplate();

                    // TODO: need to handle this in a batch way, e.g., get a list of UDFs that are all on "PUT"
                    // get udf notification event

                    // invoke udf
                    Map<String, String> params = new HashMap<String, String>();
                    params.put(bucketNamePath, bucketName);
                    params.put(objectKeyPath, objkey);

                    String path = udf_docker_uri + bucketName + "/" + objkey;

                    path = path + "/" + "?" + DEFAULT_INPUT_PARAMETERS_KEY + "=" + INPUT_PARAMETERS_COMMA_SEPARATED;

                    ResponseEntity<String> responseEntity = udfRestTemplate.getForEntity(path, String.class, params);
                    boolean isOK = responseEntity.getStatusCode().equals(HttpStatus.OK);
                    if (isOK)
                        logger.info("UDF invoked successfully");
                    else
                        logger.error("UDF invoked failed");
                }
            }
        } else { // Serverless


            DefaultApi apiInstance = new DefaultApi();
            List<FunctionListEntry> response = new ArrayList();
            try {
                response = apiInstance.systemFunctionsGet();


            } catch (ApiException e) {
                System.err.println("Exception when calling DefaultApi#asyncFunctionFunctionNamePost");
                e.printStackTrace();
            }

            System.out.println(response);

            List<FunctionListEntry> udfPutList = new ArrayList<FunctionListEntry>();
            List<FunctionListEntry> udfDeleteList = new ArrayList<FunctionListEntry>();
            List<FunctionListEntry> udfAccessList = new ArrayList<FunctionListEntry>();
            List<FunctionListEntry> udfCopyList = new ArrayList<FunctionListEntry>();


            for (int i = 0; i < response.size(); i++) {
                FunctionListEntry functionListEntry = response.get(i);
                Map<String, String> annotations = functionListEntry.getAnnotations();
                String invocation_event_types = annotations.get("invocation_event_types");

                if (invocation_event_types != null) {
                    Gson gson = new Gson();
                    JsonElement jsonElement = new JsonParser().parse(invocation_event_types);
                    JsonArray jsonArray = jsonElement.getAsJsonArray();
                    List<String> list = gson.fromJson(jsonArray, new TypeToken<List<String>>() {
                    }.getType());
                    for (int j = 0; j < list.size(); j++) {
                        String type = list.get(j);
                        if (type.compareToIgnoreCase("put") == 0)
                            udfPutList.add(functionListEntry);
                        else if (type.compareToIgnoreCase("delete") == 0)
                            udfDeleteList.add(functionListEntry);
                        else if (type.compareToIgnoreCase("get") == 0)
                            udfAccessList.add(functionListEntry);
                        else if (type.compareToIgnoreCase("copy") == 0)
                            udfCopyList.add(functionListEntry);
                    }
                }

            }

            // TODO: separate this into aws s3 (minio use this one), azure bob storage, gcs google cloud storage etc. they all have notification and own APIs
            List<String> jsonRecords = redisTemplate.opsForHash().values(action);

            List<S3EventNotificationRecord> records = new ArrayList<S3EventNotificationRecord>();
            for (String payload : jsonRecords) {
                S3EventNotification s3EventNotification = S3EventNotification.parseJson(payload);
                records.addAll(s3EventNotification.getRecords());
            }


            for (S3EventNotificationRecord currRecord : records) {

                // TODO: Need to add a mechanism to only process the "new" event, based on eventtime
                DateTime eventtime = currRecord.getEventTime();
                // eventName s3:ObjectAccessed:Get, :Put, and :DeletetmpStr.
                String eventName = currRecord.getEventName();

                if (eventName.toLowerCase().contains(invocation_event_put)) {

                    if (false == udfPutList.isEmpty()) {
                        for (int i = 0; i < udfPutList.size(); i++) {

                            String functionName = udfPutList.get(i).getName(); // String | Function name

                            S3EventNotification.S3Entity s3 = currRecord.getS3();
                            S3EventNotification.S3BucketEntity bucket = s3.getBucket();
                            // bucket name: imagesbucket
                            String bucketName = bucket.getName();


                            S3EventNotification.S3ObjectEntity s3obj = s3.getObject();
                            // objkey: sample.jpg
                            String objkey = s3obj.getKey();


                            JsonObject jsonObject = new JsonObject();
                            jsonObject.addProperty(bucketNamePath, bucketName);
                            jsonObject.addProperty(objectKeyPath, objkey);

                            List<String> list = Arrays.asList(LEN, WID);
                            String innerObjStr = new Gson().toJson(list);
                            JsonElement jsonElement = new JsonParser().parse(innerObjStr);

                            jsonObject.add(DEFAULT_INPUT_PARAMETERS_KEY, jsonElement);


                            String jsonStr = jsonObject.toString();
                            byte[] inputBytes = jsonStr.getBytes();

                            try {
                                apiInstance.functionFunctionNamePost(functionName, inputBytes);
                            } catch (ApiException e) {
                                System.err.println("Exception when calling DefaultApi#functionFunctionNamePost");
                                logger.error("UDF invoked failed");
                                e.printStackTrace();
                            }

                            logger.info("UDF invoked successfully");


                        }

                    }
                } else if (eventName.toLowerCase().contains(invocation_event_access)) {
                    //TODO
                } else if (eventName.toLowerCase().contains(invocation_event_delete)) {
                    //TODO
                } else if (eventName.toLowerCase().contains(invocation_event_copy)) {
                    //TODO
                }
            }

        }
    }
}
