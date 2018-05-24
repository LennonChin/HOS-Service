package com.coderap.hos.sdk.service.impl;

import com.coderap.hos.common.*;
import com.coderap.hos.common.util.JsonUtil;
import com.coderap.hos.sdk.service.HOSClientService;
import com.coderap.hos.sdk.MimeUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/24 21:03:46
 */
@Slf4j
public class HOSClientServiceImpl implements HOSClientService {

    private String hosServer;
    private String schema;
    private String host;
    private int port = 80;
    private String token;
    private OkHttpClient client;

    public HOSClientServiceImpl(String endPoints, String token) {
        this.hosServer = endPoints;
        String[] split = endPoints.split("://", 2);
        this.schema = split[0];
        String[] split1 = split[1].split(":", 2);
        this.host = split1[0];
        this.port = Integer.parseInt(split1[1]);
        this.token = token;
        ConnectionPool pool = new ConnectionPool(10, 30, TimeUnit.SECONDS);
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(120L, TimeUnit.SECONDS)
                .writeTimeout(120L, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .connectionPool(pool);
        Interceptor interceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                Response response = null;
                boolean success = false;
                int tryCount = 0;
                int maxLimit = 5;
                while (!success && tryCount < maxLimit) {
                    if (tryCount > 0) {
                        log.info("intercept:" + "retry request - " + tryCount);
                    }
                    response = chain.proceed(request);
                    if (response.code() == 404) {
                        break;
                    }
                    success = response.isSuccessful();
                    tryCount++;
                    if (success) {
                        return response;
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return response;
            }
        };
        client = httpClientBuilder.addInterceptor(interceptor).build();
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
    }

    @Override
    public void createBucket(String bucketName) throws Exception {
        this.createBucket(bucketName, "");
    }

    @Override
    public void createBucket(String bucketName, String detail) throws Exception {
        Headers headers = this.buildHeaders(null, token, null);
        RequestBody requestBody = RequestBody.create(null, new byte[0]);
        Request request = new Request.Builder().headers(headers).url(new HttpUrl.Builder().scheme(schema).host(host).port(port).addPathSegment("/hos/v1/bucket").addQueryParameter("bucket", bucketName).addQueryParameter("detail", detail).build()).post(requestBody).build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            String message = response.body().string();
            response.close();
            throw new IOException("create bucket error " + message);
        }
        response.close();
    }

