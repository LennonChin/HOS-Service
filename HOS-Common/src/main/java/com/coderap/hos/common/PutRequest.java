package com.coderap.hos.common;

import lombok.Data;

import java.io.File;
import java.util.Map;

@Data
public class PutRequest {

  private String bucket;
  private String key;
  private File file;
  private byte[] content;
  private String contentEncoding;
  private String mediaType;
  private Map<String, String> attrs;

  public PutRequest(String bucket, String key, File file) {
    this.file = file;
    this.bucket = bucket;
    this.key = key;
  }

  public PutRequest(String bucket, String key, File file, String mediaType) {
    this.file = file;
    this.bucket = bucket;
    this.mediaType = mediaType;
    this.key = key;
  }

  public PutRequest(String bucket, String key, byte[] content, String mediaType) {
    this.content = content;
    this.bucket = bucket;
    this.mediaType = mediaType;
    this.key = key;
  }

}
