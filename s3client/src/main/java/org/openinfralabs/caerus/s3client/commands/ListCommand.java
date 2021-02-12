package org.openinfralabs.caerus.s3client.commands;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.openinfralabs.caerus.s3client.utils.AWSUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.util.List;

/* debug input parameters:
list -b testbucket
 */

@CommandLine.Command(
        name = "list", description = "list objects within a S3 bucket."
)

@Component
public class ListCommand implements Runnable {

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display this message")
    private boolean helpRequested = false;

    @CommandLine.Option(names = {"-b", "--bucketName"}, description = "S3 bucket name", required = true)
    private String bucketName;

    @Override
    public void run() {

        AmazonS3 s3Client = AWSUtils.getAWSClient();

        ListObjectsV2Result result = s3Client.listObjectsV2(bucketName);
        List<S3ObjectSummary> objects = result.getObjectSummaries();
        for (S3ObjectSummary os : objects) {
            System.out.println("* " + os.getKey());
            System.out.println("    Last Modified: " + os.getLastModified().toString());
            System.out.println("    Size:          " + os.getSize());
            System.out.println("    Etag:          " + os.getETag());
            System.out.println("    Storage Class: " + os.getStorageClass());
        }

    }
}
