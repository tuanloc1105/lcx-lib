package com.example.lcx;

import vn.com.lcx.common.utils.CommonUtils;
import vn.com.lcx.common.utils.FileUtils;
import vn.com.lcx.vertx.base.custom.MyVertxDeployment;

import java.util.Arrays;

public class App {
    public static void main(String[] args) {
        MyVertxDeployment.getInstance().deployVerticle(
                Arrays.asList("vn.com", "com.example.lcx"),
                () -> {
                    CommonUtils.bannerLogging(FileUtils.pathJoining("banner.txt"));
                    return null;
                }
        );
    }
}
