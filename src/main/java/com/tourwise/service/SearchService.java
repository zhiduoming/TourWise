package com.tourwise.service;

import com.tourwise.dto.*;
import com.tourwise.mapper.*;
import com.tourwise.model.*;

import com.tourwise.common.BusinessException;
import com.tourwise.common.MapUtil;
import com.tourwise.common.PageResult;
import com.tourwise.vo.search.FacilityVO;
import com.tourwise.vo.search.PoiTypeVO;
import com.tourwise.vo.search.SearchTagVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class SearchService {
    private final SearchMapper searchMapper;

    public SearchService(SearchMapper searchMapper) {
        this.searchMapper = searchMapper;
    }

    public List<PoiTypeVO> listTypes() {
        return searchMapper.listTypes().stream().map(MapUtil::normalize).map(PoiTypeVO::from).toList();
    }

    public List<SearchTagVO> listSearchTags() {
        return List.of(
                new SearchTagVO("scenery", "风景"),
                new SearchTagVO("culture", "文化"),
                new SearchTagVO("museum", "博物馆"),
                new SearchTagVO("university", "高校"),
                new SearchTagVO("landmark", "地标"),
                new SearchTagVO("park", "自然公园")
        );
    }

    public PageResult<FacilityVO> searchFacilities(
            String keyword,
            String type,
            String area,
            String scene,
            Long placeGroupId,
            String province,
            String city,
            String tag,
            String shortName,
            Boolean spotOnly,
            int page,
            int pageSize) {
        String normalizedKeyword = trimToNull(keyword);
        String normalizedType = trimToNull(type);
        String normalizedArea = trimToNull(area);
        String normalizedScene = trimToNull(scene);
        String normalizedProvince = trimToNull(province);
        String normalizedCity = trimToNull(city);
        String normalizedTag = trimToNull(tag);
        String normalizedShortName = trimToNull(shortName);
        int offset = (page - 1) * pageSize;
        List<Map<String, Object>> rows = searchMapper.searchFacilities(
                normalizedKeyword,
                normalizedType,
                normalizedArea,
                normalizedScene,
                placeGroupId,
                normalizedProvince,
                normalizedCity,
                normalizedTag,
                normalizedShortName,
                Boolean.TRUE.equals(spotOnly),
                offset,
                pageSize);
        long total = searchMapper.countFacilities(
                normalizedKeyword,
                normalizedType,
                normalizedArea,
                normalizedScene,
                placeGroupId,
                normalizedProvince,
                normalizedCity,
                normalizedTag,
                normalizedShortName,
                Boolean.TRUE.equals(spotOnly));
        return new PageResult<>(rows.stream().map(MapUtil::normalize).map(FacilityVO::from).toList(), total);
    }

    public FacilityVO getFacility(Long id) {
        Map<String, Object> row = searchMapper.findFacilityById(id);
        if (row == null) {
            throw BusinessException.notFound("设施或景点不存在");
        }
        return FacilityVO.from(MapUtil.normalize(row));
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
