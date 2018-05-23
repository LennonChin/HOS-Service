package com.coderap.hos.web.security;

import com.coderap.hos.core.usermanager.model.UserInfo;

/**
 * @program: HOS-Service
 * @description: 获取和设置当前用户的类
 * @author: Lennon Chin
 * @create: 2018/05/23 19:59:51
 */
public class ContextUtil {

    public final static String SESSION_KEY = "USER_TOKEN";

    public static ThreadLocal<UserInfo> userInfoThreadLocal = new ThreadLocal<>();

    public static UserInfo getCurrentUser() {
        return userInfoThreadLocal.get();
    }

    public static void setCurrentUser(UserInfo userInfo) {
        userInfoThreadLocal.set(userInfo);
    }

    public static void clear() {
        userInfoThreadLocal.remove();
    }
}
