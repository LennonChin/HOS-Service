package com.coderap.hos.core.service.impl;

import com.coderap.hos.core.service.UserService;
import com.coderap.hos.core.usermanager.CoreUtil;
import com.coderap.hos.core.usermanager.dao.UserInfoMapper;
import com.coderap.hos.core.usermanager.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/20 23:35:45
 */
@Transactional
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public boolean addUser(UserInfo userInfo) {
        userInfoMapper.addUser(userInfo);
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
