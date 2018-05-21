package com.coderap.hos.core.authmanager.service.impl;

import com.coderap.hos.core.authmanager.dao.ServiceAuthMapper;
import com.coderap.hos.core.authmanager.dao.TokenInfoMapper;
import com.coderap.hos.core.authmanager.model.ServiceAuth;
import com.coderap.hos.core.authmanager.model.TokenInfo;
import com.coderap.hos.core.authmanager.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.jnlp.ServiceManager;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/21 20:48:21
 */
@Service("authServiceImpl")
@Transactional
public class AuthServiceImpl implements AuthService {

    @Autowired
    private TokenInfoMapper tokenInfoMapper;

    @Autowired
    private ServiceAuthMapper serviceAuthMapper;

    @Override
    public boolean addAuth(ServiceAuth serviceAuth) {
        serviceAuthMapper.addAuth(serviceAuth);
        return true;
    }

    @Override
    public boolean deleteAuth(String bucket, String token) {
        serviceAuthMapper.deleteAuth(bucket, token);
        return true;
    }

    @Override
    public boolean deleteAuthByToken(String token) {
        serviceAuthMapper.deleteAuthByToken(token);
        return true;
    }

    @Override
    public boolean deleteAuthByBucket(String bucket) {
        serviceAuthMapper.deleteAuthByBucket(bucket);
        return true;
    }

    @Override
    public ServiceAuth getAuth(String bucket, String token) {
        return serviceAuthMapper.getAuth(bucket, token);
    }

    @Override
    public boolean addToken(TokenInfo tokenInfo) {
        tokenInfoMapper.addToken(tokenInfo);
        return true;
    }

    @Override
    public boolean deleteToken(String token) {
        tokenInfoMapper.deleteToken(token);
        // 删除auth
        serviceAuthMapper.deleteAuthByToken(token);
        return true;
    }

    @Override
    public boolean updateToken(String token, int expireTime, boolean isActive) {
        tokenInfoMapper.updateToken(token, expireTime, isActive ? 1 : 0);
        return true;
    }

    @Override
    public boolean refreshToken(String token) {
        tokenInfoMapper.refreshToken(token, new Date());
        return false;
    }

    @Override
    public boolean checkToken(String token) {
        TokenInfo tokenInfo = tokenInfoMapper.getTokenInfo(token);
        if (tokenInfo == null) {
            return false;
        }
        if (tokenInfo.isActive()) {
            // 判断token是否过期
            Date now = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(tokenInfo.getRefreshTime());
            calendar.add(Calendar.DATE, tokenInfo.getExpireTime());
            return now.before(calendar.getTime());
        }
        return false;
    }

    @Override
    public TokenInfo getTokenInfo(String token) {
        return tokenInfoMapper.getTokenInfo(token);
    }

    @Override
    public List<TokenInfo> getTokenInfos(String creator) {
        return tokenInfoMapper.getTokenInfos(creator);
    }
}
