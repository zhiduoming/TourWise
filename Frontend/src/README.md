#  个性化旅游系统 - 前后端对接表

**版本**: v3.0.0  
**更新时间**: 2026-05-08

---

## 📑 完整接口对照表

> 当前文档按 `Frontend/src/api/*.js` 的实际调用路径整理。
> 接口返回字段以根目录 `docs/api_contract.md` 为准；数据库物理表以 `Database/schema.sql` 为准；后端接库细节见 `Database/backend_handoff.md`。
> 旧版说明中出现过的 `/api/route/attractions`、`/api/route/plan`、`/api/food/:id` 已废弃，当前分别使用 `/api/route/pois`、`/api/route/shortest`、`/api/route/optimal`、`/api/route/indoor`、`/api/food/list/:id`。

| 数据库表 | 前端页面 | API 接口 | 接口用途 | 前置条件 |
|----------|----------|----------|----------|----------|
| **users / user_profiles** | Login | `POST /api/user/login` | 用户登录，获取 token | 无 |
| **users / user_profiles** | Login | `POST /api/user/logout` | 用户登出，清除 token | ✅ 已登录 |
| **users / user_profiles** | Profile | `GET /api/user/profile` | 获取当前用户个人信息 | ✅ 已登录 |
| **users / user_profiles** | Profile | `PUT /api/user/profile` | 更新用户个人信息（邮箱、简介） | ✅ 已登录 |
| **users / user_profiles** | Profile | `POST /api/user/avatar` | 上传用户头像 | ✅ 已登录 |
| **users / user_profiles** | UserHome | `GET /api/user/:id` | 获取指定用户公开信息 | 无 |
| **pois / spots / foods** | Home / Recommend / SpotDetail / Food | `POST /api/admin/images/:type/:id` | 管理员上传景点、美食展示图 | ✅ 管理员 |
| | | | | |
| **pois / poi_categories** | Home | `GET /api/recommend/hot-top10` | 获取热门推荐 TOP10（景点） | 无 |
| **pois / poi_categories** | Search | `GET /api/search/facilities` | 搜索设施（教学楼、图书馆等） | 无 |
| **pois / poi_categories** | Search | `GET /api/search/facility/:id` | 获取设施详情 | 无 |
| **pois / poi_categories** | Search | `GET /api/search/types` | 获取设施类型列表 | 无 |
| **pois / poi_categories** | SpotDetail | `GET /api/search/facility/:id` | 获取景点详情信息 | 无 |
| **pois / poi_categories** | SpotDetail | `GET /api/log/list?spotId=:id` | 获取该景点下的所有日志 | 无 |
| **pois / route_edges** | RoutePlan | `GET /api/route/pois` | 获取可选路线节点 | 无 |
| **pois / route_edges** | RoutePlan | `POST /api/route/shortest` | 规划最短路线 | 无 |
| **pois / route_edges** | RoutePlan | `POST /api/route/optimal` | 规划综合最优路线 | 无 |
| **pois / route_edges** | RoutePlan | `POST /api/route/indoor` | 规划室内路线 | 无 |
| | | | | |
| **foods** | Food | `GET /api/food/list` | 获取美食列表 | 无 |
| **foods** | FoodDetail | `GET /api/food/list/:id` | 获取美食详情 | 无 |
| **foods** | FoodDetail | `GET /api/log/list?spotId=:id` | 获取该美食下的日志 | 无 |
| **foods** | SpotDetail | `GET /api/food/list?near=:id` | 获取景点附近的美食 | 无 |
| | | | | |
| **travel_logs** | Diary | `POST /api/log/create` | 发布个人日志（日记） | ✅ 已登录 |
| **travel_logs** | SpotDetail | `GET /api/log/list?spotId=:id` | 获取景点下的日志列表 | 无 |
| **travel_logs** | FoodDetail | `GET /api/log/list?spotId=:id` | 获取美食下的日志列表 | 无 |
| **travel_logs** | Profile | `GET /api/log/list?userId=:id&type=personal` | 获取我的日志列表 | ✅ 已登录 |
| **travel_logs** | Profile | `DELETE /api/log/:id` | 删除我的日志 | ✅ 已登录（作者） |
| **travel_logs** | UserHome | `GET /api/log/list?userId=:id` | 获取他人的日志列表 | 无 |
| **travel_logs** | CircleDetail | `GET /api/log/list?circleId=:id` | 获取圈子内的日志列表 | 无 |
| **travel_logs** | CircleDetail | `POST /api/log/create` | 在圈子内发布日志 | ✅ 已登录 + ✅ 已加入圈子 |
| **travel_logs** | CircleDetail | `POST /api/log/:id/like` | 点赞日志 | ✅ 已登录 |
| **travel_logs** | SpotDetail | `POST /api/log/:id/like` | 点赞景点日志 | ✅ 已登录 |
| **travel_logs** | | `GET /api/log/:id` | 获取日志详情 | 无 |
| | | | | |
| **log_comments** | CircleDetail | `GET /api/circle/:logId/comments` | 获取日志的评论列表 | 无 |
| **log_comments** | CircleDetail | `POST /api/circle/:logId/comments` | 发布评论/回复 | ✅ 已登录 |
| | | | | |
| **log_likes** | CircleDetail | `POST /api/circle/:logId/like` | 点赞圈子日志 | ✅ 已登录 |
| **log_likes** | SpotDetail | `POST /api/log/:id/like` | 点赞景点日志 | ✅ 已登录 |
| | | | | |
| **circles** | Circle | `GET /api/circle/list` | 获取圈子列表（已加入 + 其他） | ✅ 已登录 |
| **circles** | Circle | `POST /api/circle/create` | 创建新圈子 | ✅ 已登录 |
| **circles** | CircleDetail | `GET /api/circle/:id` | 获取圈子详情（含成员、日志） | 无 |
| **circles** | | `GET /api/circle/:id/logs` | 获取圈子内的日志列表 | 无 |
| **circles** | | `POST /api/circle/:id/logs` | 在圈子内发布日志 | ✅ 已登录 + ✅ 已加入圈子 |
| | | | | |
| **circle_members** | Circle | `POST /api/circle/:id/join` | 加入圈子 | ✅ 已登录 |
| **circle_members** | Circle | `POST /api/circle/:id/leave` | 退出圈子 | ✅ 已登录（非圈主） |
| **circle_members** | CircleDetail | `GET /api/circle/:id` | 获取圈子详情（含成员列表） | 无 |

