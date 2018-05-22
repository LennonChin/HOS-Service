package com.coderap.hos.server.service.impl;

import com.coderap.hos.common.HOSObject;
import com.coderap.hos.common.HOSObjectSummary;
import com.coderap.hos.common.ObjectListResult;
import com.coderap.hos.common.ObjectMetaData;
import com.coderap.hos.common.util.JsonUtil;
import com.coderap.hos.core.ErrorCode;
import com.coderap.hos.core.usermanager.HOSUserManagerException;
import com.coderap.hos.server.HOSConst;
import com.coderap.hos.server.HOSServerException;
import com.coderap.hos.server.service.HDFSService;
import com.coderap.hos.server.service.HOSStoreService;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.io.ByteBufferInputStream;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * @program: HOS-Service
 * @description: 操作Bucket和Object的service类
 * @author: Lennon Chin
 * @create: 2018/05/22 20:34:26
 */
@Slf4j
public class HOSStoreServiceImpl implements HOSStoreService {

    private Connection connection = null; // HBase连接
    private HDFSService hdfsService; // HDFS连接
    private String zookeeperUrls; // zk urls
    private CuratorFramework zookeeperClient; // zk client

    public HOSStoreServiceImpl(Connection connection, HDFSService hdfsService, String zookeeperUrls) {
        this.connection = connection;
        this.hdfsService = hdfsService;
        this.zookeeperUrls = zookeeperUrls;
        // 构造并启动Zookeeper的连接
        this.zookeeperClient = CuratorFrameworkFactory.newClient(zookeeperUrls, new ExponentialBackoffRetry(20, 5));
        this.zookeeperClient.start();
    }

    @Override
    public void createBucketStore(String bucket) throws Exception {
        // 创建目录表
        HBaseServiceImpl.createTable(this.connection, HOSConst.getDirTableName(bucket), HOSConst.getDirColumnFamilies(), null);
        // 创建文件表
        HBaseServiceImpl.createTable(this.connection, HOSConst.getObjTableName(bucket), HOSConst.getObjColumnFamilies(), HOSConst.OBJ_REGIONS);

        // 将其添加到seq表
        Put put = new Put(bucket.getBytes());
        put.addColumn(HOSConst.BUCKET_DIR_SEQ_COLUMN_FAMILY_BYTES, HOSConst.BUCKET_DIR_SEQ_COLUMN_QUALIFIER_BYTES, Bytes.toBytes(0L));
        HBaseServiceImpl.putRow(this.connection, HOSConst.BUCKET_DIR_SEQ_TABLE, put);
        // 创建HDFS目录
        this.hdfsService.makeDir(HOSConst.FILE_STORE_ROOT + File.separator + bucket);
    }

    @Override
    public void deleteBucketStore(String bucket) throws Exception {
        // 删除目录表和文件表
        HBaseServiceImpl.deleteTable(this.connection, HOSConst.getDirTableName(bucket));
        HBaseServiceImpl.deleteTable(this.connection, HOSConst.getObjTableName(bucket));
        // 删除seq表中的记录
        HBaseServiceImpl.deleteRow(this.connection, HOSConst.BUCKET_DIR_SEQ_TABLE, bucket);
        // 删除HDFS上的目录
        this.hdfsService.deleteDir(HOSConst.FILE_STORE_ROOT + File.separator + bucket);
    }

    @Override
    public void createSeqTable() throws Exception {
        HBaseServiceImpl.createTable(this.connection, HOSConst.BUCKET_DIR_SEQ_TABLE, new String[]{HOSConst.BUCKET_DIR_SEQ_COLUMN_FAMILY}, null);
    }

