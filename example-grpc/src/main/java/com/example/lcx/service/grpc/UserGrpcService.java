package com.example.lcx.service.grpc;

import com.example.grpc.UserGrpc;
import com.example.grpc.UserProto;
import com.example.lcx.service.UserService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.val;
import vn.com.lcx.common.annotation.Component;
import vn.com.lcx.common.constant.CommonConstant;
import vn.com.lcx.common.utils.LogUtils;

@Component
@RequiredArgsConstructor
public class UserGrpcService extends UserGrpc.UserImplBase {

    private final UserService userService;

    @Override
    public void getUserById(UserProto.UserRequest request, StreamObserver<UserProto.UserResponse> responseObserver) {
        LogUtils.writeLog(LogUtils.Level.INFO, "Incoming request to UserGrpcService:\n{}", request + CommonConstant.EMPTY_STRING);
        val user = this.userService.findById(request.getId());
        val reply = UserProto.UserResponse.newBuilder()
                .setId(user.getId())
                .setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setAge(user.getAge())
                .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
        LogUtils.writeLog(LogUtils.Level.INFO, "UserGrpcService return response:\n{}", reply + CommonConstant.EMPTY_STRING);
    }
}
