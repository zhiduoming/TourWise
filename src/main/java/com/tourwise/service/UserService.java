package com.tourwise.service;

import com.tourwise.dto.*;
import com.tourwise.mapper.*;
import com.tourwise.model.*;

import com.tourwise.common.BusinessException;
import com.tourwise.common.MapUtil;
import com.tourwise.security.AuthContext;
import com.tourwise.security.JwtService;
import com.tourwise.vo.common.ActionResultVO;
import com.tourwise.vo.user.AvatarVO;
import com.tourwise.vo.user.LoginVO;
import com.tourwise.vo.user.PreferenceProfileVO;
import com.tourwise.vo.user.PreferenceSourceVO;
import com.tourwise.vo.user.PreferenceTagVO;
import com.tourwise.vo.user.ProfileVO;
import com.tourwise.vo.user.RegisterVO;
import com.tourwise.vo.user.UserVO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserService {
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private static final String DEFAULT_AVATAR = "/images/avatar/default.png";
    private static final Set<String> GENDERS = Set.of("male", "female", "secret");

    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final OssStorageService ossStorageService;

    public UserService(UserMapper userMapper, JwtService jwtService, OssStorageService ossStorageService) {
        this.userMapper = userMapper;
        this.jwtService = jwtService;
        this.ossStorageService = ossStorageService;
    }

    public LoginVO login(LoginRequest request) {
        UserAccount account = userMapper.findAccountByUsernameOrPhone(request.getUsername().trim());
        if (account == null || account.getStatus() == null || account.getStatus() != 1) {
            throw BusinessException.badRequest("用户名或密码错误");
        }
        if (!PASSWORD_ENCODER.matches(request.getPassword(), account.getPasswordHash())) {
            throw BusinessException.badRequest("用户名或密码错误");
        }
        userMapper.updateLastLoginAt(account.getId());
        UserVO user = requireUserSummary(account.getId());
        return new LoginVO(jwtService.createToken(account.getId(), account.getUsername()), user);
    }

    public RegisterVO register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw BusinessException.badRequest("两次密码不一致");
        }
        if (userMapper.existsByUsername(request.getUsername().trim()) > 0) {
            throw BusinessException.badRequest("用户名已存在");
        }
        if (userMapper.existsByPhone(request.getPhone().trim()) > 0) {
            throw BusinessException.badRequest("手机号已注册");
        }
        UserAccount account = new UserAccount();
        account.setUsername(request.getUsername().trim());
        account.setPhone(request.getPhone().trim());
        account.setPasswordHash(PASSWORD_ENCODER.encode(request.getPassword()));
        userMapper.insertUser(account);
        userMapper.insertProfile(account.getId(), account.getUsername(), DEFAULT_AVATAR);
        return new RegisterVO(account.getId(), account.getUsername());
    }

    public UserVO currentUser() {
        return requireUserSummary(AuthContext.requireUserId());
    }

    public UserVO publicUser(Long id) {
        Map<String, Object> row = userMapper.findPublicUser(id);
        if (row == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return UserVO.from(MapUtil.normalize(row));
    }

    public ProfileVO profile() {
        Map<String, Object> row = userMapper.findProfile(AuthContext.requireUserId());
        if (row == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return ProfileVO.from(MapUtil.normalize(row));
    }

    public PreferenceProfileVO preferenceProfile() {
        long userId = AuthContext.requireUserId();
        Map<String, Object> summaryRow = MapUtil.normalize(userMapper.preferenceSummary(userId));
        List<PreferenceTagVO> tags = userMapper.listPreferenceTags(userId, 12)
                .stream()
                .map(MapUtil::normalize)
                .map(PreferenceTagVO::from)
                .toList();
        List<PreferenceSourceVO> sources = userMapper.listPreferenceSources(userId)
                .stream()
                .map(MapUtil::normalize)
                .map(PreferenceSourceVO::from)
                .toList();
        PreferenceProfileVO vo = new PreferenceProfileVO();
        vo.setTotalWeight(decimal(summaryRow.get("totalWeight")));
        vo.setTagCount(intValue(summaryRow.get("tagCount")));
        vo.setSignalCount(intValue(summaryRow.get("signalCount")));
        vo.setLastUpdatedAt(string(summaryRow.get("lastUpdatedAt")));
        vo.setTopTags(tags);
        vo.setSources(sources);
        vo.setSummary(buildPreferenceSummary(tags, vo.getSignalCount()));
        vo.setRecommendationTip(buildRecommendationTip(tags, sources));
        return vo;
    }

    public ActionResultVO updateProfile(ProfileUpdateRequest request) {
        String gender = StringUtils.hasText(request.getGender()) ? request.getGender() : "secret";
        if (!GENDERS.contains(gender)) {
            throw BusinessException.badRequest("性别参数不合法");
        }
        LocalDate birthday = request.getBirthday();
        userMapper.updateProfile(
                AuthContext.requireUserId(),
                trimToNull(request.getNickname()),
                trimToNull(request.getSignature()),
                gender,
                birthday);
        return ActionResultVO.updated();
    }

    public AvatarVO uploadAvatar(MultipartFile file) {
        long userId = AuthContext.requireUserId();
        String avatarUrl = ossStorageService.uploadAvatar(userId, file);
        userMapper.updateAvatar(userId, avatarUrl);
        return new AvatarVO(avatarUrl);
    }

    public ActionResultVO resetAvatar() {
        userMapper.updateAvatar(AuthContext.requireUserId(), DEFAULT_AVATAR);
        return ActionResultVO.deleted();
    }

    private UserVO requireUserSummary(Long id) {
        Map<String, Object> row = userMapper.findUserSummary(id);
        if (row == null) {
            throw BusinessException.unauthorized("未登录或登录已过期");
        }
        return UserVO.from(MapUtil.normalize(row));
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String buildPreferenceSummary(List<PreferenceTagVO> tags, Integer signalCount) {
        if (tags == null || tags.isEmpty()) {
            return "系统还没有足够的行为数据。多浏览、收藏、标记想去或去过后，推荐会逐步变得个性化。";
        }
        String first = tags.get(0).getTagName();
        String second = tags.size() > 1 ? tags.get(1).getTagName() : null;
        String signalText = signalCount == null || signalCount == 0 ? "已有少量" : "已累计 " + signalCount + " 条";
        if (second == null) {
            return signalText + "兴趣信号，目前最明显的偏好是“" + first + "”。";
        }
        return signalText + "兴趣信号，目前更偏向“" + first + "”和“" + second + "”。";
    }

    private static String buildRecommendationTip(List<PreferenceTagVO> tags, List<PreferenceSourceVO> sources) {
        if (tags == null || tags.isEmpty()) {
            return "建议先在景点详情页做几次浏览、收藏、想去或去过操作，再回到智能推荐选择“兴趣匹配”。";
        }
        boolean hasStrongAction = sources != null && sources.stream()
                .anyMatch(source -> Set.of("favorite", "want", "visited", "dislike").contains(source.getSource()));
        if (hasStrongAction) {
            return "推荐排序会优先考虑正向兴趣标签，并降低你标记不感兴趣的相似内容。";
        }
        return "当前主要来自浏览行为，继续收藏或标记想去后，兴趣匹配会更稳定。";
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private static Integer intValue(Object value) {
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

    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

}
