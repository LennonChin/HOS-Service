package com.coderap.hos.core.usermanager.dao;

import com.coderap.hos.core.usermanager.model.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;

/**
 * @program: HOS-Service
 * @description: Mapper
 * @author: Lennon Chin
 * @create: 2018/05/20 23:20:26
 */
@Mapper
public interface UserInfoMapper {

    /**
     * 添加用户
     * @param userInfo
     */
    void addUser(@Param("userInfo")UserInfo userInfo);

    /**
     * 删除用户
     * @param userId
     * @return
     */
    int deleteUser(@Param("userId") String userId);

    /**
     * 修改用户信息
     * @param userId
     * @param password
     * @param detail
     * @return
     */
    int updateUserInfo(@Param("userId") String userId, @Param("password") String password,@Param("detail") String detail);

    /**
     * 通过userID获取用户
     * @param userId
     * @return
     */
    @ResultMap("UserInfoResultMap")
    UserInfo getUserInfo(@Param("userId") String userId);

    /**
     * 通过userName获取用户
     * @param userName
     * @return
     */
    @ResultMap("UserInfoResultMap")
    UserInfo getUserInfoByName(@Param("userName") String userName);

    /**
     * 检查password
     * @param userName
     * @param password
     * @return
     */
    @ResultMap("UserInfoResultMap")
    UserInfo checkPassword(@Param("userName") String userName, @Param("password") String password);
}
