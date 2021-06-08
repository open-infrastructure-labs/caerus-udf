package org.openinfralabs.caerus.ndpService.service;

import org.openinfralabs.caerus.ndpService.model.UdfInvocationMetadata;

import java.io.InputStream;
import java.util.Map;

public interface StorageAdapter {
    void uploadFile(String bucket, String filename, InputStream inputStream, UdfInvocationMetadata metadata, String optionalParametersJson);
    byte[] getFile(String bucket, String key, UdfInvocationMetadata metadata, Map<String, String> headersMap);
    boolean deleteFile(String bucket, String key, UdfInvocationMetadata metadata);
    boolean copyObject(String fromBucket, String toBucket, String srcObjKey, String targetObjKey, UdfInvocationMetadata metadata, StringBuilder sb);
    boolean listObjects(String bucket, StringBuilder sb);
}
