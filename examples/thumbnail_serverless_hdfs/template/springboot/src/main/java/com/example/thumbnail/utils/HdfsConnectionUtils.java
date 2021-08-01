package com.example.thumbnail.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class HdfsConnectionUtils {


    // The default host to connect to
    //public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_HOST = "192.168.1.8";

    // The default port
    public static final String DEFAULT_PORT = "89870";

    // The default username
    public static final String DEFAULT_USERNAME = "root";

    // The default username
    public static final String DEFAULT_PASSWORD = "password";

    // The default protocol: TODO https support later
    public static final String DEFAULT_PROTOCOL = "http://";
    //public static final String DEFAULT_PROTOCOL = "https://";

    public static final String METHODTYPE_PUT = "PUT";
    public static final String METHODTYPE_GET = "GET";
    public static final String METHODTYPE_DELETE = "DELETE";
    public static final String METHODTYPE_POST = "POST";

/*
    HTTP GET
    OPEN (see FileSystem.open)
    GETFILESTATUS (see FileSystem.getFileStatus)
    LISTSTATUS (see FileSystem.listStatus)
    GETCONTENTSUMMARY (see FileSystem.getContentSummary)
    GETFILECHECKSUM (see FileSystem.getFileChecksum)
    GETHOMEDIRECTORY (see FileSystem.getHomeDirectory)
    GETDELEGATIONTOKEN (see FileSystem.getDelegationToken)
    HTTP PUT
    CREATE (see FileSystem.create)
    MKDIRS (see FileSystem.mkdirs)
    RENAME (see FileSystem.rename)
    SETREPLICATION (see FileSystem.setReplication)
    SETOWNER (see FileSystem.setOwner)
    SETPERMISSION (see FileSystem.setPermission)
    SETTIMES (see FileSystem.setTimes)
    RENEWDELEGATIONTOKEN (see DistributedFileSystem.renewDelegationToken)
    CANCELDELEGATIONTOKEN (see DistributedFileSystem.cancelDelegationToken)
    HTTP POST
    APPEND (see FileSystem.append)
    HTTP DELETE
    DELETE (see FileSystem.delete)*/

    public static final String GET_OP_OPEN = "OPEN";
    public static final String GET_OP_GETFILESTATUS = "GETFILESTATUS";
    public static final String GET_OP_LISTSTATUS = "LISTSTATUS";

    public static final String PUT_OP_CREATE = "CREATE";

    public static final String POST_OP_APPEND = "APPEND";

    public static final String DELETE_OP_DELETE = "DELETE";


    public static enum AuthenticationType {
        SIMPLE
        //can add KERBEROS later if needed
    }

    public static final AuthenticationType DEFAULT_AUTHENTICATION_TYPE = AuthenticationType.SIMPLE;

    private String host = Optional.ofNullable(System.getenv("HDFS_HOST")).orElse(DEFAULT_HOST);
    private String port = Optional.ofNullable(System.getenv("WEBHDFS_NAME_NODE_PORT")).orElse(DEFAULT_PORT);
    private String username = DEFAULT_USERNAME;
    private String password = DEFAULT_PASSWORD;
    private AuthenticationType authenticationType = DEFAULT_AUTHENTICATION_TYPE;

    public HdfsConnectionUtils(String host, String port) {

        this.host = host;
        this.port = port;

        username = Optional.ofNullable(System.getenv("WEBHDFS_USERNAME")).orElse(username);
        password = Optional.ofNullable(System.getenv("WEBHDFS_PASSWORD")).orElse(password);
        this.username = username;
        this.password = password;
    }

    public HdfsConnectionUtils(String host, String port, String username, String password, AuthenticationType authType) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.authenticationType = authType;
    }

    public HttpURLConnection getConnection(String webHdfsUrlSuffix, String methodType) throws IOException {

        String httpsURL = DEFAULT_PROTOCOL + host + ":" + port + webHdfsUrlSuffix;

        String userpass = username + ":" + password;

        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
        URL myUrl = new URL(httpsURL);
        HttpURLConnection conn = (HttpURLConnection) myUrl.openConnection();

        if (conn instanceof HttpURLConnection) {
            conn.setRequestProperty("Authorization", basicAuth);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod(methodType);
            //conn.setRequestProperty("Content-type", "text/plain");
        }
        System.out.println("Creating connection to webHDFS endpoint: " + httpsURL);
        return conn;
    }

    public static String responseMessage(HttpURLConnection conn, boolean input) throws IOException {
        StringBuffer sb = new StringBuffer();
        if (input) {
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = null;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            is.close();
        }
        Map<String, Object> result = new HashMap<String, Object>();


        Integer responseCode = conn.getResponseCode();
        result.put("code", responseCode);

        InputStream inputStream;
        if (200 <= responseCode && responseCode <= 299) {
            inputStream = conn.getInputStream();
        } else {
            inputStream = conn.getErrorStream();
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        inputStream));

        StringBuilder response = new StringBuilder();
        String currentLine;

        while ((currentLine = in.readLine()) != null)
            response.append(currentLine);

        in.close();

        String res = response.toString();


        result.put("message", conn.getResponseMessage());
        result.put("type", conn.getContentType());
        result.put("data", sb);

        Gson gson = new Gson();
        if (res != null && !res.isEmpty() && res.contains("Location")) {

            JsonObject jsonObject = new JsonParser().parseString(res).getAsJsonObject();

            String location = jsonObject.get("Location").getAsString();


            result.put("Location", location);
        }
        String json = gson.toJson(result);
        //logger.info("JSON Response = " + json);

        return json;
    }
}