    @Override
    public void put(String bucket, String key, ByteBuffer content, long length, String mediaType, Map<String, String> properties) throws Exception {
        InterProcessMutex lock;
        // 判断是否是创建目录
        if (key.startsWith(File.separator)) {
            putDir(bucket, key);
            return;
        }
        // 获取SequenceID
        String dir = key.substring(0, key.lastIndexOf(File.separator) + 1);
        String hash = null;
        while (hash == null) {
            if (dirExist(bucket, dir)) {
                hash = putDir(bucket, dir);
            } else {
                hash = getDirSequenceId(bucket, dir);
            }
        }
        // 上传文件到文件表（去目录表获取SequenceID）
        // 获取锁
        String lockey = key.replace(File.separator, "_");
        lock = new InterProcessMutex(zookeeperClient, File.separator + "hos" + File.separator + bucket + File.separator + lockey);
        lock.acquire();
        // 上传文件
        String fileKey = hash + "_" + key.substring(key.lastIndexOf(File.separator) + 1);
        Put contentPut = new Put(fileKey.getBytes());
        if (!StringUtils.isEmpty(mediaType)) {
            contentPut.addColumn(HOSConst.OBJ_META_COLUMN_FAMILY_BYTES, HOSConst.OBJ_MEDIATYPE_QUALIFIER, mediaType.getBytes());
        }
        // TODO 添加文件信息
        // 判断文件大小
        if (length <= HOSConst.FILE_STORE_THRESHOLD) {
            // 直接放入HBase
            ByteBuffer byteBuffer = ByteBuffer.wrap(HOSConst.OBJ_CONTENT_QUALIFIER);
            contentPut.addColumn(HOSConst.OBJ_CONTENT_COLUMN_FAMILY_BYTES, byteBuffer, System.currentTimeMillis(), content);
            byteBuffer.clear();
        } else {
            // 放入HDFS
            String fileDir = HOSConst.FILE_STORE_ROOT + File.separator + bucket + File.separator + hash;
            String name = key.substring(key.lastIndexOf(File.separator) + 1);
            InputStream inputStream = new ByteBufferInputStream(content);
            hdfsService.saveFile(fileDir, name, inputStream, length, (short)1);
        }
        HBaseServiceImpl.putRow(connection, HOSConst.getObjTableName(bucket), contentPut);
        // 释放锁
        if (lock != null) {
            lock.release();
        }
    }

    @Override
    public HOSObjectSummary getSummary(String bucket, String key) throws Exception {
        // 判断是否为文件夹
        if (key.endsWith(File.separator)) {
            Result result = HBaseServiceImpl.getRow(connection, HOSConst.getObjTableName(bucket), key);
            if (result != null) {
                // 读取目录的基础属性并转换为HOSObjectSummary
                return this.dirObjectToSummary(result, bucket, key);
            }
            return null;
        }
        // 获取文件属性
        String dir = key.substring(0, key.lastIndexOf(File.separator) + 1);
        String sequenceId = getDirSequenceId(bucket, dir);
        if (sequenceId == null) {
            return null;
        }
        String objKey = sequenceId + "_" + key.substring(key.lastIndexOf(File.separator) + 1);
        Result result = HBaseServiceImpl.getRow(connection, HOSConst.getObjTableName(bucket), objKey);
        if (result == null) {
            return null;
        }
        return this.resultObjectToSummary(result, bucket, dir);
    }

    @Override
    public List<HOSObjectSummary> getSummaries(String bucket, String key, String startKey, String endKey) throws Exception {
        String dir1 = startKey.substring(0, startKey.lastIndexOf(File.separator) + 1).trim();
        if (dir1.length() == 0) {
            dir1 = File.separator;
        }
        String dir2 = endKey.substring(0, startKey.lastIndexOf(File.separator) + 1).trim();
        if (dir2.length() == 0) {
            dir2 = File.separator;
        }
        String name1 = startKey.substring(startKey.lastIndexOf(File.separator) + 1);
        String name2 = endKey.substring(startKey.lastIndexOf(File.separator) + 1);
        String seqId = this.getDirSequenceId(bucket, dir1);
        //查询dir1中大于name1的全部文件
        List<HOSObjectSummary> keys = new ArrayList<>();
        if (seqId != null && name1.length() > 0) {
            byte[] max = Bytes.createMaxByteArray(100);
            byte[] tail = Bytes.add(Bytes.toBytes(seqId), max);
            if (dir1.equals(dir2)) {
                tail = (seqId + "_" + name2).getBytes();
            }
            byte[] start = (seqId + "_" + name1).getBytes();
            ResultScanner scanner1 = HBaseServiceImpl
                    .getScanner(connection, HOSConst.getObjTableName(bucket), start, tail);
            Result result = null;
            while ((result = scanner1.next()) != null) {
                HOSObjectSummary summary = this.resultObjectToSummary(result, bucket, dir1);
                keys.add(summary);
            }
            if (scanner1 != null) {
                scanner1.close();
            }
        }
        //startkey~endkey之间的全部目录
        ResultScanner scanner2 = HBaseServiceImpl
                .getScanner(connection, HOSConst.getDirTableName(bucket), startKey, endKey);
        Result result = null;
        while ((result = scanner2.next()) != null) {
            String seqId2 = Bytes.toString(result.getValue(HOSConst.DIR_META_COLUMN_FAMILY_BYTES,
                    HOSConst.DIR_SEQID_QUALIFIER));
            if (seqId2 == null) {
                continue;
            }
            String dir = Bytes.toString(result.getRow());
            keys.add(dirObjectToSummary(result, bucket, dir));
            getDirAllFiles(bucket, dir, seqId2, keys, endKey);
        }
        if (scanner2 != null) {
            scanner2.close();
        }
        Collections.sort(keys);
        return keys;
    }

