package com.tourwise.service;

import com.tourwise.dto.*;
import com.tourwise.mapper.*;
import com.tourwise.model.*;

import com.tourwise.common.BusinessException;
import com.tourwise.common.MapUtil;
import com.tourwise.common.PageResult;
import com.tourwise.security.AuthContext;
import com.tourwise.vo.circle.CircleListVO;
import com.tourwise.vo.circle.CircleMemberVO;
import com.tourwise.vo.circle.CircleVO;
import com.tourwise.vo.common.ActionResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class CircleService {
    private final CircleMapper circleMapper;
    private final AdminService adminService;

    public CircleService(CircleMapper circleMapper, AdminService adminService) {
        this.circleMapper = circleMapper;
        this.adminService = adminService;
    }

    public CircleListVO list(String keyword) {
        Long currentUserId = AuthContext.getUserId();
        List<CircleVO> circles = circleMapper.list(currentUserId, trimToNull(keyword))
                .stream()
                .map(CircleService::normalizeCircle)
                .map(CircleVO::from)
                .toList();
        List<CircleVO> joined = circles.stream()
                .filter(circle -> circle.getIsMember() != null && circle.getIsMember() != 0)
                .toList();
        List<CircleVO> others = circles.stream()
                .filter(circle -> circle.getIsMember() == null || circle.getIsMember() == 0)
                .toList();
        return new CircleListVO(joined, others, circles, circles.size());
    }

    public CircleVO detail(Long id) {
        Map<String, Object> row = circleMapper.findById(id, AuthContext.getUserId());
        if (row == null) {
            throw BusinessException.notFound("圈子不存在");
        }
        CircleVO circle = CircleVO.from(normalizeCircle(row));
        List<CircleMemberVO> members = circleMapper.listMembers(id)
                .stream()
                .map(CircleService::normalizeCircle)
                .map(CircleMemberVO::from)
                .toList();
        circle.setMemberList(members);
        return circle;
    }

    public PageResult<CircleVO> adminList(String keyword, Integer status, int page, int pageSize) {
        long adminUserId = adminService.requireAdmin();
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 50);
        int offset = (safePage - 1) * safePageSize;
        String normalizedKeyword = trimToNull(keyword);
        Integer normalizedStatus = normalizeStatus(status);
        List<CircleVO> circles = circleMapper.adminList(adminUserId, normalizedKeyword, normalizedStatus, offset, safePageSize)
                .stream()
                .map(CircleService::normalizeCircle)
                .map(CircleVO::from)
                .toList();
        long total = circleMapper.adminCount(normalizedKeyword, normalizedStatus);
        return new PageResult<>(circles, total);
    }

    @Transactional
    public CircleVO adminUpdate(Long id, AdminCircleUpdateRequest request) {
        adminService.requireAdmin();
        validateCircleId(id);
        if (circleMapper.existsAny(id) == 0) {
            throw BusinessException.notFound("圈子不存在");
        }
        request.setName(request.getName().trim());
        request.setDescription(request.getDescription().trim());
        request.setCover(normalizeCover(request.getCover()));
        circleMapper.updateByAdmin(id, request);
        return adminFindById(id);
    }

    @Transactional
    public CircleVO adminUpdateStatus(Long id, int status) {
        adminService.requireAdmin();
        validateCircleId(id);
        if (status != 0 && status != 1) {
            throw BusinessException.badRequest("圈子状态不合法");
        }
        if (circleMapper.updateStatusByAdmin(id, status) == 0) {
            throw BusinessException.notFound("圈子不存在");
        }
        return adminFindById(id);
    }

    @Transactional
    public ActionResultVO create(CircleCreateRequest request) {
        long userId = AuthContext.requireUserId();
        CircleRecord record = new CircleRecord();
        record.setName(request.getName().trim());
        record.setDescription(request.getDescription().trim());
        record.setCoverUrl(normalizeCover(firstText(request.getCoverUrl(), request.getCover())));
        record.setOwnerId(userId);
        circleMapper.insert(record);
        circleMapper.insertMember(record.getId(), userId, 3);
        return ActionResultVO.created("circleId", record.getId());
    }

    @Transactional
    public ActionResultVO join(Long id) {
        long userId = AuthContext.requireUserId();
        ensureExists(id);
        circleMapper.insertMember(id, userId, 1);
        return ActionResultVO.joined();
    }

    @Transactional
    public ActionResultVO leave(Long id) {
        long userId = AuthContext.requireUserId();
        ensureExists(id);
        Long ownerId = circleMapper.ownerId(id);
        if (ownerId != null && ownerId == userId) {
            throw BusinessException.badRequest("圈主不能直接退出圈子");
        }
        circleMapper.deleteMember(id, userId);
        return ActionResultVO.left();
    }

    public void ensureExists(Long id) {
        if (circleMapper.exists(id) == 0) {
            throw BusinessException.notFound("圈子不存在");
        }
    }

    private CircleVO adminFindById(Long id) {
        long adminUserId = adminService.requireAdmin();
        Map<String, Object> row = circleMapper.adminFindById(adminUserId, id);
        if (row == null) {
            throw BusinessException.notFound("圈子不存在");
        }
        return CircleVO.from(normalizeCircle(row));
    }

    public void ensureMember(Long circleId) {
        long userId = AuthContext.requireUserId();
        ensureExists(circleId);
        if (circleMapper.isMember(circleId, userId) == 0) {
            throw BusinessException.unauthorized("加入圈子后才能发布日志");
        }
    }

    private static Map<String, Object> normalizeCircle(Map<String, Object> row) {
        Map<String, Object> item = MapUtil.normalize(row);
        alias(item, "ownerId", "owner_id");
        alias(item, "createdAt", "created_at");
        alias(item, "isMember", "is_member");
        alias(item, "userId", "user_id");
        alias(item, "joinedAt", "joined_at");
        return item;
    }

    private static void alias(Map<String, Object> item, String camelKey, String snakeKey) {
        if (item.containsKey(camelKey)) {
            item.put(snakeKey, item.get(camelKey));
        }
    }

    private static String normalizeCover(String value) {
        String cover = trimToNull(value);
        if (cover == null || cover.length() > 255 || cover.startsWith("data:")) {
            return null;
        }
        return cover;
    }

    private static Integer normalizeStatus(Integer status) {
        if (status == null) {
            return null;
        }
        if (status != 0 && status != 1) {
            return null;
        }
        return status;
    }

    private static void validateCircleId(Long id) {
        if (id == null || id <= 0) {
            throw BusinessException.badRequest("圈子参数不合法");
        }
    }

    private static String firstText(String first, String second) {
        String firstValue = trimToNull(first);
        return firstValue != null ? firstValue : trimToNull(second);
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
