package com.coderap.hos.web.security.service.impl;

import com.coderap.hos.common.BucketModel;
import com.coderap.hos.core.authmanager.model.ServiceAuth;
import com.coderap.hos.core.authmanager.model.TokenInfo;
import com.coderap.hos.core.authmanager.service.AuthService;
import com.coderap.hos.core.usermanager.CoreUtil;
import com.coderap.hos.core.usermanager.model.SystemRole;
import com.coderap.hos.core.usermanager.model.UserInfo;
import com.coderap.hos.core.usermanager.service.UserService;
import com.coderap.hos.server.service.BucketService;
import com.coderap.hos.web.security.service.OperationAccessControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/23 20:50:18
 */
@Component("defaultOperationAccessControlImpl")
public class DefaultOperationAccessControlImpl implements OperationAccessControl {

    @Autowired
    @Qualifier("authServiceImpl")
    AuthService authService;

    @Autowired
    @Qualifier("userServiceImpl")
    UserService userService;

    @Autowired
    @Qualifier("bucketServiceImpl")
    BucketService bucketService;

    @Override
    public UserInfo checkLogin(String userName, String password) {
        UserInfo userInfo = userService.getUserInfoByName(userName);
        if (userInfo == null) {
            return null;
        }
        return userInfo.getPassword().equals(CoreUtil.getMd5Password(password)) ? userInfo : null;
    }

    @Override
    public boolean checkSystemRole(SystemRole systemRole1, SystemRole systemRole2) {
        if (systemRole1.equals(SystemRole.SUPERADMIN)) {
            return true;
        }
        return systemRole1.equals(SystemRole.ADMIN) && systemRole2.equals(SystemRole.USER);
    }

    @Override
    public boolean checkSystemRole(SystemRole systemRole, String userId) {
        if (systemRole.equals(SystemRole.SUPERADMIN)) {
            return true;
        }
        UserInfo userInfo = userService.getUserInfo(userId);
        return systemRole.equals(SystemRole.ADMIN) && userInfo.getSystemRole().equals(SystemRole.USER);
    }

    @Override
    public boolean checkTokenOwner(String userName, String token) {
        TokenInfo tokenInfo = authService.getTokenInfo(token);
        return tokenInfo.getCreator().equals(userName);
    }

    @Override
    public boolean checkBucketOwner(String userName, String bucket) {
        BucketModel bucketByName = bucketService.getBucketByName(bucket);
        return bucketByName.getCreator().equals(userName);
    }

    @Override
    public boolean checkPermission(String token, String bucket) {
        if (authService.checkToken(token)) {
            ServiceAuth serviceAuth = authService.getAuth(bucket, token);
            if (serviceAuth != null) {
                return true;
            }
        }
        return false;
    }
}
