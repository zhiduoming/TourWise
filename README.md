# TourWise 个性化旅游系统

TourWise 是一个面向课程设计和项目实践的个性化旅游系统，核心功能包括景点查询、智能推荐、路线规划、旅行日志、圈子社区、图片上传、后台管理和日志压缩。项目后端采用 Spring Boot + MyBatis + MySQL，前端采用 Vue 3 + Vite + Element Plus。

本项目的重点不是只做 CRUD，路线规划使用图结构与最短路径算法，旅行日志使用 Huffman 无损压缩，推荐和查询模块也包含排序、筛选和规则评分。

## 目录

- [技术栈](#技术栈)
- [核心功能](#核心功能)
- [项目结构](#项目结构)
- [环境要求](#环境要求)
- [数据库初始化](#数据库初始化)
- [后端启动](#后端启动)
- [前端启动](#前端启动)
- [环境变量](#环境变量)
- [核心算法说明](#核心算法说明)
- [后台管理功能](#后台管理功能)
- [常用接口](#常用接口)
- [测试与验证](#测试与验证)
- [演示建议](#演示建议)
- [已知边界](#已知边界)

## 技术栈

后端：

- Java 21
- Spring Boot 4
- Spring Web MVC
- MyBatis Spring Boot Starter 4.0.0
- MySQL 8.x
- JWT 轻量鉴权
- BCrypt 密码加密
- 阿里云 OSS 图片上传
- 高德地图 Web 服务与 JS API
- JUnit 5

前端：

- Vue 3
- Vite 5
- Element Plus
- Pinia
- Vue Router
- Axios
- `@amap/amap-jsapi-loader`

数据库：

- 主库名：`tourist_system`
- SQL 文件目录：`Database/`
- MyBatis XML 目录：`src/main/resources/mapper/`

## 核心功能

### 用户与鉴权

- 用户注册、登录、退出
- JWT 鉴权
- 当前用户信息获取
- 个人资料编辑
- 头像上传与删除
- 管理员角色识别

### 景点查询

- 按省份、城市、标签、简称进行叠加查询
- 支持简称模糊匹配，例如”北邮”可以匹配北京邮电大学相关校区
- 景点详情展示评分、热度、介绍、高德地图定位、图片、美食和日志
- 景点详情集成 AI 智能简介，调用 DeepSeek API 自动生成并缓存到数据库，支持手动重新生成

### 智能推荐

- 首页热门 Top10
- 规则推荐列表
- 优先展示已配置大量 POI 的重点景点
- 结合热度、评分、用户偏好和行为反馈生成推荐理由
- 支持不感兴趣、收藏、想去、去过等用户行为

### 美食推荐

- 美食列表和详情
- 景点附近美食推荐
- 美食评价
- 根据景点关联关系返回不同美食，避免所有景点展示同一批美食

### 路线规划

路线规划被拆成两种模式：

1. 景点之间路线
   - 起点和终点选择大景点
   - 使用高德地图真实道路规划
   - 支持步行、驾车
   - 支持当前位置定位与附近景点匹配

2. 景点内部路线
   - 先选择已校准的校区或景区
   - 起点、终点、途经点选择内部 POI
   - 使用平面图 + 本地路网
   - 支持路口节点、路线边、撤销、清空路线、手动补 POI
   - 当前重点维护北京邮电大学沙河校区和西土城校区

### 旅行日志与评价

- 发布旅行日志
- 日志可以关联景点、圈子和行程
- 支持多维评分：
  - 景观体验
  - 设施完善
  - 服务体验
  - 交通便利
  - 性价比
- 日志可以选择带评分或不带评分
- 不带评分的日志不影响景点总评分
- 支持点赞、评论、图片、标签
- 用户可以删除自己的日志
- 管理员可以删除景点详情页和圈子中的日志

### 用户景点行为

- 对 POI 进行收藏、想去、去过、不感兴趣标记
- 行为自动沉淀为兴趣标签权重，影响推荐排序
- 不感兴趣会对相关景点大幅降权并从推荐中排除
- 支持查看收藏、想去、去过、浏览历史列表

### 通知系统

- 日志被点赞、评论时向日志作者推送站内通知
- 行程被他人复制时推送通知
- 未读通知计数，支持一键全部已读
- 通知列表支持按未读过滤

### 行程规划

- 根据城市、出行天数、节奏、目的等参数 AI 生成行程方案
- 行程按天展示景点和美食节点，附推荐理由和距离估算
- 支持保存行程、查看我的行程列表
- 支持复制他人行程（需在圈子中共享）
- 行程可绑定旅行日志，在圈子中共享
- 支持收藏/取消收藏他人行程，展示热门共享行程

### 圈子社区

- 圈子列表
- 创建圈子
- 加入、退出圈子
- 圈子封面
- 圈子日志
- 圈子评论
- 圈子日志与景点日志、个人主页日志同步

### 图片与文件上传

- 阿里云 OSS 上传
- 用户头像上传
- 景点图片上传
- 圈子封面上传
- 景点平面图上传
- 日志图片上传

### 后台管理

- 景点图片管理
- 圈子封面管理
- 内容举报管理
- 日志删除
- 路线图编辑
- 内部 POI 补充
- 路口和路线边标注

## 项目结构

```text
TourWise
├── Database                      # 数据库建表、种子数据、迁移脚本
├── Frontend                      # Vue 3 前端项目
├── docs                          # 后端配置、接口、结构说明文档
├── logs                          # 本地运行日志
├── src
│   ├── main
│   │   ├── java/com/tourwise
│   │   │   ├── common             # 统一响应、异常、工具类、Huffman 编解码
│   │   │   ├── config             # MVC、OSS、高德、拦截器等配置
│   │   │   ├── controller         # Controller 接口层
│   │   │   ├── dto                # 请求 DTO
│   │   │   ├── mapper             # MyBatis Mapper 接口
│   │   │   ├── model              # 数据模型和内部对象
│   │   │   ├── security           # JWT、登录上下文、鉴权拦截
│   │   │   ├── service            # 业务层
│   │   │   └── vo                 # 响应 VO
│   │   └── resources
│   │       ├── application.yaml
│   │       ├── logback-spring.xml
│   │       └── mapper             # MyBatis XML SQL
│   └── test                       # 后端测试
└── pom.xml
```

后端采用清晰的三层结构：

- `Controller`：接收 HTTP 请求，做参数校验，调用 Service。
- `Service`：处理业务规则、算法、鉴权上下文和事务。
- `Mapper`：声明数据库访问方法，SQL 统一写在 XML 中。

### 内容举报与审核

- 用户可对旅行日志或评论发起举报
- 同一内容重复举报时给出提示
- 管理员在后台处理举报，可选择删除目标内容或驳回举报
- 删除日志时自动刷新景点评分和热度

## 环境要求

- JDK 21
- Maven 3.9+
- MySQL 8.x
- Node.js 18+
- npm
- 阿里云 OSS Bucket
- 高德开放平台 Key

## 数据库初始化

数据库默认名称：

```text
tourist_system
```

首次初始化建议顺序：

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS tourist_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p tourist_system < Database/schema.sql
mysql -uroot -p tourist_system < Database/seed.sql
```

如果需要完整课程设计数据，可以继续按实际需要导入 `Database/` 下的迁移脚本。当前项目经历了多轮结构演进，建议重点关注这些脚本：

```text
Database/migration_refactor_spot_poi_route_schema.sql
Database/migration_route_modes_location_and_bupt_rebuild.sql
Database/migration_rebuild_shahe_exact_pois_routes.sql
Database/migration_rebuild_xitucheng_exact_pois.sql
Database/migration_add_log_dimension_ratings.sql
Database/migration_add_log_huffman_compression.sql
Database/migration_add_admin_role_and_route_maps.sql
Database/migration_add_itinerary_plans.sql
Database/migration_add_itinerary_favorites_hot.sql
Database/migration_add_log_itinerary_plan.sql
Database/migration_add_notifications.sql
Database/migration_add_content_reports.sql
Database/migration_add_route_graph_versions.sql
Database/migration_add_user_spot_actions.sql
Database/migration_add_preference_action_sources.sql
Database/migration_add_recommend_dislike_feedback.sql
Database/migration_add_ai_summary.sql
```

更详细的数据库说明可以看：

```text
Database/backend_handoff.md
Database/database_design.md
Database/data_dictionary.md
```

## 后端启动

后端默认端口：

```text
8080
```

后端 API 前缀：

```text
/api
```

启动命令：

```bash
mvn spring-boot:run
```

如果你的环境变量写在 `~/.zshrc` 中，建议这样启动：

```bash
source ~/.zshrc && mvn spring-boot:run
```

启动后访问：

```text
http://localhost:8080/api/search/types
```

能返回数据说明后端和数据库基本连通。

## 前端启动

进入前端目录：

```bash
cd Frontend
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:5173
```

前端开发环境通过 Vite 代理访问后端：

```text
Frontend/.env.development
VITE_API_BASE_URL=/api
```

Vite 代理配置在：

```text
Frontend/vite.config.js
```

代理目标：

```text
http://localhost:8080
```

## 环境变量

不要把真实密钥提交到仓库。推荐写到系统环境变量或 `~/.zshrc`。

数据库：

```bash
export TOURWISE_DB_HOST=localhost
export TOURWISE_DB_PORT=3306
export TOURWISE_DB_NAME=tourist_system
export TOURWISE_DB_USER=root
export TOURWISE_DB_PASSWORD=your_mysql_password
```

JWT：

```bash
export TOURWISE_JWT_SECRET=your_jwt_secret
```

阿里云 OSS：

```bash
export TOURWISE_OSS_BUCKET=tourwise
export TOURWISE_OSS_AK=your_aliyun_access_key_id
export TOURWISE_OSS_SK=your_aliyun_access_key_secret
export TOURWISE_OSS_BASE_URL=https://tourwise.oss-cn-beijing.aliyuncs.com
export TOURWISE_OSS_FILE_PREFIX=uploads
```

高德地图：

```bash
export TOURWISE_AMAP_ENABLED=true
export TOURWISE_AMAP_JS_KEY=your_amap_js_key
export TOURWISE_AMAP_SECURITY_CODE=your_amap_security_code
export TOURWISE_AMAP_WEB_KEY=your_amap_web_service_key
```

AI 景点解读：

```bash
export TOURWISE_AI_API_KEY=your_deepseek_api_key
```

### 本地开发推荐方式

不想每次都 `export`，可以把密钥写到 `application-local.yaml`（已加入 `.gitignore`，不会提交）：

```bash
cp src/main/resources/application-local.yaml.example src/main/resources/application-local.yaml
# 编辑 application-local.yaml，填入真实密钥
```

示例内容：

```yaml
spring:
  datasource:
    password: "your_mysql_password"
tourwise:
  oss:
    access-key-id: "your_ak"
    access-key-secret: "your_sk"
  ai:
    api-key: "your_deepseek_key"
```

`application-local.yaml` 中的值会覆盖 `application.yaml` 的同名配置，其他配置仍从 `application.yaml` 读取。

配置读取位置：

```text
src/main/resources/application.yaml
```

## 核心算法说明

### 1. 路线规划：图结构 + 最短路径

内部路线把景区或校区抽象成图：

- 顶点：POI、路口、校门、建筑入口等。
- 边：真实可通行道路。
- 权重：距离、时间、拥挤系数等。

核心表：

```text
pois
route_edges
place_groups
route_graph_versions
```

两点路径规划：

- 优先使用 A*。
- 当坐标不足或异常时回退 Dijkstra。
- Dijkstra 保证在非负权图中得到最短路径。
- A* 在有地图坐标时使用启发式距离，减少搜索范围。

多点路线：

- 少量途经点可使用状态压缩 DP 思路求访问顺序。
- 点数较多时使用最近邻 + 2-opt 优化。
- 每一段仍然通过本地最短路径算法连接。

景点之间路线：

- 使用高德地图 API。
- 适合城市道路、跨景点导航、真实地图展示。

### 2. 推荐排序：规则评分

推荐模块不是简单查表，而是把多个因素组合成排序分数：

- 景点评分
- 景点热度
- 用户收藏、想去、去过等行为
- 用户偏好标签
- 推荐反馈
- 特定展示优先级

排序结果用于首页 Top10 和智能推荐列表。

### 3. 旅行日志压缩：Huffman 无损压缩

日志正文发布时会额外进行 Huffman 压缩：

1. 将正文转成 UTF-8 字节数组。
2. 统计每个字节出现频率。
3. 用优先队列构建 Huffman 树。
4. 高频字节使用短编码，低频字节使用长编码。
5. 将编码后的 bit 流打包成二进制存入数据库。
6. 读取日志时根据频率表重建 Huffman 树并解压。

相关字段：

```text
content                  明文正文，用于搜索和历史兼容
content_compressed       Huffman 压缩后的正文
content_encoding         压缩算法标识，例如 huffman-v1
content_original_size    原始 UTF-8 字节数
content_compressed_size  压缩后二进制字节数
```

相关代码：

```text
src/main/java/com/tourwise/common/HuffmanCodec.java
src/main/java/com/tourwise/service/LogService.java
```

说明：当前版本为了不破坏日志搜索功能，仍然保留 `content` 明文字段。因此 Huffman 在当前阶段主要体现为算法落地和压缩副本存储。后续如果要真正节省数据库整体空间，需要把搜索改成独立索引或倒排索引，再将正文主存储切换为压缩字段。

### 4. 查询与搜索

景点查询支持：

- 省份
- 城市
- 标签
- 简称
- 关键词模糊匹配

多个查询条件可以叠加。

## 后台管理功能

当前后台管理能力包括：

- 景点图片上传
- 景点平面图上传
- 圈子封面上传
- 日志删除
- 内容举报处理
- 路线图编辑
- POI 插入和删除
- 路口创建
- 路线边创建
- 撤销上一步
- 删除全部路线

路线图编辑的业务目标是解决内部路线数据不准确的问题：管理员可以直接在平面图上点选路口、POI 和路线边，让本地路径规划结果逐步接近真实平面图。

## 常用接口

用户：

```text
POST   /api/user/register
POST   /api/user/login
GET    /api/user/me
GET    /api/user/profile
PUT    /api/user/profile
POST   /api/user/avatar
DELETE /api/user/avatar
```

景点查询：

```text
GET /api/search/types
GET /api/search/facilities
GET /api/search/facility/{id}
```

推荐：

```text
GET  /api/recommend/hot-top10
GET  /api/recommend/list
POST /api/recommend/rating
```

路线：

```text
GET  /api/route/scopes
GET  /api/route/pois
POST /api/route/shortest
POST /api/route/optimal
POST /api/route/indoor
GET  /api/route/amap/config
POST /api/route/amap/plan
POST /api/route/location/resolve
```

日志：

```text
GET    /api/log/list
GET    /api/log/{id}
POST   /api/log/create
DELETE /api/log/{id}
POST   /api/log/{id}/like
GET    /api/log/{id}/comments
POST   /api/log/{id}/comments
```

圈子：

```text
GET  /api/circle/list
POST /api/circle/create
GET  /api/circle/{id}
POST /api/circle/{id}/join
POST /api/circle/{id}/leave
GET  /api/circle/{id}/logs
POST /api/circle/{id}/logs
```

行程：

```text
POST   /api/itinerary/generate
POST   /api/itinerary/plans
GET    /api/itinerary/plans
GET    /api/itinerary/plans/favorites
GET    /api/itinerary/plans/{id}
DELETE /api/itinerary/plans/{id}
POST   /api/itinerary/plans/{id}/copy
POST   /api/itinerary/plans/{id}/favorite
GET    /api/itinerary/shared-plans/{id}
GET    /api/itinerary/shared-plans/hot
```

用户景点行为：

```text
GET    /api/user/spot-actions/{targetId}
POST   /api/user/spot-actions/browse
POST   /api/user/spot-actions/favorite
POST   /api/user/spot-actions/want
POST   /api/user/spot-actions/visited
POST   /api/user/spot-actions/dislike
GET    /api/user/spot-actions/list
```

通知：

```text
GET    /api/notifications
GET    /api/notifications/unread-count
PATCH  /api/notifications/{id}/read
PATCH  /api/notifications/read-all
```

举报：

```text
POST   /api/reports
GET    /api/admin/reports
PATCH  /api/admin/reports/{id}
```

景点 AI 简介：

```text
GET    /api/search/facility/{id}/ai-summary?force=false
```

路线记录：

```text
POST   /api/route/records
GET    /api/route/records
DELETE /api/route/records/{id}
```

上传：

```text
POST /api/upload
```

后台管理：

```text
GET    /api/admin/dashboard/summary
GET    /api/admin/spots
POST   /api/admin/spots/{id}/image
POST   /api/admin/maps/{placeGroupId}
POST   /api/admin/route-graphs/{placeGroupId}/pois
DELETE /api/admin/route-graphs/{placeGroupId}/pois/{poiId}
```

## 测试与验证

后端编译：

```bash
mvn -q -DskipTests compile
```

后端测试：

```bash
mvn -q test
```

前端构建：

```bash
cd Frontend
npm run build
```

数据库连通验证：

```bash
curl http://localhost:8080/api/search/types
```

高德配置验证：

```bash
curl http://localhost:8080/api/route/amap/config
```

## 演示建议

推荐演示链路：

1. 登录系统。
2. 首页查看热门推荐。
3. 使用景点查询，按城市、标签、简称筛选景点。
4. 进入景点详情页，查看地图、图片、评分和日志。
5. 发布一条带多维评分的旅行日志。
6. 查看个人主页，确认日志同步出现。
7. 进入圈子，在圈子中发布或查看关联景点的日志。
8. 使用景点之间路线，展示高德地图真实导航。
9. 使用景点内部路线，展示平面图、本地 POI、路网和最短路径。
10. 进入后台路线编辑器，展示管理员如何标注路口、路线和补充 POI。
11. 说明日志正文使用 Huffman 进行无损压缩。

答辩时可以重点讲：

- 为什么把景点和内部 POI 分开。
- 为什么景点之间使用高德，景点内部使用本地路网。
- Dijkstra 和 A* 的区别。
- 多点路线为什么需要先确定访问顺序。
- Huffman 压缩为什么是无损压缩。
- MyBatis XML 相比注解 SQL 更适合复杂 SQL 管理。
- Controller / Service / Mapper 三层职责。

### 5. AI 景点智能简介

景点详情页集成 DeepSeek API 自动生成景点介绍：

1. 前端请求 `GET /api/search/facility/{id}/ai-summary`。
2. 后端先查 `pois.ai_summary` 字段是否已有缓存。
3. 有缓存直接返回，不消耗 token。
4. 无缓存则从数据库读取景点完整信息（名称、类型、地址、开放时间、评分等）构建 Prompt，调用 DeepSeek Chat API 生成 250-300 字介绍。
5. 生成结果写入 `pois.ai_summary`，同时记录 `ai_summary_at` 时间戳。
6. 支持 `force=true` 参数强制重新生成并更新缓存。

相关字段：

```text
pois.ai_summary          缓存的 AI 简介文本
pois.ai_summary_at       生成时间
```

相关代码：

```text
src/main/java/com/tourwise/service/AiSummaryService.java
src/main/java/com/tourwise/config/AiProperties.java
```

## 已知边界

1. 内部路线质量依赖人工标注。
   - 如果平面图上的 POI 和路网没有校准，算法结果一定会失真。
   - 当前建议重点展示北邮沙河和北邮西土城这类精修样板。

2. 高德定位不是室内米级定位。
   - 手机端通常比电脑端更准确。
   - 浏览器定位精度低时，系统会提示手动确认。

3. Huffman 当前保留明文字段。
   - 这是为了兼容日志搜索。
   - 如果要真正减少数据库总空间，需要重构搜索存储方案。

4. 后台管理仍可继续增强。
   - 用户管理、角色权限、审核流、操作日志等还可以继续完善。

5. 项目目前更适合课程设计和实习项目展示。
   - 如果要生产上线，还需要补充更完整的接口测试、安全校验、部署脚本和监控。
