package com.sep.treksphere.user;

import com.sep.treksphere.user.UserProfileResponse;
import com.sep.treksphere.user.UserResponse;
import com.sep.treksphere.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {

    @Mapping(target = "id", expression = "java(user.getUserId() != null ? user.getUserId().toString() : null)")
    @Mapping(target = "roles", source = "roles")
    UserResponse toUserResponse(User user);

    @Mapping(target = "roles", source = "roles")
    UserProfileResponse toUserProfileResponse(User user);
}
