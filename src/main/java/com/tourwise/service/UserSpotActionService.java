package com.tourwise.service;

import com.tourwise.common.BusinessException;
import com.tourwise.common.MapUtil;
import com.tourwise.common.PageResult;
import com.tourwise.dto.SpotActionRequest;
import com.tourwise.mapper.UserSpotActionMapper;
import com.tourwise.security.AuthContext;
import com.tourwise.vo.search.FacilityVO;
import com.tourwise.vo.user.SpotActionStateVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UserSpotActionService {
    private static final String TARGET_POI = "poi";
    private static final String ACTION_WANT = "want";
    private static final String ACTION_VISITED = "visited";
    private static final String ACTION_DISLIKE = "dislike";
    private static final BigDecimal BROWSE_WEIGHT = new BigDecimal("0.20");
    private static final BigDecimal FAVORITE_WEIGHT = new BigDecimal("1.20");
    private static final BigDecimal WANT_WEIGHT = new BigDecimal("0.90");
    private static final BigDecimal VISITED_WEIGHT = new BigDecimal("0.45");
    private static final BigDecimal DISLIKE_WEIGHT = new BigDecimal("-1.50");

    private final UserSpotActionMapper userSpotActionMapper;

    public UserSpotActionService(UserSpotActionMapper userSpotActionMapper) {
        this.userSpotActionMapper = userSpotActionMapper;
    }

    public SpotActionStateVO state(String targetType, Long targetId) {
        long userId = AuthContext.requireUserId();
        String normalizedType = normalizeTarget(targetType);
        validateTarget(normalizedType, targetId);
        return buildState(userId, normalizedType, targetId);
    }

    @Transactional
    public SpotActionStateVO toggleFavorite(SpotActionRequest request) {
        long userId = AuthContext.requireUserId();
        String targetType = normalizeTarget(request.getTargetType());
        Long targetId = request.getTargetId();
        validateTarget(targetType, targetId);
        if (userSpotActionMapper.existsFavorite(userId, targetType, targetId) > 0) {
            userSpotActionMapper.deleteFavorite(userId, targetType, targetId);
        } else {
            userSpotActionMapper.upsertAction(userId, targetType, targetId, ACTION_DISLIKE, false);
            userSpotActionMapper.insertFavorite(userId, targetType, targetId);
            learnPreference(userId, targetId, "favorite", FAVORITE_WEIGHT);
        }
        return buildState(userId, targetType, targetId);
    }

    @Transactional
    public SpotActionStateVO toggleWant(SpotActionRequest request) {
        return toggleAction(request, ACTION_WANT);
    }

    @Transactional
    public SpotActionStateVO toggleVisited(SpotActionRequest request) {
        return toggleAction(request, ACTION_VISITED);
    }

    @Transactional
    public SpotActionStateVO dislike(SpotActionRequest request) {
        long userId = AuthContext.requireUserId();
        String targetType = normalizeTarget(request.getTargetType());
        Long targetId = request.getTargetId();
        validateTarget(targetType, targetId);
        boolean alreadyDisliked = active(userSpotActionMapper.findActionActive(userId, targetType, targetId, ACTION_DISLIKE));
        userSpotActionMapper.deleteFavorite(userId, targetType, targetId);
        userSpotActionMapper.upsertAction(userId, targetType, targetId, ACTION_WANT, false);
        userSpotActionMapper.upsertAction(userId, targetType, targetId, ACTION_DISLIKE, true);
        if (!alreadyDisliked) {
            learnPreference(userId, targetId, ACTION_DISLIKE, DISLIKE_WEIGHT);
        }
        return buildState(userId, targetType, targetId);
    }

    @Transactional
    public SpotActionStateVO recordBrowse(SpotActionRequest request) {
        long userId = AuthContext.requireUserId();
        String targetType = normalizeTarget(request.getTargetType());
        Long targetId = request.getTargetId();
        validateTarget(targetType, targetId);
        userSpotActionMapper.insertBrowsingHistory(userId, targetType, targetId);
        learnPreference(userId, targetId, "browse", BROWSE_WEIGHT);
        userSpotActionMapper.incrementPoiVisit(targetId);
        userSpotActionMapper.incrementSpotVisitByPoi(targetId);
        return buildState(userId, targetType, targetId);
    }

    public PageResult<FacilityVO> list(String type, int page, int pageSize) {
        long userId = AuthContext.requireUserId();
        String normalizedType = StringUtils.hasText(type) ? type.trim() : "favorite";
        int offset = (page - 1) * pageSize;
        List<java.util.Map<String, Object>> rows;
        long total;
        switch (normalizedType) {
            case "favorite" -> {
                rows = userSpotActionMapper.listFavoritePois(userId, offset, pageSize);
                total = userSpotActionMapper.countFavoritePois(userId);
            }
            case ACTION_WANT, ACTION_VISITED, ACTION_DISLIKE -> {
                rows = userSpotActionMapper.listUserActionPois(userId, normalizedType, offset, pageSize);
                total = userSpotActionMapper.countUserActionPois(userId, normalizedType);
            }
            case "history" -> {
                rows = userSpotActionMapper.listBrowsingPois(userId, offset, pageSize);
                total = userSpotActionMapper.countBrowsingPois(userId);
            }
            default -> throw BusinessException.badRequest("行为类型不支持");
        }
        return new PageResult<>(rows.stream().map(MapUtil::normalize).map(FacilityVO::from).toList(), total);
    }

    private SpotActionStateVO toggleAction(SpotActionRequest request, String actionType) {
        long userId = AuthContext.requireUserId();
        String targetType = normalizeTarget(request.getTargetType());
        Long targetId = request.getTargetId();
        validateTarget(targetType, targetId);
        Integer current = userSpotActionMapper.findActionActive(userId, targetType, targetId, actionType);
        boolean active = current == null || current == 0;
        if (active && ACTION_WANT.equals(actionType)) {
            userSpotActionMapper.upsertAction(userId, targetType, targetId, ACTION_DISLIKE, false);
        }
        userSpotActionMapper.upsertAction(userId, targetType, targetId, actionType, active);
        if (active) {
            learnPreference(userId, targetId, actionType, ACTION_WANT.equals(actionType) ? WANT_WEIGHT : VISITED_WEIGHT);
        }
        return buildState(userId, targetType, targetId);
    }

    private void learnPreference(Long userId, Long targetId, String source, BigDecimal weight) {
        userSpotActionMapper.upsertPreferenceFromPoi(userId, targetId, source, weight);
    }

    private SpotActionStateVO buildState(Long userId, String targetType, Long targetId) {
        boolean favorite = userSpotActionMapper.existsFavorite(userId, targetType, targetId) > 0;
        boolean wantToGo = active(userSpotActionMapper.findActionActive(userId, targetType, targetId, ACTION_WANT));
        boolean visited = active(userSpotActionMapper.findActionActive(userId, targetType, targetId, ACTION_VISITED));
        boolean disliked = active(userSpotActionMapper.findActionActive(userId, targetType, targetId, ACTION_DISLIKE));
        return new SpotActionStateVO(targetType, targetId, favorite, wantToGo, visited, disliked);
    }

    private static boolean active(Integer value) {
        return value != null && value == 1;
    }

    private String normalizeTarget(String targetType) {
        String normalized = StringUtils.hasText(targetType) ? targetType.trim().toLowerCase() : "";
        if (!TARGET_POI.equals(normalized)) {
            throw BusinessException.badRequest("当前仅支持景点/POI行为");
        }
        return normalized;
    }

    private void validateTarget(String targetType, Long targetId) {
        if (targetId == null || targetId <= 0) {
            throw BusinessException.badRequest("对象ID不合法");
        }
        if (TARGET_POI.equals(targetType) && userSpotActionMapper.existsPoi(targetId) == 0) {
            throw BusinessException.notFound("景点不存在或已停用");
        }
    }
}
