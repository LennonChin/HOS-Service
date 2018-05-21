package com.coderap.hos.core.authmanager.model;

import lombok.Data;

import java.util.Date;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/21 20:32:38
 */
@Data
public class ServiceAuth {

    private String bucketName;
    private String targetToken;
    private Date authTime;

}
