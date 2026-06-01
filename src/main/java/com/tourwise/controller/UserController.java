package com.tourwise.controller;

import com.tourwise.dto.*;
import com.tourwise.service.*;

import com.tourwise.common.ApiResponse;
import com.tourwise.vo.common.ActionResultVO;
import com.tourwise.vo.user.AvatarVO;
import com.tourwise.vo.user.LoginVO;
import com.tourwise.vo.user.PreferenceProfileVO;
import com.tourwise.vo.user.ProfileVO;
import com.tourwise.vo.user.RegisterVO;
import com.tourwise.vo.user.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.ok(userService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<RegisterVO> register(@RequestBody @Valid RegisterRequest request) {
        return ApiResponse.ok(userService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> me() {
        return ApiResponse.ok(userService.currentUser());
    }

    @GetMapping("/{id}")
    public ApiResponse<UserVO> getPublicUser(@PathVariable Long id) {
        return ApiResponse.ok(userService.publicUser(id));
    }

    @GetMapping("/profile")
    public ApiResponse<ProfileVO> profile() {
        return ApiResponse.ok(userService.profile());
    }

    @GetMapping("/profile/preferences")
    public ApiResponse<PreferenceProfileVO> preferenceProfile() {
        return ApiResponse.ok(userService.preferenceProfile());
    }

    @PutMapping("/profile")
    public ApiResponse<ActionResultVO> updateProfile(@RequestBody @Valid ProfileUpdateRequest request) {
        return ApiResponse.ok(userService.updateProfile(request));
    }

    @PostMapping("/avatar")
    public ApiResponse<AvatarVO> uploadAvatar(@RequestParam("avatar") MultipartFile file) {
        return ApiResponse.ok(userService.uploadAvatar(file));
    }

    @DeleteMapping("/avatar")
    public ApiResponse<ActionResultVO> deleteAvatar() {
        return ApiResponse.ok(userService.resetAvatar());
    }

    @PostMapping("/logout")
    public ApiResponse<ActionResultVO> logout() {
        return ApiResponse.ok(ActionResultVO.logout());
    }
}
