package com.coderap.hos.server.service;

import com.coderap.hos.common.BucketModel;
import com.coderap.hos.core.usermanager.model.UserInfo;

import java.util.List;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/21 21:50:05
 */
public interface BucketService {

    public boolean addBucket(UserInfo userInfo, String bucketName, String detail);
    public boolean deleteBucket(String bucketName);
    public boolean updateBucket(String bucketName, String detail);
    public BucketModel getBucketById(String bucketId);
    public BucketModel getBucketByName(String bucketName);
    public List<BucketModel> getBucketsByCreator(String creator);
    public List<BucketModel> getUserAuthorizedBuckets(String token);
}
