package com.camping.duneinsolite.service.impl;


import com.camping.duneinsolite.dto.request.UserRequest;
import com.camping.duneinsolite.dto.response.UserResponse;
import com.camping.duneinsolite.mapper.UserMapper;
import com.camping.duneinsolite.model.User;
import com.camping.duneinsolite.model.enums.LoyaltyTier;
import com.camping.duneinsolite.model.enums.UserRole;
import com.camping.duneinsolite.repository.UserRepository;
import com.camping.duneinsolite.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(request.getRole())
                .loyaltyPoints(0)
                .loyaltyTier(LoyaltyTier.BRONZE)
                .taxId(request.getRole() == UserRole.PARTENAIRE ? request.getTaxId() : null)
                .commissionRate(request.getRole() == UserRole.PARTENAIRE ? request.getCommissionRate() : null)
                .build();
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        return userMapper.toResponse(findUserById(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        return userMapper.toResponse(
                userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found with email: " + email))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse updateUser(UUID userId, UserRequest request) {
        User user = findUserById(userId);
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        if (request.getRole() == UserRole.PARTENAIRE) {
            user.setTaxId(request.getTaxId());
            user.setCommissionRate(request.getCommissionRate());
        }
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void deleteUser(UUID userId) {
        userRepository.delete(findUserById(userId));
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }
    // Add this to your UserServiceImpl, alongside getAllUsers()

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRoles(List<UserRole> roles) {
        return userRepository.findByRoleIn(roles).stream()
                .map(userMapper::toResponse)
                .toList();
    }
}
