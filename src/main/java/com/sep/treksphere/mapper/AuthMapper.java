package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.response.LoginResponse;
import com.sep.treksphere.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AuthMapper {

    @Mapping(target = "user", source = "user")
    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    LoginResponse toLoginResponse(User user, String accessToken, String refreshToken);
}