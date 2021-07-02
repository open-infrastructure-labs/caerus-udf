package com.example.thumbnail.controller;

import com.example.thumbnail.service.ThumbnailGenerator;

import com.example.thumbnail.utils.HdfsConnectionUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.JSONArray;
import org.json.XML;
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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.*;

import org.json.JSONObject;

@RestController
public class RequestHandler {
    Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    static final String thumbnailsBucket = "thumbnailsbucket";
    public static String DEFAULT_INPUT_PARAMETER = "inputParameters";
    static final String tmpSourceDir = "/tmp/";

    //private static String DEFAULT_HOST = "localhost";
    private static String DEFAULT_HOST = "192.168.1.11";
    private static int DEFAULT_WEBHDFS_PORT = 9870;
    private static String METHOD_TYPE_GET = "GET";
    private static String METHOD_TYPE_PUT = "PUT";

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


            // Read data from stream: there are two options here:
            //      1. read into memory: for super large file, it might cause OOM exception, and it will need to reset JVM -Xmx,
            //      2. read into a temp file, it doesn't have memory issue, but the down side is that we need to create/delete a temp file. Decide to use this.


            // Option #2: read into a temp file
            String tmpFileName = tmpSourceDir + key;


            // Step 1 ----- Send request to name node and Get the "Location" string (for data node)
            String url_ops = "?op=OPEN&noredirect=true";
            String hdfsFileName = bucket + "/" + key;


            String hdfs_host = Optional.ofNullable(System.getenv("HDFS_HOST")).orElse(DEFAULT_HOST);
            String hdfs_name_node_port_str = Optional.ofNullable(System.getenv("WEBHDFS_NAME_NODE_PORT")).orElse(String.valueOf(DEFAULT_WEBHDFS_PORT));

            String webHdfsUrlSuffix = "/webhdfs/v1/" + hdfsFileName + url_ops;
            HdfsConnectionUtils hdfsClientConnUtil = new HdfsConnectionUtils(hdfs_host, hdfs_name_node_port_str);

            String responseJSON = "";
            try {
                HttpURLConnection conn = hdfsClientConnUtil.getConnection(webHdfsUrlSuffix, METHOD_TYPE_GET);

                conn.connect();
                responseJSON = hdfsClientConnUtil.responseMessage(conn, false);
                conn.disconnect();

            } catch (IOException e) {
                logger.error("Caught an IO Exception, which " + "means the client encountered " + "an internal error while trying to "
                        + "communicate with hdfs, " + "such as not being able to access the network.");
                logger.error("Error Message: " + e.getMessage());
                logger.error("Exception", e);
                e.printStackTrace();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("can't connect to HDFS");
            }

            // Step 2 ----- get redirect data node location
            JsonObject jsonObject = new JsonParser().parseString(responseJSON).getAsJsonObject();
            String originalLocation = jsonObject.get("Location").getAsString();

            URL originalURL = null;
            try {
                originalURL = new URL(originalLocation);
            } catch (MalformedURLException e) {
                logger.error("Exception", e);
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Bad URL: " + originalLocation);
            }

            String dataNodeName = originalURL.getHost();

            //TODO: change data node from "localhost" to VM IP for now, since the function in container can't access to "localhost". Official set up should not have this issue.
            dataNodeName = hdfs_host;
            Integer dataNodePort = originalURL.getPort();

            // Here we replace host and port with NDP service host and port
            String file = originalURL.getFile();

            HdfsConnectionUtils secondRequestHdfsClientConnUtil = new HdfsConnectionUtils(dataNodeName, String.valueOf(dataNodePort));

            String secondResponseJSON = "";
            try {
                HttpURLConnection conn = secondRequestHdfsClientConnUtil.getConnection(file, METHOD_TYPE_GET);
                conn.setDoInput(true);
                conn.connect();

                BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
                BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(tmpFileName));

