package com.example.lcx;

import com.example.lcx.service.grpc.UserGrpcService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import vn.com.lcx.common.config.ClassPool;
import vn.com.lcx.common.utils.LogUtils;
import vn.com.lcx.vertx.base.annotation.app.ComponentScan;
import vn.com.lcx.vertx.base.annotation.app.VertxApplication;
import vn.com.lcx.vertx.base.custom.MyVertxDeployment;

import java.io.IOException;

@VertxApplication
@ComponentScan(value = {"com.example"})
public class App {
    public static void main(String[] args) throws InterruptedException, IOException {
        MyVertxDeployment.getInstance().deployVerticle(App.class);

        Server server = ServerBuilder.forPort(50051)
                .addService(ClassPool.getInstance(UserGrpcService.class))
                .build()
                .start();

        LogUtils.writeLog(LogUtils.Level.INFO, "Server started on port 50051");
        server.awaitTermination();
    }
}
