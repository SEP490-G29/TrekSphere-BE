package com.sep.treksphere.user;

import com.sep.treksphere.user.Role;
import org.mapstruct.Mapper;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    default List<String> toRoleNames(Set<Role> roles) {
        if (roles == null) {
            return Collections.emptyList();
        }
        return roles.stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());
    }
}
