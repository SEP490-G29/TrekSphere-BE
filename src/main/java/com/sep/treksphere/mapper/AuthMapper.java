package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.response.LoginResponse;
import com.sep.treksphere.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AuthMapper {

    LoginResponse toLoginResponse(User user, String accessToken, String refreshToken);
}