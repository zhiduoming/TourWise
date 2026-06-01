package com.tourwise.service;

import com.tourwise.common.BusinessException;
import com.tourwise.mapper.RouteMapMapper;
import com.tourwise.model.RouteMap;
import com.tourwise.vo.route.RouteMapVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class RouteMapService {
    private final RouteMapMapper routeMapMapper;
    private final OssStorageService ossStorageService;
    private final AdminService adminService;

    public RouteMapService(RouteMapMapper routeMapMapper, OssStorageService ossStorageService, AdminService adminService) {
        this.routeMapMapper = routeMapMapper;
        this.ossStorageService = ossStorageService;
        this.adminService = adminService;
    }

    public RouteMapVO getMap(Long placeGroupId) {
        validatePlaceGroup(placeGroupId);
        return RouteMapVO.from(routeMapMapper.findByPlaceGroupId(placeGroupId));
    }

    public RouteMapVO uploadMap(Long placeGroupId, MultipartFile file) {
        validatePlaceGroup(placeGroupId);
        long adminUserId = adminService.requireAdmin();
        ImageSize imageSize = readImageSize(file);
        String imageUrl = ossStorageService.uploadRouteMap(adminUserId, placeGroupId, file);

        RouteMap routeMap = new RouteMap();
        routeMap.setPlaceGroupId(placeGroupId);
        routeMap.setImageUrl(imageUrl);
        routeMap.setOriginalName(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        routeMap.setMapWidth(imageSize.getWidth());
        routeMap.setMapHeight(imageSize.getHeight());
        routeMap.setUploadedBy(adminUserId);
        routeMapMapper.upsert(routeMap);
        return RouteMapVO.from(routeMapMapper.findByPlaceGroupId(placeGroupId));
    }

    private void validatePlaceGroup(Long placeGroupId) {
        if (placeGroupId == null || placeGroupId <= 0) {
            throw BusinessException.badRequest("景点分组参数不合法");
        }
        if (routeMapMapper.existsPlaceGroup(placeGroupId) <= 0) {
            throw BusinessException.notFound("景点分组不存在");
        }
    }

    private static ImageSize readImageSize(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw BusinessException.badRequest("地图底图必须是可识别的图片文件");
            }
            return new ImageSize(image.getWidth(), image.getHeight());
        } catch (IOException ex) {
            throw BusinessException.badRequest("地图底图读取失败");
        }
    }

    private static class ImageSize {
        private final int width;
        private final int height;

        private ImageSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        private int getWidth() {
            return width;
        }

        private int getHeight() {
            return height;
        }
    }
}
