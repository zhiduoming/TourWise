package com.tourwise.service;

import com.tourwise.common.BusinessException;
import com.tourwise.common.MapUtil;
import com.tourwise.common.PageResult;
import com.tourwise.dto.ItineraryGenerateRequest;
import com.tourwise.dto.ItineraryPlanSaveRequest;
import com.tourwise.dto.ItineraryDaySaveRequest;
import com.tourwise.dto.ItineraryItemSaveRequest;
import com.tourwise.mapper.ItineraryMapper;
import com.tourwise.mapper.ItineraryPlanMapper;
import com.tourwise.model.ItineraryPlanItemRecord;
import com.tourwise.model.ItineraryPlanRecord;
import com.tourwise.security.AuthContext;
import com.tourwise.vo.VoConvert;
import com.tourwise.vo.itinerary.ItineraryDayVO;
import com.tourwise.vo.itinerary.ItineraryFavoriteStateVO;
import com.tourwise.vo.itinerary.ItineraryItemVO;
import com.tourwise.vo.itinerary.ItineraryPlanVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class ItineraryService {
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ItineraryMapper itineraryMapper;
    private final ItineraryPlanMapper itineraryPlanMapper;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ItineraryService(
            ItineraryMapper itineraryMapper,
            ItineraryPlanMapper itineraryPlanMapper,
            NotificationService notificationService) {
        this.itineraryMapper = itineraryMapper;
        this.itineraryPlanMapper = itineraryPlanMapper;
        this.notificationService = notificationService;
    }

    public ItineraryPlanVO generate(ItineraryGenerateRequest request) {
        String city = trimToDefault(request.getCity(), "北京");
        String duration = normalizeDuration(request.getDuration());
        String pace = normalizePace(request.getPace());
        String purpose = normalizePurpose(request.getPurpose());
        String budgetLevel = normalizeBudgetLevel(request.getBudgetLevel());
        boolean includeFood = request.getIncludeFood() == null || request.getIncludeFood();
        boolean routeRequired = Boolean.TRUE.equals(request.getRouteRequired());
        boolean avoidVisited = Boolean.TRUE.equals(request.getAvoidVisited());
        List<String> preferences = normalizePreferences(request.getPreferences());

        int days = daysOf(duration);
        int spotsPerDay = spotsPerDay(duration, pace);
        int spotLimit = days * spotsPerDay;
        Long userId = AuthContext.getUserId();
        List<Map<String, Object>> spots = itineraryMapper.listCandidateSpots(
                        userId,
                        city,
                        preferences,
                        purpose,
                        routeRequired,
                        avoidVisited,
                        Math.max(spotLimit * 3, 12))
                .stream()
                .map(MapUtil::normalize)
                .toList();
        if (spots.isEmpty()) {
            throw BusinessException.notFound("当前城市下没有可生成行程的景点数据");
        }

        List<Map<String, Object>> selectedSpots = orderByNearestNeighbor(spots.stream().limit(spotLimit).toList());
        List<Long> spotIds = selectedSpots.stream()
                .map(row -> VoConvert.longValue(row, "spotId"))
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        List<Map<String, Object>> foods = includeFood
                ? itineraryMapper.listCandidateFoods(city, spotIds, budgetLevel, Math.max(days * 2, 4)).stream().map(MapUtil::normalize).toList()
                : List.of();

        ItineraryPlanVO plan = new ItineraryPlanVO();
        plan.setCity(city);
        plan.setDuration(duration);
        plan.setPace(pace);
        plan.setTotalDays(days);
        plan.setSpotCount(selectedSpots.size());
        plan.setPreferences(preferences);
        plan.setSummary(buildSummary(city, duration, pace, selectedSpots.size(), includeFood, purpose, routeRequired));
        plan.setRouteHint(buildRouteHint(selectedSpots));
        plan.setDays(buildDays(selectedSpots, foods, days, spotsPerDay, includeFood));
        plan.setTips(buildTips(includeFood, foods.isEmpty(), userId != null, routeRequired, avoidVisited));
        return plan;
    }

    @Transactional
    public ItineraryPlanVO save(ItineraryPlanSaveRequest request) {
        long userId = AuthContext.requireUserId();
        validateSaveRequest(request);

        ItineraryPlanRecord record = new ItineraryPlanRecord();
        record.setUserId(userId);
        record.setTitle(trimToDefault(request.getTitle(), defaultTitle(request)));
        record.setCity(trimToDefault(request.getCity(), "未知城市"));
        record.setDuration(normalizeDuration(request.getDuration()));
        record.setPace(normalizePace(request.getPace()));
        record.setTotalDays(request.getTotalDays() == null || request.getTotalDays() <= 0 ? request.getDays().size() : request.getTotalDays());
        record.setSpotCount(request.getSpotCount() == null ? countSpotItems(request.getDays()) : Math.max(0, request.getSpotCount()));
        record.setSummary(trimToDefault(request.getSummary(), record.getTitle()));
        record.setPreferencesJson(toJson(normalizePreferences(request.getPreferences())));
        itineraryPlanMapper.insertPlan(record);

        for (ItineraryDaySaveRequest day : request.getDays()) {
            int dayNo = day.getDayNo() == null || day.getDayNo() <= 0 ? 1 : day.getDayNo();
            List<ItineraryItemSaveRequest> items = day.getItems() == null ? List.of() : day.getItems();
            for (int i = 0; i < items.size(); i++) {
                ItineraryItemSaveRequest source = items.get(i);
                ItineraryPlanItemRecord item = new ItineraryPlanItemRecord();
                item.setPlanId(record.getId());
                item.setDayNo(dayNo);
                item.setOrderNo(source.getOrderNo() == null || source.getOrderNo() <= 0 ? i + 1 : source.getOrderNo());
                item.setItemType(normalizeItemType(source.getItemType()));
                item.setTimeSlot(trimToDefault(source.getTimeSlot(), "待安排"));
                item.setTargetId(source.getTargetId());
                item.setSpotId(source.getSpotId());
                item.setPlaceGroupId(source.getPlaceGroupId());
                item.setName(trimToDefault(source.getName(), item.getItemType().equals("food") ? "美食节点" : "景点节点"));
                item.setDescription(trimToNull(source.getDescription()));
                item.setAddress(trimToNull(source.getAddress()));
                item.setImageUrl(trimToNull(source.getImage()));
                item.setRating(source.getRating());
                item.setHotness(source.getHotness());
                item.setLongitude(source.getLongitude());
                item.setLatitude(source.getLatitude());
                item.setRecommendReason(trimToNull(source.getRecommendReason()));
                itineraryPlanMapper.insertItem(item);
            }
        }
        return detail(record.getId());
    }

    public PageResult<ItineraryPlanVO> list(int page, int pageSize) {
        long userId = AuthContext.requireUserId();
        int offset = (page - 1) * pageSize;
        List<ItineraryPlanVO> list = itineraryPlanMapper.listPlans(userId, offset, pageSize)
                .stream()
                .map(MapUtil::normalize)
                .map(row -> buildPlan(row, false))
                .toList();
        return new PageResult<>(list, itineraryPlanMapper.countPlans(userId));
    }

    public PageResult<ItineraryPlanVO> favoritePlans(int page, int pageSize) {
        long userId = AuthContext.requireUserId();
        int offset = (page - 1) * pageSize;
        List<ItineraryPlanVO> list = itineraryPlanMapper.listFavoritePlans(userId, offset, pageSize)
                .stream()
                .map(MapUtil::normalize)
                .map(row -> buildPlan(row, false))
                .toList();
        return new PageResult<>(list, itineraryPlanMapper.countFavoritePlans(userId));
    }

    public ItineraryPlanVO detail(Long id) {
        long userId = AuthContext.requireUserId();
        Map<String, Object> raw = itineraryPlanMapper.findPlanById(id, userId);
        if (raw == null) {
            throw BusinessException.notFound("行程不存在或无权访问");
        }
        return buildPlan(MapUtil.normalize(raw), true);
    }

    public ItineraryPlanVO sharedDetail(Long id) {
        long userId = AuthContext.requireUserId();
        Map<String, Object> raw = itineraryPlanMapper.findAccessiblePlanById(id, userId);
        if (raw == null) {
            throw BusinessException.notFound("行程不存在或未被分享");
        }
        return buildPlan(MapUtil.normalize(raw), true);
    }

    public List<ItineraryPlanVO> hotSharedPlans(int limit) {
        long userId = AuthContext.requireUserId();
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        return itineraryPlanMapper.listHotSharedPlans(userId, safeLimit)
                .stream()
                .map(MapUtil::normalize)
                .map(row -> buildPlan(row, false))
                .toList();
    }

    @Transactional
    public ItineraryPlanVO copy(Long id) {
        long userId = AuthContext.requireUserId();
        Map<String, Object> source = itineraryPlanMapper.findAccessiblePlanById(id, userId);
        if (source == null) {
            throw BusinessException.notFound("行程不存在或未被分享");
        }
        Map<String, Object> notificationInfo = itineraryPlanMapper.findNotificationInfo(id);
        Map<String, Object> normalized = MapUtil.normalize(source);
        ItineraryPlanRecord record = new ItineraryPlanRecord();
        record.setUserId(userId);
        record.setSourcePlanId(id);
        record.setTitle(copiedTitle(VoConvert.string(normalized, "title")));
        record.setCity(trimToDefault(VoConvert.string(normalized, "city"), "未知城市"));
        record.setDuration(normalizeDuration(VoConvert.string(normalized, "duration")));
        record.setPace(normalizePace(VoConvert.string(normalized, "pace")));
        record.setTotalDays(Math.max(1, intOrDefault(VoConvert.intValue(normalized, "totalDays"), 1)));
        record.setSpotCount(Math.max(0, intOrDefault(VoConvert.intValue(normalized, "spotCount"), 0)));
        record.setSummary(trimToDefault(VoConvert.string(normalized, "summary"), record.getTitle()));
        record.setPreferencesJson(toJson(parsePreferences(VoConvert.string(normalized, "preferencesJson"))));
        itineraryPlanMapper.insertPlan(record);

        List<Map<String, Object>> items = itineraryPlanMapper.listItems(id)
                .stream()
                .map(MapUtil::normalize)
                .toList();
        for (Map<String, Object> sourceItem : items) {
            ItineraryPlanItemRecord item = new ItineraryPlanItemRecord();
            item.setPlanId(record.getId());
            item.setDayNo(intOrDefault(VoConvert.intValue(sourceItem, "dayNo"), 1));
            item.setOrderNo(intOrDefault(VoConvert.intValue(sourceItem, "orderNo"), 1));
            item.setItemType(normalizeItemType(VoConvert.string(sourceItem, "itemType")));
            item.setTimeSlot(trimToNull(VoConvert.string(sourceItem, "timeSlot")));
            item.setTargetId(VoConvert.longValue(sourceItem, "targetId"));
            item.setSpotId(VoConvert.longValue(sourceItem, "spotId"));
            item.setPlaceGroupId(VoConvert.longValue(sourceItem, "placeGroupId"));
            item.setName(trimToDefault(VoConvert.string(sourceItem, "name"), "行程节点"));
            item.setDescription(trimToNull(VoConvert.string(sourceItem, "description")));
            item.setAddress(trimToNull(VoConvert.string(sourceItem, "address")));
            item.setImageUrl(trimToNull(VoConvert.string(sourceItem, "image")));
            item.setRating(VoConvert.decimal(sourceItem, "rating"));
            item.setHotness(VoConvert.intValue(sourceItem, "hotness"));
            item.setLongitude(VoConvert.decimal(sourceItem, "longitude"));
            item.setLatitude(VoConvert.decimal(sourceItem, "latitude"));
            item.setRecommendReason(trimToNull(VoConvert.string(sourceItem, "recommendReason")));
            itineraryPlanMapper.insertItem(item);
        }
        itineraryPlanMapper.increaseCopyCount(id);
        if (notificationInfo != null) {
            Map<String, Object> normalizedNotificationInfo = MapUtil.normalize(notificationInfo);
            notificationService.notifyItineraryCopied(
                    VoConvert.longValue(normalizedNotificationInfo, "userId"),
                    userId,
                    id,
                    VoConvert.string(normalizedNotificationInfo, "title"));
        }
        return detail(record.getId());
    }

    @Transactional
    public ItineraryFavoriteStateVO toggleFavorite(Long id) {
        long userId = AuthContext.requireUserId();
        Map<String, Object> source = itineraryPlanMapper.findAccessiblePlanById(id, userId);
        if (source == null) {
            throw BusinessException.notFound("行程不存在或未被分享");
        }
        boolean favorited;
        if (itineraryPlanMapper.existsFavorite(userId, id) > 0) {
            itineraryPlanMapper.deleteFavorite(userId, id);
            itineraryPlanMapper.decreaseFavoriteCount(id);
            favorited = false;
        } else {
            int inserted = itineraryPlanMapper.insertFavorite(userId, id);
            if (inserted > 0) {
                itineraryPlanMapper.increaseFavoriteCount(id);
            }
            favorited = true;
        }
        return new ItineraryFavoriteStateVO(id, favorited, itineraryPlanMapper.countFavorites(id));
    }

    @Transactional
    public void delete(Long id) {
        long userId = AuthContext.requireUserId();
        int affected = itineraryPlanMapper.softDeletePlan(id, userId);
        if (affected == 0) {
            throw BusinessException.notFound("行程不存在或无权删除");
        }
    }

    private List<ItineraryDayVO> buildDays(
            List<Map<String, Object>> spots,
            List<Map<String, Object>> foods,
            int days,
            int spotsPerDay,
            boolean includeFood) {
        List<ItineraryDayVO> result = new ArrayList<>();
        int spotIndex = 0;
        int foodIndex = 0;
        for (int day = 1; day <= days; day++) {
            ItineraryDayVO dayVO = new ItineraryDayVO();
            dayVO.setDayNo(day);
            dayVO.setTitle(days == 1 ? "当日行程" : "第 " + day + " 天");
            dayVO.setSummary("按评分、热度和你的行为偏好筛选，建议按当前顺序游览。");
            int orderNo = 1;

            for (int slot = 0; slot < spotsPerDay && spotIndex < spots.size(); slot++) {
                if (includeFood && slot == 1 && foodIndex < foods.size()) {
                    dayVO.getItems().add(foodItem(foods.get(foodIndex++), orderNo++, "午餐"));
                }
                String timeSlot = switch (slot) {
                    case 0 -> "上午";
                    case 1 -> "下午";
                    default -> "傍晚";
                };
                dayVO.getItems().add(spotItem(spots.get(spotIndex++), orderNo++, timeSlot));
            }
            if (includeFood && foodIndex < foods.size() && dayVO.getItems().stream().noneMatch(item -> "food".equals(item.getItemType()))) {
                dayVO.getItems().add(foodItem(foods.get(foodIndex++), orderNo, spotsPerDay <= 2 ? "简餐" : "晚餐"));
            }
            dayVO.setEstimatedDistanceM(estimateDayDistance(dayVO));
            result.add(dayVO);
        }
        return result;
    }

    private ItineraryItemVO spotItem(Map<String, Object> row, int orderNo, String timeSlot) {
        ItineraryItemVO item = new ItineraryItemVO();
        item.setOrderNo(orderNo);
        item.setItemType("spot");
        item.setTimeSlot(timeSlot);
        item.setTargetId(VoConvert.longValue(row, "id"));
        item.setSpotId(VoConvert.longValue(row, "spotId"));
        item.setPlaceGroupId(VoConvert.longValue(row, "placeGroupId"));
        item.setName(VoConvert.string(row, "name"));
        item.setDescription(VoConvert.string(row, "description"));
        item.setAddress(VoConvert.string(row, "address"));
        item.setImage(VoConvert.string(row, "image"));
        item.setRating(VoConvert.decimal(row, "rating"));
        item.setHotness(VoConvert.intValue(row, "hotness"));
        item.setLongitude(VoConvert.decimal(row, "longitude"));
        item.setLatitude(VoConvert.decimal(row, "latitude"));
        item.setRecommendReason(buildSpotReason(row));
        return item;
    }

    private ItineraryItemVO foodItem(Map<String, Object> row, int orderNo, String timeSlot) {
        ItineraryItemVO item = new ItineraryItemVO();
        item.setOrderNo(orderNo);
        item.setItemType("food");
        item.setTimeSlot(timeSlot);
        item.setTargetId(VoConvert.longValue(row, "id"));
        item.setSpotId(VoConvert.longValue(row, "spotId"));
        item.setName(VoConvert.string(row, "name"));
        item.setDescription(VoConvert.string(row, "description"));
        item.setAddress(VoConvert.string(row, "address"));
        item.setImage(VoConvert.string(row, "image"));
        item.setRating(VoConvert.decimal(row, "rating"));
        item.setHotness(VoConvert.intValue(row, "hotness"));
        item.setRecommendReason("按关联景点、评分和人气插入餐饮节点");
        return item;
    }

    private ItineraryPlanVO buildPlan(Map<String, Object> row, boolean includeItems) {
        ItineraryPlanVO plan = new ItineraryPlanVO();
        Long id = VoConvert.longValue(row, "id");
        plan.setId(id);
        plan.setTitle(VoConvert.string(row, "title"));
        plan.setCity(VoConvert.string(row, "city"));
        plan.setDuration(VoConvert.string(row, "duration"));
        plan.setPace(VoConvert.string(row, "pace"));
        plan.setTotalDays(VoConvert.intValue(row, "totalDays"));
        plan.setSpotCount(VoConvert.intValue(row, "spotCount"));
        plan.setCopyCount(intOrDefault(VoConvert.intValue(row, "copyCount"), 0));
        plan.setFavoriteCount(intOrDefault(VoConvert.intValue(row, "favoriteCount"), 0));
        plan.setFavorited(Boolean.TRUE.equals(VoConvert.bool(row, "favorited")));
        plan.setSourcePlanId(VoConvert.longValue(row, "sourcePlanId"));
        plan.setSourcePlanTitle(VoConvert.string(row, "sourcePlanTitle"));
        plan.setOwnerName(VoConvert.string(row, "ownerName"));
        plan.setSummary(VoConvert.string(row, "summary"));
        plan.setCreatedAt(VoConvert.string(row, "createdAt"));
        plan.setUpdatedAt(VoConvert.string(row, "updatedAt"));
        plan.setPreferences(parsePreferences(VoConvert.string(row, "preferencesJson")));
        if (includeItems && id != null) {
            plan.setDays(buildSavedDays(itineraryPlanMapper.listItems(id).stream().map(MapUtil::normalize).toList()));
        }
        return plan;
    }

    private List<ItineraryDayVO> buildSavedDays(List<Map<String, Object>> rows) {
        Map<Integer, ItineraryDayVO> days = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Integer dayNo = VoConvert.intValue(row, "dayNo");
            if (dayNo == null || dayNo <= 0) {
                dayNo = 1;
            }
            ItineraryDayVO day = days.computeIfAbsent(dayNo, key -> {
                ItineraryDayVO item = new ItineraryDayVO();
                item.setDayNo(key);
                item.setTitle("第 " + key + " 天");
                item.setSummary("已保存行程");
                return item;
            });
            day.getItems().add(savedItem(row));
        }
        return new ArrayList<>(days.values());
    }

    private ItineraryItemVO savedItem(Map<String, Object> row) {
        ItineraryItemVO item = new ItineraryItemVO();
        item.setOrderNo(VoConvert.intValue(row, "orderNo"));
        item.setItemType(VoConvert.string(row, "itemType"));
        item.setTimeSlot(VoConvert.string(row, "timeSlot"));
        item.setTargetId(VoConvert.longValue(row, "targetId"));
        item.setSpotId(VoConvert.longValue(row, "spotId"));
        item.setPlaceGroupId(VoConvert.longValue(row, "placeGroupId"));
        item.setName(VoConvert.string(row, "name"));
        item.setDescription(VoConvert.string(row, "description"));
        item.setAddress(VoConvert.string(row, "address"));
        item.setImage(VoConvert.string(row, "image"));
        item.setRating(VoConvert.decimal(row, "rating"));
        item.setHotness(VoConvert.intValue(row, "hotness"));
        item.setLongitude(VoConvert.decimal(row, "longitude"));
        item.setLatitude(VoConvert.decimal(row, "latitude"));
        item.setRecommendReason(VoConvert.string(row, "recommendReason"));
        return item;
    }

    private String buildSpotReason(Map<String, Object> row) {
        List<String> reasons = new ArrayList<>();
        if (bool(row.get("favoriteMatched"))) {
            reasons.add("你收藏过");
        }
        if (bool(row.get("wantMatched"))) {
            reasons.add("你标记过想去");
        }
        if (intValue(row.get("browseCount")) > 0) {
            reasons.add("你浏览过");
        }
        if (decimalPositive(row.get("preferenceScore"))) {
            reasons.add("匹配兴趣偏好");
        }
        if (hasRouteGraph(row)) {
            reasons.add("支持内部路线展示");
        }
        int foodCount = intValue(row.get("foodCount"));
        if (foodCount > 0) {
            reasons.add("关联 " + foodCount + " 个美食点");
        }
        int logCount = intValue(row.get("logCount"));
        if (logCount > 0) {
            reasons.add("有用户日志参考");
        }
        if (reasons.isEmpty()) {
            reasons.add("评分和热度较高");
        }
        return String.join("，", reasons);
    }

    private List<String> buildTips(boolean includeFood, boolean noFood, boolean personalized, boolean routeRequired, boolean avoidVisited) {
        List<String> tips = new ArrayList<>();
        tips.add("行程顺序已按景点评分、热度、个人行为和经纬度近邻做规则优化，真实道路请进入路线规划页用高德路线校验。");
        if (personalized) {
            tips.add("已叠加你的收藏、想去、浏览和兴趣行为。");
        } else {
            tips.add("登录后会叠加个人收藏、想去和浏览历史，行程会更个性化。");
        }
        if (routeRequired) {
            tips.add("已优先筛选支持内部路网的景点，适合展示平面图路线能力。");
        }
        if (avoidVisited) {
            tips.add("已尝试避开你标记去过的景点。");
        }
        if (includeFood && noFood) {
            tips.add("当前城市或景点附近美食数据不足，暂未插入餐饮节点。");
        }
        return tips;
    }

    private String buildSummary(String city, String duration, String pace, int spotCount, boolean includeFood, String purpose, boolean routeRequired) {
        return city + " · " + durationLabel(duration) + " · " + paceLabel(pace)
                + " · " + purposeLabel(purpose)
                + "，共安排 " + spotCount + " 个景点"
                + (includeFood ? "，并尝试插入餐饮推荐" : "")
                + (routeRequired ? "，优先选择可规划景点。" : "。");
    }

    private List<Map<String, Object>> orderByNearestNeighbor(List<Map<String, Object>> spots) {
        if (spots.size() <= 2) {
            return spots;
        }
        List<Map<String, Object>> remaining = new ArrayList<>(spots);
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> current = remaining.remove(0);
        result.add(current);
        while (!remaining.isEmpty()) {
            Map<String, Object> nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (Map<String, Object> candidate : remaining) {
                double distance = geoDistanceMeters(current, candidate);
                if (distance < nearestDistance) {
                    nearest = candidate;
                    nearestDistance = distance;
                }
            }
            current = nearest == null ? remaining.remove(0) : nearest;
            remaining.remove(current);
            result.add(current);
        }
        return result;
    }

    private Integer estimateDayDistance(ItineraryDayVO day) {
        List<ItineraryItemVO> spots = day.getItems().stream()
                .filter(item -> "spot".equals(item.getItemType()))
                .filter(item -> item.getLongitude() != null && item.getLatitude() != null)
                .toList();
        if (spots.size() < 2) {
            return 0;
        }
        int total = 0;
        for (int i = 1; i < spots.size(); i++) {
            total += geoDistanceMeters(spots.get(i - 1), spots.get(i));
        }
        return total;
    }

    private String buildRouteHint(List<Map<String, Object>> spots) {
        int distance = 0;
        int routeReadyCount = 0;
        for (int i = 0; i < spots.size(); i++) {
            if (hasRouteGraph(spots.get(i))) {
                routeReadyCount++;
            }
            if (i > 0) {
                distance += geoDistanceMeters(spots.get(i - 1), spots.get(i));
            }
        }
        if (spots.size() < 2) {
            return "当前行程景点较少，暂不需要串联路线。";
        }
        return "景点间直线距离约 " + formatDistance(distance)
                + "，其中 " + routeReadyCount + " 个景点支持内部平面图路线；真实通行距离建议在路线规划页用高德校验。";
    }

    private static int geoDistanceMeters(Map<String, Object> from, Map<String, Object> to) {
        BigDecimal fromLng = VoConvert.decimal(from, "longitude");
        BigDecimal fromLat = VoConvert.decimal(from, "latitude");
        BigDecimal toLng = VoConvert.decimal(to, "longitude");
        BigDecimal toLat = VoConvert.decimal(to, "latitude");
        if (fromLng == null || fromLat == null || toLng == null || toLat == null) {
            return 0;
        }
        return geoDistanceMeters(fromLng, fromLat, toLng, toLat);
    }

    private static int geoDistanceMeters(ItineraryItemVO from, ItineraryItemVO to) {
        return geoDistanceMeters(from.getLongitude(), from.getLatitude(), to.getLongitude(), to.getLatitude());
    }

    private static int geoDistanceMeters(BigDecimal fromLng, BigDecimal fromLat, BigDecimal toLng, BigDecimal toLat) {
        if (fromLng == null || fromLat == null || toLng == null || toLat == null) {
            return 0;
        }
        double lat1 = Math.toRadians(fromLat.doubleValue());
        double lat2 = Math.toRadians(toLat.doubleValue());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(toLng.subtract(fromLng).doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.max(0, (int) Math.round(6371000 * c));
    }

    private static String formatDistance(int meters) {
        if (meters >= 1000) {
            return BigDecimal.valueOf(meters)
                    .divide(BigDecimal.valueOf(1000), 1, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString() + " 公里";
        }
        return meters + " 米";
    }

    private static boolean hasRouteGraph(Map<String, Object> row) {
        String status = String.valueOf(row.getOrDefault("routeGraphStatus", "none"));
        return "draft".equals(status) || "verified".equals(status);
    }

    private static int daysOf(String duration) {
        return switch (duration) {
            case "two_day" -> 2;
            case "three_day" -> 3;
            default -> 1;
        };
    }

    private static int spotsPerDay(String duration, String pace) {
        if ("half_day".equals(duration)) {
            return 2;
        }
        return switch (pace) {
            case "relaxed" -> 2;
            case "compact" -> 4;
            default -> 3;
        };
    }

    private static String normalizeDuration(String duration) {
        String value = trimToDefault(duration, "one_day");
        if (!List.of("half_day", "one_day", "two_day", "three_day").contains(value)) {
            return "one_day";
        }
        return value;
    }

    private static String normalizePace(String pace) {
        String value = trimToDefault(pace, "normal");
        if (!List.of("relaxed", "normal", "compact").contains(value)) {
            return "normal";
        }
        return value;
    }

    private static String normalizePurpose(String purpose) {
        String value = trimToDefault(purpose, "balanced");
        if (!List.of("balanced", "photo", "study", "food", "relax", "route").contains(value)) {
            return "balanced";
        }
        return value;
    }

    private static String normalizeBudgetLevel(String budgetLevel) {
        String value = trimToDefault(budgetLevel, "normal");
        if (!List.of("economy", "normal", "premium").contains(value)) {
            return "normal";
        }
        return value;
    }

    private static List<String> normalizePreferences(List<String> preferences) {
        if (preferences == null) {
            return List.of();
        }
        return preferences.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(8)
                .toList();
    }

    private void validateSaveRequest(ItineraryPlanSaveRequest request) {
        if (request.getDays() == null || request.getDays().isEmpty()) {
            throw BusinessException.badRequest("没有可保存的行程内容");
        }
        boolean hasItem = request.getDays().stream()
                .anyMatch(day -> day.getItems() != null && !day.getItems().isEmpty());
        if (!hasItem) {
            throw BusinessException.badRequest("没有可保存的行程节点");
        }
    }

    private static int countSpotItems(List<ItineraryDaySaveRequest> days) {
        if (days == null) {
            return 0;
        }
        return (int) days.stream()
                .flatMap(day -> day.getItems() == null ? java.util.stream.Stream.empty() : day.getItems().stream())
                .filter(item -> !"food".equals(normalizeItemType(item.getItemType())))
                .count();
    }

    private String toJson(List<String> preferences) {
        try {
            return objectMapper.writeValueAsString(preferences == null ? List.of() : preferences);
        } catch (JsonProcessingException ex) {
            throw BusinessException.badRequest("行程偏好保存失败");
        }
    }

    private List<String> parsePreferences(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private static String defaultTitle(ItineraryPlanSaveRequest request) {
        String city = trimToDefault(request.getCity(), "目的地");
        String duration = durationLabel(normalizeDuration(request.getDuration()));
        return city + duration + "旅行计划";
    }

    private static String copiedTitle(String title) {
        String source = trimToDefault(title, "旅行计划");
        String suffix = "（复制）";
        if (source.endsWith(suffix)) {
            return source;
        }
        return source.length() + suffix.length() > 100
                ? source.substring(0, Math.max(1, 100 - suffix.length())) + suffix
                : source + suffix;
    }

    private static int intOrDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static String normalizeItemType(String itemType) {
        String value = trimToDefault(itemType, "spot");
        return "food".equals(value) ? "food" : "spot";
    }

    private static String durationLabel(String duration) {
        return switch (duration) {
            case "half_day" -> "半日";
            case "two_day" -> "两日";
            case "three_day" -> "三日";
            default -> "一日";
        };
    }

    private static String paceLabel(String pace) {
        return switch (pace) {
            case "relaxed" -> "轻松节奏";
            case "compact" -> "紧凑节奏";
            default -> "标准节奏";
        };
    }

    private static String purposeLabel(String purpose) {
        return switch (purpose) {
            case "photo" -> "拍照打卡";
            case "study" -> "研学参观";
            case "food" -> "美食顺路";
            case "relax" -> "轻松休闲";
            case "route" -> "路线展示";
            default -> "综合决策";
        };
    }

    private static String trimToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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
        return 0;
    }

    private static boolean decimalPositive(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.compareTo(BigDecimal.ZERO) > 0;
        }
        if (value instanceof Number n) {
            return n.doubleValue() > 0;
        }
        return false;
    }
}
