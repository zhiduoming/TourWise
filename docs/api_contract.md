# 前端接口契约

本文档基于当前 `Frontend/src/api/*.js` 和 `Frontend/src/views/*.vue` 整理，用于后端开发和数据库设计对齐。第一版后端默认使用 Java / Spring Boot，接口统一挂载在前端配置的 `baseURL` 下，当前默认值为 `http://localhost:3000/api`。

## 统一约定

所有接口返回统一 JSON 结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

前端请求拦截器会在存在 token 时附加：

```http
Authorization: Bearer <token>
```

错误约定：

```json
{
  "code": 401,
  "message": "未登录或登录已过期",
  "data": null
}
```

分页列表统一建议：

```json
{
  "list": [],
  "total": 0
}
```

## 用户与资料

### POST `/user/login`

对应页面：登录注册页。

请求体：

```json
{
  "username": "chen",
  "password": "123456",
  "remember": false
}
```

返回 `data`：

```json
{
  "token": "jwt-token",
  "user": {
    "id": 1,
    "username": "chen",
    "nickname": "陈同学",
    "avatar": "/images/avatar/default.png",
    "signature": "喜欢城市漫游",
    "phone": "13800000000"
  }
}
```

前端依赖字段：`token`、`user.id`、`user.username`、`user.nickname`、`user.avatar`、`user.signature`。

### POST `/user/register`

对应页面：登录注册页。

请求体：

```json
{
  "username": "chen",
  "phone": "13800000000",
  "password": "123456",
  "confirmPassword": "123456"
}
```

返回 `data`：

```json
{
  "id": 1,
  "username": "chen"
}
```

前端依赖字段：注册成功即可，不强依赖返回字段。

### GET `/user/me`

对应组件：用户状态初始化、用户主页判断。

返回 `data`：

```json
{
  "id": 1,
  "username": "chen",
  "nickname": "陈同学",
  "avatar": "/images/avatar/default.png",
  "signature": "喜欢城市漫游"
}
```

### GET `/user/profile`

对应页面：个人中心。

返回 `data`：

```json
{
  "id": 1,
  "username": "chen",
  "nickname": "陈同学",
  "signature": "喜欢城市漫游",
  "avatar": "/images/avatar/default.png",
  "phone": "13800000000",
  "email": "chen@example.com",
  "gender": "secret",
  "birthday": "2005-01-01",
  "createdAt": "2026-05-01T10:00:00",
  "lastLoginAt": "2026-05-08T17:00:00",
  "visits": 12,
  "favorites": 5
}
```

前端依赖字段：以上全部字段均会展示或编辑。

### PUT `/user/profile`

对应页面：个人中心编辑资料。

请求体：

```json
{
  "nickname": "陈同学",
  "signature": "喜欢城市漫游",
  "gender": "secret",
  "birthday": "2005-01-01"
}
```

返回 `data`：

```json
{
  "updated": true
}
```

### POST `/user/avatar`

对应页面：个人中心头像上传。

请求类型：`multipart/form-data`，字段名 `avatar`。

返回 `data`：

```json
{
  "avatarUrl": "/uploads/avatar/1.png"
}
```

## 用户景点行为

用于支撑收藏、想去、去过、浏览历史和后续个性化推荐。以下接口均需要登录。

### GET `/user/spot-actions/{targetId}`

查询当前用户对某个景点/POI 的行为状态。

请求参数：

```http
targetType=poi
```

返回 `data`：

```json
{
  "targetType": "poi",
  "targetId": 550,
  "favorite": true,
  "wantToGo": false,
  "visited": true
}
```

### POST `/user/spot-actions/browse`

记录一次景点浏览，并同步增加景点热度/访问量。

请求体：

```json
{
  "targetType": "poi",
  "targetId": 550
}
```

返回 `data`：同状态结构。

### POST `/user/spot-actions/favorite`

切换收藏状态。已收藏则取消，未收藏则收藏。

请求体：

```json
{
  "targetType": "poi",
  "targetId": 550
}
```

返回 `data`：同状态结构。

### POST `/user/spot-actions/want`

切换“想去”状态。

请求体：

```json
{
  "targetType": "poi",
  "targetId": 550
}
```

返回 `data`：同状态结构。

### POST `/user/spot-actions/visited`

切换“去过”状态。

请求体：

