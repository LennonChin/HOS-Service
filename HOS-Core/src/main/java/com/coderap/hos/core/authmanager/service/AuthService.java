package com.coderap.hos.core.authmanager.service;

import com.coderap.hos.core.authmanager.model.ServiceAuth;
import com.coderap.hos.core.authmanager.model.TokenInfo;
import jdk.nashorn.internal.parser.Token;

import java.util.List;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/21 20:44:16
 */
public interface AuthService {

    public boolean addAuth(ServiceAuth serviceAuth);
    public boolean deleteAuth(String bucket, String token);
    public boolean deleteAuthByToken(String token);
    public boolean deleteAuthByBucket(String bucket);
    public ServiceAuth getAuth(String bucket, String token);

    public boolean addToken(TokenInfo tokenInfo);
    public boolean deleteToken(String token);
    public boolean updateToken(String token, int expireTime, boolean isActive);
    public boolean refreshToken(String token);
    public boolean checkToken(String token);
    public TokenInfo getTokenInfo(String token);
    public List<TokenInfo> getTokenInfos(String creator);
}
