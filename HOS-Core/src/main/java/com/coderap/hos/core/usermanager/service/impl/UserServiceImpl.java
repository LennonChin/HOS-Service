package com.coderap.hos.core.usermanager.service.impl;

import com.coderap.hos.core.authmanager.model.TokenInfo;
import com.coderap.hos.core.authmanager.service.AuthService;
import com.coderap.hos.core.usermanager.CoreUtil;
import com.coderap.hos.core.usermanager.dao.UserInfoMapper;
import com.coderap.hos.core.usermanager.model.UserInfo;
import com.coderap.hos.core.usermanager.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/20 23:35:45
 */
@Transactional
@Service
public class UserServiceImpl implements UserService {

    // 设置token的过期时间是一百年以后
    private final long LONG_REFRESH_TIME = 4670409600000L;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    @Qualifier("authServiceImpl")
    private AuthService authService;

    @Override
    public boolean addUser(UserInfo userInfo) {
        userInfoMapper.addUser(userInfo);
        // 给用户设置token
        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setToken(userInfo.getUserId());
        tokenInfo.setActive(true);
        tokenInfo.setExpireTime(7);
        tokenInfo.setRefreshTime(new Date(LONG_REFRESH_TIME));
        tokenInfo.setCreator(CoreUtil.SYSTEM_USER);
        tokenInfo.setCreateTime(new Date());
        authService.addToken(tokenInfo);
        return true;
    }

    @Override
    public boolean updateUserInfo(String userId, String password, String detail) {
        // 这里需要判断密码是否为空，当为空时则不更改密码
        userInfoMapper.updateUserInfo(userId, StringUtils.isEmpty(password) ? null : CoreUtil.getMd5Password(password), detail);
        return true;
    }

    @Override
    public boolean deleteUser(String userId) {
        userInfoMapper.deleteUser(userId);
        // 删除用户的token
        authService.deleteToken(userId);
        authService.deleteAuthByToken(userId);
        return true;
    }

    @Override
    public UserInfo getUserInfo(String userId) {
        return userInfoMapper.getUserInfo(userId);
    }

    @Override
    public UserInfo getUserInfoByName(String userName) {
        return userInfoMapper.getUserInfoByName(userName);
    }

    @Override
    public UserInfo checkPassword(String userName, String password) {
        return userInfoMapper.checkPassword(userName, password);
    }
}
