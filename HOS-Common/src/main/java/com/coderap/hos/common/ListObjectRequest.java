package com.coderap.hos.common;

import lombok.Data;

@Data
public class ListObjectRequest {

  private String bucket;
  private String startKey;
  private String endKey;
  private String prefix;
  private int maxKeyNumber;
  private String listId;

}