```json
{
  "targetType": "poi",
  "targetId": 550
}
```

返回 `data`：同状态结构。

### GET `/user/spot-actions/list`

个人主页“我的足迹”列表。

请求参数：

```http
type=favorite|want|visited|history
page=1
pageSize=12
```

返回 `data`：

```json
{
  "list": [],
  "total": 0
}
```

### DELETE `/user/avatar`

对应页面：当前 API 已定义，页面暂未显式使用。

返回 `data`：

```json
{
  "deleted": true
}
```

### POST `/user/logout`

对应组件：布局页退出登录。

返回 `data`：

```json
{
  "logout": true
}
```

## 设施 / 景点查询

### GET `/search/facilities`

对应页面：设施查询页。

请求参数：

```json
{
  "keyword": "图书馆",
  "type": "library",
  "area": "teaching",
  "page": 1,
  "pageSize": 12
}
```

返回 `data`：

```json
{
  "list": [
    {
      "id": 1,
      "name": "沙河校区图书馆",
      "type": "library",
      "typeName": "图书馆",
      "category": "图书馆",
      "description": "校园核心学习空间",
      "location": "教学区",
      "distance": 320,
      "openTime": "08:00",
      "closeTime": "22:00",
      "phone": "010-00000000",
      "longitude": 116.358,
      "latitude": 39.962,
      "rating": 4.8,
      "hotness": 1200,
      "image": "/images/spots/1.jpg"
    }
  ],
  "total": 1
}
```

前端依赖字段：`id`、`name`、`type`、`typeName`、`description`、`location`、`distance`。

### GET `/search/facility/{id}`

对应页面：景点详情页、设施详情弹窗。

返回 `data`：

```json
{
  "id": 1,
  "name": "沙河校区图书馆",
  "category": "图书馆",
  "type": "library",
  "typeName": "图书馆",
  "description": "校园核心学习空间",
  "location": "教学区",
  "openTime": "08:00",
  "closeTime": "22:00",
  "phone": "010-00000000",
  "longitude": 116.358,
  "latitude": 39.962,
  "rating": 4.8,
  "hotness": 1200,
  "image": "/images/spots/1.jpg"
}
```

前端依赖字段：`id`、`name`、`category`、`rating`、`hotness`、`longitude`、`latitude`、`description`、`image`。

### GET `/search/types`

对应页面：设施查询页。当前页面有本地默认类型，后端可返回同等结构。

返回 `data`：

```json
[
  { "value": "teaching", "label": "教学楼" },
  { "value": "library", "label": "图书馆" },
  { "value": "cafeteria", "label": "食堂" },
  { "value": "scenic", "label": "景观" },
  { "value": "museum", "label": "博物馆" },
  { "value": "natural", "label": "自然风景" },
  { "value": "historic", "label": "历史古迹" },
  { "value": "theme_park", "label": "主题乐园" },
  { "value": "waterfront", "label": "滨水海岛" }
]
```

## 个性化推荐

### GET `/recommend/list`

对应页面：推荐页。

请求参数：

```json
{
  "type": "interest",
  "category": "scenic",
  "distance": "1000",
  "page": 1,
  "pageSize": 12
}
```

返回 `data`：

```json
{
  "list": [
    {
      "id": 2,
      "name": "甲子钟广场",
      "image": "/images/spots/2.jpg",
      "description": "适合拍照、集合和校园导览的标志性景观",
      "category": "scenic",
      "categoryName": "景观",
      "score": 4.7,
      "distance": 580,
      "visits": 3600,
      "openTime": "00:00",
      "closeTime": "23:59",
      "recommendReason": "你最近浏览和点赞了多个景观类游记"
    }
  ],
  "total": 1
}
```

前端依赖字段：`id`、`name`、`image`、`description`、`category`、`categoryName`、`score`、`distance`、`visits`。

## 旅行计划

### POST `/itinerary/generate`

对应页面：旅行计划页。第一版采用规则生成，不保存到数据库。

请求体：

```json
{
  "city": "北京",
  "duration": "one_day",
  "pace": "normal",
  "preferences": ["university", "museum"],
  "includeFood": true
}
```

字段说明：

- `duration`: `half_day`、`one_day`、`two_day`、`three_day`
- `pace`: `relaxed`、`normal`、`compact`
- `preferences`: `university`、`scenery`、`culture`、`museum`、`food`、`photo`

