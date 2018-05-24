package com.coderap.hos.sdk.service;

import com.coderap.hos.common.*;
import com.coderap.hos.common.util.JsonUtil;
import com.coderap.hos.sdk.MimeUtil;
import okhttp3.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/24 20:56:41
 */
public interface HOSClientService {

    public void createBucket(String bucketName) throws Exception;
    public void createBucket(String bucketName, String detail) throws Exception;
    public void deleteBucket(String bucketName) throws Exception;
    public List<BucketModel> listBucket() throws Exception;
    public void putObject(PutRequest putRequest) throws Exception;
    public void putObject(String bucket, String key) throws Exception;
    public void putObject(String bucket, String key, byte[] content, String mediaType) throws Exception;
    public void putObject(String bucket, String key, File content) throws Exception;
    public void putObject(String bucket, String key, byte[] content, String mediaType,
                          String contentEncoding) throws Exception;
    public void putObject(String bucket, String key, File content, String mediaType)
            throws Exception;
    public void putObject(String bucket, String key, File content, String mediaType,
                          String contentEncoding) throws Exception;
    public void deleteObject(String bucket, String key) throws Exception;
    public HOSObjectSummary getObjectSummary(String bucket, String key) throws Exception;
    public ObjectListResult listObject(String bucket, String startKey, String endKey)
            throws IOException;
    public ObjectListResult listObject(ListObjectRequest request) throws IOException;
    public ObjectListResult listObjectByPrefix(String bucket, String dir, String prefix,
                                               String startKey)
            throws IOException;
    public ObjectListResult listObjectByDir(String bucket, String dir, String startKey)
            throws IOException;
    public HOSObject getObject(String bucket, String key) throws IOException;
    public BucketModel getBucketInfo(String bucketName) throws IOException;
}
