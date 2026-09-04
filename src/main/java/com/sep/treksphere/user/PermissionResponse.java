package com.sep.treksphere.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PermissionResponse {
    private UUID permissionId;
    private String resource;
    private String action;
    private String description;
}
