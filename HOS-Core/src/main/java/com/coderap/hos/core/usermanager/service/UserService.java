package com.coderap.hos.core.usermanager.service;

import com.coderap.hos.core.usermanager.model.UserInfo;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/20 23:33:20
 */
public interface UserService {

    public boolean addUser(UserInfo userInfo);

    public boolean updateUserInfo(String userId, String password, String detail);

    public boolean deleteUser(String userId);

    public UserInfo getUserInfo(String userId);

    public UserInfo getUserInfoByName(String userName);

    public UserInfo checkPassword(String userName, String password);
}
