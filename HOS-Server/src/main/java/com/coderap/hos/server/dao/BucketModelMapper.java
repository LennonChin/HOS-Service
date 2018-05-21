package com.coderap.hos.server.dao;

import com.coderap.hos.common.BucketModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;

import java.util.List;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/21 21:50:32
 */
@Mapper
public interface BucketModelMapper {

    public void addBucket(@Param("bucket") BucketModel bucketModel);
    public void deleteBucket(@Param("bucketName") String bucketName);
    public void updateBucket(@Param("bucketName") String bucketName, @Param("detail") String detail);

    @ResultMap("BucketResultMap")
    public BucketModel getBucket(@Param("bucketId") String bucketId);

    @ResultMap("BucketResultMap")
    public BucketModel getBucketByName(@Param("bucketName") String bucketName);

    @ResultMap("BucketResultMap")
    public List<BucketModel> getBucketsByCreator(@Param("creator") String creator);

    @ResultMap("BucketResultMap")
    public List<BucketModel> getUserAuthorizedBuckets(@Param("token") String token);
}
