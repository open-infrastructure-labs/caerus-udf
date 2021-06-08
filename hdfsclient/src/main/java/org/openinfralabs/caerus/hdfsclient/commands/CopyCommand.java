package org.openinfralabs.caerus.hdfsclient.commands;


import org.springframework.stereotype.Component;
import picocli.CommandLine;

/* debug input parameters:
copy -sb testbucket -sk sample3.jpg -tb -images -tk sample3.copied.jpg
 */

@CommandLine.Command(
        name = "copy", description = "Creates a copy of an object that is already stored in HDFS."
)
@Component
public class CopyCommand implements Runnable {

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display this message")
    private boolean helpRequested = false;

    @CommandLine.Option(names = {"-sb", "--source_bucket"}, description = "S3 source bucket name", required = true)
    private String fromBucketName;

    @CommandLine.Option(names = {"-tb", "--target_bucket"}, description = "S3 target bucket name", required = true)
    private String toBucketName;

    @CommandLine.Option(names = {"-tk", "--targetObjKey"}, description = "Key of the target Object", required = false)
    private String targetObjectKey;

    @CommandLine.Option(names = {"-sk", "--sourceObjKey"}, description = "Key of the source object", required = false)
    private String srcObjectKey;

    @Override
    public void run() {

     /*   AmazonS3 s3Client = AWSUtils.getAWSClient();

        try {
            CopyObjectResult copyObjectResult = s3Client.copyObject(fromBucketName, srcObjectKey, toBucketName, targetObjectKey);

            System.out.println("CopyObjectResult: lastModified = " + copyObjectResult.getLastModifiedDate().toString());
            System.out.println("CopyObjectResult: etag = " + copyObjectResult.getETag());

        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        }

        System.out.println("Copy Done!");
*/
    }
}
