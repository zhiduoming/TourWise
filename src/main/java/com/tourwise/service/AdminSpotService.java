package com.tourwise.service;

import com.tourwise.common.BusinessException;
import com.tourwise.common.MapUtil;
import com.tourwise.common.PageResult;
import com.tourwise.dto.AdminPoiRequest;
import com.tourwise.dto.AdminSpotCreateRequest;
import com.tourwise.dto.AdminSpotTagsRequest;
import com.tourwise.dto.AdminSpotUpdateRequest;
import com.tourwise.mapper.AdminSpotMapper;
import com.tourwise.vo.VoConvert;
import com.tourwise.vo.admin.AdminPoiCategoryVO;
import com.tourwise.vo.admin.AdminPoiVO;
import com.tourwise.vo.admin.AdminSpotVO;
import com.tourwise.vo.admin.AdminTagVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class AdminSpotService {
    private final AdminSpotMapper adminSpotMapper;
    private final AdminService adminService;

    public AdminSpotService(AdminSpotMapper adminSpotMapper, AdminService adminService) {
        this.adminSpotMapper = adminSpotMapper;
        this.adminService = adminService;
    }

    public PageResult<AdminSpotVO> spots(
            String keyword,
            Integer status,
            String routeGraphStatus,
            String qualityIssue,
            int page,
            int pageSize) {
        adminService.requireAdmin();
        String normalizedKeyword = trimToNull(keyword);
        String normalizedRouteGraphStatus = trimToNull(routeGraphStatus);
        String normalizedQualityIssue = trimToNull(qualityIssue);
        int offset = (page - 1) * pageSize;
        List<AdminSpotVO> list = adminSpotMapper
                .listSpots(normalizedKeyword, status, normalizedRouteGraphStatus, normalizedQualityIssue, offset, pageSize)
                .stream()
                .map(MapUtil::normalize)
                .map(AdminSpotVO::from)
                .toList();
        long total = adminSpotMapper.countSpots(normalizedKeyword, status, normalizedRouteGraphStatus, normalizedQualityIssue);
        return new PageResult<>(list, total);
    }

    public AdminSpotVO spot(Long id) {
        adminService.requireAdmin();
        return findSpot(id);
    }

    @Transactional
    public AdminSpotVO createSpot(AdminSpotCreateRequest request) {
        adminService.requireAdmin();
        normalizeSpotRequest(request);
        validateSpotRequest(request);
        fillDefaultSpotCategory(request);
        String groupType = groupTypeForSpotType(request.getSpotType());
        adminSpotMapper.insertPlaceGroup(request, groupType);
        if (request.getPlaceGroupId() == null) {
            throw BusinessException.badRequest("新增景点空间分组失败");
        }
        adminSpotMapper.insertSpot(request);
        if (request.getId() == null) {
            throw BusinessException.badRequest("新增景点失败");
        }
        String scene = representativeScene(request.getSpotType());
        String areaCode = representativeAreaCode(request.getSpotType());
        String areaName = representativeAreaName(request.getSpotType());
        adminSpotMapper.insertRepresentativePoi(request.getId(), request.getPlaceGroupId(), request, scene, areaCode, areaName);
        if (request.getRepresentativePoiId() == null) {
            throw BusinessException.badRequest("新增景点代表 POI 失败");
        }
        adminSpotMapper.updateSpotRepresentativePoi(request.getId(), request.getRepresentativePoiId());
        return findSpot(request.getId());
    }

    @Transactional
    public AdminSpotVO updateSpot(Long id, AdminSpotUpdateRequest request) {
        adminService.requireAdmin();
        AdminSpotVO before = findSpot(id);
        normalizeSpotRequest(request);
        validateSpotRequest(request);
        adminSpotMapper.updateSpot(id, request);
        if (before.getPlaceGroupId() != null) {
            adminSpotMapper.updatePlaceGroupFromSpot(before.getPlaceGroupId(), request);
        }
        if (before.getRepresentativePoiId() != null) {
            adminSpotMapper.updateRepresentativePoiFromSpot(
                    before.getRepresentativePoiId(),
                    request,
                    representativeScene(request.getSpotType()),
                    representativeAreaCode(request.getSpotType()),
                    representativeAreaName(request.getSpotType()));
        }
        return findSpot(id);
    }

    @Transactional
    public AdminSpotVO updateSpotStatus(Long id, Integer status) {
        adminService.requireAdmin();
        AdminSpotVO spot = findSpot(id);
        adminSpotMapper.updateSpotStatus(id, status);
        if (spot.getRepresentativePoiId() != null) {
            adminSpotMapper.updateRepresentativePoiStatus(spot.getRepresentativePoiId(), status);
        }
        return findSpot(id);
    }

    public PageResult<AdminPoiVO> pois(
            Long spotId,
            String keyword,
            Integer status,
            String qualityIssue,
            int page,
            int pageSize) {
        adminService.requireAdmin();
        AdminSpotVO spot = findSpot(spotId);
        String normalizedKeyword = trimToNull(keyword);
        String normalizedQualityIssue = trimToNull(qualityIssue);
        int offset = (page - 1) * pageSize;
        List<AdminPoiVO> list = adminSpotMapper
                .listPois(spotId, spot.getPlaceGroupId(), normalizedKeyword, status, normalizedQualityIssue, offset, pageSize)
                .stream()
                .map(MapUtil::normalize)
                .map(AdminPoiVO::from)
                .toList();
        long total = adminSpotMapper.countPois(spotId, spot.getPlaceGroupId(), normalizedKeyword, status, normalizedQualityIssue);
        return new PageResult<>(list, total);
    }

    @Transactional
    public AdminPoiVO createPoi(Long spotId, AdminPoiRequest request) {
        adminService.requireAdmin();
        AdminSpotVO spot = findSpot(spotId);
        normalizePoiRequest(request);
        validatePoiRequest(request);
        adminSpotMapper.insertPoi(spotId, spot.getPlaceGroupId(), request);
        if (request.getId() == null) {
            throw BusinessException.badRequest("新增 POI 失败");
        }
        return findPoi(request.getId());
    }

    @Transactional
    public AdminPoiVO updatePoi(Long poiId, AdminPoiRequest request) {
        adminService.requireAdmin();
        findPoi(poiId);
        normalizePoiRequest(request);
        validatePoiRequest(request);
        adminSpotMapper.updatePoi(poiId, request);
        return findPoi(poiId);
    }

    @Transactional
    public AdminPoiVO updatePoiStatus(Long poiId, Integer status) {
        adminService.requireAdmin();
        findPoi(poiId);
        adminSpotMapper.updatePoiStatus(poiId, status);
        return findPoi(poiId);
    }

    public List<AdminPoiCategoryVO> categories() {
        adminService.requireAdmin();
        return adminSpotMapper.listCategories()
                .stream()
                .map(MapUtil::normalize)
                .map(AdminPoiCategoryVO::from)
                .toList();
    }

    public List<AdminTagVO> tags(String tagType) {
        adminService.requireAdmin();
        String normalizedTagType = trimToNull(tagType);
        return adminSpotMapper.listTags(normalizedTagType)
                .stream()
                .map(MapUtil::normalize)
                .map(AdminTagVO::from)
                .toList();
    }

    public List<AdminTagVO> spotTags(Long spotId) {
        adminService.requireAdmin();
        findSpot(spotId);
        return adminSpotMapper.listSpotTags(spotId)
                .stream()
                .map(MapUtil::normalize)
                .map(AdminTagVO::from)
                .toList();
    }

    @Transactional
    public List<AdminTagVO> updateSpotTags(Long spotId, AdminSpotTagsRequest request) {
        adminService.requireAdmin();
        findSpot(spotId);
        List<String> tags = normalizeTagNames(request == null ? null : request.getTags());
        adminSpotMapper.deleteSpotTags(spotId);
        for (String tag : tags) {
            Long tagId = adminSpotMapper.findTagId(tag, "poi");
            if (tagId == null) {
                adminSpotMapper.insertTag(tag, "poi");
                tagId = adminSpotMapper.findTagId(tag, "poi");
            }
            if (tagId != null) {
                adminSpotMapper.insertSpotTag(spotId, tagId);
            }
        }
        return spotTags(spotId);
    }

    private AdminSpotVO findSpot(Long id) {
        Map<String, Object> row = adminSpotMapper.findSpotById(id);
        if (row == null) {
            throw BusinessException.notFound("景点不存在");
        }
        return AdminSpotVO.from(MapUtil.normalize(row));
    }

    private List<String> normalizeTagNames(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String tag : tags) {
            String normalized = trimToNull(tag);
            if (normalized != null) {
                result.add(normalized);
            }
            if (result.size() >= 12) {
                break;
            }
        }
        return List.copyOf(result);
    }

    private AdminPoiVO findPoi(Long id) {
        Map<String, Object> row = adminSpotMapper.findPoiById(id);
        if (row == null) {
            throw BusinessException.notFound("POI 不存在");
        }
        return AdminPoiVO.from(MapUtil.normalize(row));
    }

    private void validateSpotRequest(AdminSpotUpdateRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw BusinessException.badRequest("景点名称不能为空");
        }
        if (!StringUtils.hasText(request.getSpotType())) {
            throw BusinessException.badRequest("景点类型不能为空");
        }
        if (request.getLocationRadiusM() == null || request.getLocationRadiusM() < 20) {
            request.setLocationRadiusM(500);
        }
        if (request.getStatus() == null) {
            request.setStatus(1);
        }
        if (!List.of("none", "draft", "verified").contains(request.getRouteGraphStatus())) {
            request.setRouteGraphStatus("none");
        }
        if (request.getRating() == null) {
            request.setRating(BigDecimal.valueOf(5.0));
        }
        if (request.getHotness() == null) {
            request.setHotness(0);
        }
    }

    private void fillDefaultSpotCategory(AdminSpotUpdateRequest request) {
        if (request.getCategoryId() != null) {
            return;
        }
        String code = switch (request.getSpotType()) {
            case "university", "campus" -> "university";
            case "museum" -> "museum";
            case "park" -> "natural";
            case "landmark" -> "landmark";
            case "business" -> "commercial";
            default -> "scenic";
        };
        Long categoryId = adminSpotMapper.findCategoryIdByCode(code);
        if (categoryId == null) {
            categoryId = adminSpotMapper.findCategoryIdByCode("scenic");
        }
        if (categoryId == null) {
            throw BusinessException.badRequest("缺少可用的景点分类，无法新增景点");
        }
        request.setCategoryId(categoryId);
    }

    private static String groupTypeForSpotType(String spotType) {
        return switch (spotType) {
            case "university" -> "university";
            case "campus" -> "campus";
            case "museum" -> "museum_area";
            case "park" -> "park";
            case "business" -> "business_area";
            case "scenic" -> "scenic_area";
            default -> "other";
        };
    }

    private static boolean isCampusSpot(String spotType) {
        return "university".equals(spotType) || "campus".equals(spotType);
    }

    private static String representativeScene(String spotType) {
        return isCampusSpot(spotType) ? "campus" : "city";
    }

    private static String representativeAreaCode(String spotType) {
        return isCampusSpot(spotType) ? "campus_summary" : "spot_summary";
    }

    private static String representativeAreaName(String spotType) {
        return isCampusSpot(spotType) ? "校区概览" : "景点概览";
    }

    private void validatePoiRequest(AdminPoiRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw BusinessException.badRequest("POI 名称不能为空");
        }
        if (request.getCategoryId() == null) {
            throw BusinessException.badRequest("POI 分类不能为空");
        }
        if (!StringUtils.hasText(request.getScene())) {
            request.setScene("campus");
        }
        if (request.getRating() == null) {
            request.setRating(BigDecimal.valueOf(5.0));
        }
        if (request.getHotness() == null) {
            request.setHotness(0);
        }
        if (request.getStatus() == null) {
            request.setStatus(1);
        }
    }

    private void normalizeSpotRequest(AdminSpotUpdateRequest request) {
        request.setName(trimToNull(request.getName()));
        request.setShortName(trimToNull(request.getShortName()));
        request.setSpotType(trimToNull(request.getSpotType()));
        request.setProvince(trimToNull(request.getProvince()));
        request.setCity(trimToNull(request.getCity()));
        request.setDistrict(trimToNull(request.getDistrict()));
        request.setAddress(trimToNull(request.getAddress()));
        request.setDescription(trimToNull(request.getDescription()));
        request.setCoverImage(trimToNull(request.getCoverImage()));
        request.setRouteGraphStatus(trimToNull(request.getRouteGraphStatus()));
    }

    private void normalizePoiRequest(AdminPoiRequest request) {
        request.setName(trimToNull(request.getName()));
        request.setScene(trimToNull(request.getScene()));
        request.setAreaCode(trimToNull(request.getAreaCode()));
        request.setAreaName(trimToNull(request.getAreaName()));
        request.setProvince(trimToNull(request.getProvince()));
        request.setCity(trimToNull(request.getCity()));
        request.setAddress(trimToNull(request.getAddress()));
        request.setLocationText(trimToNull(request.getLocationText()));
        request.setDescription(trimToNull(request.getDescription()));
        request.setImageUrl(trimToNull(request.getImageUrl()));
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
