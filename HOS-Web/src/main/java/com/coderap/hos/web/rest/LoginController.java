package com.coderap.hos.web.rest;

import com.coderap.hos.core.ErrorCode;
import com.coderap.hos.core.usermanager.CoreUtil;
import com.coderap.hos.core.usermanager.model.UserInfo;
import com.coderap.hos.web.security.ContextUtil;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/**
 * @program: HOS-Service
 * @description: 登录API
 * @author: Lennon Chin
 * @create: 2018/05/23 21:05:23
 */
@Controller
public class LoginController extends BaseController {

    @RequestMapping("/loginPost")
    @ResponseBody
    public Object loginPost(String userName, String password, HttpSession session) throws Exception {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password)) {
            return getError(ErrorCode.ERROR_PERMISSION_DENIED, "login error");
        }
        UserInfo userInfo = operationAccessControl.checkLogin(userName, password);
        if (userInfo != null) {
            session.setAttribute(ContextUtil.SESSION_KEY, userInfo.getUserId());
            return getResult("success");
        } else {
            return getError(ErrorCode.ERROR_PERMISSION_DENIED, "login error");
        }
    }

    @RequestMapping("/logout")
    @ResponseBody
    public Object logout(HttpSession session) throws Exception {
        session.removeAttribute(ContextUtil.SESSION_KEY);
        return getResult("success");
    }
}
