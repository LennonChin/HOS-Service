package com.coderap.hos.server;

import com.coderap.hos.common.BucketModel;
import com.coderap.hos.core.usermanager.model.UserInfo;
import com.coderap.hos.core.usermanager.service.UserService;
import com.coderap.hos.mybatis.BaseTest;
import com.coderap.hos.server.service.BucketService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/21 22:28:46
 */
public class BucketServiceTest extends BaseTest {

    @Autowired
    @Qualifier("bucketServiceImpl")
    private BucketService bucketService;

    @Autowired
    @Qualifier("userServiceImpl")
    private UserService userService;

    @Test
    public void addBucket() {

        UserInfo userInfo = userService.getUserInfoByName("Tom");
        bucketService.addBucket(userInfo, "bucket-1", "this is a test bucket");
    }

    @Test
    public void getBucket() {
        BucketModel bucketModel = bucketService.getBucketByName("bucket-1");
        System.out.println(bucketModel.getBucketId() + " : " + bucketModel.getBucketName() + " : " + bucketModel.getCreator());
    }
}
