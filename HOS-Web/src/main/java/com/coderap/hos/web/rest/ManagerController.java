package com.coderap.hos.web.rest;

import com.coderap.hos.core.ErrorCode;
import com.coderap.hos.core.authmanager.model.ServiceAuth;
import com.coderap.hos.core.authmanager.model.TokenInfo;
import com.coderap.hos.core.authmanager.service.AuthService;
import com.coderap.hos.core.usermanager.model.SystemRole;
import com.coderap.hos.core.usermanager.model.UserInfo;
import com.coderap.hos.core.usermanager.service.UserService;
import com.coderap.hos.web.security.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

/**
 * @program: HOS-Service
 * @description: 管理API
 * @author: Lennon Chin
 * @create: 2018/05/23 21:09:57
 */
@RestController
@RequestMapping("hos/v1/sys")
public class ManagerController extends BaseController {

    @Autowired
    @Qualifier("userServiceImpl")
    UserService userService;

    @Autowired
    @Qualifier("authServiceImpl")
    AuthService authService;

    // 创建用户
    @RequestMapping(value = "user", method = RequestMethod.POST)
    public Object createUser(@RequestParam("userName") String userName,
                             @RequestParam("password") String password,
                             @RequestParam(name = "detail", required = false, defaultValue = "") String detail,
                             @RequestParam(name = "role", required = false, defaultValue = "USER") String role) {
        UserInfo userInfo = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkSystemRole(userInfo.getSystemRole(), SystemRole.valueOf(role))) {
            UserInfo newUserInfo = new UserInfo(userName, password, detail, SystemRole.valueOf(role));
            userService.addUser(newUserInfo);
            return getResult("success");
        }
        return getError(ErrorCode.ERROR_PERMISSION_DENIED, "create user error");
    }

    // 删除用户
    @RequestMapping(value = "user", method = RequestMethod.DELETE)
    public Object deleteUser(@RequestParam("userId") String userId) {
        UserInfo userInfo = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkSystemRole(userInfo.getSystemRole(), userId)) {
            userService.deleteUser(userId);
            return getResult("success");
        }
        return getError(ErrorCode.ERROR_PERMISSION_DENIED, "delete user error");
    }

    // 添加token
    @RequestMapping(value = "token", method = RequestMethod.POST)
    public Object createToken(@RequestParam(name = "expireTime", required = false, defaultValue = "7") String expireTime,
                              @RequestParam(name = "isActive", required = false, defaultValue = "true") String isActive) {
        UserInfo userInfo = ContextUtil.getCurrentUser();
        if (!userInfo.getSystemRole().equals(SystemRole.VISITOR)) {
            TokenInfo tokenInfo = new TokenInfo(userInfo.getUserName());
            tokenInfo.setExpireTime(Integer.parseInt(expireTime));
            tokenInfo.setActive(Boolean.parseBoolean(isActive));
            authService.addToken(tokenInfo);
            return getResult("success");
        }
        return getError(ErrorCode.ERROR_PERMISSION_DENIED, "create token error");
    }

    // 删除token
    @RequestMapping(value = "token", method = RequestMethod.DELETE)
    public Object deleteToken(@RequestParam("token") String token) {
        UserInfo userInfo = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkTokenOwner(userInfo.getUserName(), token)) {
            authService.deleteToken(token);
            return getResult("success");
        }
        return getError(ErrorCode.ERROR_PERMISSION_DENIED, "delete token error");
    }

    // 授权
    @RequestMapping(value = "auth", method = RequestMethod.POST)
    public Object createAuth(@RequestBody ServiceAuth serviceAuth) {
        UserInfo userInfo = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkTokenOwner(userInfo.getUserName(), serviceAuth.getTargetToken()) && operationAccessControl.checkBucketOwner(userInfo.getUserName(), serviceAuth.getBucketName())) {
            authService.addAuth(serviceAuth);
            return getResult("success");
        }
        return getError(ErrorCode.ERROR_PERMISSION_DENIED, "create auth error");
    }

    // 取消授权
    @RequestMapping(value = "auth", method = RequestMethod.DELETE)
    public Object deleteAuth(@RequestParam("token") String token,
                             @RequestParam("bucket") String bucket) {
        UserInfo userInfo = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkTokenOwner(userInfo.getUserName(), token) && operationAccessControl.checkBucketOwner(userInfo.getUserName(), bucket)) {
            authService.deleteAuth(bucket, token);
            return getResult("success");
        }
        return getError(ErrorCode.ERROR_PERMISSION_DENIED, "create auth error");
    }
}