---

## 📊 按页面分类统计

### 1. Home.vue (首页)
| 数据库表 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| pois / poi_categories | `GET /api/recommend/hot-top10` | 获取热门推荐 TOP10 | 无 |

### 2. Search.vue (设施查询)
| 数据库表 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| pois / poi_categories | `GET /api/search/facilities` | 搜索设施 | 无 |
| pois / poi_categories | `GET /api/search/facility/:id` | 获取设施详情 | 无 |
| pois / poi_categories | `GET /api/search/types` | 获取设施类型 | 无 |

### 3. RoutePlan.vue (路线规划)
| 数据库表 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| pois / route_edges | `GET /api/route/pois` | 获取可选路线节点 | 无 |
| pois / route_edges | `POST /api/route/shortest` | 规划最短路线 | 无 |
| pois / route_edges | `POST /api/route/optimal` | 规划综合最优路线 | 无 |
| pois / route_edges | `POST /api/route/indoor` | 规划室内路线 | 无 |

### 4. SpotDetail.vue (景点详情)
| 数据库表 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| pois / poi_categories | `GET /api/search/facility/:id` | 获取景点详情 | 无 |
| travel_logs | `GET /api/log/list?spotId=:id` | 获取景点下的日志 | 无 |
| travel_logs | `POST /api/log/:id/like` | 点赞日志 | ✅ 已登录 |
| foods | `GET /api/food/list?near=:id` | 获取附近美食 | 无 |

### 5. Food.vue (美食列表)
| 数据库表 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| foods | `GET /api/food/list` | 获取美食列表 | 无 |

### 6. FoodDetail.vue (美食详情)
| 数据库表 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| foods | `GET /api/food/list/:id` | 获取美食详情 | 无 |
| travel_logs | `GET /api/log/list?spotId=:id` | 获取美食下的日志 | 无 |

### 7. Diary.vue (写日记)
| 数据库表 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| travel_logs | `POST /api/log/create` | 发布日志 | ✅ 已登录 |

### 8. Circle.vue (圈子列表)
| 数据库表 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| circles | `GET /api/circle/list` | 获取圈子列表 | ✅ 已登录 |
| circles | `POST /api/circle/create` | 创建圈子 | ✅ 已登录 |
| circle_members | `POST /api/circle/:id/join` | 加入圈子 | ✅ 已登录 |
| circle_members | `POST /api/circle/:id/leave` | 退出圈子 | ✅ 已登录 |

### 9. CircleDetail.vue (圈子详情)
| 数据库表 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| circles | `GET /api/circle/:id` | 获取圈子详情 | 无 |
| circles | `GET /api/circle/:id/logs` | 获取圈子日志 | 无 |
| circles | `POST /api/circle/:id/logs` | 发布圈子日志 | ✅ 已登录 + ✅ 已加入 |
| travel_logs | `POST /api/circle/:logId/like` | 点赞日志 | ✅ 已登录 |
| log_comments | `GET /api/circle/:logId/comments` | 获取评论 | 无 |
| log_comments | `POST /api/circle/:logId/comments` | 发布评论 | ✅ 已登录 |

