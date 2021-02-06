package org.openinfralabs.caerus.s3client.commands;

import org.openinfralabs.caerus.s3client.utils.AWSUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;

import java.io.*;
import java.util.UUID;

import static picocli.CommandLine.*;

/* debug input parameters:
put -b testbucket -f "/home/ubuntu/images/new/sample3.jpg" -k sample3.jpg -u caerus-faas-spring-thumbnail -i "400, 600"
 */

@Command(
        name = "put", description = "Upload a object/file to an S3 bucket while provide option to invoke UDF on the object/file."
)

@Component
public class PutCommand implements Runnable {

    //private static String caerusNdpServiceEndpoint = "http://localhost:8000";
    //private static String bucketName = "testbucket";
    //private static String keyName = "sample3.jpg";
    //private static String uploadFileName = "/home/ubuntu/images/new/sample3.jpg";
    private static String FUNCTION_NAME_KEY = "function_name";
    //private static String FUNCTION_NAME_VALUE = "caerus-faas-spring-thumbnail";
    private static String FUNCTION_INPUTPARAMETERS_KEY = "function_inputParameters";
    //private static String FUNCTION_INPUTPARAMETERS_VALUE = "400, 600";

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "display this message")
    private boolean helpRequested = false;

    @Option(names = {"-b", "--bucketName"}, description = "S3 bucket name", required = true)
    private String bucketName;

    @Option(names = {"-f", "--uploadFileName"}, description = "Full path of the file will be uploaded", required = true)
    private String uploadFileName;

    @Option(names = {"-k", "--keyName"}, description = "new object key name in s3, use the same file name as key if not supplied", required = false)
    private String keyName;

    @Option(names = {"-u", "--udfName"}, description = "UDF function name", required = false)
    private String udfName;

    @Option(names = {"-i", "--udfInputParameters"}, description = "UDF input metadata", required = false)
    private String udfInputParameters;


    @Override
    public void run() {


        AmazonS3 s3Client = AWSUtils.getAWSClient();

        try {

            System.out.println("Uploading a new object to S3 from a file\n");
            File file = new File(uploadFileName);

            // Upload a file as a new object with ContentType and title specified.
            PutObjectRequest request = new PutObjectRequest(bucketName, keyName, file);

            // only invoke UDF if the udf name is supplied, otherwise just put object
            if (udfName != null) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.addUserMetadata(FUNCTION_NAME_KEY, udfName);
                metadata.addUserMetadata(FUNCTION_INPUTPARAMETERS_KEY, udfInputParameters);
                request.setMetadata(metadata);
            }
            s3Client.putObject(request);


        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which " + "means your request made it "
                    + "to S3, but was rejected with an error response" + " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());

        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " + "means the client encountered " + "an internal error while trying to "
                    + "communicate with S3, " + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());

        }
    }
}
