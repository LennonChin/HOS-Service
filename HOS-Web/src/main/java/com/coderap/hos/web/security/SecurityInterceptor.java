package com.coderap.hos.web.security;

import com.coderap.hos.core.authmanager.model.TokenInfo;
import com.coderap.hos.core.authmanager.service.AuthService;
import com.coderap.hos.core.usermanager.model.SystemRole;
import com.coderap.hos.core.usermanager.model.UserInfo;
import com.coderap.hos.core.usermanager.service.UserService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.mysql.jdbc.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

/**
 * @program: HOS-Service
 * @description: 权限拦截器
 * @author: Lennon Chin
 * @create: 2018/05/23 20:02:24
 */
@Component
public class SecurityInterceptor implements HandlerInterceptor {

    @Autowired
    @Qualifier("authServiceImpl")
    AuthService authService;

    @Autowired
    @Qualifier("userServiceImpl")
    UserService userService;

    private Cache<String, UserInfo> userInfoCache = CacheBuilder.newBuilder().expireAfterWrite(20, TimeUnit.MINUTES).build();

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {
        if (httpServletRequest.getRequestURL().equals("/loginPost")) {
            return true;
        }
        String token = "";
        HttpSession session = httpServletRequest.getSession();
        if (session.getAttribute(ContextUtil.SESSION_KEY) != null) {
            token = session.getAttribute(ContextUtil.SESSION_KEY).toString();
        } else {
            token = httpServletRequest.getHeader("X-Auth-Token");
        }
        TokenInfo tokenInfo = authService.getTokenInfo(token);
        if (tokenInfo == null) {
            String url = "loginPost";
            httpServletResponse.sendRedirect(url);
            return false;
        }
        UserInfo userInfo = userInfoCache.getIfPresent(tokenInfo.getToken());
        if (userInfo == null) {
            userInfo = userService.getUserInfo(token);
            if (userInfo == null) {
                // 可能是游客
                userInfo = new UserInfo();
                userInfo.setSystemRole(SystemRole.VISITOR);
                userInfo.setUserName("Visitor");
                userInfo.setDetail("this is a visitor");
                userInfo.setUserId(token);
            }
            userInfoCache.put(tokenInfo.getToken(), userInfo);
        }
        ContextUtil.setCurrentUser(userInfo);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {

    }
}