### 10. Profile.vue (个人中心)
| 数据库表 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| users / user_profiles | `GET /api/user/profile` | 获取个人信息 | ✅ 已登录 |
| users / user_profiles | `PUT /api/user/profile` | 更新个人信息 | ✅ 已登录 |
| users / user_profiles | `POST /api/user/avatar` | 上传头像 | ✅ 已登录 |
| travel_logs | `GET /api/log/my` | 获取我的日志，后端按 token 识别当前用户 | ✅ 已登录 |
| travel_logs | `DELETE /api/log/:id` | 删除日志 | ✅ 已登录（作者） |

### 11. UserHome.vue (他人主页)
| 数据库表 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| users / user_profiles | `GET /api/user/:id` | 获取用户信息 | 无 |
| travel_logs | `GET /api/log/list?userId=:id` | 获取用户日志 | 无 |

### 12. Login.vue (登录)
| 数据库表 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| users / user_profiles | `POST /api/user/login` | 用户登录 | 无 |
| users / user_profiles | `POST /api/user/logout` | 用户登出 | ✅ 已登录 |

---

## 📊 按数据库表分类统计

### users / user_profiles (用户表)
| 前端页面 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| Login | `POST /api/user/login` | 用户登录 | 无 |
| Login | `POST /api/user/logout` | 用户登出 | ✅ 已登录 |
| Profile | `GET /api/user/profile` | 获取个人信息 | ✅ 已登录 |
| Profile | `PUT /api/user/profile` | 更新个人信息 | ✅ 已登录 |
| Profile | `POST /api/user/avatar` | 上传头像 | ✅ 已登录 |
| UserHome | `GET /api/user/:id` | 获取他人信息 | 无 |

### pois / poi_categories (景点表)
| 前端页面 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| Home | `GET /api/recommend/hot-top10` | 热门推荐 | 无 |
| Search | `GET /api/search/facilities` | 搜索设施 | 无 |
| Search | `GET /api/search/facility/:id` | 设施详情 | 无 |
| Search | `GET /api/search/types` | 设施类型 | 无 |
| SpotDetail | `GET /api/search/facility/:id` | 景点详情 | 无 |

### foods (美食表)
| 前端页面 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| Food | `GET /api/food/list` | 美食列表 | 无 |
| FoodDetail | `GET /api/food/list/:id` | 美食详情 | 无 |
| SpotDetail | `GET /api/food/list?near=:id` | 附近美食 | 无 |

### travel_logs (统一日志表) ⭐核心表
| 前端页面 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| Diary | `POST /api/log/create` | 发布日志 | ✅ 已登录 |
| SpotDetail | `GET /api/log/list?spotId=:id` | 景点日志 | 无 |
| FoodDetail | `GET /api/log/list?spotId=:id` | 美食日志 | 无 |
| Profile | `GET /api/log/my` | 我的日志，后端按 token 识别当前用户 | ✅ 已登录 |
| Profile | `DELETE /api/log/:id` | 删除日志 | ✅ 已登录 |
| UserHome | `GET /api/log/list?userId=:id` | 他人日志 | 无 |
| CircleDetail | `GET /api/log/list?circleId=:id` | 圈子日志 | 无 |
| CircleDetail | `POST /api/log/create` | 发布圈子日志 | ✅ 已登录 + ✅ 已加入 |
| CircleDetail | `POST /api/log/:id/like` | 点赞日志 | ✅ 已登录 |
| SpotDetail | `POST /api/log/:id/like` | 点赞日志 | ✅ 已登录 |
| | `GET /api/log/:id` | 日志详情 | 无 |

### log_comments (评论表)
| 前端页面 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| CircleDetail | `GET /api/circle/:logId/comments` | 获取评论 | 无 |
| CircleDetail | `POST /api/circle/:logId/comments` | 发布评论 | ✅ 已登录 |

### log_likes (点赞表)
| 前端页面 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| CircleDetail | `POST /api/circle/:logId/like` | 点赞日志 | ✅ 已登录 |
| SpotDetail | `POST /api/log/:id/like` | 点赞日志 | ✅ 已登录 |

### circles (圈子表)
| 前端页面 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| Circle | `GET /api/circle/list` | 圈子列表 | ✅ 已登录 |
| Circle | `POST /api/circle/create` | 创建圈子 | ✅ 已登录 |
| CircleDetail | `GET /api/circle/:id` | 圈子详情 | 无 |
| CircleDetail | `GET /api/circle/:id/logs` | 圈子日志 | 无 |
| CircleDetail | `POST /api/circle/:id/logs` | 发布日志 | ✅ 已登录 + ✅ 已加入 |

