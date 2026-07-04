package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.response.UserProfileResponse;
import com.sep.treksphere.dto.response.UserResponse;
import com.sep.treksphere.entity.Role;
import com.sep.treksphere.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {

    @Mapping(target = "id", expression = "java(user.getUserID() != null ? user.getUserID().toString() : null)")
    @Mapping(target = "roles", source = "roles")
    UserResponse toUserResponse(User user);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRoles")
    UserProfileResponse toUserProfileResponse(User user);

    @Named("mapRoles")
    default List<String> mapRoles(Set<Role> roles) {
        if (roles == null) {
            return null;
        }
        return roles.stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());
    }
}