    @Override
    public ObjectListResult listDir(String bucket, String dir, String startKey, int maxCount) throws Exception {
        // 查询目录表
        startKey = Strings.nullToEmpty(startKey);
        Get get = new Get(Bytes.toBytes(dir));
        get.addFamily(HOSConst.DIR_SUB_COLUMN_FAMILY_BYTES);
        if (!StringUtils.isEmpty(startKey)) {
            get.setFilter(new QualifierFilter(CompareFilter.CompareOp.GREATER_OR_EQUAL, new BinaryComparator(Bytes.toBytes(startKey))));
        }
        Result dirResult = HBaseServiceImpl.getRow(connection, HOSConst.getDirTableName(bucket), get);
        List<HOSObjectSummary> subDirs = null;
        if (!dirResult.isEmpty()) {
            subDirs = new ArrayList<>();
            for (Cell cell : dirResult.rawCells()) {
                HOSObjectSummary hosObjectSummary = new HOSObjectSummary();
                byte[] qualifierBytes = new byte[cell.getQualifierLength()];
                CellUtil.copyQualifierTo(cell, qualifierBytes, 0);
                String name = Bytes.toString(qualifierBytes);
                hosObjectSummary.setKey(dir + name + File.separator);
                hosObjectSummary.setName(name);
                hosObjectSummary.setLastModifyTime(cell.getTimestamp());
                hosObjectSummary.setMediaType("");
                hosObjectSummary.setBucket(bucket);
                hosObjectSummary.setLength(0);
                subDirs.add(hosObjectSummary);
                if (subDirs.size() > maxCount + 1) {
                    break;
                }
            }
        }
        // 查询文件表
        String sequenceId = getDirSequenceId(bucket, dir);
        byte[] objStart = Bytes.toBytes(sequenceId + "_" + startKey);
        Scan objScan = new Scan();
        objScan.setStartRow(objStart);
        objScan.setRowPrefixFilter(Bytes.toBytes(sequenceId + "_"));
        objScan.setMaxResultsPerColumnFamily(maxCount + 1);
        objScan.addFamily(HOSConst.OBJ_META_COLUMN_FAMILY_BYTES);
        ResultScanner scanner = HBaseServiceImpl.getScanner(connection, HOSConst.getObjTableName(bucket), objScan);
        List<HOSObjectSummary> objectSummaryList = null;
        Result result = null;
        while (objectSummaryList.size() < maxCount + 2 && (result = scanner.next()) != null) {
            HOSObjectSummary hosObjectSummary = resultObjectToSummary(result, bucket, dir);
            objectSummaryList.add(hosObjectSummary);
        }
        if (scanner != null) {
            scanner.close();
        }
        if (subDirs != null && subDirs.size() > 0) {
            objectSummaryList.addAll(subDirs);
        }
        // 排序并删除多余的object
        Collections.sort(objectSummaryList);
        if (objectSummaryList.size() > maxCount) {
            objectSummaryList = objectSummaryList.subList(0, maxCount);
        }
        ObjectListResult listResult = new ObjectListResult();
        HOSObjectSummary nextMarkerObj = objectSummaryList.size() > maxCount ? objectSummaryList.get(objectSummaryList.size() - 1) : null;
        if (nextMarkerObj != null) {
            listResult.setNextMarker(nextMarkerObj.getKey());
        }
        listResult.setMaxKeyNumber(maxCount);
        if (objectSummaryList.size() > 0) {
            listResult.setMinKey(objectSummaryList.get(0).getKey());
            listResult.setMaxKey(objectSummaryList.get(objectSummaryList.size() - 1).getKey());
        }
        listResult.setObjectCount(objectSummaryList.size());
        listResult.setObjectSummaryList(objectSummaryList);
        listResult.setBucket(bucket);
        return listResult;
    }

