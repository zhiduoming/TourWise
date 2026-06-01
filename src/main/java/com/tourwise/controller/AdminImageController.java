package com.tourwise.controller;

import com.tourwise.common.ApiResponse;
import com.tourwise.service.AdminImageService;
import com.tourwise.vo.upload.UploadVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/images")
public class AdminImageController {
    private final AdminImageService adminImageService;

    public AdminImageController(AdminImageService adminImageService) {
        this.adminImageService = adminImageService;
    }

    @PostMapping("/poi/{poiId}")
    public ApiResponse<UploadVO> uploadPoiImage(
            @PathVariable Long poiId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(adminImageService.uploadPoiImage(poiId, file));
    }

    @PostMapping("/spot/{spotId}")
    public ApiResponse<UploadVO> uploadSpotImage(
            @PathVariable Long spotId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(adminImageService.uploadSpotImage(spotId, file));
    }

    @PostMapping("/food/{foodId}")
    public ApiResponse<UploadVO> uploadFoodImage(
            @PathVariable Long foodId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(adminImageService.uploadFoodImage(foodId, file));
    }

    @PostMapping("/circle/{circleId}")
    public ApiResponse<UploadVO> uploadCircleImage(
            @PathVariable Long circleId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(adminImageService.uploadCircleImage(circleId, file));
    }
}
