package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.UpdateProfileRequest;
import com.sep.treksphere.dto.response.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserProfileResponse getUserProfile(String email);
    UserProfileResponse updateProfile(String email, UpdateProfileRequest request, MultipartFile avatar);
}
