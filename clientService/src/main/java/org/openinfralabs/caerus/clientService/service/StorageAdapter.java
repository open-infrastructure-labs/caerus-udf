package org.openinfralabs.caerus.clientService.service;

import org.openinfralabs.caerus.clientService.model.UdfInvocationMetadata;

import java.io.InputStream;

public interface StorageAdapter {
    void uploadFile(String bucket, String filename, InputStream inputStream, UdfInvocationMetadata metadata);
    byte[] getFile(String bucket, String key, UdfInvocationMetadata metadata);
    void deleteFile(String bucket, String key, UdfInvocationMetadata metadata);
}
