package org.openinfralabs.caerus.hdfsclient.commands;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.List;

/* debug input parameters:
list -b testbucket
 */

@CommandLine.Command(
        name = "list", description = "list objects within a HDFS directory."
)

@Component
public class
ListCommand implements Runnable {

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display this message")
    private boolean helpRequested = false;

    @CommandLine.Option(names = {"-b", "--bucketName"}, description = "S3 bucket name", required = true)
    private String bucketName;

    @Override
    public void run() {
/*
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
*/
    }
}