返回 `data`：

```json
{
  "city": "北京",
  "duration": "one_day",
  "pace": "normal",
  "totalDays": 1,
  "spotCount": 3,
  "summary": "北京 · 一日 · 标准节奏，共安排 3 个景点，并尝试插入餐饮推荐。",
  "preferences": ["university"],
  "days": [
    {
      "dayNo": 1,
      "title": "当日行程",
      "summary": "按评分、热度和你的行为偏好筛选，建议按当前顺序游览。",
      "items": [
        {
          "orderNo": 1,
          "itemType": "spot",
          "timeSlot": "上午",
          "targetId": 550,
          "spotId": 4,
          "placeGroupId": 4,
          "name": "北京邮电大学沙河校区",
          "rating": 4.8,
          "recommendReason": "匹配兴趣偏好"
        }
      ]
    }
  ],
  "tips": []
}
```

### POST `/itinerary/plans`

保存当前生成的行程，需要登录。删除和更新不物理删除节点数据，删除采用 `status = 0` 软删除。

请求体结构与生成接口返回的 `data` 基本一致，额外可传 `title`：

```json
{
  "title": "北京一日旅行计划",
  "city": "北京",
  "duration": "one_day",
  "pace": "normal",
  "totalDays": 1,
  "spotCount": 3,
  "summary": "北京 · 一日 · 标准节奏，共安排 3 个景点，并尝试插入餐饮推荐。",
  "preferences": ["university"],
  "days": []
}
```

返回 `data`：保存后的完整行程，包含 `id`。

### GET `/itinerary/plans`

我的行程列表，需要登录。

请求参数：

```http
page=1
pageSize=10
```

返回 `data`：

```json
{
  "list": [],
  "total": 0
}
```

### GET `/itinerary/plans/{id}`

查看保存行程详情，需要登录。

返回 `data`：完整行程计划。

### DELETE `/itinerary/plans/{id}`

删除保存行程，需要登录。后端采用软删除。

返回 `data`：

```json
null
```

### GET `/recommend/hot-top10`

对应页面：首页、写日记页、圈子详情页、景点选择。

返回 `data`：

```json
[
  {
    "id": 1,
    "name": "沙河校区图书馆",
    "category": "library",
    "categoryName": "图书馆",
    "score": 4.8,
    "hotness": 1200,
    "visits": 1200,
    "image": "/images/spots/1.jpg"
  }
]
```

前端依赖字段：`id`、`name`；首页和推荐卡片还会使用评分、热度、图片类字段。

### POST `/recommend/rating`

对应 API：提交用户评分，当前页面未集中使用。

请求体：

```json
{
  "targetType": "poi",
  "targetId": 1,
  "rating": 4.5
}
```

返回 `data`：

```json
{
  "rated": true
}
```

## 美食

### GET `/food/list`

对应页面：美食页、景点详情页附近美食。

请求参数：

```json
{
  "cuisine": "sichuan",
  "price": "2",
  "sort": "score",
  "limit": 5
}
```

返回 `data`：

```json
[
  {
    "id": 1,
    "name": "麻辣香锅",
    "image": "/images/food/1.jpg",
    "description": "校园热门美食",
    "score": 4.6,
    "rating": 4.6,
    "priceLevel": 2,
    "avgPrice": 28,
    "distance": 300,
    "cuisine": "sichuan",
    "cuisineName": "川菜",
    "cuisine_type": "川菜",
    "recommend": true,
    "address": "北区食堂二层",
    "openTime": "10:00",
    "closeTime": "21:00",
    "phone": "010-00000000",
    "hotness": 900,
    "spot_id": 3,
    "spot_name": "北区食堂"
  }
]
```

前端依赖字段：`id`、`name`、`image`、`description`、`score`、`rating`、`priceLevel`、`distance`、`cuisine`、`cuisineName`、`cuisine_type`、`recommend`。

### GET `/food/list/{id}`

对应页面：美食详情页。

返回 `data`：

```json
{
  "id": 1,
  "name": "麻辣香锅",
  "image": "/images/food/1.jpg",
  "description": "校园热门美食",
  "cuisine_type": "川菜",
  "rating": 4.6,
  "hotness": 900,
  "avgPrice": 28,
  "address": "北区食堂二层",
  "openTime": "10:00",
  "closeTime": "21:00",
  "phone": "010-00000000",
  "spot_id": 3,
  "spot_name": "北区食堂"
}
```

