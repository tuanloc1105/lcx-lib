package com.example.lcx.repository;

import com.example.lcx.entity.User;
import vn.com.lcx.common.annotation.Repository;
import vn.com.lcx.common.database.pool.entry.ConnectionEntry;
import vn.com.lcx.common.database.repository.LCXRepository;

import java.util.List;

@Repository
public interface UserRepository extends LCXRepository<User> {

    User findById(ConnectionEntry connection, Long id);

    List<User> findAll(ConnectionEntry connection);

}
