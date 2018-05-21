package com.coderap.hos.core;

import com.coderap.hos.core.usermanager.service.UserService;
import com.coderap.hos.core.usermanager.model.SystemRole;
import com.coderap.hos.core.usermanager.model.UserInfo;
import com.coderap.hos.mybatis.BaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/20 23:47:45
 */
public class UserServiceTest extends BaseTest {

    @Autowired
    @Qualifier("userServiceImpl")
    UserService userService;

    @Test
    public void addUserTest() {
        UserInfo userInfo = new UserInfo("Jack", "123456", "this is a test user", SystemRole.ADMIN);
        userService.addUser(userInfo);
    }

    @Test
    public void getUserInfoTest() {
        UserInfo userInfo = userService.getUserInfoByName("Tom");
        System.out.println(userInfo.getUserName());
    }
}
