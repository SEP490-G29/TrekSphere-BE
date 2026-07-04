package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.response.UserProfileResponse;
import com.sep.treksphere.dto.response.UserResponse;
import com.sep.treksphere.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {

    @Mapping(target = "id", expression = "java(user.getUserID() != null ? user.getUserID().toString() : null)")
    @Mapping(target = "roles", source = "roles")
    UserResponse toUserResponse(User user);

    @Mapping(target = "roles", source = "roles")
    UserProfileResponse toUserProfileResponse(User user);
}
