package org.openinfralabs.caerus.clientService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.httpclient.ChunkedInputStream;

import org.openinfralabs.caerus.clientService.service.StorageAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

import org.openinfralabs.caerus.clientService.model.UdfInvocationMetadata;
import javax.servlet.http.HttpServletRequest;


@RestController
public class StorageController {

    private static String FUNCTION_NAME_KEY = "function_name";
    private static String FUNCTION_INPUTPARAMETERS_KEY = "function_inputParameters";
    private static String AWS_METADATA_PREFIX = "x-amz-meta-";


    @Autowired
    StorageAdapter adapter;


    // This is to downlaod a object from a S3 bucket
    @GetMapping("{bucket}/{objKey}")
    public ResponseEntity<byte[]
            > getObject(@PathVariable String bucket, @PathVariable String objKey,
                                              @RequestHeader Map<String, String> headers,
                                              HttpServletRequest request) throws IOException {

        // metadata is for future use
        UdfInvocationMetadata metadataObj = new UdfInvocationMetadata();
        Map<String, String> headersMap = new HashMap<String, String>();
        byte[] data = adapter.getFile(bucket, objKey, metadataObj, headersMap);

        // build headers
        HttpHeaders responseHeaders = new HttpHeaders();
        for (Map.Entry<String, String> entry : headersMap.entrySet()) {
            responseHeaders.add(entry.getKey(), entry.getValue());
        }

        if (data != null){
            return new ResponseEntity<byte[]>(data, responseHeaders, HttpStatus.OK);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Delete an object from a S3 bucket
    @DeleteMapping("{bucket}/{objKey}")
    public ResponseEntity<String> deleteObjects(@PathVariable String bucket, @PathVariable String objKey,
                                                @RequestHeader Map<String, String> headers,
                                              HttpServletRequest request) throws IOException {
        UdfInvocationMetadata metadataObj = new UdfInvocationMetadata();
        boolean success = adapter.deleteFile(bucket, objKey, metadataObj);
        if (success)
            return ResponseEntity.ok("File deleted successfully.");
        else
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    // This is to list objects of a S3 bucket
    @GetMapping("{bucket}/")
    public ResponseEntity<String> listObjects(@PathVariable String bucket, @RequestHeader Map<String, String> headers,
                                              HttpServletRequest request) throws IOException {
        StringBuilder contentXMLBuilder = new StringBuilder();
        boolean success = adapter.listObjects(bucket, contentXMLBuilder);

        if (success) {
            return new ResponseEntity<String>(contentXMLBuilder.toString(), HttpStatus.OK);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // This is to handle putObject (upload) or copyObject request sent from aws sdk
    @PutMapping("{bucket}/{filename}")
    public ResponseEntity<String> awsUploadFileM(@PathVariable String bucket, @PathVariable String filename,
                                                 @RequestHeader Map<String, String> headers,
                                                 @RequestBody MultipartFile file,
                                                 HttpServletRequest request) throws IOException {

            if (headers.containsKey("x-amz-copy-source")) {

                String bucketAndKey = headers.get("x-amz-copy-source");
                // format likes this: \bucket\objkey?nnnnnn
                // where objkey might have format like: \photos\2020\feb\image1.jpg
                String [] temp = bucketAndKey.split("\\/");
                if (temp.length < 3) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }

                String srcBucket = temp[1];
                String srcObjKey;
                String joinedSrcKeyCombo = temp[2];

                for (int i = 3; i < temp.length; i++) {
                    joinedSrcKeyCombo = String.join(temp[i]);
                }

                srcObjKey = joinedSrcKeyCombo;
                if (joinedSrcKeyCombo.contains("?")) {
                    String [] keys = joinedSrcKeyCombo.split("\\?");
                    srcObjKey = keys[0];
                }

                StringBuilder contentXMLBuilder = new StringBuilder();
                UdfInvocationMetadata metadata = new UdfInvocationMetadata();
                boolean success = adapter.copyObject(srcBucket, bucket, srcObjKey, filename, metadata, contentXMLBuilder);

                if (success) {
                    return new ResponseEntity<String>(contentXMLBuilder.toString(), HttpStatus.CREATED);
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            } else {
                // this is putObject request

                //doesn't work?
                // InputStream inputStream =  new BufferedInputStream(request.getInputStream());
                // https://www.baeldung.com/spring-reading-httpservletrequest-multiple-times


                // NOTE: AWS SDK seems not follow the HTTP standard: when http 1.1 chunking is used, it should either have transfer-encoding
                //  header and should not have content-length header, so we can only use http 1.1 protocol to decide if the chunking encoding is used
                boolean useChunking = false;
                String protocol = request.getProtocol();
                if (protocol.compareToIgnoreCase("HTTP/1.1") == 0) {
                    useChunking = true;
                }

                InputStream requestInputStream = request.getInputStream();
                // we will probably have to cache because of the chunking
                byte[] cachedBody = StreamUtils.copyToByteArray(requestInputStream);

                ByteArrayInputStream inputStream;

                if (useChunking) {

                    byte[] unchunkedBody = unchunkdata(cachedBody);

                    inputStream = new ByteArrayInputStream(unchunkedBody);
                } else {
                    inputStream = new ByteArrayInputStream(cachedBody);
                }

                // get metadata info from aws request headers
                String function_name = headers.get((AWS_METADATA_PREFIX + FUNCTION_NAME_KEY).toLowerCase());
                String comma_separated_inputParameters = headers.get((AWS_METADATA_PREFIX + FUNCTION_INPUTPARAMETERS_KEY).toLowerCase());

                if (function_name == null || function_name.isEmpty() ||
                        comma_separated_inputParameters == null || comma_separated_inputParameters.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }


                UdfInvocationMetadata metadataObj = new UdfInvocationMetadata();
                metadataObj.setName(function_name);
                String[] elements = comma_separated_inputParameters.split(", ");
                List<String> inputParameters = Arrays.asList(elements);
                metadataObj.setInputParameters(inputParameters);

                adapter.uploadFile(bucket, filename, inputStream, metadataObj);

                return ResponseEntity.ok("File uploaded and function invoked successfully.");
            }

    }


    @PostMapping(path = "/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ApiOperation(value = "Upload object to object store, while invoke specified UDF.",
                notes = "Send via an input form with followings:" +
                    "   1. bucket name in 'bucket', " +
                    "   2. a MultipartFile in 'uploadFile'. " +
                    "   3. an optional udf metadata in json, it includes followings:" +
                        "            private String name;\n" +
                        "            (optional) private List<String> inputParameters;\n" +
                        "            (optional) private List<String> extraResources;" +
                        "or curl command: " +
                        "curl --location --request POST 'localhost:8000/upload' \\\n" +
                        "--form 'bucket=\"b100\"' \\\n" +
                        "--form 'uploadFile=@\"/home/ubuntu/images/new/sample0.jpg\"' \\\n" +
                        "--form 'metadata=\"{   \\\"name\\\": \\\"caerus-faas-spring-thumbnail\\\",\n" +
                        "    \\\"inputParameters\\\": [\\\"400\\\", \\\"600\\\"]\n" +
                        "}\";type=application/json'" )
    public Map<String, String> uploadFile(@RequestParam("bucket") String bucket, @RequestParam("uploadFile") MultipartFile file, @RequestParam("metadata") Optional<String> metadata ) throws IOException {

        String filename = file.getOriginalFilename();
        InputStream inputStream =  new BufferedInputStream(file.getInputStream());

        UdfInvocationMetadata metadataObj = getMetadataObject(metadata);

        adapter.uploadFile(bucket, filename, inputStream, metadataObj);


        Map<String, String> result = new HashMap<>();
        result.put("key", filename);
        return result;
    }

    @ApiOperation(value = "Download object from object store, while invoke specified UDF.",
            notes = "Send via an input form with followings:" +
                    "   1. bucket name in 'bucket', " +
                    "   2. an object key in 'file'. " +
                    "   3. an optional udf metadata in json, it includes followings:" +
                    "            private String name;\n" +
                    "            (optional) private List<String> inputParameters;\n" +
                    "            (optional) private List<String> extraResources;")
    @GetMapping(path = "/download")
    public ResponseEntity<ByteArrayResource> downloadFile(@RequestParam("bucket") String bucket, @RequestParam(value = "file") String key, @RequestParam("metadata") Optional<String> metadata) throws IOException {

        UdfInvocationMetadata metadataObj = getMetadataObject(metadata);
        Map<String, String> headersMap = new HashMap<String, String>();
        byte[] data = adapter.getFile(bucket, key, metadataObj, headersMap);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity
                .ok()
                .contentLength(data.length)
                .header("Content-type", "application/octet-stream")
                .header("Content-disposition", "attachment; filename=\"" + key + "\"")
                .body(resource);

    }

    private UdfInvocationMetadata getMetadataObject (Optional<String> metadata) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        UdfInvocationMetadata metadataObj = null;


        if (metadata.isPresent()) {
            try {
                metadataObj = mapper.readValue(metadata.get(), UdfInvocationMetadata.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return metadataObj;
    }

    private static byte[] unchunkdata(byte[] contentBytes) throws IOException {
        byte[] unchunkedData = null;
        byte[] buffer = new byte[1024];
        ByteArrayInputStream bis = new ByteArrayInputStream(contentBytes);
        ChunkedInputStream cis = new ChunkedInputStream(bis);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        int read = -1;
        while ((read = cis.read(buffer)) != -1) {
            bos.write(buffer, 0, read);
        }
        unchunkedData = bos.toByteArray();
        bos.close();

        return unchunkedData;
    }
}
