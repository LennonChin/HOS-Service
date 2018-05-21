package com.coderap.hos.server.service.impl;

import com.coderap.hos.core.ErrorCode;
import com.coderap.hos.core.HOSConfiguration;
import com.coderap.hos.server.HOSServerException;
import com.coderap.hos.server.service.HDFSService;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;

import java.io.File;
import java.io.InputStream;
import java.net.URI;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/21 23:16:40
 */
@Slf4j
public class HDFSServiceImpl implements HDFSService {

    private FileSystem fileSystem;
    private long defaultBlockSize = 128 * 1024 * 1024;
    // 当一个文件小于BlockSize的一半的时候，手动将其大小置为BlockSize的一半
    private long initBlockSize = defaultBlockSize / 2;

    public HDFSServiceImpl() throws Exception {
        // 获取HDFS相关的配置信息
        HOSConfiguration hosConfiguration = HOSConfiguration.getConfiguration();
        String confDir = hosConfiguration.getString("hadoop.conf.dir"); // hadoop.conf.dir即存放各种-site.xml的路径
        String hdfsUri = hosConfiguration.getString("hadoop.uri"); // hdfs://s100:9000
        // 通过配置，获取一个FileSystem实例
        Configuration configuration = new Configuration();
        configuration.addResource(new Path(confDir + "/hdfs-site.xml"));
        configuration.addResource(new Path(confDir + "/core-site.xml"));
        // 通过uri和配置获取FileSystem的实例
        this.fileSystem = FileSystem.get(new URI(hdfsUri), configuration);
    }

    @Override
    public void saveFile(String dir, String name, InputStream inputStream, long length, short replication) throws Exception {

        // 1.判断dir是否存在，不存在则新建
        Path dirPath = new Path(dir);
        try {
            if (!this.fileSystem.exists(dirPath)) {
                // 如果目录不存在，则创建目录
                boolean mkdirsResult = this.fileSystem.mkdirs(dirPath, FsPermission.getDirDefault());
                log.info("create dir " + dirPath);

                if (!mkdirsResult) {
                    throw new HOSServerException(ErrorCode.ERROR_HDFS, "Create Dir " + dirPath + " Error");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. 保存文件
        Path path = new Path(dirPath + File.separator + name);
        // 设置文件块大小
        long blockSize = length <= initBlockSize ? initBlockSize : defaultBlockSize;
        // 创建文件输出流
        FSDataOutputStream outputStream = this.fileSystem.create(path, true, 512 * 1024, replication, blockSize);

        try {
            // 设置文件权限
            this.fileSystem.setPermission(path, FsPermission.getFileDefault());
            // 开始写入文件
            byte[] buffer = new byte[512 * 1024];
            int len = -1;
            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭流
            inputStream.close();
            outputStream.close();
        }
    }

    @Override
    public void deleteFile(String dir, String name) throws Exception {
        this.fileSystem.delete(new Path(dir + File.separator + name), false);
    }

    @Override
    public InputStream openFile(String dir, String name) throws Exception {
        return this.fileSystem.open(new Path(dir + File.separator + name));
    }

    @Override
    public void makeDir(String dir) throws Exception {
        this.fileSystem.mkdirs(new Path(dir));
    }

    @Override
    public void deleteDir(String dir) throws Exception {
        this.fileSystem.delete(new Path(dir), true);
    }
}
