package com.sep.treksphere.user.controller;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.user.dto.request.AssignPermissionsRequest;
import com.sep.treksphere.user.dto.response.RoleResponse;
import com.sep.treksphere.user.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Role", description = "Các API quản lý role và permission (Dành cho Admin)")
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "Lấy danh sách Role", description = "Trả về toàn bộ role kèm permission đang được gán")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles() {
        List<RoleResponse> response = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }

    @Operation(summary = "Gán permission cho Role", description = "Thay thế toàn bộ danh sách permission hiện có của một role")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @PutMapping("/{roleId}/permissions")
    public ResponseEntity<ApiResponse<RoleResponse>> assignPermissions(
            @PathVariable UUID roleId,
            @Valid @RequestBody AssignPermissionsRequest request) {
        RoleResponse response = roleService.assignPermissions(roleId, request.getPermissionIds());
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                response,
                MessageConstant.ROLE_PERMISSIONS_UPDATED_SUCCESSFULLY
        ));
    }
}
