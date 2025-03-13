package com.example.lcx.mapper;

import com.example.lcx.dto.UserDTO;
import com.example.lcx.entity.User;
import vn.com.lcx.common.annotation.mapper.MapperClass;

@MapperClass
public interface UserMapper {

    User map(UserDTO dto);

    UserDTO map(User user);

}
