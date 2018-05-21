package com.coderap.hos.server.service.impl;

import com.coderap.hos.common.BucketModel;
import com.coderap.hos.core.authmanager.model.ServiceAuth;
import com.coderap.hos.core.authmanager.service.AuthService;
import com.coderap.hos.core.usermanager.model.UserInfo;
import com.coderap.hos.server.service.BucketService;
import com.coderap.hos.server.dao.BucketModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/21 21:50:14
 */
@Service("bucketServiceImpl")
@Transactional
public class BucketServiceImpl implements BucketService {

    @Autowired
    private BucketModelMapper bucketModelMapper;

    @Autowired
    @Qualifier("authServiceImpl")
    private AuthService authService;

    @Override
    public boolean addBucket(UserInfo userInfo, String bucketName, String detail) {
        BucketModel bucketModel = new BucketModel(bucketName, userInfo.getUserName(), detail);
        bucketModelMapper.addBucket(bucketModel);
        // TODO add auth for bucket and user
        ServiceAuth serviceAuth = new ServiceAuth();
        serviceAuth.setAuthTime(new Date());
        serviceAuth.setTargetToken(userInfo.getUserId());
        serviceAuth.setBucketName(bucketName);
        authService.addAuth(serviceAuth);
        return true;
    }

    @Override
    public boolean deleteBucket(String bucketName) {
        bucketModelMapper.deleteBucket(bucketName);
        // TODO delete auth for bucket
        authService.deleteAuthByBucket(bucketName);
        return true;
    }

    @Override
    public boolean updateBucket(String bucketName, String detail) {
        bucketModelMapper.updateBucket(bucketName, detail);
        return true;
    }

    @Override
    public BucketModel getBucketById(String bucketId) {
        return bucketModelMapper.getBucket(bucketId);
    }

    @Override
    public BucketModel getBucketByName(String bucketName) {
        return bucketModelMapper.getBucketByName(bucketName);
    }

    @Override
    public List<BucketModel> getBucketsByCreator(String creator) {
        return bucketModelMapper.getBucketsByCreator(creator);
    }

    @Override
    public List<BucketModel> getUserAuthorizedBuckets(String token) {
        return bucketModelMapper.getUserAuthorizedBuckets(token);
    }
}
