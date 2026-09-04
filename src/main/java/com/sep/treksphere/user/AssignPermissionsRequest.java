package com.sep.treksphere.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class AssignPermissionsRequest {

    @NotNull
    private Set<UUID> permissionIds;
}
