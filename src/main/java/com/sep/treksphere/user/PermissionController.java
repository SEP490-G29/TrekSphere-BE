package com.sep.treksphere.user;

import com.sep.treksphere.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Tag(name = "Permission", description = "Các API quản lý danh mục permission (Dành cho Admin)")
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "Lấy danh mục Permission", description = "Trả về toàn bộ permission hiện có trong hệ thống")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissions() {
        List<PermissionResponse> response = permissionService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }
}
