# 数据字典

本文档说明 `schema.sql` 中各表的用途和关键字段。完整字段类型以 SQL 文件为准。

## 用户模块

### users

系统用户账号表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| id | 用户ID | 主键 |
| username | 用户名 | 唯一 |
| phone | 手机号 | 唯一，可用于登录 |
| password_hash | 密码哈希 | 不存明文密码 |
| status | 用户状态 | 1正常，0禁用 |
| last_login_at | 最后登录时间 | 可为空 |
| created_at | 创建时间 | 自动生成 |
| updated_at | 更新时间 | 自动更新 |

### user_profiles

用户展示资料表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| user_id | 用户ID | 主键，关联 users |
| nickname | 昵称 | 前端个人中心展示 |
| avatar_url | 头像地址 | 只存路径 |
| signature | 个性签名 | 最长120字符 |
| email | 邮箱 | 可为空 |
| gender | 性别 | male/female/secret |
| birthday | 生日 | 可为空 |
| visit_count | 主页访问数 | 前端 visits |

### user_preferences

用户兴趣偏好表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| id | 偏好ID | 主键 |
| user_id | 用户ID | 关联 users |
| tag_id | 标签ID | 关联 tags |
| weight | 权重 | 推荐排序使用 |
| source | 来源 | manual/browse/favorite/like/comment/rating/circle |

## 空间分组、POI 与标签模块

### place_groups

空间分组表。它不是高校专用表，而是用于统一表达高校、校区、景区、公园、商圈等地点容器。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| id | 空间分组ID | 主键 |
| parent_id | 父级空间分组ID | 例如校区的父级是高校 |
| name | 分组名称 | 如 北京邮电大学沙河校区 |
| short_name | 简称 | 如 北邮沙河 |
| group_type | 分组类型 | university/campus/scenic_area/business_area/park/museum_area/city_area/other |
| city | 城市 | 可为空 |
| district | 行政区 | 可为空 |
| address | 地址 | 可为空 |
| longitude | 中心经度 | 可为空 |
| latitude | 中心纬度 | 可为空 |
| description | 说明 | 可用于导览页 |
| sort_order | 排序值 | 前端展示排序 |

### poi_categories

POI 分类表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| id | 分类ID | 主键 |
| code | 分类编码 | 如 library/cafeteria/scenic/natural/historic |
| name | 分类名称 | 如 图书馆/食堂/景观/自然风景/历史古迹 |
| parent_id | 父分类 | 支持分类树 |
| scene | 场景 | campus/city/both |
| sort_order | 排序值 | 前端类型列表排序 |

### pois

POI 主表，统一存储校园设施和城市景点。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| id | POI ID | 主键 |
| place_group_id | 所属空间分组 | 关联 place_groups，可为空 |
| category_id | 分类ID | 关联 poi_categories |
| name | 名称 | 支持全文检索 |
| scene | 场景 | campus/city |
| area_code | 区域编码 | teaching/living/sports 等 |
| area_name | 区域名称 | 教学区/生活区等 |
| address | 地址 | 可为空 |
| location_text | 前端展示位置 | 对应 location |
| description | 描述 | 详情页展示 |
| image_url | 图片地址 | 对应 image |
| longitude | 经度 | 路线和距离计算 |
| latitude | 纬度 | 路线和距离计算 |
| open_time | 开放时间 | 对应 openTime |
| close_time | 关闭时间 | 对应 closeTime |
| phone | 联系电话 | 可为空 |
| rating | 综合评分 | 对应 rating/score |
| hotness | 热度 | 推荐排序使用 |
| visit_count | 访问量 | 对应 visits |
| status | 状态 | 1启用，0停用 |

### tags

标签表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| id | 标签ID | 主键 |
| name | 标签名称 | 如 摄影/美食/校园 |
| tag_type | 标签类型 | interest/poi/food/log/circle |

### poi_tag_relations

POI 与标签多对多关联表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| poi_id | POI ID | 联合主键 |
| tag_id | 标签ID | 联合主键 |

## 用户行为模块

### favorites

收藏表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| id | 收藏ID | 主键 |
| user_id | 用户ID | 关联 users |
| target_type | 收藏对象类型 | poi/food/log |
| target_id | 收藏对象ID | 由 target_type 决定 |
| created_at | 收藏时间 | 推荐依据 |

### browsing_history

浏览记录表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| id | 浏览记录ID | 主键 |
| user_id | 用户ID | 关联 users |
| target_type | 浏览对象类型 | poi/food/log/circle |
| target_id | 浏览对象ID | 由 target_type 决定 |
| created_at | 浏览时间 | 推荐依据 |

### ratings

评分表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| id | 评分ID | 主键 |
| user_id | 用户ID | 关联 users |
| target_type | 评分对象类型 | poi/food/log |
| target_id | 评分对象ID | 由 target_type 决定 |
| rating | 评分 | 0到5 |

## 美食模块

### foods