                //logger.info("Writing the file to local filesystem: " + localFileName);
                byte dataBuffer[] = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }

                secondResponseJSON = hdfsClientConnUtil.responseMessage(conn, false);
                System.out.println("ResponseJson in 181: " + responseJSON);
                conn.disconnect();

            } catch (IOException e) {
                System.out.println("Caught an IO Exception, which " + "means the client encountered " + "an internal error while trying to "
                        + "communicate with hdfs, " + "such as not being able to access the network.");
                System.out.println("Error Message: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("can't connect to HDFS");
            }

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


            // upload to hdfs
            // Step 1.
            //       Submit a HTTP PUT request without automatically following redirects and without sending the file data.
            //       curl -i -X PUT "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=CREATE
            //                    [&overwrite=<true|false>][&blocksize=<LONG>][&replication=<SHORT>]
            //                    [&permission=<OCTAL>][&buffersize=<INT>]"
            // Step 2.
            //       Submit another HTTP PUT request using the URL in the Location header with the file data to be written.
            //       curl -i -X PUT -T <LOCAL_FILE> "http://<DATANODE>:<PORT>/webhdfs/v1/<PATH>?op=CREATE..."
            //

            // Step 1 ----- Send request to name node and Get the "Location" string (for data node)
            url_ops = "?op=CREATE&overwrite=true&noredirect=true";

            webHdfsUrlSuffix = "/webhdfs/v1/" + thumbnailsBucket + "/" + thumbnailObjName + url_ops;

            hdfsClientConnUtil = new HdfsConnectionUtils(hdfs_host, hdfs_name_node_port_str);

            try {
                HttpURLConnection conn = hdfsClientConnUtil.getConnection(webHdfsUrlSuffix, METHOD_TYPE_PUT);

                conn.connect();
                responseJSON = hdfsClientConnUtil.responseMessage(conn, false);
                System.out.println("ResponseJson in 232: " + responseJSON);
                conn.disconnect();

            } catch (IOException e) {
                logger.error("Caught an IO Exception, which " + "means the client encountered " + "an internal error while trying to "
                        + "communicate with hdfs, " + "such as not being able to access the network.");
                logger.error("Error Message: " + e.getMessage());

                logger.error("Exception.", e);
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("can't connect to HDFS");
            }

            // Step 2 ----- modify the "Location" and redirect HTTP to NDP service that is running on data node or a data node adjacent
            jsonObject = new JsonParser().parseString(responseJSON).getAsJsonObject();
            originalLocation = jsonObject.get("Location").getAsString();


            try {
                originalURL = new URL(originalLocation);
            } catch (MalformedURLException e) {
                logger.error("Exception.", e);
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("bad URL: " + originalLocation);
            }

            dataNodeName = originalURL.getHost();
            //TODO: change data node from "localhost" to VM IP for now, since the function in container can't access to "localhost". Official set up should not have this issue.
            dataNodeName = hdfs_host;
            dataNodePort = originalURL.getPort();

            // Here we replace host and port with NDP service host and port
            file = originalURL.getFile();

            // the host name should be NDP service host, it uses "localhost" here
            secondRequestHdfsClientConnUtil = new HdfsConnectionUtils(dataNodeName, String.valueOf(dataNodePort));

            // for test only: send to data node directly
            //HdfsConnectionUtils secondRequestHdfsClientConnUtil = new HdfsConnectionUtils(DATA_NODE1_DEFAULT_IP, String.valueOf(dataNodePort));
            try {
                HttpURLConnection conn = secondRequestHdfsClientConnUtil.getConnection(file, METHOD_TYPE_PUT);
                conn.connect();

                // Create Output HDFS stream to write the local file content
                ByteArrayInputStream thumbnailInStream = new ByteArrayInputStream(os.toByteArray());
                BufferedOutputStream fileOutputStream = new BufferedOutputStream(conn.getOutputStream());

                //logger.info("Writing the file to HDFS: " + hdfsFileName);

                int bytesRead;
                byte dataBuffer[] = new byte[1024];
                // read byte by byte until end of stream
                while ((bytesRead = thumbnailInStream.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }

                secondResponseJSON = hdfsClientConnUtil.responseMessage(conn, false);
                conn.disconnect();

            } catch (IOException e) {
                logger.error("Caught an IO Exception, which " + "means the client encountered " + "an internal error while trying to "
                        + "communicate with hdfs, " + "such as not being able to access the network.");
                logger.error("Error Message: " + e.getMessage());
                logger.error("Exception.", e);
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("can't connect to HDFS");
            }

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