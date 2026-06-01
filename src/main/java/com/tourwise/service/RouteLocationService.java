package com.tourwise.service;

import com.tourwise.common.BusinessException;
import com.tourwise.common.CoordinateUtil;
import com.tourwise.common.MapUtil;
import com.tourwise.dto.LocationResolveRequest;
import com.tourwise.mapper.RouteMapper;
import com.tourwise.vo.VoConvert;
import com.tourwise.vo.route.LocationMatchVO;
import com.tourwise.vo.route.LocationResolveVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class RouteLocationService {
    private final RouteMapper routeMapper;

    public RouteLocationService(RouteMapper routeMapper) {
        this.routeMapper = routeMapper;
    }

    public LocationResolveVO resolve(LocationResolveRequest request) {
        String mode = StringUtils.hasText(request.getMode()) ? request.getMode().trim() : "between";
        BigDecimal[] gcj02 = CoordinateUtil.toGcj02(
                request.getLongitude(),
                request.getLatitude(),
                request.getCoordinateSystem());
        BigDecimal longitude = gcj02[0];
        BigDecimal latitude = gcj02[1];
        int accuracy = request.getAccuracy() == null || request.getAccuracy() <= 0 ? 200 : request.getAccuracy();

        LocationResolveVO.CoordinateVO coordinate = new LocationResolveVO.CoordinateVO(
                longitude,
                latitude,
                accuracy,
                StringUtils.hasText(request.getProvider()) ? request.getProvider() : "unknown",
                "gcj02");

        if ("internal".equalsIgnoreCase(mode)) {
            if (request.getPlaceGroupId() == null || request.getPlaceGroupId() <= 0) {
                throw BusinessException.badRequest("景点内部定位必须传 placeGroupId");
            }
            List<LocationMatchVO> candidates = routeMapper.findNearestInternalPois(
                            request.getPlaceGroupId(),
                            longitude.doubleValue(),
                            latitude.doubleValue())
                    .stream()
                    .map(MapUtil::normalize)
                    .map(row -> toMatch(row, accuracy))
                    .toList();
            return new LocationResolveVO(
                    coordinate,
                    null,
                    candidates.isEmpty() ? null : candidates.get(0),
                    candidates);
        }

        List<LocationMatchVO> candidates = routeMapper.findNearestSpots(
                        longitude.doubleValue(),
                        latitude.doubleValue())
                .stream()
                .map(MapUtil::normalize)
                .map(row -> toMatch(row, accuracy))
                .toList();
        return new LocationResolveVO(
                coordinate,
                candidates.isEmpty() ? null : candidates.get(0),
                null,
                candidates);
    }

    private static LocationMatchVO toMatch(Map<String, Object> row, int accuracy) {
        Integer distance = VoConvert.intValue(row, "distance");
        LocationMatchVO vo = new LocationMatchVO();
        vo.setId(VoConvert.longValue(row, "id"));
        vo.setSpotId(VoConvert.longValue(row, "spotId"));
        vo.setPoiId(VoConvert.longValue(row, "poiId"));
        vo.setPlaceGroupId(VoConvert.longValue(row, "placeGroupId"));
        vo.setName(VoConvert.string(row, "name"));
        vo.setType(VoConvert.string(row, "type"));
        vo.setLongitude(VoConvert.decimal(row, "longitude"));
        vo.setLatitude(VoConvert.decimal(row, "latitude"));
        vo.setDistance(distance);
        vo.setConfidence(confidence(distance, accuracy));
        return vo;
    }

    private static String confidence(Integer distance, int accuracy) {
        if (distance == null) {
            return "low";
        }
        int highThreshold = Math.max((int) Math.ceil(accuracy * 1.5), 50);
        int mediumThreshold = Math.max(accuracy * 3, 120);
        if (distance <= highThreshold) {
            return "high";
        }
        if (distance <= mediumThreshold) {
            return "medium";
        }
        return "low";
    }
}
