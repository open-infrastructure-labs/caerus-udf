package org.openinfralabs.caerus.service.receiver;

import io.minio.messages.Bucket;
import org.apache.logging.log4j.message.Message;
import org.joda.time.DateTime;
import org.openinfralabs.caerus.service.config.RedisConfig;
import org.openinfralabs.caerus.service.model.Udf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

//TODO: aws s3 should be in separate class
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;

//TODO: mino related imports should be in separate class
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.GetObjectArgs;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

@Component
public class KeySpaceNotificationMessageListener implements MessageListener {

    final String udf_registry_service_uri = "http://localhost:8080/udf";
    final String udf_docker_uri = "http://localhost:8090/";
    //final String udf_docker_uri = "http://172.17.0.2:8090/";
    final String invocation_event_put = "put";
    final String invocation_event_access = "get";
    final String invocation_event_delete = "delete";
    final String bucketNamePath = "bucketName";
    final String objectKeyPath = "objectkey";

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

        String action = new String(message.getBody());
        String key = new String(message.getChannel());
        String bytesStr = new String(bytes);
        String messageToStr = message.toString();


        System.out.println("Recieved action = " + action + " " +
                " key info = " + key
                + " bytes = " + bytesStr
                + "message string = " + messageToStr);

        // get invocationEvents types from UdfRegistryService
        RestTemplate restTemplate = new RestTemplate();

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
            System.out.println("no udf found, so ignore the bucket notification.");
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
                ResponseEntity<String> responseEntity = udfRestTemplate.getForEntity(path, String.class, params);
                boolean isOK = responseEntity.getStatusCode().equals(HttpStatus.OK);
                if (isOK)
                    System.out.println("UDF invoked successfully");
                else
                    System.out.println("UDF invoked failed");


   /*
                //  1. separate this into different storage, e.g. s3: aws, minio, ceph; azure blob storage; google cloud storage etc.
                //  2. this should be done on minio node, might have more efficient reading
                MinioClient minioClient =
                        MinioClient.builder()
                                .endpoint("http://localhost:9000")
                                .credentials("minioadmin", "minioadmin")
                                .build();

                // Make 'asiatrip' bucket if not exist.
                try {

                    boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
                    if (found) {
                        System.out.println("found Bucket: " + bucketName);
                    } else {
                        // if there is no such bucket, it might not be error, the bucket might be deleted after the event, log warning and move on
                        System.out.println("Warning: bucket removed: " + bucketName);
                        return;
                    }

                    // get object given the bucket and object name
                    InputStream stream = minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(objkey)
                                    .build());
                    // Read data from stream
                    URL url = new URL(udf_docker_uri);
                    URLConnection connection = url.openConnection();
                    try (ObjectInputStream inputStream = new ObjectInputStream(connection.getInputStream())) {
                        Object o = inputStream.readObject();
                    }


                } catch (Exception e) {
                    System.out.println(e);
                    e.printStackTrace();
                    return;
                }*/

            }
        }

    }
}
