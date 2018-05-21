package com.coderap.hos.server;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/21 22:52:43
 */
public class HOSConst {

    // 目录表前缀
    final static String DIR_TABLE_PREFIX = "hos_dir_";
    // 对象表前缀
    final static String OBJ_TABLE_PREFIX = "hos_obj_";

    // 目录表meta信息列族名
    final static String DIR_META_COLUMN_FAMILY = "cf";
    final static byte[] DIR_META_COLUMN_FAMILY_BYTES = DIR_META_COLUMN_FAMILY.getBytes();
    // 目录表sub信息列族名
    final static String DIR_SUB_COLUMN_FAMILY = "sub";
    final static byte[] DIR_SUB_COLUMN_FAMILY_BYTES = DIR_SUB_COLUMN_FAMILY.getBytes();

    // 文件表meta信息列族名
    final static String OBJ_META_COLUMN_FAMILY = "cf";
    final static byte[] OBJ_META_COLUMN_FAMILY_BYTES = OBJ_META_COLUMN_FAMILY.getBytes();
    // 文件表content信息列族名
    final static String OBJ_CONTENT_COLUMN_FAMILY = "c";
    final static byte[] OBJ_CONTENT_COLUMN_FAMILY_BYTES = OBJ_CONTENT_COLUMN_FAMILY.getBytes();

    // 目录表seqId列名
    final static byte[] DIR_SEQID_QUALIFIER = "u".getBytes();
    // 文件表content列名
    final static byte[] OBJ_CONTENT_QUALIFIER = "c".getBytes();
    // 文件表length列名
    final static byte[] OBJ_LENGTH_QUALIFIER = "l".getBytes();
    // 文件表property列名
    final static byte[] OBJ_PROPERTY_QUALIFIER = "p".getBytes();
    // 文件表mediatype列名
    final static byte[] OBJ_MEDIATYPE_QUALIFIER = "m".getBytes();

    // 文件根目录
    final static String FILE_STORE_ROOT = "/hos";
    // 文件大小阈值 20M
    final static int FILE_STORE_THRESHOLD = 20 * 1024 * 1024;
    // 存储hbase目录表的seqId的表，协助生成目录的SequenceID的表
    final static String BUCKET_DIR_SEQ_TABLE = "hos_dir_seq";
    final static String BUCKET_DIR_SEQ_COLUMN_FAMILY = "s";
    final static byte[] BUCKET_DIR_SEQ_COLUMN_FAMILY_BYTES = BUCKET_DIR_SEQ_COLUMN_FAMILY.getBytes();
    final static String BUCKET_DIR_QUALIFIER_FAMILY = "s";
    final static byte[] BUCKET_DIR_QUALIFIER_FAMILY_BYTES = BUCKET_DIR_QUALIFIER_FAMILY.getBytes();

    public static String getDirTableName(String bucketName) {
        return DIR_TABLE_PREFIX + bucketName;
    }

    public static String getObjTableName(String bucketName) {
        return OBJ_TABLE_PREFIX + bucketName;
    }

    public static String[] getDirColumnFamilies() {
        return new String[]{DIR_META_COLUMN_FAMILY, DIR_SUB_COLUMN_FAMILY};
    }

    public static String[] getObjColumnFamilies() {
        return new String[]{OBJ_META_COLUMN_FAMILY, OBJ_CONTENT_COLUMN_FAMILY};
    }

}
