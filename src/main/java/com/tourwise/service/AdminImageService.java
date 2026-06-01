package com.tourwise.service;

import com.tourwise.common.BusinessException;
import com.tourwise.mapper.AdminImageMapper;
import com.tourwise.vo.upload.UploadVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminImageService {
    private final AdminImageMapper adminImageMapper;
    private final AdminService adminService;
    private final OssStorageService ossStorageService;

    public AdminImageService(AdminImageMapper adminImageMapper, AdminService adminService, OssStorageService ossStorageService) {
        this.adminImageMapper = adminImageMapper;
        this.adminService = adminService;
        this.ossStorageService = ossStorageService;
    }

    @Transactional
    public UploadVO uploadPoiImage(Long poiId, MultipartFile file) {
        validateId(poiId, "POI");
        if (adminImageMapper.existsPoi(poiId) == 0) {
            throw BusinessException.notFound("POI不存在");
        }
        long adminUserId = adminService.requireAdmin();
        String imageUrl = ossStorageService.uploadAdminImage(adminUserId, "poi-" + poiId, file);
        adminImageMapper.updatePoiImage(poiId, imageUrl);
        adminImageMapper.updateSpotCoverByPoiId(poiId, imageUrl);
        return uploadResult(imageUrl, file);
    }

    @Transactional
    public UploadVO uploadSpotImage(Long spotId, MultipartFile file) {
        validateId(spotId, "景点");
        if (adminImageMapper.existsSpot(spotId) == 0) {
            throw BusinessException.notFound("景点不存在");
        }
        long adminUserId = adminService.requireAdmin();
        String imageUrl = ossStorageService.uploadAdminImage(adminUserId, "spot-" + spotId, file);
        adminImageMapper.updateSpotCover(spotId, imageUrl);
        adminImageMapper.updateRepresentativePoiImage(spotId, imageUrl);
        return uploadResult(imageUrl, file);
    }

    @Transactional
    public UploadVO uploadFoodImage(Long foodId, MultipartFile file) {
        validateId(foodId, "美食");
        if (adminImageMapper.existsFood(foodId) == 0) {
            throw BusinessException.notFound("美食不存在");
        }
        long adminUserId = adminService.requireAdmin();
        String imageUrl = ossStorageService.uploadAdminImage(adminUserId, "food-" + foodId, file);
        adminImageMapper.updateFoodImage(foodId, imageUrl);
        return uploadResult(imageUrl, file);
    }

    @Transactional
    public UploadVO uploadCircleImage(Long circleId, MultipartFile file) {
        validateId(circleId, "圈子");
        if (adminImageMapper.existsCircle(circleId) == 0) {
            throw BusinessException.notFound("圈子不存在");
        }
        long adminUserId = adminService.requireAdmin();
        String imageUrl = ossStorageService.uploadAdminImage(adminUserId, "circle-" + circleId, file);
        adminImageMapper.updateCircleCover(circleId, imageUrl);
        return uploadResult(imageUrl, file);
    }

    private static void validateId(Long id, String label) {
        if (id == null || id <= 0) {
            throw BusinessException.badRequest(label + "参数不合法");
        }
    }

    private static UploadVO uploadResult(String imageUrl, MultipartFile file) {
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        return new UploadVO(imageUrl, imageUrl, fileName);
    }
}
