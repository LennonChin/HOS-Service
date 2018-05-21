package com.coderap.hos.core.authmanager.dao;

import com.coderap.hos.core.authmanager.model.ServiceAuth;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/21 20:34:12
 */
@Mapper
public interface ServiceAuthMapper {

    public void addAuth(@Param("auth") ServiceAuth auth);
    public void deleteAuth(@Param("bucket") String bucket, @Param("token") String token);
    public void deleteAuthByToken(@Param("token") String token);
    public void deleteAuthByBucket(@Param("bucket") String bucket);
    public void updateAuth(@Param("auth") ServiceAuth auth);

    @ResultMap("ServiceAuthResultMap")
    public ServiceAuth getAuth(@Param("bucket") String bucket, @Param("token") String token);
}
