package com.sep.treksphere.auth;

import com.sep.treksphere.auth.dto.response.LoginResponse;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AuthMapper {

    LoginResponse toLoginResponse(User user, String accessToken, String refreshToken);
}