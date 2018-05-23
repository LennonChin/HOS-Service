package com.coderap.hos.web.rest;

import com.coderap.hos.common.HOSHeaders;
import com.coderap.hos.common.HOSObject;
import com.coderap.hos.common.HOSObjectSummary;
import com.coderap.hos.common.ObjectListResult;
import com.coderap.hos.core.usermanager.CoreUtil;
import com.coderap.hos.core.usermanager.model.SystemRole;
import com.coderap.hos.core.usermanager.model.UserInfo;
import com.coderap.hos.server.service.BucketService;
import com.coderap.hos.server.service.HOSStoreService;
import com.coderap.hos.web.security.ContextUtil;
import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/23 21:29:27
 */
@RestController
@RequestMapping("hos/v1")
@Slf4j
public class HOSController extends BaseController {

    @Autowired
    @Qualifier("bucketServiceImpl")
    BucketService bucketService;

    @Autowired
    @Qualifier("hosStoreServiceImpl")
    HOSStoreService hosStoreService;

    // 上传文件时内存中能存储的最大大小，超过这个大小会先存放在磁盘
    private static long MAX_FILE_IN_MEMORY = 2 * 1024 * 1024;

    private final int readBufferSize = 32 * 1024;
    private static String TMP_DIR = System.getProperty("user.dir") + File.separator + "tmp";

    private HOSController() {
        // 创建临时目录
        File file = new File(TMP_DIR);
        file.mkdirs();
    }

    // 创建bucket
    @RequestMapping(value = "bucket", method = RequestMethod.POST)
    public Object createBucket(@RequestParam("bucket") String bucketName,
                               @RequestParam(name = "detail", required = false, defaultValue = "") String detail) {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (!currentUser.getSystemRole().equals(SystemRole.VISITOR)) {
            bucketService.addBucket(currentUser, bucketName, detail);
            try {
                hosStoreService.createBucketStore(bucketName);
            } catch (Exception e) {
                // 创建bucket store如果失败需要将数据库中的记录删除
                bucketService.deleteBucket(bucketName);
                return "error";
            }
            return "success";
        }
        return "permission denied";
    }

