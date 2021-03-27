package org.openinfralabs.caerus.s3client.commands;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.openinfralabs.caerus.s3client.utils.AWSUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.StatusAssertions;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(SpringExtension.class)
class ListCommandTest {

    @MockBean
    private AmazonS3 s3Client;

    @MockBean
    private ListObjectsV2Result result;

    @Test
    void run() {

        Date lastModified = new Date();
        List<S3ObjectSummary> list = new ArrayList<>();
        S3ObjectSummary objectSummary1 = new S3ObjectSummary();
        objectSummary1.setBucketName("test-bucket");
        objectSummary1.setKey("a");
        objectSummary1.setLastModified(lastModified);
        objectSummary1.setSize(1000);
        objectSummary1.setETag("etag");
        objectSummary1.setStorageClass("standard");

        list.add(objectSummary1);

        try (MockedStatic<AWSUtils> mockAWSUtils = Mockito.mockStatic(AWSUtils.class)) {
            mockAWSUtils.when(() -> AWSUtils.getAWSClient()).thenReturn(s3Client);
        }

        //AWSUtils mockawsUtils = mock(AWSUtils.class);
        //doReturn(s3Client).when(this.awsUtils).getAWSClient();
        //doReturn(result).when(this.s3Client).listObjectsV2("test-bucket");
        //doReturn(list).when(this.result).getObjectSummaries();

        //Mockito.when(this.awsUtils.getAWSClient()).thenReturn(s3Client);
        Mockito.when(this.s3Client.listObjectsV2("test-bucket")).thenReturn(result);
        Mockito.when(this.result.getObjectSummaries()).thenReturn(list);

        String[] args = new String[] {"-b", "test-bucket"};
        ListCommand listCommand = new ListCommand();
        new CommandLine(listCommand).parse(args);

        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.execute(listCommand);


    }
}


