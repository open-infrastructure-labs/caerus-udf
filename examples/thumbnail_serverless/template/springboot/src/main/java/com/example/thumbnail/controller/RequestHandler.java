package com.example.thumbnail.controller;

import com.example.thumbnail.service.ThumbnailGenerator;
import io.minio.*;
import io.minio.errors.MinioException;
import org.json.JSONArray;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import function.Handler;

import com.google.common.io.Files;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

@RestController
public class RequestHandler {
    Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    @Autowired
    MinioClient minioClient;

    static final String thumbnailsBucket = "thumbnailsbucket";
    public static String DEFAULT_INPUT_PARAMETER = "inputParameters";
    static final String tmpSourceDir = "/tmp/";

    @Autowired
    private Handler handler;

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity<String> handle(@RequestBody byte[] payload) {
        String response = handler.handle(payload);

        String payloadStr = new String(payload);
        JSONObject inputJson = new JSONObject(payloadStr);
        String bucket = null;
        String key = null;
        List<String> inputParameters = new ArrayList<>();

        if (inputJson.has("bucketName")) {
            bucket = inputJson.getString("bucketName");
        }

        if (inputJson.has("objectKey")) {
            key = inputJson.getString("objectKey");
        }

        if (inputJson.has("inputParameters")) {
            JSONArray arrayInput = inputJson.getJSONArray("inputParameters");
            for (int i = 0, l = arrayInput.length(); i < l; i++) {
                inputParameters.add(arrayInput.getString(i));
            }
        }

        if (bucket == null || key == null || inputParameters.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Paylaod should be a json to contain \"bucketName\", \"objectKey\", and \"inputParameters\". Invalid payload: " + payloadStr);
        } else {
            return handleRequest_helper(bucket, key, inputParameters);
        }
    }

    @GetMapping("/{bucketName}/{objectKey}")
    public ResponseEntity<String> handleRequest(@PathVariable("bucketName") String bucket, @PathVariable("objectKey") String key, @RequestParam List<String> inputParameters) {

        return handleRequest_helper(bucket, key, inputParameters);
    }

    private ResponseEntity<String> handleRequest_helper(String bucket, String key, List<String> inputParameters) {

        ArrayList<InputStream> inputStreams = new ArrayList<InputStream>();
        ArrayList<OutputStream> outputStreams = new ArrayList<OutputStream>();

        try {

            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (found) {
                logger.info("found Bucket: " + bucket);
            } else {
                // if there is no such bucket, it might not be error, the bucket might be deleted after the event, log warning and move on
                logger.warn("bucket not exist unexpectedly: " + bucket);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("can't find bucket: " + bucket);
            }


            // Read data from stream: there are two options here:
            //      1. read into memory: for super large file, it might cause OOM exception, and it will need to reset JVM -Xmx,
            //      2. read into a temp file, it doesn't have memory issue, but the down side is that we need to create/delete a temp file. Decide to use this.

            // Option #1: Read the input stream and into memory. the JVM size might need to be readjusted
            /*
            InputStream inputStreamFromStorage = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build());

            ByteArrayOutputStream data = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int bytesRead;
            try {
                while ((bytesRead = inputStreamFromStorage.read(buf, 0, buf.length)) != -1) {
                    logger.info("read something: " + String.valueOf(bytesRead));
                    data.write(buf, 0, bytesRead);
                }
            } catch (Exception e) {
                logger.warn("Minio bug: premature close of socket with the last packet, moving on....");
            }

            data.flush();
            try(OutputStream outputStream = new FileOutputStream("/tmp/sample.jpg")) {
                data.writeTo(outputStream);
            }
            /// Close the input stream.
            inputStreamFromStorage.close();
            InputStream inputStream = new ByteArrayInputStream(data.toByteArray());

             */

            // Option #2: read into a temp file
            String tmpFileName = tmpSourceDir + key;
            minioClient.downloadObject(DownloadObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .filename(tmpFileName)
                    .build());
            InputStream inputStream = new BufferedInputStream(new FileInputStream(tmpFileName));
            // add stream to list
            inputStreams.add(inputStream);

            String fNameNoExtension = Files.getNameWithoutExtension(key);
            String thumbnailObjName = fNameNoExtension + "_thumbnail.png";

            ByteArrayOutputStream os = new ByteArrayOutputStream();

            outputStreams.add(os);

            Map<String, String> parameters = new HashMap<String, String>();
            String inputParametersCommaSeparated = String.join(",", inputParameters);
            parameters.put ("inputParameters", inputParametersCommaSeparated);
            //parameters.put(WATERMARKFILENAME, watermarkFileName);
            ThumbnailGenerator.invoke(inputStreams, outputStreams, parameters);


            // upload to minio
            boolean foundTarget = minioClient.bucketExists(BucketExistsArgs.builder().bucket(thumbnailsBucket).build());
            if (foundTarget) {
                logger.info("found Bucket: " + thumbnailsBucket);
            } else {
                // if there is no such bucket, it might not be error, the bucket might be deleted after the event, log warning and move on
                logger.warn("bucket not exist unexpectedly: " + thumbnailsBucket);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("can't find bucket: " + thumbnailsBucket);
            }

            // thumbnail should be smaller enough by using byte array transfer, otherwise we can use NIO channels to redirect
            // https://howtodoinjava.com/java/io/outputstream-to-inputstream/

            //byte[] -> InputStream
            ByteArrayInputStream thumbnailInStream = new ByteArrayInputStream(os.toByteArray());

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(thumbnailsBucket)
                            .object(thumbnailObjName)
                            .stream(thumbnailInStream, thumbnailInStream.available(), -1)
                            .build());

            thumbnailInStream.close();

            logger.info("Successfully uploaded as object " + thumbnailObjName + " to Bucket: " + thumbnailsBucket);

            // clean up temp files
            File infile = new File(tmpFileName);
            if (infile.delete())
                logger.debug("file deleted: " + tmpFileName);
            else
                logger.error("failed to delete file: " + tmpFileName);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error:" + e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unexpected exception: " + e);
        }


        return ResponseEntity.ok("Udf invoked successfully.");
    }
}