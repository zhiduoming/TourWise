package com.tourwise.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourwise.common.BusinessException;
import com.tourwise.common.MapUtil;
import com.tourwise.common.PageResult;
import com.tourwise.dto.RouteRecordPointRequest;
import com.tourwise.dto.RouteRecordSaveRequest;
import com.tourwise.mapper.RouteRecordMapper;
import com.tourwise.model.RouteRecord;
import com.tourwise.model.RouteRecordPoint;
import com.tourwise.security.AuthContext;
import com.tourwise.vo.route.RouteRecordPointVO;
import com.tourwise.vo.route.RouteRecordVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RouteRecordService {
    private static final TypeReference<Map<String, Object>> JSON_MAP_TYPE = new TypeReference<>() {
    };

    private final RouteRecordMapper routeRecordMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RouteRecordService(RouteRecordMapper routeRecordMapper) {
        this.routeRecordMapper = routeRecordMapper;
    }

    @Transactional
    public RouteRecordVO save(RouteRecordSaveRequest request) {
        long userId = AuthContext.requireUserId();
        validateSaveRequest(request);

        RouteRecord record = new RouteRecord();
        record.setUserId(userId);
        record.setRouteName(trimToDefault(request.getRouteName(), defaultRouteName(request)));
        record.setMode(normalizedMode(request.getMode()));
        record.setTotalDistanceM(request.getTotalDistanceM());
        record.setTotalDurationMin(request.getTotalDurationMin());
        record.setPreferences(metadataJson(request));
        routeRecordMapper.insertRecord(record);

        List<RouteRecordPointRequest> points = request.getPoints();
        for (int i = 0; i < points.size(); i++) {
            RouteRecordPointRequest source = points.get(i);
            RouteRecordPoint point = new RouteRecordPoint();
            point.setRouteRecordId(record.getId());
            point.setPoiId(source.getPoiId());
            point.setPointName(trimToDefault(source.getPointName(), "路线点" + (i + 1)));
            point.setSortOrder(i + 1);
            point.setDistanceFromStartM(source.getDistanceFromStartM());
            point.setDescription(trimToNull(source.getDescription()));
            routeRecordMapper.insertPoint(point);
        }
        return buildRecordVO(routeRecordMapper.findRecordById(record.getId(), userId));
    }

    public PageResult<RouteRecordVO> list(int page, int pageSize) {
        long userId = AuthContext.requireUserId();
        int offset = (page - 1) * pageSize;
        List<RouteRecordVO> list = routeRecordMapper.listRecords(userId, offset, pageSize)
                .stream()
                .map(this::buildRecordVO)
                .toList();
        long total = routeRecordMapper.countRecords(userId);
        return new PageResult<>(list, total);
    }

    @Transactional
    public void delete(Long id) {
        long userId = AuthContext.requireUserId();
        int affected = routeRecordMapper.deleteRecord(id, userId);
        if (affected == 0) {
            throw BusinessException.notFound("路线记录不存在或无权删除");
        }
    }

    private RouteRecordVO buildRecordVO(Map<String, Object> rawRow) {
        Map<String, Object> row = MapUtil.normalize(rawRow);
        RouteRecordVO vo = RouteRecordVO.from(row);
        vo.setMetadata(parseMetadata(vo.getPreferencesRaw()));
        vo.setPoints(routeRecordMapper.listPoints(vo.getId())
                .stream()
                .map(MapUtil::normalize)
                .map(RouteRecordPointVO::from)
                .toList());
        return vo;
    }

    private void validateSaveRequest(RouteRecordSaveRequest request) {
        if (request.getTotalDistanceM() == null || request.getTotalDistanceM() < 0) {
            throw BusinessException.badRequest("路线距离不合法");
        }
        if (request.getTotalDurationMin() == null || request.getTotalDurationMin() < 0) {
            throw BusinessException.badRequest("路线时间不合法");
        }
        if (request.getPoints() == null || request.getPoints().size() < 2) {
            throw BusinessException.badRequest("至少需要两个路线点才能保存");
        }
    }

    private String metadataJson(RouteRecordSaveRequest request) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("routeType", trimToDefault(request.getRouteType(), "internal"));
        metadata.put("provider", trimToDefault(request.getProvider(), "local"));
        metadata.put("algorithm", trimToNull(request.getAlgorithm()));
        metadata.put("placeGroupId", request.getPlaceGroupId());
        metadata.put("spotId", request.getSpotId());
        metadata.put("sourcePlanId", request.getSourcePlanId());
        metadata.put("sourcePlanTitle", trimToNull(request.getSourcePlanTitle()));
        metadata.put("startName", trimToNull(request.getStartName()));
        metadata.put("endName", trimToNull(request.getEndName()));
        metadata.put("preferences", request.getPreferences() == null ? List.of() : request.getPreferences());
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            throw BusinessException.badRequest("路线记录元数据保存失败");
        }
    }

    private Map<String, Object> parseMetadata(String preferences) {
        if (!StringUtils.hasText(preferences)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(preferences, JSON_MAP_TYPE);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private String defaultRouteName(RouteRecordSaveRequest request) {
        String start = trimToDefault(request.getStartName(), "起点");
        String end = trimToDefault(request.getEndName(), "终点");
        return start + " 到 " + end;
    }

    private String normalizedMode(String mode) {
        String value = trimToDefault(mode, "shortest");
        if (List.of("shortest", "optimal", "indoor").contains(value)) {
            return value;
        }
        return "shortest";
    }

    private static String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
