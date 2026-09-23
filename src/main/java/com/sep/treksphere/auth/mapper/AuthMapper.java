package com.sep.treksphere.auth.mapper;

import com.sep.treksphere.auth.dto.response.LoginResponse;
import com.sep.treksphere.user.entity.User;
import com.sep.treksphere.user.mapper.UserMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AuthMapper {

    LoginResponse toLoginResponse(User user, String accessToken, String refreshToken);
}