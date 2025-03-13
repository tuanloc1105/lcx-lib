package com.example.lcx;

import vn.com.lcx.vertx.base.annotation.app.ComponentScan;
import vn.com.lcx.vertx.base.annotation.app.VertxApplication;
import vn.com.lcx.vertx.base.custom.MyVertxDeployment;

@VertxApplication
@ComponentScan(value = {"com.example"})
public class App {
    public static void main(String[] args) {
        MyVertxDeployment.getInstance().deployVerticle(App.class);
    }
}
