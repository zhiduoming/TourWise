# 前端页面字段需求

本文档按页面整理当前前端已经依赖的数据字段。数据库和后端第一版应优先保证“必须支持”的字段和交互能跑通；占位功能可以先保留页面，不强制第一版后端完整实现。

## 登录注册页

文件：`Frontend/src/views/Login.vue`

调用接口：`POST /user/login`、`POST /user/register`

展示字段：

- 系统名称：个性化旅游系统
- 登录表单：用户名/手机号、密码、记住我
- 注册表单：用户名、手机号、密码、确认密码

提交字段：

- 登录：`username`、`password`、`remember`
- 注册：`username`、`phone`、`password`、`confirmPassword`

第一版必须支持：

- 用户名或手机号登录
- 注册后可登录
- 登录返回 `token` 和 `user`

占位或后续扩展：

- 忘记密码
- 用户协议和隐私政策详情

## 首页

文件：`Frontend/src/views/Home.vue`

调用接口：`GET /recommend/hot-top10`

展示字段：

- 热门推荐列表：`id`、`name`、`image`、`categoryName`、`score`、`hotness` 或 `visits`
- 功能入口：设施查询、推荐、美食、路线、圈子、日记等

第一版必须支持：

- 返回热门 Top10，点击条目可进入详情或推荐相关页面

占位或后续扩展：

- 首页个性化欢迎语
- 更复杂的运营位和轮播图

## 设施查询页

文件：`Frontend/src/views/Search.vue`

调用接口：`GET /search/facilities`、`GET /search/types`

展示字段：

- 查询条件：`keyword`、`type`、`area`
- 列表项：`id`、`name`、`type`、`typeName`、`description`、`location`、`distance`
- 详情弹窗：`name`、`typeName`、`location`、`openTime`、`closeTime`、`phone`、`description`
- 分页：`page`、`pageSize`、`total`

提交字段：

- 搜索参数：`keyword`、`type`、`area`、`page`、`pageSize`

第一版必须支持：

- 关键词搜索
- 类型筛选
- 区域筛选
- 分页返回
- 点击详情跳转到 `/spot/{id}`

占位或后续扩展：

- 按真实当前位置计算 `distance`
- 设施详情弹窗和详情页的展示方式后续可统一

## 景点详情页

文件：`Frontend/src/views/SpotDetail.vue`

调用接口：`GET /search/facility/{id}`、`GET /log/list`、`GET /food/list`、`POST /log/{id}/like`

展示字段：

- 景点主信息：`id`、`name`、`category`、`rating`、`hotness`、`longitude`、`latitude`、`description`、`image`
- 关联日志：`id`、`user_id`、`username`、`title`、`content`、`rating`、`hotness`、`like_count`、`created_at`
- 附近美食：`id`、`name`、`image`、`rating`、`cuisine_type`

提交字段：

- 日志点赞：日志 `id`

第一版必须支持：

- 根据景点 ID 查询详情
- 根据 `spotId` 查询相关游记
- 展示附近美食列表
- 登录后可点赞游记
- 跳转路线规划时携带终点坐标和名称

占位或后续扩展：

- 分享功能
- 附近美食按真实地理距离筛选
- 景点收藏入口

## 推荐页

文件：`Frontend/src/views/Recommend.vue`

调用接口：`GET /recommend/list`

展示字段：

- 筛选条件：`type`、`category`、`distance`
- 推荐项：`id`、`name`、`image`、`description`、`category`、`categoryName`、`score`、`distance`、`visits`
- 分页：`page`、`pageSize`、`total`

提交字段：

- 查询参数：`type`、`category`、`distance`、`page`、`pageSize`

第一版必须支持：

- 热度优先
- 距离优先
- 兴趣匹配
- 按景点类型筛选
- 加入路线时跳转 `/route-plan?addPOI={id}`

占位或后续扩展：

- 推荐理由可先返回但前端暂未重点展示
- 真正 AI 对话式推荐

## 美食页

文件：`Frontend/src/views/Food.vue`

调用接口：`GET /food/list`、`POST /food/review`

展示字段：

- 筛选条件：`cuisine`、`price`、`sort`
- 美食项：`id`、`name`、`image`、`description`、`score`、`priceLevel`、`distance`、`cuisine`、`cuisineName`、`recommend`
- 详情弹窗字段：`name`、`image`、`score`、`cuisineName`、`avgPrice`、`address`、`openTime`、`closeTime`、`phone`

提交字段：

- 查询参数：`cuisine`、`price`、`sort`
- 评价：`foodId`、`content`

第一版必须支持：

- 美食列表
- 菜系筛选
- 价格筛选
- 评分、距离、人气排序
- 点击详情跳转 `/food/{id}`

占位或后续扩展：

- 美食页弹窗和详情页可以后续统一
- 导航按钮可先只跳路线页或提示

## 美食详情页

文件：`Frontend/src/views/FoodDetail.vue`

调用接口：`GET /food/list/{id}`、`GET /log/list`

展示字段：

- 美食详情：`id`、`name`、`image`、`description`、`cuisine_type`、`rating`、`hotness`、`spot_name`
- 用户日志：`id`、`username`、`title`、`content`、`rating`、`hotness`、`created_at`

提交字段：

- 无直接提交

第一版必须支持：

