package com.coderap.hos.server.service.impl;

import com.coderap.hos.core.ErrorCode;
import com.coderap.hos.server.HOSServerException;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.FilterList;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * @program: HOS-Service
 * @description: HBase操作service
 * @author: Lennon Chin
 * @create: 2018/05/21 23:39:34
 */
public interface HBaseServiceImpl {

    /**
     * 创建表
     * @param connection 连接
     * @param tableName 表名
     * @param columnFamilies 列族
     * @param splitKeys 预先分区，用于分区的键
     * @return
     */
    public static boolean createTable(Connection connection, String tableName, String[] columnFamilies, byte[][] splitKeys) {

        try (HBaseAdmin admin = (HBaseAdmin)connection.getAdmin()) {

            if (admin.tableExists(tableName)) {
                // 表存在
                return false;
            }

            // 创建列族
            HTableDescriptor hTableDescriptor = new HTableDescriptor(TableName.valueOf(tableName));
            Arrays.stream(columnFamilies).forEach(columnFamily -> {
                HColumnDescriptor columnDescriptor = new HColumnDescriptor(columnFamily);
                columnDescriptor.setMaxVersions(1);
                hTableDescriptor.addFamily(columnDescriptor);
            });

            // 创建表
            admin.createTable(hTableDescriptor, splitKeys);

        } catch (Exception e) {
            e.printStackTrace();
            throw new HOSServerException(ErrorCode.ERROR_HBASE, "Create table " + tableName + " error");
        }
        return true;
    }

    /**
     * 删除表操作
     * @param connection 连接
     * @param tableName 表名
     * @return
     */
    public static boolean deleteTable(Connection connection, String tableName) {

        try(HBaseAdmin admin = (HBaseAdmin)connection.getAdmin()) {

            // 删除表
            admin.disableTable(tableName);
            admin.deleteTable(tableName);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            throw new HOSServerException(ErrorCode.ERROR_HBASE, "delete table " + tableName + " error");
        }
    }

    public static boolean deleteColumnFamily(Connection connection, String tableName, String columnFamily) {

        try(HBaseAdmin admin = (HBaseAdmin)connection.getAdmin()) {

            // 删除列族
            admin.deleteColumn(tableName, columnFamily);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            throw new HOSServerException(ErrorCode.ERROR_HBASE, "delete column family " + columnFamily + " in table " + tableName + " error");
        }
    }

    /**
     * 删除某条数据的某列
     * @param connection 连接
     * @param tableName 表名
     * @param rowKey
     * @param columnFamily
     * @param columnQulifier
     * @return
     */
    public static boolean deleteColumnQualifier(Connection connection, String tableName, String rowKey, String columnFamily, String columnQulifier) {

        try(Table table = connection.getTable(TableName.valueOf(tableName))) {

            Delete delete = new Delete(rowKey.getBytes());
            delete.addColumn(columnFamily.getBytes(), columnQulifier.getBytes());
            return deleteRow(connection, tableName, delete);

        } catch (Exception e) {
            e.printStackTrace();
            throw new HOSServerException(ErrorCode.ERROR_HBASE, "delete column qualifier " + columnQulifier + " of column family " + columnFamily + " in table " + tableName + " error");
        }
    }

    /**
     * 根据delete进行数据删除
     * @param connection
     * @param tableName
     * @param delete
     * @return
     */
    public static boolean deleteRow(Connection connection, String tableName, Delete delete) {

        try(Table table = connection.getTable(TableName.valueOf(tableName))) {

            table.delete(delete);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            throw new HOSServerException(ErrorCode.ERROR_HBASE, "delete column qualifier error");
        }
    }

    /**
     * 删除行数据
     * @param connection
     * @param tableName
     * @param rowKey
     * @return
     */
    public static boolean deleteRow(Connection connection, String tableName, String rowKey) {
        Delete delete = new Delete(rowKey.getBytes());
        return deleteRow(connection, tableName, delete);
    }

    /**
     * 根据Get对象读取行数据
     * @param connection
     * @param tableName
     * @param get
     * @return
     */
    public static Result getRow(Connection connection, String tableName, Get get) {

        try(Table table = connection.getTable(TableName.valueOf(tableName))) {
            return table.get(get);
        } catch (Exception e) {
            e.printStackTrace();
            throw new HOSServerException(ErrorCode.ERROR_HBASE, "get row error");
        }
    }

    /**
     * 根据rowkey获取行数据
     * @param connection
     * @param tableName
     * @param rowKey
     * @return
     */
    public static Result getRow(Connection connection, String tableName, String rowKey) {

        Get get = new Get(rowKey.getBytes());
        return getRow(connection, tableName, get);
    }

    /**
     * 获取scanner对象
     * @param connection
     * @param tableName
     * @param scan
     * @return
     */
    public static ResultScanner getScanner(Connection connection, String tableName, Scan scan) {

        try(Table table = connection.getTable(TableName.valueOf(tableName))) {
            return table.getScanner(scan);
        } catch (Exception e) {
            e.printStackTrace();
            throw new HOSServerException(ErrorCode.ERROR_HBASE, "get row error");
        }
    }

    /**
     * 根据起始rowkey，filter来获取scanner
     * @param connection
     * @param tableName
     * @param startKey
     * @param endKey
     * @param filterList
     * @return
     */
    public static ResultScanner getScanner(Connection connection, String tableName, String startKey, String endKey, FilterList filterList) {

        Scan scan = new Scan();
        scan.setStartRow(startKey.getBytes());
        scan.setStopRow(endKey.getBytes());
        scan.setFilter(filterList);
        scan.setCaching(1000);
        return getScanner(connection, tableName, scan);
    }

    /**
     * 根据put对象添加行
     * @param connection
     * @param tableName
     * @param put
     * @return
     */
    public static boolean putRow(Connection connection, String tableName, Put put) {
        try(Table table = connection.getTable(TableName.valueOf(tableName))) {
            table.put(put);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new HOSServerException(ErrorCode.ERROR_HBASE, "put row error");
        }
    }

    /**
     * 根据列族、列、数据来插入行
     * @param connection
     * @param tableName
     * @param rowKey
     * @param columnFamily
     * @param columnQuanlifier
     * @param data
     * @return
     */
    public static boolean putRow(Connection connection, String tableName, String rowKey, String columnFamily, String columnQuanlifier, String data) {
        Put put = new Put(rowKey.getBytes());
        put.addColumn(columnFamily.getBytes(), columnQuanlifier.getBytes(), data.getBytes());
        return putRow(connection, tableName, put);
    }

    /**
     * 批量插入行数据
     * @param connection
     * @param tableName
     * @param putList
     * @return
     */
    public static boolean batchPutRows(Connection connection, String tableName, List<Put> putList) {
        try(Table table = connection.getTable(TableName.valueOf(tableName))) {
            table.put(putList);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new HOSServerException(ErrorCode.ERROR_HBASE, "put row error");
        }
    }

    /**
     * 通过这个方法生成目录的SequenceID
     * @param connection
     * @param tableName
     * @param row
     * @param columnFamily
     * @param columnQuanlifier
     * @param number
     * @return
     */
    public static long incrementColumnValue(Connection connection, String tableName, String row, String columnFamily, String columnQuanlifier, int number) {
        try(Table table = connection.getTable(TableName.valueOf(tableName))) {
            return table.incrementColumnValue(row.getBytes(), columnFamily.getBytes(), columnQuanlifier.getBytes(), number);
        } catch (Exception e) {
            e.printStackTrace();
            throw new HOSServerException(ErrorCode.ERROR_HBASE, "put row error");
        }
    }
}
