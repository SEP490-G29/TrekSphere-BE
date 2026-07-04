package com.sep.treksphere.mapper;

import com.sep.treksphere.entity.Role;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    default List<String> toRoleNames(Set<Role> roles) {
        return roles.stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());
    }
}
