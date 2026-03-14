package com.camping.duneinsolite.service;

import com.camping.duneinsolite.dto.request.RegisterRequest;
import com.camping.duneinsolite.model.User;
import com.camping.duneinsolite.model.enums.LoyaltyTier;
import com.camping.duneinsolite.model.enums.UserRole;
import com.camping.duneinsolite.repository.UserRepository;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserSyncService {

    private final Keycloak keycloak;
    private final UserRepository userRepository;

    @Value("${keycloak.realm}")
    private String realm;

    @Transactional
    public User registerUser(RegisterRequest request) {

        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();

        // ── Step 1: Build password credential ────────────────
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);

        // ── Step 2: Build user representation ────────────────
        UserRepresentation keycloakUser = new UserRepresentation();
        keycloakUser.setUsername(request.getEmail());
        keycloakUser.setEmail(request.getEmail());
        keycloakUser.setFirstName(request.getName());
        keycloakUser.setEnabled(true);
        keycloakUser.setEmailVerified(true);
        keycloakUser.setCredentials(List.of(credential));

        // ── Step 3: Create user in Keycloak ──────────────────
        Response response = usersResource.create(keycloakUser);
        int status = response.getStatus();

        if (status == 409) {
            throw new RuntimeException("User already exists in Keycloak: " + request.getEmail());
        }
        if (status != 201) {
            String body = response.readEntity(String.class);
            throw new RuntimeException("Failed to create user in Keycloak. Status: " + status + " - " + body);
        }

        // ── Step 4: Get Keycloak user ID from Location header ─
        String locationHeader = response.getHeaderString("Location");
        String keycloakUserId = locationHeader.substring(locationHeader.lastIndexOf("/") + 1);
        log.info("User {} created in Keycloak with id {}", request.getEmail(), keycloakUserId);

        // ── Step 5: Assign role — auto-create if missing ──────
        try {
            String roleName = request.getRole().name();

            // Check if role already exists in realm
            List<RoleRepresentation> availableRoles = realmResource.roles().list();
            boolean roleExists = availableRoles.stream()
                    .anyMatch(r -> r.getName().equals(roleName));

            if (!roleExists) {
                // Auto-create the role so registration never fails
                RoleRepresentation newRole = new RoleRepresentation();
                newRole.setName(roleName);
                newRole.setDescription("Auto-created role for " + roleName);
                realmResource.roles().create(newRole);
                log.info("Role {} auto-created in Keycloak realm", roleName);
            }

            RoleRepresentation role = realmResource.roles()
                    .get(roleName)
                    .toRepresentation();

            realmResource.users()
                    .get(keycloakUserId)
                    .roles()
                    .realmLevel()
                    .add(List.of(role));

            log.info("Role {} assigned to user {} in Keycloak", roleName, request.getEmail());

        } catch (Exception e) {
            // User is still created — just log the warning
            log.warn("Could not assign role to user {}: {}. User created without role.",
                    request.getEmail(), e.getMessage());
        }

        // ── Step 6: Save user in local DB ─────────────────────
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User already exists in local DB: " + request.getEmail());
        }

        User user = User.builder()
                .userId(UUID.fromString(keycloakUserId))
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(request.getRole())
                .loyaltyPoints(0)
                .loyaltyTier(LoyaltyTier.BRONZE)
                .taxId(request.getRole() == UserRole.PARTENAIRE ? request.getTaxId() : null)
                .commissionRate(request.getRole() == UserRole.PARTENAIRE ? request.getCommissionRate() : null)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User {} saved in local DB with id {}", request.getEmail(), savedUser.getUserId());

        return savedUser;
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        List<UserRepresentation> keycloakUsers = keycloak.realm(realm)
                .users()
                .searchByEmail(user.getEmail(), true);

        if (!keycloakUsers.isEmpty()) {
            keycloak.realm(realm)
                    .users()
                    .get(keycloakUsers.get(0).getId())
                    .remove();
            log.info("User {} deleted from Keycloak", user.getEmail());
        }

        userRepository.delete(user);
        log.info("User {} deleted from local DB", user.getEmail());
    }

    @Transactional
    public User updateUserRole(UUID userId, UserRole newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        List<UserRepresentation> keycloakUsers = keycloak.realm(realm)
                .users()
                .searchByEmail(user.getEmail(), true);

        if (!keycloakUsers.isEmpty()) {
            String keycloakUserId = keycloakUsers.get(0).getId();
            RealmResource realmResource = keycloak.realm(realm);

            // Remove old role
            try {
                RoleRepresentation oldRole = realmResource.roles()
                        .get(user.getRole().name())
                        .toRepresentation();
                realmResource.users().get(keycloakUserId)
                        .roles().realmLevel().remove(List.of(oldRole));
            } catch (Exception e) {
                log.warn("Could not remove old role: {}", e.getMessage());
            }

            // Add new role
            try {
                RoleRepresentation role = realmResource.roles()
                        .get(newRole.name())
                        .toRepresentation();
                realmResource.users().get(keycloakUserId)
                        .roles().realmLevel().add(List.of(role));
                log.info("User {} role updated to {} in Keycloak", user.getEmail(), newRole);
            } catch (Exception e) {
                log.warn("Could not assign new role: {}", e.getMessage());
            }
        }

        user.setRole(newRole);
        return userRepository.save(user);
    }
}