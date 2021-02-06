package org.openinfralabs.caerus.clientService.service;

import org.openinfralabs.caerus.clientService.model.UdfInvocationMetadata;
import org.springframework.http.HttpHeaders;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface StorageAdapter {
    void uploadFile(String bucket, String filename, InputStream inputStream, UdfInvocationMetadata metadata);
    byte[] getFile(String bucket, String key, UdfInvocationMetadata metadata, Map<String, String> headersMap);
    boolean deleteFile(String bucket, String key, UdfInvocationMetadata metadata);
    boolean copyObject(String fromBucket, String toBucket, String srcObjKey, String targetObjKey, UdfInvocationMetadata metadata, StringBuilder sb);
    boolean listObjects(String bucket, StringBuilder sb);
}
