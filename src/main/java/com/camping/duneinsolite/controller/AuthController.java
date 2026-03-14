package com.camping.duneinsolite.controller;



import com.camping.duneinsolite.dto.request.LoginRequest;
import com.camping.duneinsolite.dto.request.RegisterRequest;
import com.camping.duneinsolite.dto.response.LoginResponse;
import com.camping.duneinsolite.model.User;
import com.camping.duneinsolite.service.AuthService;
import com.camping.duneinsolite.service.KeycloakUserSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakUserSyncService keycloakUserSyncService;
    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Creates user in Keycloak + saves in local DB
     * Body: { name, email, password, phone, role, taxId?, commissionRate? }
     */
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest request) {
        User createdUser = keycloakUserSyncService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /**
     * POST /api/auth/login
     * Authenticates against Keycloak, returns JWT token
     * Body: { email, password }
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
