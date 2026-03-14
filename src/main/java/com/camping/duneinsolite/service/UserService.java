package com.camping.duneinsolite.service;


import com.camping.duneinsolite.dto.request.UserRequest;
import com.camping.duneinsolite.dto.response.UserResponse;
import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponse createUser(UserRequest request);
    UserResponse getUserById(UUID userId);
    UserResponse getUserByEmail(String email);
    List<UserResponse> getAllUsers();
    UserResponse updateUser(UUID userId, UserRequest request);
    void deleteUser(UUID userId);
}
