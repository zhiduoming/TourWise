package com.tourwise.service;

import com.tourwise.dto.*;
import com.tourwise.mapper.*;
import com.tourwise.model.*;

import com.tourwise.common.BusinessException;
import com.tourwise.common.MapUtil;
import com.tourwise.common.PageResult;
import com.tourwise.common.TopKSelector;
import com.tourwise.security.AuthContext;
import com.tourwise.vo.common.ActionResultVO;
import com.tourwise.vo.recommend.RecommendVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

@Service
public class RecommendService {
    private static final Set<String> TARGET_TYPES = Set.of("poi", "food", "log");

    private final RecommendMapper recommendMapper;

    public RecommendService(RecommendMapper recommendMapper) {
        this.recommendMapper = recommendMapper;
    }

    public List<RecommendVO> hotTop10() {
        // 用大小为 K=10 的最小堆从全部候选中选出 TopK，时间 O(N log K)，无需对全部数据完全排序。
        // 综合得分 = 热度 * 0.6 + 评分 * 0.25 * 20（评分 0-5 归一到 0-100）+ 访问量 * 0.15。
        List<Map<String, Object>> candidates = recommendMapper.hotCandidates();
        Comparator<Map<String, Object>> byScore = Comparator.comparingDouble(RecommendService::compositeHotScore);
        TopKSelector<Map<String, Object>> selector = new TopKSelector<>(10, byScore);
        for (Map<String, Object> row : candidates) {
            selector.offer(MapUtil.normalize(row));
        }
        return selector.toListDescending().stream().map(RecommendVO::from).toList();
    }

    /** 热度推荐综合得分：把热度、评分、访问量按经验权重融合，便于堆排序按单一维度比较。 */
    private static double compositeHotScore(Map<String, Object> row) {
        double hotness = toDouble(row.get("hotness"));
        double rating = toDouble(row.get("rating"));
        double visits = toDouble(row.get("visits"));
        return hotness * 0.6 + rating * 20.0 * 0.25 + visits * 0.15;
    }

    private static double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    public PageResult<RecommendVO> list(
            String strategy,
            String scene,
            String type,
            String category,
            String distance,
            String purpose,
            String city,
            boolean routeRequired,
            boolean foodRequired,
            boolean avoidVisited,
            int page,
            int pageSize) {
        Long userId = AuthContext.getUserId();
        String normalizedStrategy = normalizeStrategy(firstText(strategy, type));
        String normalizedScene = normalizeScene(firstText(scene, category));
        String normalizedPurpose = normalizePurpose(purpose);
        String normalizedCity = trimToNull(city);
        int offset = (page - 1) * pageSize;
        List<Map<String, Object>> rows = recommendMapper.list(
                userId,
                normalizedStrategy,
                normalizedScene,
                normalizedPurpose,
                normalizedCity,
                routeRequired,
                foodRequired,
                avoidVisited,
                offset,
                pageSize);
        long total = recommendMapper.count(
                userId,
                normalizedScene,
                normalizedCity,
                routeRequired,
                foodRequired,
                avoidVisited);
        return new PageResult<>(rows.stream()
                .map(MapUtil::normalize)
                .map(row -> enrichReason(row, normalizedStrategy, normalizedScene, normalizedPurpose, routeRequired, foodRequired, avoidVisited))
                .map(RecommendVO::from)
                .toList(), total);
    }

    public ActionResultVO rate(RatingRequest request) {
        long userId = AuthContext.requireUserId();
        String targetType = request.getTargetType().trim();
        if (!TARGET_TYPES.contains(targetType)) {
            throw BusinessException.badRequest("评分对象类型不合法");
        }
        if (!recommendMapper.targetExists(targetType, request.getTargetId())) {
            throw BusinessException.notFound("评分对象不存在");
        }
        recommendMapper.upsertRating(userId, targetType, request.getTargetId(), request.getRating());
        if ("poi".equals(targetType)) {
            recommendMapper.refreshPoiRating(request.getTargetId());
        } else if ("food".equals(targetType)) {
            recommendMapper.refreshFoodRating(request.getTargetId());
        }
        return ActionResultVO.rated();
    }

