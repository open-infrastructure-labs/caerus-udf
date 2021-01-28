package org.openinfralabs.caerus.clientService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.httpclient.ChunkedInputStream;

import org.openinfralabs.caerus.clientService.service.StorageAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
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

    // This is to handle putObject (upload) request sent from aws sdk
    @PutMapping("{bucket}/{filename}")
    public ResponseEntity<String> awsUploadFileM(@PathVariable String bucket, @PathVariable String filename,
                                                 @RequestHeader Map<String, String> headers,
                                                 @RequestBody MultipartFile file,
                                                 HttpServletRequest request) throws IOException {

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

        InputStream requestInputStream =  request.getInputStream();
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
        String function_name = headers.get((AWS_METADATA_PREFIX+FUNCTION_NAME_KEY).toLowerCase());
        String comma_separated_inputParameters = headers.get((AWS_METADATA_PREFIX+FUNCTION_INPUTPARAMETERS_KEY).toLowerCase());

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

        byte[] data = adapter.getFile(bucket, key, metadataObj);
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
