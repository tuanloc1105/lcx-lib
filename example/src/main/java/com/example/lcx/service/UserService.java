package com.example.lcx.service;

import com.example.lcx.dto.UserDTO;
import com.example.lcx.http.request.CreateUserRequest;
import com.example.lcx.http.request.UpdateUserRequest;
import com.example.lcx.mapper.UserMapper;
import com.example.lcx.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.val;
import vn.com.lcx.common.annotation.Component;
import vn.com.lcx.common.database.pool.LCXDataSource;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserService {

    private final LCXDataSource dataSource;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserDTO save(CreateUserRequest request) {
        val connection = dataSource.getConnection();
        connection.openTransaction();
        try {
            val user = this.userMapper.map(request);
            this.userRepository.save2(connection, user);
            return this.userMapper.map(user);
        } catch (Exception e) {
            connection.rollback();
            throw new RuntimeException(e);
        } finally {
            connection.close();
        }
    }

    public UserDTO findById(Long id) {
        try (
                val connection = dataSource.getConnection();
        ) {
            val user = this.userRepository.findById(connection, id);
            return this.userMapper.map(user);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<UserDTO> findAll() {
        try (
                val connection = dataSource.getConnection();
        ) {
            val users = this.userRepository.findAll(connection);
            return users.stream()
                    .map(this.userMapper::map)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteById(Long id) {
        val connection = dataSource.getConnection();
        connection.openTransaction();
        try {
            val user = this.userRepository.findById(connection, id);
            if (user.getId() == null) {
                throw new RuntimeException("Cannot find user");
            }
            this.userRepository.delete(connection, user);
        } catch (Exception e) {
            connection.rollback();
            throw new RuntimeException(e);
        } finally {
            connection.close();
        }
    }

    public UserDTO update(UpdateUserRequest request) {
        val connection = dataSource.getConnection();
        connection.openTransaction();
        try {
            val user = this.userRepository.findById(connection, request.getId());
            if (user.getId() == null) {
                throw new RuntimeException("Cannot find user");
            }
            if (request.getAge() != null) {
                user.setAge(request.getAge());
            }
            if (request.getFirstName() != null) {
                user.setFirstName(request.getFirstName());
            }
            if (request.getLastName() != null) {
                user.setLastName(request.getLastName());
            }
            this.userRepository.update(connection, user);
            return this.userMapper.map(user);
        } catch (Exception e) {
            connection.rollback();
            throw new RuntimeException(e);
        } finally {
            connection.close();
        }
    }

}
