package com.coderap.hos.web.security.service;

import com.coderap.hos.core.usermanager.model.SystemRole;
import com.coderap.hos.core.usermanager.model.UserInfo;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/23 20:13:09
 */
public interface OperationAccessControl {

    public UserInfo checkLogin(String userName, String password);
    public boolean checkSystemRole(SystemRole systemRole1, SystemRole systemRole2);
    public boolean checkSystemRole(SystemRole systemRole, String userId);
    public boolean checkTokenOwner(String userName, String name);
    public boolean checkBucketOwner(String userName, String bucket);
    public boolean checkPermission(String token, String bucket);
}
