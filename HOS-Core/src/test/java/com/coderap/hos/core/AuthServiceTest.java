package com.coderap.hos.core;

import com.coderap.hos.core.authmanager.model.ServiceAuth;
import com.coderap.hos.core.authmanager.model.TokenInfo;
import com.coderap.hos.core.authmanager.service.AuthService;
import com.coderap.hos.mybatis.BaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Date;
import java.util.List;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/21 21:24:25
 */
public class AuthServiceTest extends BaseTest {

    @Autowired
    @Qualifier("authServiceImpl")
    AuthService authService;

    @Test
    public void addToken() {
        TokenInfo tokenInfo = new TokenInfo("Tom");
        authService.addToken(tokenInfo);
    }

    @Test
    public void getTokenByUser() {
        List<TokenInfo> tokenInfos = authService.getTokenInfos("Tom");
        tokenInfos.forEach(tokenInfo -> {
            System.out.println(tokenInfo.getToken());
        });
    }

    @Test
    public void addServiceAuth() {
        ServiceAuth serviceAuth = new ServiceAuth();
        serviceAuth.setBucketName("bucket-1");
        serviceAuth.setTargetToken("358fae97a1084a829cebd5d9907b5122");
        serviceAuth.setAuthTime(new Date());
        authService.addAuth(serviceAuth);
    }

    @Test
    public void getServiceAuth() {
        ServiceAuth auth = authService.getAuth("bucket-1", "358fae97a1084a829cebd5d9907b5122");
        System.out.println(auth.getBucketName() + " : " + auth.getTargetToken());
    }
}