### circle_members (成员表)
| 前端页面 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| Circle | `POST /api/circle/:id/join` | 加入圈子 | ✅ 已登录 |
| Circle | `POST /api/circle/:id/leave` | 退出圈子 | ✅ 已登录 |

### route_edges (路线表)
| 前端页面 | API 接口 | 用途 | 前置条件 |
|----------|----------|------|----------|
| RoutePlan | `GET /api/route/pois` | 可选路线节点 | 无 |
| RoutePlan | `POST /api/route/shortest` | 最短路线规划 | 无 |
| RoutePlan | `POST /api/route/optimal` | 综合最优路线规划 | 无 |
| RoutePlan | `POST /api/route/indoor` | 室内路线规划 | 无 |

---

## 📈 统计汇总

### 按前置条件分类

| 前置条件 | 接口数量 | 占比 |
|----------|----------|------|
| 无（公开接口） | 22 | 48% |
| ✅ 已登录 | 20 | 43% |
| ✅ 已登录 + ✅ 已加入圈子 | 2 | 5% |
| ✅ 已登录（作者） | 2 | 5% |
| **总计** | **46** | **100%** |

### 按数据库表分类

| 数据库表 | 接口数量 | 核心程度 |
|----------|----------|----------|
| travel_logs | 11 | ⭐⭐⭐ 核心 |
| pois / poi_categories | 5 | ⭐⭐ 重要 |
| users / user_profiles | 6 | ⭐⭐ 重要 |
| circles | 5 | ⭐⭐ 重要 |
| foods | 3 | ⭐ 一般 |
| log_comments | 2 | ⭐ 一般 |
| log_likes | 2 | ⭐ 一般 |
| circle_members | 2 | ⭐ 一般 |
| route_edges | 4 | ⭐ 一般 |

### 按前端页面分类

| 前端页面 | 接口数量 | 复杂度 |
|----------|----------|--------|
| CircleDetail | 6 | 🔴 高 |
| SpotDetail | 4 | 🟡 中 |
| Profile | 4 | 🟡 中 |
| Circle | 4 | 🟡 中 |
| Search | 3 | 🟡 中 |
| FoodDetail | 2 | 🟢 低 |
| UserHome | 2 | 🟢 低 |
| Diary | 1 | 🟢 低 |
| Home | 1 | 🟢 低 |
| RoutePlan | 4 | 🟢 低 |
| Food | 1 | 🟢 低 |
| Login | 2 | 🟢 低 |

---

## 🔑 核心接口说明

### 1. 统一日志 API (`/api/log/*`)

**最重要的核心接口**，所有日志相关操作都使用这组 API：

```javascript
// 发布日志（任何场景）
POST /api/log/create
{
  "title": "string",
  "content": "string",
  "spotId": 1,        // 可选：关联景点
  "circleId": 1,      // 可选：关联圈子
  "rating": 4.5,      // 可选：评分
  "images": []
}

显示规则:
- spotId=null, circleId=null → 仅个人空间
- spotId=1, circleId=null   → 景点下 + 个人空间
- spotId=null, circleId=1   → 圈子内 + 个人空间
- spotId=1, circleId=1      → 圈子内 + 景点下 + 个人空间
```

### 2. 认证接口 (`/api/user/*`)

```javascript
// 登录
POST /api/user/login
{
  "username": "string",
  "password": "string"
}
// 返回：{ token: "jwt_token", user: {...} }

// 登出
POST /api/user/logout
// Header: Authorization: Bearer <token>
```

### 3. 圈子日志互动 (`/api/circle/:logId/*`)

```javascript
// 获取评论（含回复）
GET /api/circle/:logId/comments

// 发布评论/回复
POST /api/circle/:logId/comments
{
  "content": "string",
  "parentId": 0  // 0=评论，其他=回复某评论
}

// 点赞
POST /api/circle/:logId/like
```

---

## 📝 开发注意事项

### 1. 认证处理
- 所有需要登录的接口必须在 Header 中携带 token
- 格式：`Authorization: Bearer <token>`
- token 过期时间：7 天

### 2. 错误处理
- 401: 未登录或 token 过期
- 403: 权限不足（如删除他人日志）
- 404: 资源不存在
- 400: 参数错误

### 3. 数据一致性
- 创建圈子、加入圈子等操作使用事务
- 删除日志时同步更新计数（like_count, comment_count）

### 4. 性能优化
- 列表接口必须支持分页
- 热门数据考虑缓存（Redis）
- 图片使用 CDN

---

**文档维护**: 系统架构组  
**最后更新**: 2026-05-08  
**版本**: v3.0.0
