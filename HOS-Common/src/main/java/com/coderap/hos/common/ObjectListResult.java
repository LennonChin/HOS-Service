package com.coderap.hos.common;

import lombok.Data;

import java.util.List;

/**
 * @program: HOS-Service
 * @description: 对象列表
 * @author: Lennon Chin
 * @create: 2018/05/22 20:26:47
 */
@Data
public class ObjectListResult {
    private String bucket;
    private String maxKey;
    private String minKey;
    private String nextMarker;
    private int maxKeyNumber;
    private int objectCount;
    private String listId;
    private List<HOSObjectSummary> objectSummaryList;
}
