package com.camping.duneinsolite.mapper;

import com.camping.duneinsolite.dto.response.UserResponse;
import com.camping.duneinsolite.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "role", source = "role")

    // CLIENT fields
    @Mapping(target = "loyaltyPoints", source = "loyaltyPoints")
    @Mapping(target = "loyaltyTier", source = "loyaltyTier")

    // PARTENAIRE fields (will be null for other roles if not present in User)
    @Mapping(target = "matriculeFiscal", source = "matriculeFiscal")
    @Mapping(target = "agencyAddress", source = "agencyAddress")

    UserResponse toResponse(User user);
}