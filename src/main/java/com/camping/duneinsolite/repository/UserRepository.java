package com.camping.duneinsolite.repository;


import com.camping.duneinsolite.model.User;
import com.camping.duneinsolite.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // ── Used by auth flow ─────────────────────────────────────────────────
    // After Keycloak validates the JWT, we fetch the local user by email
    // to get userId, role, loyaltyPoints, etc.
    Optional<User> findByEmail(String email);

    // Used during registration to check if email already exists
    // before creating the user in Keycloak
    boolean existsByEmail(String email);

    // ── Used by admin/management ──────────────────────────────────────────
    List<User> findByRole(UserRole role);
}