    @Override
    public void deleteBucket(String bucketName) throws Exception {
        Headers headers = this.buildHeaders(null, token, null);
        RequestBody requestBody = RequestBody.create(null, new byte[0]);
        Request request = new Request.Builder().headers(headers).url(new HttpUrl.Builder().scheme(schema).host(host).port(port).addPathSegment("/hos/v1/bucket").addQueryParameter("bucket", bucketName).build()).delete(requestBody).build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            String message = response.body().string();
            response.close();
            throw new IOException("delete bucket error " + message);
        }
        response.close();
    }

    @Override
    public List<BucketModel> listBucket() throws Exception {
        Headers headers = this.buildHeaders(null, token, null);
        Request request = new Request.Builder().headers(headers).url(new HttpUrl.Builder().scheme(schema).host(host).port(port).addPathSegment("/hos/v1/bucket/list").build()).get().build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            String message = response.body().string();
            response.close();
            throw new IOException("list bucket error " + message);
        }
        response.close();
        String json = response.body().string();
        List<BucketModel> bucketModels = JsonUtil.fromJsonList(BucketModel.class, json);
        return bucketModels;
    }

    @Override
    public void putObject(PutRequest putRequest) throws Exception {
        Headers headers = this.buildHeaders(null, token, null);
        RequestBody requestBody = null;
        // 判断是否为创建文件
        if (putRequest.getContent() != null) {
            // 上传文件
            if (putRequest.getMediaType() == null) {
                putRequest.setMediaType("application/octet-stream");
            }
            requestBody = RequestBody.create(MediaType.parse(putRequest.getMediaType()), putRequest.getContent());
        }
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        builder.addFormDataPart("bucket", putRequest.getBucket());
        if (putRequest.getMediaType() != null) {
            builder.addFormDataPart("mediaType", putRequest.getMediaType());
        }
        builder.addFormDataPart("key", putRequest.getKey());
        if (requestBody != null) {
            builder.addFormDataPart("content", "content", requestBody);
        }
        requestBody = builder.build();

        Request request = new Request.Builder()
                .headers(headers)
                .url(new HttpUrl.Builder().scheme(schema).host(host).port(port).addPathSegment("/hos/v1/object")
                        .build())
                .post(requestBody).build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            String message = response.body().string();
            response.close();
            throw new IOException("put bucket error " + message);
        }
        response.close();
    }

    @Override
    public void putObject(String bucket, String key) throws Exception {
        if (!key.endsWith("/")) {
            throw new IOException("plain object content is empty");
        }
        PutRequest putRequest = new PutRequest(bucket, key, null);
        this.putObject(putRequest);
    }

    @Override
    public void putObject(String bucket, String key, byte[] content, String mediaType) throws Exception {
        PutRequest putRequest = new PutRequest(bucket, key, content, mediaType);
        putObject(putRequest);
    }

    @Override
    public void putObject(String bucket, String key, File content) throws Exception {
        PutRequest putRequest = new PutRequest(bucket, key, content, MimeUtil.getFileMimeType(content));
        this.putObject(putRequest);
    }

    @Override
    public void putObject(String bucket, String key, byte[] content, String mediaType,
                          String contentEncoding) throws Exception {
        PutRequest putRequest = new PutRequest(bucket, key, content, mediaType);
        putRequest.setContentEncoding(contentEncoding);
        this.putObject(putRequest);
    }

    @Override
    public void putObject(String bucket, String key, File content, String mediaType)
            throws Exception {
        if (!content.exists()) {
            throw new FileNotFoundException(content.getAbsolutePath());
        }
        if (content.length() == 0) {
            throw new IOException("plain object content is empty");
        }
        PutRequest putRequest = new PutRequest(bucket, key, content, mediaType);
        this.putObject(putRequest);

    }

    @Override
    public void putObject(String bucket, String key, File content, String mediaType,
                          String contentEncoding) throws Exception {
        PutRequest putRequest = new PutRequest(bucket, key, content, mediaType);
        putRequest.setContentEncoding(contentEncoding);
        this.putObject(putRequest);
    }

    @Override
    public void deleteObject(String bucket, String key) throws Exception {
        Headers headers = this.buildHeaders(null, token, null);
        RequestBody requestBody = RequestBody.create(null, new byte[0]);
        Request request = new Request.Builder()
                .headers(headers)
                .url(new HttpUrl.Builder()
                        .scheme(schema)
                        .host(host)
                        .port(port)
                        .addPathSegment("/hos/v1/object")
                        .addQueryParameter("bucket", bucket)
                        .addQueryParameter("key", key)
                        .build())
                .delete(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            String message = response.body().string();
            response.close();
            throw new IOException("delete object error " + message);
        }
        response.close();
    }

    @Override
    public HOSObjectSummary getObjectSummary(String bucket, String key) throws Exception {
        Headers headers = this.buildHeaders(null, this.token, null);
        Request request =
                new Request.Builder()
                        .headers(headers)
                        .url(new HttpUrl.Builder()
                                .scheme(this.schema)
                                .host(this.host)
                                .port(this.port)
                                .addPathSegment("/hos/v1/object/info")
                                .addQueryParameter("bucket", bucket)
                                .addQueryParameter("key", key)
                                .build()).get().build();
        Response response = client.newCall(request).execute();
        try {
            if (!response.isSuccessful()) {
                if (response.code() == 404) {
                    return null;
                }
                throw new RuntimeException(response.body().string());
            }
            String json = response.body().string();
            HOSObjectSummary summary = JsonUtil.fromJson(HOSObjectSummary.class, json);
            return summary;
        } finally {
            if (response != null) {
                response.close();
            }
        }

    }

    @Override
    public ObjectListResult listObject(String bucket, String startKey, String endKey)
            throws IOException {
        Headers headers = this.buildHeaders(null, this.token, null);
        Request request =
                new Request.Builder()
                        .headers(headers)
                        .url(new HttpUrl.Builder()
                                .scheme(this.schema)
                                .host(this.host)
                                .port(this.port)
                                .addPathSegment("/hos/v1/object/list")
                                .addQueryParameter("bucket", bucket)
                                .addQueryParameter("startKey", startKey)
                                .addQueryParameter("endKey", endKey)
                                .build()).build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            String json = response.body().string();
            response.close();
            return JsonUtil.fromJson(ObjectListResult.class, json);
        }
        response.close();
        throw new IOException("list object error");
    }

    @Override
    public ObjectListResult listObject(ListObjectRequest request) throws IOException {
        return this.listObject(request.getBucket(), request.getStartKey(), request.getEndKey());
    }


    @Override
    public ObjectListResult listObjectByPrefix(String bucket, String dir, String prefix,
                                               String startKey)
            throws IOException {
        Headers headers = this.buildHeaders(null, this.token, null);
        Request request =
                new Request.Builder()
                        .headers(headers)
                        .url(new HttpUrl.Builder()
                                .scheme(this.schema)
                                .host(this.host)
                                .port(this.port)
                                .addPathSegment("/hos/v1/object/list/prefix")
                                .addQueryParameter("bucket", bucket)
                                .addQueryParameter("prefix", prefix)
                                .addQueryParameter("dir", dir)
                                .addQueryParameter("startKey", startKey)
                                .build()).build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            String json = response.body().string();
            response.close();
            return JsonUtil.fromJson(ObjectListResult.class, json);
        }
        response.close();
        throw new IOException("list object error");
    }

    @Override
    public ObjectListResult listObjectByDir(String bucket, String dir, String startKey)
            throws IOException {
        if (!(dir.startsWith("/") && dir.endsWith("/"))) {
            throw new RuntimeException("dir must start with / and end with /");
        }
        if (startKey == "" || startKey == null) {
            startKey = dir;
        }
        Headers headers = this.buildHeaders(null, this.token, null);
        Request request =
                new Request.Builder()
                        .headers(headers)
                        .url(new HttpUrl.Builder()
                                .scheme(this.schema)
                                .host(this.host)
                                .port(this.port)
                                .addPathSegment("/hos/v1/object/list/dir")
                                .addQueryParameter("bucket", bucket)
                                .addQueryParameter("dir", dir)
                                .addQueryParameter("startKey", startKey)
                                .build()).build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            String json = response.body().string();
            response.close();
            return JsonUtil.fromJson(ObjectListResult.class, json);
        }
        response.close();
        throw new IOException("list object error");
    }

    @Override
    public HOSObject getObject(String bucket, String key) throws IOException {
        Headers headers = this.buildHeaders(null, this.token, null);
        Request request =
                new Request.Builder()
                        .headers(headers)
                        .url(new HttpUrl.Builder()
                                .scheme(this.schema)
                                .host(this.host)
                                .port(this.port)
                                .addPathSegments("/hos/v1/object/content")
                                .addQueryParameter("bucket", bucket)
                                .addQueryParameter("key", key)
                                .build()).get().build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            HOSObject object = new HOSObject(response);
            object.setContent(response.body().byteStream());
            object.setMetaData(this.buildMetaData(response));
            return object;
        }
        response.close();
        return null;
    }

    @Override
    public BucketModel getBucketInfo(String bucketName) throws IOException {
        Headers headers = this.buildHeaders(null, this.token, null);
        Request request =
                new Request.Builder()
                        .headers(headers)
                        .url(new HttpUrl.Builder()
                                .scheme(this.schema)
                                .host(this.host)
                                .port(this.port)
                                .addPathSegment("/hos/v1/bucket")
                                .addQueryParameter("bucket", bucketName)
                                .build()).get().build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            String message = response.body().string();
            response.close();
            throw new IOException("bucket not found");
        }
        String json = response.body().string();
        return JsonUtil.fromJson(BucketModel.class, json);
    }

    private Headers buildHeaders(Map<String, String> attrs, String token, String contentEncoding) {
        Map<String, String> headerMap = new HashMap<>();
        if (contentEncoding != null) {
            headerMap.put("content-encoding", contentEncoding);
        }
        headerMap.put("X-Auth-Token", token);
        if (attrs != null && attrs.size() > 0) {
            attrs.forEach(new BiConsumer<String, String>() {
                @Override
                public void accept(String s, String s2) {
                    headerMap.put(HOSHeaders.COMMON_ATTR_PREFIX + s, s2);
                }
            });
        }
        Headers headers = Headers.of(headerMap);
        return headers;
    }

    private ObjectMetaData buildMetaData(Response response) {
        ObjectMetaData metaData = new ObjectMetaData();
        metaData.setBucket(response.header(HOSHeaders.COMMON_OBJ_BUCKET));
        metaData.setKey(response.header(HOSHeaders.COMMON_OBJ_KEY));
        metaData.setLastModifyTime(Long.parseLong(response.header("Last-Modified")));
        metaData.setMediaType(response.header("Content-Type"));
        metaData.setLength(Long.parseLong(response.header(HOSHeaders.RESPONSE_OBJ_LENGTH)));
        return metaData;
    }
}
