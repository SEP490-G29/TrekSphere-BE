package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.UpdateProfileRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import com.sep.treksphere.dto.request.UserFilterRequest;
import com.sep.treksphere.enums.user.UserStatus;

public interface UserService {
    UserProfileResponse getUserProfile(String email);
    UserProfileResponse getUserById(String userId);
    UserProfileResponse updateProfile(String email, UpdateProfileRequest request, MultipartFile avatar);
    PaginationResponse<UserProfileResponse> getUsers(UserFilterRequest request);
    void changeUserStatus(String userId, UserStatus status);
}