    private static Map<String, Object> enrichReason(
            Map<String, Object> row,
            String strategy,
            String scene,
            String purpose,
            boolean routeRequired,
            boolean foodRequired,
            boolean avoidVisited) {
        String sceneReason = switch (scene) {
            case "campus" -> "高校和校区数据更完整，适合展示校园 POI 与路线规划能力";
            case "culture" -> "文化和历史属性突出，适合博物馆、古迹和城市文化参观";
            case "nature" -> "自然景观和开放空间占比高，适合休闲观光";
            case "city" -> "城市地标和商圈热度较高，适合城市漫游";
            case "family" -> "主题游乐和轻松游览属性更强，适合亲子或轻量行程";
            default -> "综合热度、评分和访问量生成推荐";
        };
        List<String> reasons = new ArrayList<>();
        reasons.add(sceneReason);
        String purposeReason = switch (purpose) {
            case "photo" -> "更适合拍照打卡和内容分享";
            case "study" -> "研学属性更强，适合高校、博物馆或主题参观";
            case "food" -> "周边或内部美食数据更丰富";
            case "relax" -> "休闲属性更强，适合轻松游览";
            case "route" -> "路线规划条件更好，适合直接生成游览路线";
            default -> null;
        };
        if (purposeReason != null) {
            reasons.add(purposeReason);
        }
        if (routeRequired && hasRouteGraph(row)) {
            reasons.add("已配置内部路网，可展示平面图路线");
        }
        int foodCount = intValue(row.get("foodCount"));
        if (foodRequired && foodCount > 0) {
            reasons.add("已关联 " + foodCount + " 个美食点");
        }
        int logCount = intValue(row.get("logCount"));
        if (logCount > 0) {
            reasons.add("有 " + logCount + " 篇用户日志可参考");
        }
        if (avoidVisited) {
            reasons.add("已避开你标记去过的景点");
        }
        if (bool(row.get("favoriteMatched"))) {
            reasons.add("你收藏过这个目的地");
        }
        if (bool(row.get("wantMatched"))) {
            reasons.add("你标记过想去");
        }
        if (bool(row.get("visitedMatched"))) {
            reasons.add("你曾标记去过，可用于回顾或二刷");
        }
        if (bool(row.get("dislikeMatched"))) {
            reasons.add("你已标记不感兴趣，系统会减少推荐");
        }
        int browseCount = intValue(row.get("browseCount"));
        if (browseCount > 0) {
            reasons.add("你最近浏览过 " + browseCount + " 次");
        }
        if (decimalPositive(row.get("userRating"))) {
            reasons.add("你曾给出较高评分");
        }
        if (decimalPositive(row.get("preferenceScore"))) {
            reasons.add("与你的兴趣标签匹配");
        }
        if (intValue(row.get("similarActionCount")) > 0) {
            reasons.add("和你互动过的同类景点相似");
        }
        if (reasons.size() == 1) {
            reasons.add(switch (strategy) {
                case "rating" -> "当前按评分优先排序";
                case "interest" -> "当前按兴趣行为、评分和热度综合排序";
                default -> "当前按热度和访问量优先排序";
            });
        }
        row.put("recommendReason", String.join("；", reasons));
        return row;
    }

    private static boolean hasRouteGraph(Map<String, Object> row) {
        String status = String.valueOf(row.getOrDefault("routeGraphStatus", "none"));
        return "draft".equals(status) || "verified".equals(status);
    }

    private static boolean bool(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private static int intValue(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static boolean decimalPositive(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue() > 0;
        }
        if (value == null) {
            return false;
        }
        try {
            return Double.parseDouble(value.toString()) > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static String normalizeStrategy(String type) {
        String value = trimToNull(type);
        if (value == null) {
            return "hot";
        }
        if (!Set.of("hot", "rating", "interest").contains(value)) {
            return "hot";
        }
        return value;
    }

    private static String normalizeScene(String scene) {
        String value = trimToNull(scene);
        if (value == null) {
            return "all";
        }
        if (!Set.of("all", "campus", "culture", "nature", "city", "family").contains(value)) {
            return "all";
        }
        return value;
    }

    private static String normalizePurpose(String purpose) {
        String value = trimToNull(purpose);
        if (value == null) {
            return "balanced";
        }
        if (!Set.of("balanced", "photo", "study", "food", "relax", "route").contains(value)) {
            return "balanced";
        }
        return value;
    }

    private static String firstText(String first, String second) {
        String value = trimToNull(first);
        return value != null ? value : trimToNull(second);
    }

    private static Integer parseDistance(String distance) {
        String value = trimToNull(distance);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
