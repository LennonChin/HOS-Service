package com.coderap.hos.sdk;

import com.coderap.hos.common.BucketModel;
import com.coderap.hos.sdk.service.HOSClientService;

import java.util.List;

/**
 * Created by jixin on 18-3-16.
 */
public class HOSSdkTest {

  private static String token = "ebb76539d62942abbad779a903a46b91";
  private static String endPoints = "http://127.0.0.1:9080";

  public static void main(String[] args) {
    final HOSClientService client = HOSClientFactory.getOrCreateHOSClient(endPoints, token);
    try {
      List<BucketModel> bucketModelList = client.listBucket();
      bucketModelList.forEach(bucketModel -> {
        System.out.println(bucketModel.getBucketName());
      });
    } catch (Exception e) {
      //nothing to do
    }
  }
}