### GET `/food/recommend`

对应 API：获取美食推荐，当前页面主要使用 `/food/list`。

返回 `data`：

```json
[
  {
    "id": 1,
    "name": "麻辣香锅",
    "score": 4.6,
    "recommendReason": "圈子中近期讨论热度高"
  }
]
```

### POST `/food/review`

对应页面：美食页详情弹窗。

请求体：

```json
{
  "foodId": 1,
  "content": "味道不错，排队时间略长"
}
```

返回 `data`：

```json
{
  "reviewId": 1,
  "created": true
}
```

## 路线规划

### GET `/route/pois`

对应页面：路线规划页 POI 选择器。

请求参数：当前页面未传参数，后端可预留 `keyword`、`category`。

返回 `data`：

```json
[
  {
    "id": 1,
    "name": "沙河校区图书馆",
    "category": "图书馆",
    "type": "library",
    "longitude": 116.358,
    "latitude": 39.962
  }
]
```

前端依赖字段：`id`、`name`、`category`。

### POST `/route/shortest`

对应页面：路线规划页最短路径。

请求体：

```json
{
  "start": "39.962,116.358",
  "end": "39.963,116.360",
  "waypoints": ["沙河校区图书馆"]
}
```

返回 `data`：

```json
{
  "duration": 15,
  "distance": 1200,
  "points": [
    { "id": 1, "name": "起点", "description": "从这里出发", "distance": 0 },
    { "id": 2, "name": "沙河校区图书馆", "description": "途经点", "distance": 400 },
    { "id": 3, "name": "终点", "description": "到达目的地", "distance": 1200 }
  ]
}
```

前端依赖字段：`duration`、`distance`、`points[].name`、`points[].description`、`points[].distance`。

### POST `/route/optimal`

对应页面：路线规划页最优路线。

请求体：

```json
{
  "points": ["起点", "沙河校区图书馆", "终点"]
}
```

返回 `data`：同 `/route/shortest`。

### POST `/route/indoor`

对应 API：楼宇内路径，当前页面有模式选项但调用逻辑尚未完整接入。

请求体：

```json
{
  "start": "S1-101",
  "end": "S1-305"
}
```

返回 `data`：同 `/route/shortest`。

## 游记 / 日志

### GET `/log/list`

对应页面：景点详情页、用户主页、个人中心、美食详情页。

请求参数：

```json
{
  "page": 1,
  "pageSize": 10,
  "spotId": 1,
  "userId": 1,
  "tab": "all"
}
```

返回 `data`：

```json
{
  "list": [
    {
      "id": 1,
      "user_id": 1,
      "username": "陈同学",
      "title": "沙河校区图书馆半日游",
      "content": "今天在图书馆学习并参观了展区",
      "spotId": 1,
      "spot_id": 1,
      "location": "沙河校区图书馆",
      "rating": 4.5,
      "mood": 4,
      "hotness": 120,
      "view_count": 120,
      "like_count": 8,
      "comment_count": 2,
      "is_top": false,
      "images": ["/images/logs/1-1.jpg"],
      "tags": ["学习", "安静"],
      "created_at": "2026-05-08T10:00:00",
      "date": "2026-05-08"
    }
  ],
  "total": 1
}
```

前端依赖字段：`id`、`user_id`、`username`、`title`、`content`、`rating`、`mood`、`hotness`、`like_count`、`comment_count`、`view_count`、`images`、`tags`、`created_at`、`date`、`location`。

### GET `/log/{id}`

对应 API：日志详情，当前页面未完整实现详情页。

返回 `data`：单条日志对象，字段同 `/log/list` 的列表项。

### POST `/log/create`

对应页面：写日记页。

请求体：

```json
{
  "title": "沙河校区图书馆半日游",
  "content": "今天在图书馆学习并参观了展区",
  "spotId": 1,
  "circleId": null,
  "rating": 2.5,
  "images": []
}
```

返回 `data`：

```json
{
  "id": 1,
  "created": true
}
```

### DELETE `/log/{id}`

对应页面：个人中心删除日记。

返回 `data`：

```json
{
  "deleted": true
}
```

