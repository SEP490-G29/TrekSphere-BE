package com.sep.treksphere.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleResponse {
    private UUID roleId;
    private String roleName;
    private String description;
    private List<PermissionResponse> permissions;
}
