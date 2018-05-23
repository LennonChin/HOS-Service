package com.coderap.hos.web;

import com.coderap.hos.core.usermanager.CoreUtil;
import com.coderap.hos.core.usermanager.model.SystemRole;
import com.coderap.hos.core.usermanager.model.UserInfo;
import com.coderap.hos.core.usermanager.service.UserService;
import com.coderap.hos.server.service.HOSStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/23 19:47:16
 */
@Component
public class ApplicationInitialization implements ApplicationRunner {

    @Autowired
    @Qualifier("userServiceImpl")
    UserService userService;

    @Autowired
    @Qualifier("hosStoreServiceImpl")
    HOSStoreService hosStoreService;

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {

        // 创建管理员用户
        UserInfo userInfo = userService.getUserInfoByName(CoreUtil.SYSTEM_USER);
        if (userInfo == null) {
            userInfo = new UserInfo(CoreUtil.SYSTEM_USER, "superadmin", "this is a default super admin", SystemRole.SUPERADMIN);
            userService.addUser(userInfo);
        }
        // 创建SequenceID table
        hosStoreService.createSeqTable();
    }
}