### POST `/log/{id}/like`

对应页面：景点详情页日志点赞。

返回 `data`：

```json
{
  "liked": true
}
```

注意：当前前端代码在部分位置读取 `res.liked`，后端实现时建议兼容返回顶层 `liked` 或前端后续统一改为 `res.data.liked`。

## 圈子社区

### GET `/circle/list`

对应页面：圈子页。

请求参数：

```json
{
  "keyword": "摄影"
}
```

返回 `data`：

```json
{
  "joinedCircles": [
    {
      "id": 1,
      "name": "校园摄影圈",
      "description": "分享校园和城市旅行照片",
      "cover": "/images/circles/1.jpg",
      "members": 20,
      "posts": 12
    }
  ],
  "otherCircles": [
    {
      "id": 2,
      "name": "城市漫游圈",
      "description": "北京周边路线交流",
      "cover": "/images/circles/2.jpg",
      "members": 35,
      "posts": 18
    }
  ]
}
```

前端依赖字段：`joinedCircles`、`otherCircles`、`id`、`name`、`description`、`cover`、`members`、`posts`。

### POST `/circle/create`

对应页面：圈子页创建圈子。

请求体：

```json
{
  "name": "校园摄影圈",
  "description": "分享校园和城市旅行照片",
  "cover": "base64-or-url"
}
```

返回 `data`：

```json
{
  "id": 1,
  "created": true
}
```

### GET `/circle/{id}`

对应页面：圈子详情页。

返回 `data`：

```json
{
  "id": 1,
  "name": "校园摄影圈",
  "description": "分享校园和城市旅行照片",
  "cover": "/images/circles/1.jpg",
  "members": 20,
  "logs": 12,
  "is_member": true,
  "created_at": "2026-05-01T10:00:00",
  "member_list": [
    { "id": 1, "username": "chen", "role": 3 },
    { "id": 2, "username": "guest", "role": 1 }
  ]
}
```

前端依赖字段：`id`、`name`、`description`、`cover`、`members`、`logs`、`is_member`、`created_at`、`member_list[].id`、`member_list[].username`、`member_list[].role`。

### POST `/circle/{id}/join`

返回 `data`：

```json
{
  "joined": true
}
```

### POST `/circle/{id}/leave`

返回 `data`：

```json
{
  "left": true
}
```

### GET `/circle/{id}/logs`

对应页面：圈子详情页日志列表。

请求参数：

```json
{
  "page": 1,
  "pageSize": 10
}
```

返回 `data`：

```json
{
  "list": [
    {
      "id": 1,
      "username": "陈同学",
      "title": "校园摄影路线",
      "content": "推荐从沙河校区图书馆走到甲子钟广场",
      "images": ["/images/logs/1-1.jpg"],
      "like_count": 8,
      "comment_count": 2,
      "view_count": 120,
      "is_top": false,
      "created_at": "2026-05-08T10:00:00"
    }
  ],
  "total": 1
}
```

### POST `/circle/{id}/logs`

对应页面：圈子详情页发布日志。

请求体：

```json
{
  "title": "校园摄影路线",
  "content": "推荐从沙河校区图书馆走到甲子钟广场",
  "spotId": 1,
  "rating": 4,
  "images": []
}
```

返回 `data`：

```json
{
  "id": 1,
  "created": true
}
```

### POST `/circle/{logId}/like`

对应页面：圈子详情页日志点赞。

返回 `data`：

```json
{
  "liked": true
}
```

注意：当前路径中的 `logId` 是日志 ID，不是圈子 ID。

### GET `/circle/{logId}/comments`

对应页面：圈子详情页评论展开。

返回 `data`：

```json
{
  "list": [
    {
      "id": 1,
      "username": "陈同学",
      "content": "这条路线不错",
      "parentId": 0,
      "created_at": "2026-05-08T11:00:00",
      "replies": [
        {
          "id": 2,
          "username": "guest",
          "content": "周末试试",
          "parentId": 1,
          "created_at": "2026-05-08T11:10:00"
        }
      ]
    }
  ]
}
```

### POST `/circle/{logId}/comments`

对应页面：圈子详情页发表评论和回复。

请求体：

```json
{
  "content": "这条路线不错",
  "parentId": 0
}
```

返回 `data`：

```json
{
  "id": 1,
  "created": true
}
```
