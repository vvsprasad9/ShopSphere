package com.shopsphere.service;

import com.shopsphere.dto.request.user.ChangePasswordRequest;
import com.shopsphere.dto.request.user.UpdateProfileRequest;
import com.shopsphere.dto.response.user.UserResponse;

public interface UserService {

    UserResponse getProfile(Long userId);

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);
}