    @Override
    public ObjectListResult listDirByPrefix(String bucket, String dir, String startKey, String keyPrefix, int maxCount) throws Exception {
        if (startKey == null) {
            startKey = "";
        }
        FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
        filterList.addFilter(new ColumnPrefixFilter(keyPrefix.getBytes()));
        if (startKey.length() > 0) {
            filterList.addFilter(new QualifierFilter(CompareFilter.CompareOp.GREATER_OR_EQUAL,
                    new BinaryComparator(Bytes.toBytes(startKey))));
        }
        int maxCount1 = maxCount + 2;
        Result dirResult = HBaseServiceImpl
                .getRow(connection, HOSConst.getDirTableName(bucket), dir, filterList);
        List<HOSObjectSummary> subDirs = null;
        if (!dirResult.isEmpty()) {
            subDirs = new ArrayList<>();
            for (Cell cell : dirResult.rawCells()) {
                HOSObjectSummary summary = new HOSObjectSummary();
                byte[] qualifierBytes = new byte[cell.getQualifierLength()];
                CellUtil.copyQualifierTo(cell, qualifierBytes, 0);
                String name = Bytes.toString(qualifierBytes);
                summary.setKey(dir + name + "/");
                summary.setName(name);
                summary.setLastModifyTime(cell.getTimestamp());
                summary.setMediaType("");
                summary.setBucket(bucket);
                summary.setLength(0);
                subDirs.add(summary);
                if (subDirs.size() >= maxCount1) {
                    break;
                }
            }
        }

        String dirSeq = this.getDirSequenceId(bucket, dir);
        byte[] objStart = Bytes.toBytes(dirSeq + "_" + startKey);
        Scan objScan = new Scan();
        objScan.setRowPrefixFilter(Bytes.toBytes(dirSeq + "_" + keyPrefix));
        objScan.setFilter(new PageFilter(maxCount + 1));
        objScan.setStartRow(objStart);
        objScan.setMaxResultsPerColumnFamily(maxCount1);
        objScan.addFamily(HOSConst.OBJ_META_COLUMN_FAMILY_BYTES);
        log.info("scan start: " + Bytes.toString(objStart) + " - ");
        ResultScanner objScanner = HBaseServiceImpl
                .getScanner(connection, HOSConst.getObjTableName(bucket), objScan);
        List<HOSObjectSummary> objectSummaryList = new ArrayList<>();
        Result result = null;
        while (objectSummaryList.size() < maxCount1 && (result = objScanner.next()) != null) {
            HOSObjectSummary summary = this.resultObjectToSummary(result, bucket, dir);
            objectSummaryList.add(summary);
        }
        if (objScanner != null) {
            objScanner.close();
        }
        log.info("scan complete: " + Bytes.toString(objStart) + " - ");
        if (subDirs != null && subDirs.size() > 0) {
            objectSummaryList.addAll(subDirs);
        }
        Collections.sort(objectSummaryList);
        ObjectListResult listResult = new ObjectListResult();
        HOSObjectSummary nextMarkerObj =
                objectSummaryList.size() > maxCount ? objectSummaryList.get(objectSummaryList.size() - 1)
                        : null;
        if (nextMarkerObj != null) {
            listResult.setNextMarker(nextMarkerObj.getKey());
        }
        if (objectSummaryList.size() > maxCount) {
            objectSummaryList = objectSummaryList.subList(0, maxCount);
        }
        listResult.setMaxKeyNumber(maxCount);
        if (objectSummaryList.size() > 0) {
            listResult.setMinKey(objectSummaryList.get(0).getKey());
            listResult.setMaxKey(objectSummaryList.get(objectSummaryList.size() - 1).getKey());
        }
        listResult.setObjectCount(objectSummaryList.size());
        listResult.setObjectSummaryList(objectSummaryList);
        listResult.setBucket(bucket);

        return listResult;
    }

