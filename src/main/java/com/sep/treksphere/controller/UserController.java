package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.UserProfileResponse;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.sep.treksphere.dto.request.UpdateProfileRequest;
import com.sep.treksphere.dto.request.UserFilterRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.enums.user.UserStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "Các API liên quan đến quản lý người dùng")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Xem thông tin cá nhân", description = "Trả về thông tin profile của người dùng đang đăng nhập")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        UserProfileResponse profile = userService.getUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, profile));
    }

    @Operation(summary = "Cập nhật thông tin cá nhân", description = "Cập nhật họ tên, số điện thoại và ảnh đại diện")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute UpdateProfileRequest request) {
        
        if (userDetails == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        UserProfileResponse profile = userService.updateProfile(userDetails.getUsername(), request, request.getAvatar());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, profile, MessageConstant.PROFILE_UPDATED_SUCCESSFULLY));
    }

    @Operation(summary = "Lấy danh sách User", description = "Trả về danh sách tất cả user, có thể lọc theo trạng thái và role (Dành cho Admin)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<UserProfileResponse>>> getUsers(
            @Valid @ParameterObject @ModelAttribute UserFilterRequest request) {
        
        PaginationResponse<UserProfileResponse> response = userService.getUsers(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }

    @Operation(summary = "Khoá/Mở khoá tài khoản", description = "Thay đổi trạng thái của người dùng (Dành cho Admin)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<Void>> changeUserStatus(
            @PathVariable String userId,
            @RequestParam UserStatus status) {
        
        userService.changeUserStatus(userId, status);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.STATUS_UPDATED_SUCCESSFULLY));
    }
}
