package com.example.lcx;

import vn.com.lcx.common.utils.CommonUtils;
import vn.com.lcx.common.utils.FileUtils;
import vn.com.lcx.vertx.base.custom.MyVertxDeployment;

import java.util.Arrays;
import java.util.Collections;

public class App {
    public static void main(String[] args) {
        MyVertxDeployment.getInstance().deployVerticle(
                "com.example.lcx",
                () -> {
                    CommonUtils.bannerLogging(FileUtils.pathJoining("banner.txt"));
                    return null;
                }
        );
    }
}
