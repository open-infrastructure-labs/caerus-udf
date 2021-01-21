package org.openinfralabs.caerus.clientService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.swagger.annotations.ApiOperation;
import org.openinfralabs.caerus.clientService.service.StorageAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.openinfralabs.caerus.clientService.model.UdfInvocationMetadata;

import javax.persistence.Column;

@RestController
public class StorageController {

    @Autowired
    StorageAdapter adapter;

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
}