    // 删除bucket
    @RequestMapping(value = "bucket", method = RequestMethod.DELETE)
    public Object deleteBucket(@RequestParam("bucket") String bucketName) {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkBucketOwner(bucketName, currentUser.getUserName())) {
            try {
                hosStoreService.deleteBucketStore(bucketName);
            } catch (Exception e) {
                e.printStackTrace();
                return "error";
            }
            bucketService.deleteBucket(bucketName);
            return "success";
        }
        return "permission denied";
    }

    // 获取bucket列表
    @RequestMapping(value = "bucket/list", method = RequestMethod.GET)
    public Object getBuckets() {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        return bucketService.getUserAuthorizedBuckets(currentUser.getUserId());
    }

    // 上传文件
    @RequestMapping(value = "object", method = {RequestMethod.PUT, RequestMethod.POST})
    @ResponseBody
    public Object putObject(@RequestParam("bucket") String bucket,
                            @RequestParam("key") String key,
                            @RequestParam(value = "mediaType", required = false) String mediaType,
                            @RequestParam(value = "content", required = false) MultipartFile file,
                            HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (!operationAccessControl.checkPermission(currentUser.getUserId(), bucket)) {
            response.setStatus(HttpStatus.SC_FORBIDDEN);
            response.getWriter().write("Permission denied");
            return "Permission denied";
        }
        if (!key.startsWith(File.separator)) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            response.getWriter().write("object key must start with /");
        }

        Enumeration<String> headNames = request.getHeaderNames();
        Map<String, String> attrs = new HashMap<>();
        String contentEncoding = request.getHeader("content-encoding");
        if (contentEncoding != null) {
            attrs.put("content-encoding", contentEncoding);
        }
        while (headNames.hasMoreElements()) {
            String header = headNames.nextElement();
            if (header.startsWith(HOSHeaders.COMMON_ATTR_PREFIX)) {
                attrs.put(header.replace(HOSHeaders.COMMON_ATTR_PREFIX, ""), request.getHeader(header));
            }
        }
        ByteBuffer buffer = null;
        File distFile = null;
        try {
            //put dir object
            if (key.endsWith(File.separator)) {
                if (file != null) {
                    response.setStatus(HttpStatus.SC_BAD_REQUEST);
                    file.getInputStream().close();
                    return null;
                }
                hosStoreService.put(bucket, key, null, 0, mediaType, attrs);
                response.setStatus(HttpStatus.SC_OK);
                return "success";
            }
            if (file == null || file.getSize() == 0) {
                response.setStatus(HttpStatus.SC_BAD_REQUEST);
                response.getWriter().write("object content could not be empty");
                return "object content could not be empty";
            }

            if (file != null) {
                if (file.getSize() > MAX_FILE_IN_MEMORY) {
                    distFile = new File(TMP_DIR + File.separator + UUID.randomUUID().toString());
                    file.transferTo(distFile);
                    file.getInputStream().close();
                    buffer = new FileInputStream(distFile).getChannel()
                            .map(FileChannel.MapMode.READ_ONLY, 0, file.getSize());
                } else {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    org.apache.commons.io.IOUtils.copy(file.getInputStream(), outputStream);
                    buffer = ByteBuffer.wrap(outputStream.toByteArray());
                    file.getInputStream().close();
                }
            }
            hosStoreService.put(bucket, key, buffer, file.getSize(), mediaType, attrs);
            return "success";
        } catch (Exception e) {
            log.error(e.toString());
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("server error");
            return "server error";
        } finally {
            if (buffer != null) {
                buffer.clear();
            }
            if (file != null) {
                try {
                    file.getInputStream().close();
                } catch (Exception e) {
                    //nothing to do
                }
            }
            if (distFile != null) {
                distFile.delete();
            }
        }
    }

    // 列出目录下的文件
    @RequestMapping(value = "object/list", method = RequestMethod.GET)
    public ObjectListResult listObject(@RequestParam("bucket") String bucket,
                                       @RequestParam("startKey") String startKey,
                                       @RequestParam("endKey") String endKey,
                                       HttpServletResponse response)
            throws Exception {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (!operationAccessControl.checkPermission(currentUser.getUserId(), bucket)) {
            response.setStatus(HttpStatus.SC_FORBIDDEN);
            response.getWriter().write("Permission denied");
            return null;
        }
        if (startKey.compareTo(endKey) > 0) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return null;
        }
        ObjectListResult result = new ObjectListResult();
        List<HOSObjectSummary> summaryList = hosStoreService.list(bucket, startKey, endKey);
        result.setBucket(bucket);
        if (summaryList.size() > 0) {
            result.setMaxKey(summaryList.get(summaryList.size() - 1).getKey());
            result.setMinKey(summaryList.get(0).getKey());
        }
        result.setObjectCount(summaryList.size());
        result.setObjectSummaryList(summaryList);
        return result;
    }

    @RequestMapping(value = "object/info", method = RequestMethod.GET)
    public HOSObjectSummary getSummary(String bucket, String key, HttpServletResponse response)
            throws Exception {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (!operationAccessControl.checkPermission(currentUser.getUserId(), bucket)) {
            response.setStatus(HttpStatus.SC_FORBIDDEN);
            response.getWriter().write("Permission denied");
            return null;
        }
        HOSObjectSummary summary = hosStoreService.getSummary(bucket, key);
        if (summary == null) {
            response.setStatus(HttpStatus.SC_NOT_FOUND);
        }
        return summary;
    }

    @RequestMapping(value = "object/list/prefix", method = RequestMethod.GET)
    public ObjectListResult listObjectByPrefix(@RequestParam("bucket") String bucket,
                                               @RequestParam("dir") String dir,
                                               @RequestParam("prefix") String prefix,
                                               @RequestParam(value = "startKey", required = false, defaultValue = "") String start,
                                               HttpServletResponse response)
            throws Exception {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (!operationAccessControl.checkPermission(currentUser.getUserId(), bucket)) {
            response.setStatus(HttpStatus.SC_FORBIDDEN);
            response.getWriter().write("Permission denied");
            return null;
        }
        if (!dir.startsWith(File.separator) || !dir.endsWith(File.separator)) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            response.getWriter().write("dir must start with / and end with /");
            return null;
        }
        if ("".equals(start) || start.equals(File.separator)) {
            start = null;
        }
        if (start != null) {
            List<String> segs = StreamSupport.stream(Splitter
                    .on(File.separator)
                    .trimResults()
                    .omitEmptyStrings().split(start).spliterator(), false).collect(Collectors.toList());
            start = segs.get(segs.size() - 1);
        }
        ObjectListResult result = this.hosStoreService.listDirByPrefix(bucket, dir, prefix, start, 100);
        return result;
    }

    @RequestMapping(value = "object/list/dir", method = RequestMethod.GET)
    public ObjectListResult listObjectByDir(@RequestParam("bucket") String bucket,
                                            @RequestParam("dir") String dir,
                                            @RequestParam(value = "startKey", required = false, defaultValue = "") String start,
                                            HttpServletResponse response)
            throws Exception {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (!operationAccessControl.checkPermission(currentUser.getUserId(), bucket)) {
            response.setStatus(HttpStatus.SC_FORBIDDEN);
            response.getWriter().write("Permission denied");
            return null;
        }
        if (!dir.startsWith(File.separator) || !dir.endsWith(File.separator)) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            response.getWriter().write("dir must start with / and end with /");
            return null;
        }
        if ("".equals(start) || start.equals(File.separator)) {
            start = null;
        }
        if (start != null) {
            List<String> segs = StreamSupport.stream(Splitter
                    .on(File.separator)
                    .trimResults()
                    .omitEmptyStrings().split(start).spliterator(), false).collect(Collectors.toList());
            start = segs.get(segs.size() - 1);
        }

        ObjectListResult result = this.hosStoreService.listDir(bucket, dir, start, 100);
        return result;
    }

    // 删除文件
    @RequestMapping(value = "object", method = RequestMethod.DELETE)
    public Object deleteObject(@RequestParam("bucket") String bucket,
                               @RequestParam("key") String key) throws Exception {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (!operationAccessControl.checkPermission(currentUser.getUserId(), bucket)) {
            return  "PERMISSION DENIED";
        }
        this.hosStoreService.deleteObject(bucket, key);
        return "success";
    }

    // 下载文件
    @RequestMapping(value = "object/content", method = RequestMethod.GET)
    public void getObject(@RequestParam("bucket") String bucket,
                          @RequestParam("key") String key, HttpServletRequest request,
                          HttpServletResponse response) throws Exception {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (!operationAccessControl.checkPermission(currentUser.getUserId(), bucket)) {
            response.setStatus(HttpStatus.SC_FORBIDDEN);
            response.getWriter().write("Permission denied");
            return;
        }
        HOSObject object = this.hosStoreService.getObject(bucket, key);
        if (object == null) {
            response.setStatus(HttpStatus.SC_NOT_FOUND);
            return;
        }
        response.setHeader(HOSHeaders.COMMON_OBJ_BUCKET, bucket);
        response.setHeader(HOSHeaders.COMMON_OBJ_KEY, key);
        response.setHeader(HOSHeaders.RESPONSE_OBJ_LENGTH, "" + object.getMetaData().getLength());
        String iflastModify = request.getHeader("If-Modified-Since");
        String lastModify = object.getMetaData().getLastModifyTime() + "";
        response.setHeader("Last-Modified", lastModify);
        String contentEncoding = object.getMetaData().getContentEncoding();
        if (contentEncoding != null) {
            response.setHeader("content-encoding", contentEncoding);
        }
        if (iflastModify != null && iflastModify.equals(lastModify)) {
            response.setStatus(HttpStatus.SC_NOT_MODIFIED);
            return;
        }
        response.setHeader(HOSHeaders.COMMON_OBJ_BUCKET, object.getMetaData().getBucket());
        response.setContentType(object.getMetaData().getMediaType());
        OutputStream outputStream = response.getOutputStream();
        InputStream inputStream = object.getContent();
        try {
            byte[] buffer = new byte[readBufferSize];
            int len = -1;
            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
            response.flushBuffer();
        } finally {
            inputStream.close();
            outputStream.close();
        }

    }


}
