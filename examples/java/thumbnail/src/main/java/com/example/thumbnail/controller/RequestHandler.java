package com.example.thumbnail.controller;

import com.example.thumbnail.service.ThumbnailGenerator;
import io.minio.*;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.common.io.Files;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
public class RequestHandler {
    static final String thumbnailsBucket = "thumbnailsbucket";
    //static final String tmpThumbnailDir = "/tmp/thumbnails/";
    static final String tmpThumbnailDir = "/tmp/";

    @GetMapping("/{bucketName}/{objectKey}")
    public ResponseEntity<String> handleRequest(@PathVariable("bucketName") String bucket, @PathVariable("objectKey") String key) {
        // public ResponseEntity<StreamingResponseBody> handleRequest(InputStream is) {

        // StreamingResponseBody stream = new StreamingResponseBody() {
        //     @Override
        //     public void writeTo(OutputStream outputStream) throws IOException {

        ArrayList<InputStream> inputStreams = new ArrayList<InputStream>();
        ArrayList<OutputStream> outputStreams = new ArrayList<OutputStream>();

        MinioClient minioClient =
                MinioClient.builder()
                        .endpoint("http://localhost:9000")
                        .credentials("minioadmin", "minioadmin")
                        .build();

        try {

            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (found) {
                System.out.println("found Bucket: " + bucket);
            } else {
                // if there is no such bucket, it might not be error, the bucket might be deleted after the event, log warning and move on
                System.out.println("Warning: bucket removed: " + bucket);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("can't find bucket: " + bucket);
            }

            // TODO: Need to find a way to use stream, in stead of tmp file
            // get object given the bucket and object name
            /*InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build());*/
            // Read data from stream

            String tmpFileName = tmpThumbnailDir+key;
            minioClient.downloadObject(DownloadObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .filename(tmpFileName)
                    .build());

            InputStream inputStream = new BufferedInputStream(new FileInputStream(tmpFileName));

            inputStreams.add(inputStream);
            // TODO: Minio doesn't seem to support steraming output directly, can only upload a file
            //       so, store a temp file here, then upload to minio

            //                    String outputFileName = "/tmp/sample_thumbnail.png";
            String fNameNoExtension = Files.getNameWithoutExtension(key);
            String thumbnailObjName = fNameNoExtension + "_thumbnail.png";
            String outputFileName = tmpThumbnailDir + bucket + "_" + thumbnailObjName;
            OutputStream os = new FileOutputStream(outputFileName);
            outputStreams.add(os);

            Map<String, String> parameters = new HashMap<String, String>();
            //parameters.put(WATERMARKFILENAME, watermarkFileName);
            ThumbnailGenerator.invoke(inputStreams, outputStreams, parameters);


            // upload to minio
            boolean foundTarget = minioClient.bucketExists(BucketExistsArgs.builder().bucket(thumbnailsBucket).build());
            if (foundTarget) {
                System.out.println("found Bucket: " + thumbnailsBucket);
            } else {
                // if there is no such bucket, it might not be error, the bucket might be deleted after the event, log warning and move on
                System.out.println("Warning: bucket removed: " + thumbnailsBucket);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("can't find bucket: " + thumbnailsBucket);
            }

            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(thumbnailsBucket)
                            .object(thumbnailObjName)
                            .filename(outputFileName)
                            .build());
            System.out.println("Successfully uploaded as object " + thumbnailObjName + " to Bucket: " + thumbnailsBucket);

            // clean up tmp files
            File outfile = new File(outputFileName);
            if (outfile.delete())
                System.out.println("file deleted: " + outputFileName);
            else
                System.out.println("failed to delete file: " + outputFileName);

            File infile = new File(tmpFileName);
            if (infile.delete())
                System.out.println("file deleted: " + tmpFileName);
            else
                System.out.println("failed to delete file: " + tmpFileName);



        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unexpected exception: " + e);
        }


        return ResponseEntity.ok("Udf invoked successfully.");
    }
}