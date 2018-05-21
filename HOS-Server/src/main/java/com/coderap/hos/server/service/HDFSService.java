package com.coderap.hos.server.service;

import java.io.InputStream;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/21 23:10:27
 */
public interface HDFSService {

    public void saveFile(String dir, String name, InputStream inputStream, long length, short replication) throws Exception;
    public void deleteFile(String dir, String name) throws Exception;
    public InputStream openFile(String dir, String name) throws Exception;
    public void makeDir(String dir) throws Exception;
    public void deleteDir(String dir) throws Exception;
}
