package com.coderap.hos.server.service;

import com.coderap.hos.common.HOSObject;
import com.coderap.hos.common.HOSObjectSummary;
import com.coderap.hos.common.ObjectListResult;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/22 20:29:35
 */
public interface HOSStoreService {

    public void createBucketStore(String bucket) throws Exception;
    public void deleteBucketStore(String bucket) throws Exception;
    public void createSeqTable() throws Exception;
    public void put(String bucket, String key, ByteBuffer content, long length, String mediaType, Map<String, String> properties) throws Exception;
    public HOSObjectSummary getSummary(String bucket, String key) throws Exception;
    public List<HOSObjectSummary> getSummaries(String bucket, String key, String startKey, String endKey) throws Exception;
    public ObjectListResult listDir(String bucket, String dir, String startKey, int maxCount) throws Exception;
    public ObjectListResult listDirByPrefix(String bucket, String dir, String startKey, String prefix, int maxCount) throws Exception;
    public HOSObject getObject(String bucket, String key) throws Exception;
    public void deleteObject(String bucket, String key) throws Exception;
}
