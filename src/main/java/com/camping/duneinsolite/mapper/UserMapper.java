package com.camping.duneinsolite.mapper;


import com.camping.duneinsolite.dto.response.UserResponse;
import com.camping.duneinsolite.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