    @Override
    public HOSObject getObject(String bucket, String key) throws Exception {
        // 判断是否为目录
        if (key.endsWith(File.separator)) {
            // 是目录
            Result result = HBaseServiceImpl.getRow(connection, HOSConst.getDirTableName(bucket), key);
            if (result == null) {
                return null;
            }
            ObjectMetaData objectMetaData = new ObjectMetaData();
            objectMetaData.setBucket(bucket);
            objectMetaData.setKey(key);
            objectMetaData.setLength(0);
            objectMetaData.setLastModifyTime(result.rawCells()[0].getTimestamp());
            HOSObject hosObject = new HOSObject();
            hosObject.setMetaData(objectMetaData);
            return hosObject;
        }
        // 获取文件属性
        String dir = key.substring(0, key.lastIndexOf(File.separator) + 1);
        String sequenceId = getDirSequenceId(bucket, dir);
        if (sequenceId == null) {
            return null;
        }
        String objKey = sequenceId + "_" + key.substring(key.lastIndexOf(File.separator) + 1);
        Result result = HBaseServiceImpl.getRow(connection, HOSConst.getObjTableName(bucket), objKey);
        if (result.isEmpty()) {
            return null;
        }
        HOSObject hosObject = new HOSObject();
        ObjectMetaData objectMetaData = new ObjectMetaData();
        long length = Bytes.toLong(result.getValue(HOSConst.OBJ_META_COLUMN_FAMILY_BYTES, HOSConst.OBJ_LENGTH_QUALIFIER));
        objectMetaData.setLength(length);
        objectMetaData.setBucket(bucket);
        objectMetaData.setKey(key);
        objectMetaData.setLastModifyTime(result.rawCells()[0].getTimestamp());
        objectMetaData.setMediaType(Bytes.toString(result.getValue(HOSConst.OBJ_META_COLUMN_FAMILY_BYTES, HOSConst.OBJ_MEDIATYPE_QUALIFIER)));
        byte[] attrs = result.getValue(HOSConst.OBJ_META_COLUMN_FAMILY_BYTES, HOSConst.OBJ_PROPERTY_QUALIFIER);
        if (attrs != null) {
            objectMetaData.setAttrs(JsonUtil.fromJson(Map.class, Bytes.toString(attrs)));
        }
        hosObject.setMetaData(objectMetaData);
        // 读取文件内容
        if (result.containsNonEmptyColumn(HOSConst.OBJ_CONTENT_COLUMN_FAMILY_BYTES, HOSConst.OBJ_CONTENT_QUALIFIER)) {
            // 从HBase中读取
            ByteArrayInputStream inputStream = new ByteArrayInputStream(result.getValue(HOSConst.OBJ_CONTENT_COLUMN_FAMILY_BYTES, HOSConst.OBJ_CONTENT_QUALIFIER));
            hosObject.setContent(inputStream);
        } else {
            // 从HDFS中读取
            String fileDir = HOSConst.FILE_STORE_ROOT + File.separator + bucket + File.separator + sequenceId;
            InputStream inputStream = this.hdfsService.openFile(fileDir, key.substring(key.lastIndexOf(File.separator)));
            hosObject.setContent(inputStream);
        }
        return hosObject;
    }

