package com.example.client;

import com.example.grpc.UserGrpc;
import com.example.grpc.UserProto;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import vn.com.lcx.common.utils.LogUtils;

public class Client {

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        UserGrpc.UserBlockingStub stub = UserGrpc.newBlockingStub(channel);

        UserProto.UserRequest request = UserProto.UserRequest.newBuilder().setId(30).build();
        UserProto.UserResponse response = stub.getUserById(request);

        LogUtils.writeLog(LogUtils.Level.INFO, "Received from server: " + response);

        channel.shutdown();
    }

}