美食表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| id | 美食ID | 主键 |
| poi_id | 所属POI | 食堂/商圈/景区 |
| name | 美食名称 | 列表和详情展示 |
| cuisine_code | 菜系编码 | sichuan/fastfood 等 |
| cuisine_name | 菜系名称 | 川菜/快餐等 |
| description | 描述 | 前端 description |
| image_url | 图片地址 | 前端 image |
| price_level | 价格等级 | 1/2/3 |
| avg_price | 人均价格 | 详情页展示 |
| rating | 评分 | 前端 score/rating |
| hotness | 热度 | 排序使用 |
| address | 地址 | 详情页展示 |
| open_time | 营业开始 | 前端 openTime |
| close_time | 营业结束 | 前端 closeTime |
| phone | 电话 | 可为空 |

### food_reviews

美食评价表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| id | 评价ID | 主键 |
| food_id | 美食ID | 关联 foods |
| user_id | 用户ID | 关联 users |
| rating | 评分 | 可为空 |
| content | 评价内容 | 前端提交 |
| created_at | 创建时间 | 评价时间 |

## 路线模块

### route_edges

路线边表，用于图算法。

北邮双校区演示路线边由 `bupt_route_edges_seed.sql` 补充，执行顺序应在 `campus_poi_seed.sql` 之后。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| id | 路线边ID | 主键 |
| from_poi_id | 起点POI | 图节点 |
| to_poi_id | 终点POI | 图节点 |
| distance_m | 距离 | 米 |
| duration_min | 耗时 | 分钟 |
| transport_type | 通行方式 | walk/bike/bus/indoor |
| congestion_factor | 拥挤系数 | 路线权重 |
| is_indoor | 是否室内 | 1室内 |
| description | 路段说明 | 前端 points 描述来源 |

### route_records

路线规划记录表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| id | 路线记录ID | 主键 |
| user_id | 用户ID | 可为空 |
| route_name | 路线名称 | 可为空 |
| mode | 规划模式 | shortest/optimal/indoor |
| total_distance_m | 总距离 | 前端 distance |
| total_duration_min | 总耗时 | 前端 duration |
| preferences | 路线偏好 | JSON |

### route_record_points

路线点表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| id | 路线点ID | 主键 |
| route_record_id | 路线记录ID | 关联 route_records |
| poi_id | POI ID | 可为空 |
| point_name | 点位名称 | 前端 points.name |
| sort_order | 顺序 | 路线顺序 |
| distance_from_start_m | 距起点距离 | 前端 points.distance |
| description | 点位说明 | 前端 points.description |

## 社区模块

### circles

圈子表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| id | 圈子ID | 主键 |
| name | 圈子名称 | 前端 name |
| description | 圈子描述 | 前端 description |
| cover_url | 封面图 | 前端 cover |
| owner_id | 圈主ID | 关联 users |
| status | 状态 | 1正常 |
| created_at | 创建时间 | 前端 created_at |

### circle_members

圈子成员表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| circle_id | 圈子ID | 联合主键 |
| user_id | 用户ID | 联合主键 |
| role | 角色 | 1成员 2管理员 3圈主 |
| joined_at | 加入时间 | 可展示 |

### travel_logs

游记和圈子日志表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| id | 日志ID | 主键 |
| user_id | 作者ID | 关联 users |
| poi_id | 关联POI | 可为空 |
| circle_id | 关联圈子 | 为空表示个人游记 |
| title | 标题 | 可为空 |
| content | 内容 | 必填 |
| rating | 作者综合评分 | 可为空；为空时不参与景点均分 |
| scenery_rating | 景观体验评分 | 可为空，1-5 星 |
| facility_rating | 设施完善评分 | 可为空，1-5 星 |
| service_rating | 服务体验评分 | 可为空，1-5 星 |
| traffic_rating | 交通便利评分 | 可为空，1-5 星 |
| value_rating | 性价比评分 | 可为空，1-5 星 |
| hotness | 热度 | 推荐依据 |
| view_count | 浏览数 | 前端 view_count |
| is_top | 是否置顶 | 圈子日志使用 |
| status | 状态 | 1正常 |
| created_at | 创建时间 | 前端 created_at/date |

### log_images

日志图片表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| id | 图片ID | 主键 |
| log_id | 日志ID | 关联 travel_logs |
| image_url | 图片地址 | 前端 images |
| sort_order | 排序 | 图片顺序 |

### log_tag_relations

日志标签关联表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| log_id | 日志ID | 联合主键 |
| tag_id | 标签ID | 联合主键 |

### log_likes

日志点赞表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| log_id | 日志ID | 联合主键 |
| user_id | 用户ID | 联合主键 |
| created_at | 点赞时间 | 推荐依据 |

### log_comments

日志评论表。

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| id | 评论ID | 主键 |
| log_id | 日志ID | 关联 travel_logs |
| user_id | 评论用户ID | 关联 users |
| parent_id | 父评论ID | 支持回复 |
| content | 评论内容 | 前端提交 |
| created_at | 创建时间 | 前端 created_at |
