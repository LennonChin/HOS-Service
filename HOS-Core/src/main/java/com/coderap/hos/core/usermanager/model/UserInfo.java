package com.coderap.hos.core.usermanager.model;

import com.coderap.hos.core.usermanager.CoreUtil;
import lombok.Data;

import java.util.Date;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/20 23:13:54
 */
@Data
public class UserInfo {

    private String userId;
    private String userName;
    private String password;
    private String detail;
    private SystemRole systemRole;
    private Date createTime;

    public UserInfo() {
    }

    public UserInfo(String userName, String password, String detail, SystemRole systemRole) {
        // TODO UUID
        this.userId = CoreUtil.getUUIDString();
        this.userName = userName;
        // TODO MD5加密
        this.password = CoreUtil.getMd5Password(password);
        this.detail = detail;
        this.systemRole = systemRole;
        this.createTime = new Date();
    }
}
