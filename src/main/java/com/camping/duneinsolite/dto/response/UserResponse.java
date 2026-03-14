package com.camping.duneinsolite.dto.response;


import com.camping.duneinsolite.model.enums.LoyaltyTier;
import com.camping.duneinsolite.model.enums.UserRole;
import lombok.Data;
import java.util.UUID;

@Data
public class UserResponse {
    private UUID userId;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private Integer loyaltyPoints;
    private LoyaltyTier loyaltyTier;
    private String taxId;
    private Double commissionRate;
}
