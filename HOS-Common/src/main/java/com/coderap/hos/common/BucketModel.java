package com.coderap.hos.common;

import com.coderap.hos.core.usermanager.CoreUtil;
import lombok.Data;

import java.util.Date;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/21 21:47:46
 */
@Data
public class BucketModel {

    private String bucketId;
    private String bucketName;
    private String creator;
    private String detail;
    private Date createTime;

    public BucketModel() {}

    public BucketModel(String bucketName, String creator, String detail) {
        this.bucketId = CoreUtil.getUUIDString();
        this.bucketName = bucketName;
        this.creator = creator;
        this.detail = detail;
        this.createTime = new Date();
    }
}
