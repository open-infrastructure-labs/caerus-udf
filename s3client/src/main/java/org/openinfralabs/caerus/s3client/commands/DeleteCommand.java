package org.openinfralabs.caerus.s3client.commands;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.openinfralabs.caerus.s3client.utils.AWSUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;

/* debug input parameters:
delete -b testbucket -k sample3.jpg
 */

@CommandLine.Command(
        name = "delete", description = "Remove an object/file from anS3 bucket."
)

@Component
public class DeleteCommand implements Runnable {
    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display this message")
    private boolean helpRequested = false;

    @CommandLine.Option(names = {"-b", "--bucketName"}, description = "S3 bucket name", required = true)
    private String bucketName;

    @CommandLine.Option(names = {"-k", "--keyName"}, description = "Key of the Object that is to be deleted", required = false)
    private String objectKey;

    @Override
    public void run() {


        AmazonS3 s3Client = AWSUtils.getAWSClient();

        try {
            s3Client.deleteObject(bucketName, objectKey);
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        }
        System.out.println("Done Deletion!");
    }
}
