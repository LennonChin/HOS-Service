package com.coderap.hos.common;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @program: HOS-Service
 * @description: 将HBase表中查出来的数据将实例化为该类的一个对象
 * @author: Lennon Chin
 * @create: 2018/05/22 20:17:13
 */
@Data
public class HOSObjectSummary implements Comparable<HOSObjectSummary>, Serializable {

    private static final long serialVersionUID = -252127212343978988L;

    private String id;
    private String key;
    private String name;
    private long length;
    private String mediaType;
    private long lastModifyTime;
    private String bucket;
    private Map<String, String> attrs;

    public String getContentEncoding() {
        return this.attrs != null ? this.attrs.get("content-encoding") : null;
    }

    @Override
    public int compareTo(HOSObjectSummary o) {
        return this.getKey().compareTo(o.getKey());
    }
}
