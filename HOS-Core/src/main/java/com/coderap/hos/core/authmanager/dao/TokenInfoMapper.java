package com.coderap.hos.core.authmanager.dao;

import com.coderap.hos.core.authmanager.model.TokenInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;

import java.util.Date;
import java.util.List;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/21 20:34:00
 */
@Mapper
public interface TokenInfoMapper {

    public void addToken(@Param("token") TokenInfo tokenInfo);
    public void deleteToken(@Param("token") String token);
    public void updateToken(@Param("token") String token, @Param("expireTime") int expireTime, @Param("isActive") int isActive);
    public void refreshToken(@Param("token") String token, @Param("refreshTime") Date expireTime);

    @ResultMap("TokenInfoResultMap")
    public TokenInfo getTokenInfo(@Param("token") String token);

    @ResultMap("TokenInfoResultMap")
    public List<TokenInfo> getTokenInfos(@Param("creator") String creator);
}
