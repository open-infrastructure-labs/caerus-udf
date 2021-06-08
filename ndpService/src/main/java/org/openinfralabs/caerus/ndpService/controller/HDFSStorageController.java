package org.openinfralabs.caerus.ndpService.controller;

import com.google.gson.JsonObject;
import org.openinfralabs.caerus.ndpService.model.UdfInvocationMetadata;
import org.openinfralabs.caerus.ndpService.service.StorageAdapterHdfsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.*;


@RestController
@RequestMapping("/webhdfs/v1")
public class HDFSStorageController {

    private static String FUNCTION_NAME_KEY = "function_name";
    private static String FUNCTION_INPUTPARAMETERS_KEY = "function_inputParameters";
    private static String CAERUS_UDF_PARAMETERS_NAME = "CaerusUDF";
    private static String CAERUS_REDIRECT_URL_NAME = "CaerusRediectURL";

    @Autowired
    //@Qualifier("storageAdapterHdfsImpl")
    //@Qualifier("storageAdapterMinioImpl")
    StorageAdapterHdfsImpl adapter;

    // This is to handle putObject (upload) or copyObject request sent from aws sdk
    @PutMapping("{bucket}/{filename}")
    public ResponseEntity<String> awsUploadFileM(@PathVariable String bucket, @PathVariable String filename,
                                                 @RequestHeader Map<String, String> headers,
                                                 @RequestBody MultipartFile file,
                                                 //@RequestBody File file2,
                                                 HttpServletRequest request) throws IOException {

        /*
        boolean useChunking = false;
        String protocol = request.getProtocol();
        if (protocol.compareToIgnoreCase("HTTP/1.1") == 0) {
            useChunking = true;
        }*/

        InputStream requestInputStream = request.getInputStream();
        // we will probably have to cache because of the chunking
        byte[] cachedBody = StreamUtils.copyToByteArray(requestInputStream);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(cachedBody);

        String udfXML = "";
        UdfInvocationMetadata metadataObj = new UdfInvocationMetadata();

        if (headers.containsKey(CAERUS_UDF_PARAMETERS_NAME.toLowerCase(Locale.ROOT))) {
            udfXML = headers.get(CAERUS_UDF_PARAMETERS_NAME.toLowerCase(Locale.ROOT));
            System.out.println("UDF XML: " + udfXML);
            //TODO: construct UdfInvocationMetadata from XML for serverless invocation
        }

        String redirectURL = "";
        String optionalParametersJson = "";
        if (headers.containsKey(CAERUS_REDIRECT_URL_NAME.toLowerCase(Locale.ROOT))) {
            redirectURL = headers.get(CAERUS_REDIRECT_URL_NAME.toLowerCase(Locale.ROOT));
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty(CAERUS_REDIRECT_URL_NAME, redirectURL);
            optionalParametersJson = jsonObject.toString();
        }

        adapter.uploadFile(bucket, filename, inputStream, metadataObj, optionalParametersJson);
        return new ResponseEntity<String>(filename + " created.", HttpStatus.CREATED);
    }

}
