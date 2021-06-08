package org.openinfralabs.caerus.ndpService.service;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.openinfralabs.caerus.ndpService.model.UdfInvocationMetadata;
import org.openinfralabs.caerus.ndpService.utils.HdfsConnectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Service
//@Qualifier("storageAdapterHdfsImpl")
public class StorageAdapterHdfsImpl implements StorageAdapter {

    // adding conditional compiling flag
    public static final boolean SERVERLESS = true;

    public static String userAgent = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.66 Safari/537.36";
    public static final String URL_ROOT = "http://localhost:9870/webhdfs/v1/user/root/";
    public static final String OP_LISTSTATUS_ADDON = "?op=LISTSTATUS";
    public static final String DEF_CHATSET = "UTF-8";
    public static final int DEF_CONN_TIMEOUT = 3000;
    public static final int DEF_READ_TIMEOUT = 3000;

    private static String  CAERUS_REDIRECT_URL_NAME = "CaerusRediectURL";

    Logger logger = LoggerFactory.getLogger(StorageAdapterHdfsImpl.class);

    final String udfService_uri = "http://localhost:8002/";
    final String invocation_event_put = "put";
    final String invocation_event_access = "get";
    final String invocation_event_delete = "delete";
    final String bucketNamePath = "bucketName";
    final String objectKeyPath = "objectKey";
    final String DEFAULT_UDF_KEY = "udfKey";
    final String DEFAULT_INPUT_PARAMETERS_KEY = "inputParameters";




    @Override
    public void uploadFile(String bucket, String filename, InputStream inputStream, UdfInvocationMetadata metadata, String optionalParametersJson) {
        // Step 1: upload object to HDFS
        // the host name should be data node name that was passed in
        JsonObject jsonObject = new JsonParser().parse(optionalParametersJson).getAsJsonObject();
        String dataNodeURL = jsonObject.get(CAERUS_REDIRECT_URL_NAME).getAsString();

        URL newURL;
        try {
            newURL = new URL(dataNodeURL);

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }

        if (newURL == null) {
            logger.error("Internal error: redirect URL is empty");
            return;
        }

        String responseJSON = "";
        try {
            String dataNode = newURL.getHost();
            Integer dataNodePort = newURL.getPort();
            // get url suffix
            String urlSuffix = newURL.getFile();

            HdfsConnectionUtils hdfsClientConnUtil = new HdfsConnectionUtils(dataNode, String.valueOf(dataNodePort));

            HttpURLConnection conn = hdfsClientConnUtil.getConnection(urlSuffix, "PUT");

            conn.connect();

            // Create Output HDFS stream to write inputStream (from client)

            BufferedOutputStream fileOutputStream = new BufferedOutputStream(conn.getOutputStream());

            //logger.info("Writing the file to HDFS: " + hdfsFileName);

            int bytesRead;
            byte dataBuffer[] = new byte[1024];
            // read byte by byte until end of stream
            while ((bytesRead = inputStream.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }

            responseJSON = hdfsClientConnUtil.responseMessage(conn, false);
            conn.disconnect();

        } catch (IOException e) {
            System.out.println("Caught an IO Exception, which " + "means the client encountered " + "an internal error while trying to "
                    + "communicate with hdfs, " + "such as not being able to access the network.");
            System.out.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Step 2: invoke UDF on object
        // TODO: add udf invocation
    }

    @Override
    public byte[] getFile(String bucket, String key, UdfInvocationMetadata metadata, Map<String, String> headersMap) {

        return null;
    }


    @Override
    public boolean deleteFile(String bucket, String key, UdfInvocationMetadata metadata) {

        return true;
    }


    @Override
    public boolean copyObject(String fromBucket, String toBucket, String sourceObjKey, String targetObjKey, UdfInvocationMetadata metadata, StringBuilder sb) {
        return true;
    }

    @Override
    public boolean listObjects(String bucket, StringBuilder sb) {

        // TODO: Change to use new utility calls

        try {
            BufferedReader reader = null;
            String rs = null;
            HttpURLConnection conn = null;

            String urlStr = URL_ROOT + bucket + OP_LISTSTATUS_ADDON;

            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-agent", userAgent);
            conn.setUseCaches(false);
            conn.setConnectTimeout(DEF_CONN_TIMEOUT);
            conn.setReadTimeout(DEF_READ_TIMEOUT);
            conn.setInstanceFollowRedirects(false);
            conn.connect();

            InputStream is = conn.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is, DEF_CHATSET));
            String strRead = null;
            while ((strRead = reader.readLine()) != null) {
                sb.append(strRead);
            }
            rs = sb.toString();
            System.out.println(rs);

            int statusCode = conn.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                logger.error("Error occurred, return code is: " + String.valueOf(statusCode));
                return false;
            }


        } catch (Exception e) {
            logger.error("Error occurred: " + e);
        }
        return true;
    }
}