    @Override
    public void deleteObject(String bucket, String key) throws Exception {
        // 判断是否为目录
        if (key.endsWith(File.separator)) {
            // 判断目录是否为空
            if (!isDirEmpty(bucket, key)) {
                throw new HOSServerException(ErrorCode.ERROR_PERMISSION_DENIED, "dir is not empty");
            }
            // 获取锁
            InterProcessMutex lock = null;
            String lockey = key.replace(File.separator, "_");
            lock = new InterProcessMutex(zookeeperClient, File.separator + "hos" + File.separator + bucket + File.separator + lockey);
            lock.acquire();
            // 从父目录删除数据
            String dir1 = key.substring(0, key.lastIndexOf(File.separator));
            String name = dir1.substring(key.lastIndexOf(File.separator) + 1);
            if (name.length() > 0) {
                String parentDir = key.substring(0, key.lastIndexOf(name));
                HBaseServiceImpl.deleteColumnQualifier(connection, HOSConst.getDirTableName(bucket), parentDir, HOSConst.DIR_SUB_COLUMN_FAMILY, name);
            }
            // 从目录表中删除
            HBaseServiceImpl.deleteRow(connection, HOSConst.getDirTableName(bucket), key);
            // 释放锁
            lock.release();
            return;
        } else {
            // 获取文件的length，判断是存在HBase还是HDFS
            String parentDir = key.substring(0, key.lastIndexOf(File.separator) + 1);
            String name = key.substring(key.lastIndexOf(File.separator) + 1);
            String seqId = getDirSequenceId(bucket, parentDir);
            String objKey = seqId + "_" + name;
            Get get = new Get(objKey.getBytes());
            // 查询长度
            get.addColumn(HOSConst.OBJ_META_COLUMN_FAMILY_BYTES, HOSConst.OBJ_LENGTH_QUALIFIER);
            Result result = HBaseServiceImpl.getRow(connection, HOSConst.getObjTableName(bucket), get);
            if (result.isEmpty()) {
                return;
            }
            long length = Bytes.toLong(result.getValue(HOSConst.OBJ_META_COLUMN_FAMILY_BYTES, HOSConst.OBJ_LENGTH_QUALIFIER));
            if (length > HOSConst.FILE_STORE_THRESHOLD) {
                // 从HDFS删除
                String fileDir = HOSConst.FILE_STORE_ROOT + File.separator + bucket + File.separator + seqId;
                this.hdfsService.deleteFile(fileDir, name);
            } else {
                // 从HBase删除
                HBaseServiceImpl.deleteRow(connection, HOSConst.getObjTableName(bucket), objKey);
            }
        }
    }

    private boolean dirExist(String bucket, String dir) throws Exception {
        return HBaseServiceImpl.existsRow(this.connection, HOSConst.getDirTableName(bucket), dir);
    }

    private String getDirSequenceId(String bucket, String key) throws Exception {
        Result result = HBaseServiceImpl.getRow(connection, HOSConst.getDirTableName(bucket), key);
        if (result == null) {
            return null;
        }
        return Bytes.toString(result.getValue(HOSConst.DIR_META_COLUMN_FAMILY_BYTES, HOSConst.DIR_SEQID_QUALIFIER));
    }

    private String putDir(String bucket, String key) throws Exception {

        if (dirExist(bucket, key)) {
            return null;
        }

        // 从Zookeeper获取锁
        InterProcessMutex lock = null;
        // 创建目录
        try {
            // 获取锁
            String lockey = key.replace(File.separator, "_");
            lock = new InterProcessMutex(zookeeperClient, File.separator + "hos" + File.separator + bucket + File.separator + lockey);
            // 创建目录
            String dir1 = key.substring(0, key.lastIndexOf(File.separator));
            String name = dir1.substring(dir1.lastIndexOf(File.separator));

            if (name.length() > 0) {
                String parent = dir1.substring(0, dir1.lastIndexOf(File.separator) + 1);
                if (!dirExist(bucket, dir1)) {
                    this.putDir(bucket, parent);
                }
                // 将父目录添加到sub列族内，添加子项
                Put put = new Put(Bytes.toBytes(parent));
                put.addColumn(HOSConst.DIR_SUB_COLUMN_FAMILY_BYTES, Bytes.toBytes(name), Bytes.toBytes(1));
                HBaseServiceImpl.putRow(connection, HOSConst.getDirTableName(bucket), put);
            }

            // 再添加到目录表
            String sequenceId = getDirSequenceId(bucket, key);
            String hash = sequenceId == null ? makeDirSequenceId(bucket) : sequenceId;
            Put dirPut = new Put(key.getBytes());
            dirPut.addColumn(HOSConst.DIR_META_COLUMN_FAMILY_BYTES, HOSConst.DIR_SEQID_QUALIFIER, Bytes.toBytes(hash));
            HBaseServiceImpl.putRow(connection, HOSConst.getDirTableName(bucket), dirPut);
            return hash;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            // 释放锁
            if (lock != null) {
                lock.release();
            }
        }
    }

