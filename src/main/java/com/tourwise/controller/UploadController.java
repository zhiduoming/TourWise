package com.tourwise.controller;

import com.tourwise.dto.*;
import com.tourwise.service.*;

import com.tourwise.common.ApiResponse;
import com.tourwise.security.AuthContext;
import com.tourwise.vo.upload.UploadVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
public class UploadController {
    private final OssStorageService ossStorageService;

    public UploadController(OssStorageService ossStorageService) {
        this.ossStorageService = ossStorageService;
    }

    @PostMapping
    public ApiResponse<UploadVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "log") String scene) {
        long userId = AuthContext.requireUserId();
        String url = ossStorageService.uploadUserImage(userId, file, scene);
        return ApiResponse.ok(new UploadVO(url, url, file.getOriginalFilename() == null ? "" : file.getOriginalFilename()));
    }
}
