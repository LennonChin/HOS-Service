package com.coderap.hos.core.usermanager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * @program: HOS-Service
 * @description: 工具类
 * @author: Lennon Chin
 * @create: 2018/05/20 23:16:42
 */
public class CoreUtil {

    public final static String SYSTEM_USER = "SuperAdmin";

    /**
     * 生成UUID
     * @return
     */
    public static String getUUIDString() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * MD5加密
     * @param str
     * @return
     */
    public static String getMd5Password(String str) {
        String reStr = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(str.getBytes());
            StringBuffer stringBuffer = new StringBuffer();
            for (byte b : bytes) {
                int bt = b & 0xff;
                if (bt < 16) {
                    stringBuffer.append(0);
                }
                stringBuffer.append(Integer.toHexString(bt));
            }
            reStr = stringBuffer.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return reStr;
    }
}
