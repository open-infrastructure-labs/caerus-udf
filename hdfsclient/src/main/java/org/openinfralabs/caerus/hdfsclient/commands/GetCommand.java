package org.openinfralabs.caerus.hdfsclient.commands;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.XML;
import org.openinfralabs.caerus.hdfsclient.utils.HdfsConnectionUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/* debug input parameters:
get -b testbucket -k sample0.jpg
 */

@CommandLine.Command(
        name = "get", description = "Retrieve/Download object from HDFS."
)
@Component
public class GetCommand implements Runnable{

    private static String DEFAULT_HOST = "localhost";
    private static Integer DEFAULT_WEBHDFS_PORT = 9870;
    private static String METHOD_TYPE = "GET";


    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display this message")
    private boolean helpRequested = false;

    @CommandLine.Option(names = {"-s", "--hdfsFileName"}, description = "HDFS full path of the file (source) to be downloaded", required = true)
    private String hdfsFileName;

    @CommandLine.Option(names = {"-t", "--localFileName"}, description = "Name of the local (target) downloaded file", required = true)
    private String localFileName;

    @CommandLine.Option(names = {"-o", "--optionalOperationParameters"}, description = "Optional HDFS download parameters", required = false)
    private String optionalOperationParameters;




    @Override
    public void run() {
        /*
        Open and Read a File
            Submit a HTTP GET request with automatically following redirects.
            curl -i -L "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=OPEN
                                [&offset=<LONG>][&length=<LONG>][&buffersize=<INT>][&noredirect=<true|false>]"
            Usually the request is redirected to a datanode where the file data can be read:

            HTTP/1.1 307 TEMPORARY_REDIRECT
            Location: http://<DATANODE>:<PORT>/webhdfs/v1/<PATH>?op=OPEN...
            Content-Length: 0
            However if you do not want to be automatically redirected, you can set the noredirect flag.

            HTTP/1.1 200 OK
            Content-Type: application/json
            {"Location":"http://<DATANODE>:<PORT>/webhdfs/v1/<PATH>?op=OPEN..."}
            The client follows the redirect to the datanode and receives the file data:

            HTTP/1.1 200 OK
            Content-Type: application/octet-stream
            Content-Length: 22

            Hello, webhdfs user!
            See also: offset, length, buffersize, FileSystem.open
         */

        // Step 1 ----- Send request to name node and Get the "Location" string (for data node)
        String url_ops = "?op=OPEN&noredirect=true";

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

        // Step 2 ----- get redirect data node location
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonParser().parseString(responseJSON).getAsJsonObject();
        String originalLocation = jsonObject.get("Location").getAsString();

        URL originalURL = null;
        try {
            originalURL = new URL(originalLocation);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        String dataNodeName = originalURL.getHost();
        Integer dataNodePort = originalURL.getPort();

        // Here we replace host and port with NDP service host and port
        String file = originalURL.getFile();

        // TODO: for now just talk to data node directly, later NDPService GET support can be added
        HdfsConnectionUtils secondRequestHdfsClientConnUtil = new HdfsConnectionUtils(dataNodeName, String.valueOf(dataNodePort));

        // for test only: send to data node directly
        //HdfsConnectionUtils secondRequestHdfsClientConnUtil = new HdfsConnectionUtils(DATA_NODE1_DEFAULT_IP, String.valueOf(dataNodePort));

        String secondResponseJSON = "";
        try {
            HttpURLConnection conn = secondRequestHdfsClientConnUtil.getConnection(file, METHOD_TYPE);
            conn.setDoInput(true);
            conn.connect();

            BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
            BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(localFileName));

            //logger.info("Writing the file to local filesystem: " + localFileName);
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
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