- 根据美食 ID 查询详情
- 查询与该美食或所属地点相关的日志

占位或后续扩展：

- 美食地图定位
- 更细的美食评论列表

## 路线规划页

文件：`Frontend/src/views/RoutePlan.vue`

调用接口：`GET /route/pois`、`POST /route/shortest`、`POST /route/optimal`、`POST /route/indoor`

展示字段：

- 路线表单：`start`、`end`、`waypoints`、`mode`、`preferences`
- POI 选择器：`id`、`name`、`category`
- 路线结果：`duration`、`distance`、`points[].name`、`points[].description`、`points[].distance`

提交字段：

- 最短路径：`start`、`end`、`waypoints`
- 最优路线：`points`
- 楼宇内路径：`start`、`end`

第一版必须支持：

- 查询所有可选 POI
- 两点最短路径
- 多点最优路线
- 返回路线总时间、总距离和途经点

占位或后续扩展：

- 真实地图绘制
- 浏览器定位结果与后端坐标系统的精确转换
- 避开拥挤、风景好、时间最短等偏好权重

## 写日记页

文件：`Frontend/src/views/Diary.vue`

调用接口：`GET /recommend/hot-top10`、`POST /log/create`

展示字段：

- 表单字段：`title`、`date`、`spotId`、`mood`、`content`、`images`、`tags`
- 景点下拉：`id`、`name`

提交字段：

- `title`
- `content`
- `spotId`
- `circleId`
- `rating`
- `images`

第一版必须支持：

- 创建个人日记
- 可选关联景点
- 评分字段入库

占位或后续扩展：

- 图片上传
- 日记标签持久化，当前页面有输入但提交体暂未带 `tags`
- 编辑日记和查看详情

## 个人中心

文件：`Frontend/src/views/Profile.vue`

调用接口：`GET /user/profile`、`PUT /user/profile`、`POST /user/avatar`、`GET /log/list`、`DELETE /log/{id}`

展示字段：

- 个人资料：`username`、`nickname`、`signature`、`avatar`、`phone`、`email`、`gender`、`birthday`、`createdAt`、`lastLoginAt`、`visits`、`favorites`
- 我的日记：`id`、`title`、`content`、`date`、`location`、`mood`、`tags`
- 分页：`page`、`pageSize`、`total`

提交字段：

- 更新资料：`nickname`、`signature`、`gender`、`birthday`
- 上传头像：`avatar`
- 删除日志：日志 `id`

第一版必须支持：

- 查看个人资料
- 编辑昵称、签名、性别、生日
- 查看我的日志
- 删除日志

占位或后续扩展：

- 头像文件真实上传存储
- 精华日记筛选规则
- 收藏列表页面

## 用户主页

文件：`Frontend/src/views/UserHome.vue`

调用接口：`GET /log/list`

展示字段：

- 用户信息：`id`、`username`、`nickname`、`signature`、`avatar`
- 用户日志：`id`、`title`、`content`、`rating`、`hotness`、`created_at`
- 日志统计：`diaryCount`

提交字段：

- 查询参数：`userId`、`page`、`pageSize`

第一版必须支持：

- 根据用户 ID 查询该用户日志
- 当前用户访问自己主页时使用本地登录用户信息

占位或后续扩展：

- 独立获取其他用户资料接口
- 关注用户

## 圈子页

文件：`Frontend/src/views/Circle.vue`

调用接口：`GET /circle/list`、`POST /circle/create`、`POST /circle/{id}/join`

展示字段：

- 已加入圈子：`id`、`name`、`description`、`cover`、`members`、`posts`
- 其他圈子：`id`、`name`、`description`、`cover`、`members`、`posts`
- 搜索：`keyword`

提交字段：

- 创建圈子：`name`、`description`、`cover`
- 加入圈子：圈子 `id`

第一版必须支持：

- 查询已加入和未加入圈子
- 搜索圈子
- 创建圈子
- 加入圈子

占位或后续扩展：

- 圈子封面真实上传
- 圈子推荐排序

## 圈子详情页

文件：`Frontend/src/views/CircleDetail.vue`

调用接口：`GET /circle/{id}`、`POST /circle/{id}/join`、`POST /circle/{id}/leave`、`GET /circle/{id}/logs`、`POST /circle/{id}/logs`、`POST /circle/{logId}/like`、`GET /circle/{logId}/comments`、`POST /circle/{logId}/comments`

展示字段：

- 圈子详情：`id`、`name`、`description`、`cover`、`members`、`logs`、`is_member`、`created_at`
- 成员列表：`member_list[].id`、`member_list[].username`、`member_list[].role`
- 日志列表：`id`、`username`、`title`、`content`、`images`、`like_count`、`comment_count`、`view_count`、`is_top`、`created_at`
- 评论列表：`id`、`username`、`content`、`created_at`、`replies`

提交字段：

- 发布日志：`title`、`content`、`spotId`、`rating`、`images`
- 评论：`content`、`parentId`
- 加入 / 退出 / 点赞：对应 ID

第一版必须支持：

- 查询圈子详情
- 查询成员列表
- 查询圈子日志
- 发布圈子日志
- 加入和退出圈子
- 日志点赞
- 评论和回复

占位或后续扩展：

- 日志详情页
- 图片上传落盘
- 圈子管理员权限
