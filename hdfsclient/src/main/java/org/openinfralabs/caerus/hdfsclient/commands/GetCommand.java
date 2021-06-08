package org.openinfralabs.caerus.hdfsclient.commands;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/* debug input parameters:
get -b testbucket -k sample0.jpg
 */

@CommandLine.Command(
        name = "get", description = "Retrieve/Download object from HDFS."
)
@Component
public class GetCommand implements Runnable{

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display this message")
    private boolean helpRequested = false;

    @CommandLine.Option(names = {"-b", "--bucketName"}, description = "S3 bucket name", required = true)
    private String bucketName;

    @CommandLine.Option(names = {"-k", "--keyName"}, description = "Key of the Object that is to be retrieved", required = true)
    private String objectKey;

    @Override
    public void run() {
/*
        AmazonS3 s3Client = AWSUtils.getAWSClient();

        try {
            S3Object o = s3Client.getObject(bucketName, objectKey);
            S3ObjectInputStream s3is = o.getObjectContent();
            FileOutputStream fos = new FileOutputStream(new File(objectKey));
            byte[] read_buf = new byte[1024];
            int read_len = 0;
            while ((read_len = s3is.read(read_buf)) > 0) {
                fos.write(read_buf, 0, read_len);
            }
            s3is.close();
            fos.close();
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        System.out.println("Done!");
*/
    }

}
