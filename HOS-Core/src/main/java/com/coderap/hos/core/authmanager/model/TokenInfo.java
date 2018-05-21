package com.coderap.hos.core.authmanager.model;

import com.coderap.hos.core.usermanager.CoreUtil;
import lombok.Data;

import java.util.Date;

/**
 * @program: HOS-Service
 * @description: Token model
 * @author: Lennon Chin
 * @create: 2018/05/21 20:29:22
 */
@Data
public class TokenInfo {

    private String token;
    private int expireTime;
    private Date refreshTime;
    private Date createTime;
    private boolean isActive;
    private String creator;

    public TokenInfo() {
    }

    public TokenInfo(String creator) {
        this.token = CoreUtil.getUUIDString();
        this.expireTime = 7;
        this.creator = creator;
        Date now = new Date();
        this.refreshTime = now;
        this.createTime = now;
        this.isActive = true;
    }
}
