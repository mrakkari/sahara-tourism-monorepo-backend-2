package com.camping.duneinsolite.dto.request;


import com.camping.duneinsolite.model.enums.UserRole;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String phone;

    @NotNull(message = "Role is required")
    private UserRole role;

    // Only required when role = PARTENAIRE
    private String taxId;
    private Double commissionRate;
}