    private String makeDirSequenceId(String bucket) throws Exception {
        long incrementColumnValue = HBaseServiceImpl.incrementColumnValue(connection, HOSConst.BUCKET_DIR_SEQ_TABLE, bucket, HOSConst.BUCKET_DIR_SEQ_COLUMN_FAMILY_BYTES, HOSConst.BUCKET_DIR_SEQ_COLUMN_QUALIFIER_BYTES, 1);
        return String.format("%d%d", incrementColumnValue % 64, incrementColumnValue);
    }

    /**
     * 从目录结果转换得到HOSObjectSummary
     * @param result
     * @param bucket
     * @param dir
     * @return
     */
    private HOSObjectSummary dirObjectToSummary(Result result, String bucket, String dir) {
        HOSObjectSummary hosObjectSummary = new HOSObjectSummary();
        hosObjectSummary.setId(Bytes.toString(result.getRow()));
        hosObjectSummary.setAttrs(new HashMap<>(0));
        hosObjectSummary.setBucket(bucket);
        hosObjectSummary.setLastModifyTime(result.rawCells()[0].getTimestamp());
        hosObjectSummary.setLength(0);
        hosObjectSummary.setMediaType("");
        if (dir.length() > 1) {
            hosObjectSummary.setName(dir.substring(dir.lastIndexOf(File.separator) + 1));
        } else {
            hosObjectSummary.setName("");
        }
        return hosObjectSummary;
    }

    /**
     * 从文件结果转换得到HOSObjectSummary
     * @param result
     * @param bucket
     * @param dir
     * @return
     */
    private HOSObjectSummary resultObjectToSummary(Result result, String bucket, String dir) {

        HOSObjectSummary hosObjectSummary = new HOSObjectSummary();
        hosObjectSummary.setLastModifyTime(result.rawCells()[0].getTimestamp());
        String id = new String(result.getRow());
        hosObjectSummary.setId(id);
        String name = id.split("_", 2)[1];
        hosObjectSummary.setName(name);
        hosObjectSummary.setKey(dir + name);
        hosObjectSummary.setBucket(bucket);
        hosObjectSummary.setMediaType(Bytes.toString(result.getValue(HOSConst.OBJ_META_COLUMN_FAMILY_BYTES, HOSConst.OBJ_MEDIATYPE_QUALIFIER)));
        // TODO length attrs
        return hosObjectSummary;
    }

    private void getDirAllFiles(String bucket, String dir, String seqId, List<HOSObjectSummary> keys,
                                String endKey) throws IOException {

        byte[] max = Bytes.createMaxByteArray(100);
        byte[] tail = Bytes.add(Bytes.toBytes(seqId), max);
        if (endKey.startsWith(dir)) {
            String endKeyLeft = endKey.replace(dir, "");
            String fileNameMax = endKeyLeft;
            if (endKeyLeft.indexOf("/") > 0) {
                fileNameMax = endKeyLeft.substring(0, endKeyLeft.indexOf("/"));
            }
            tail = Bytes.toBytes(seqId + "_" + fileNameMax);
        }

        Scan scan = new Scan(Bytes.toBytes(seqId), tail);
        scan.setFilter(HOSConst.OBJ_META_SCAN_FILTER);
        ResultScanner scanner = HBaseServiceImpl
                .getScanner(connection, HOSConst.getObjTableName(bucket), scan);
        Result result = null;
        while ((result = scanner.next()) != null) {
            HOSObjectSummary summary = this.resultObjectToSummary(result, bucket, dir);
            keys.add(summary);
        }
        if (scanner != null) {
            scanner.close();
        }
    }

    public boolean isDirEmpty(String bucket, String dir) throws Exception {
        return listDir(bucket, dir, null, 2).getObjectSummaryList().size() == 0;
    }
}
