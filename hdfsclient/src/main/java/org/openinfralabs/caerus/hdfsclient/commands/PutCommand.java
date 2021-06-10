package org.openinfralabs.caerus.hdfsclient.commands;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.XML;
import org.openinfralabs.caerus.hdfsclient.utils.HdfsConnectionUtils;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

/* debug input parameters:
    put -s "/home/ubuntu/images/new/sample.jpg" -t input/sample.jpg -u caerus-faas-spring-thumbnail -i "400, 600"
 */

@Command(
        name = "put", description = "Upload an object/file to HDFS while provide option to invoke UDF on the object/file."
)

@Component
public class PutCommand implements Runnable {

    private static String DEFAULT_HOST = "localhost";
    private static Integer DEFAULT_WEBHDFS_PORT = 9870;
    private static Integer DEFAULT_NDP_UDF_PORT = 8000;
    private static String METHOD_TYPE = "PUT";

    private static String CAERUS_UDF_PARAMETERS_NAME = "CaerusUDF";
    private static String CAERUS_REDIRECT_URL_NAME = "CaerusRediectURL";

    private static String FUNCTION_NAME_KEY = "function_name";
    //private static String FUNCTION_NAME_VALUE = "caerus-faas-spring-thumbnail";
    private static String FUNCTION_INPUTPARAMETERS_KEY = "function_inputParameters";
    //private static String FUNCTION_INPUTPARAMETERS_VALUE = "400, 600";

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this message")
    private boolean helpRequested = false;

    @Option(names = {"-s", "--sourceFileName"}, description = "Source local full path of the file will be uploaded", required = true)
    private String localFileName;

    @Option(names = {"-t", "--hdfsFileName"}, description = "Target HDFS full path of the file", required = true)
    private String hdfsFileName;

    @Option(names = {"-o", "--optionalOperationParameters"}, description = "Optional HDFS upload parameters", required = false)
    private String optionalOperationParameters;

    @Option(names = {"-u", "--udfName"}, description = "UDF function name", required = false)
    private String udfName;

    @Option(names = {"-i", "--udfInputParameters"}, description = "UDF input metadata", required = false)
    private String udfInputParameters;


    @Override
    public void run() {
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
        String url_ops = "?op=CREATE&overwrite=true&noredirect=true";

        if (optionalOperationParameters != null) {
            url_ops = optionalOperationParameters;
        }

        String webHdfsUrlSuffix = "/webhdfs/v1/" + hdfsFileName + url_ops;
        HdfsConnectionUtils hdfsClientConnUtil = new HdfsConnectionUtils(DEFAULT_HOST, String.valueOf(DEFAULT_WEBHDFS_PORT));

        String responseJSON = "";
        try {
            HttpURLConnection conn = hdfsClientConnUtil.getConnection(webHdfsUrlSuffix, METHOD_TYPE);

            conn.connect();
            responseJSON = hdfsClientConnUtil.responseMessage(conn, false);
            conn.disconnect();

        } catch (IOException e) {
            System.out.println("Caught an IO Exception, which " + "means the client encountered " + "an internal error while trying to "
                    + "communicate with hdfs, " + "such as not being able to access the network.");
            System.out.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // Step 2 ----- modify the "Location" and redirect HTTP to NDP service that is running on data node or a data node adjacent
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonParser().parseString(responseJSON).getAsJsonObject();
        String originalLocation = jsonObject.get("Location").getAsString();

        URL originalURL = null;
        URL newURL = null;
        try {
            originalURL = new URL(originalLocation);
            newURL = new URL(originalURL.getProtocol(), DEFAULT_HOST, DEFAULT_NDP_UDF_PORT, originalURL.getFile());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        String dataNodeName = originalURL.getHost();
        Integer dataNodePort = originalURL.getPort();

        // Here we replace host and port with NDP service host and port
        String file = newURL.getFile();

        // the host name should be NDP service host, it uses "localhost" here
        HdfsConnectionUtils secondRequestHdfsClientConnUtil = new HdfsConnectionUtils(DEFAULT_HOST, String.valueOf(DEFAULT_NDP_UDF_PORT));

        // for test only: send to data node directly
        //HdfsConnectionUtils secondRequestHdfsClientConnUtil = new HdfsConnectionUtils(DATA_NODE1_DEFAULT_IP, String.valueOf(dataNodePort));

        String secondResponseJSON = "";
        try {
            HttpURLConnection conn = secondRequestHdfsClientConnUtil.getConnection(file, METHOD_TYPE);

            // set custom headers

            //String udfParametersXML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> <caerusudf id =\"1\"> <function_name>caerus-faas-spring-thumbnail</function_name> <function_inputParameters>400, 600 </function_inputParameters> </caerusudf>";

            // only invoke UDF if the udf name is supplied, otherwise just put object
            if (udfName != null) {
                JsonObject gsonObject = new JsonObject();
                gsonObject.addProperty(FUNCTION_NAME_KEY, udfName);
                gsonObject.addProperty(FUNCTION_INPUTPARAMETERS_KEY, udfInputParameters);

                // change gson to json
                org.json.JSONObject udfjson = new org.json.JSONObject(gsonObject.toString());

                String udfParametersXML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> <caerusudf id =\"1\"> " + XML.toString(udfjson) + "</caerusudf>";
                conn.setRequestProperty(CAERUS_UDF_PARAMETERS_NAME, udfParametersXML);
            }

            conn.setRequestProperty(CAERUS_REDIRECT_URL_NAME, originalLocation);
            conn.connect();

            // Create Output HDFS stream to write the local file content
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(localFileName));
            BufferedOutputStream fileOutputStream = new BufferedOutputStream(conn.getOutputStream());

            //logger.info("Writing the file to HDFS: " + hdfsFileName);

            int bytesRead;
            byte dataBuffer[] = new byte[1024];
            // read byte by byte until end of stream
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }

            secondResponseJSON = hdfsClientConnUtil.responseMessage(conn, false);
            conn.disconnect();

        } catch (IOException e) {
            System.out.println("Caught an IO Exception, which " + "means the client encountered " + "an internal error while trying to "
                    + "communicate with hdfs, " + "such as not being able to access the network.");
            System.out.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
