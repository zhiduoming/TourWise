# TourWise 后端启动与验收说明

## 1. 环境要求

- JDK 21
- Maven 3.9+
- MySQL 8.x
- Node.js 18+，用于前端联调
- 阿里云 OSS Bucket：`tourwise`
- OSS 地区：北京，endpoint 使用 `https://oss-cn-beijing.aliyuncs.com`

## 2. 数据库导入顺序

数据库名默认使用 `tourist_system`。

按 `Database/backend_handoff.md` 的顺序导入：

1. `Database/schema.sql`
2. `Database/seed.sql`
3. `Database/campus_poi_seed.sql`
4. `Database/placeholder_poi_seed.sql`
5. `Database/bupt_route_edges_seed.sql`
6. 需要文件表时再导入 `Database/migration_add_files_table.sql`

## 3. 本地配置

推荐使用本地 profile，不把密钥提交到代码里。

本地配置文件：

```text
src/main/resources/application-local.yaml
```

需要配置：

```yaml
spring:
  datasource:
    username: root
    password: your_mysql_password

tourwise:
  jwt:
    secret: your_local_jwt_secret
  oss:
    endpoint: https://oss-cn-beijing.aliyuncs.com
    bucket: tourwise
    access-key-id: your_access_key_id
    access-key-secret: your_access_key_secret
    public-base-url: https://tourwise.oss-cn-beijing.aliyuncs.com
    avatar-prefix: avatar
    file-prefix: uploads
```

`application-local.yaml` 已放入 `.gitignore`，不要把 AccessKey 写进可提交文件。

## 4. 启动命令

后端：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

后端地址：

```text
http://localhost:8080/api
```

前端：

```bash
cd Frontend
npm install
npm run dev
```

前端开发环境通过 `Frontend/.env.development` 配置：

```text
VITE_API_BASE_URL=/api
```

Vite 代理负责转发到后端 `localhost:8080`。

更完整的配置说明见 `docs/backend_configuration.md`。

## 5. 已实现后端模块

### 用户与鉴权

- `POST /api/user/register`
- `POST /api/user/login`
- `GET /api/user/me`
- `GET /api/user/{id}`
- `GET /api/user/profile`
- `PUT /api/user/profile`
- `POST /api/user/avatar`
- `DELETE /api/user/avatar`
- `POST /api/user/logout`

鉴权方式：

```text
Authorization: Bearer <token>
```

### 搜索与推荐

- `GET /api/search/types`
- `GET /api/search/facilities`
- `GET /api/search/facility/{id}`
- `GET /api/recommend/hot-top10`
- `GET /api/recommend/list`
- `POST /api/recommend/rating`

### 美食

- `GET /api/food/list`
- `GET /api/food/list/{id}`
- `GET /api/food/recommend`
- `POST /api/food/review`

### 路线规划

- `GET /api/route/pois`
- `POST /api/route/shortest`
- `POST /api/route/optimal`
- `POST /api/route/indoor`

### 游记

- `GET /api/log/list`
- `GET /api/log/{id}`
- `POST /api/log/create`
- `DELETE /api/log/{id}`
- `POST /api/log/{id}/like`

### 圈子

- `GET /api/circle/list`
- `POST /api/circle/create`
- `GET /api/circle/{id}`
- `POST /api/circle/{id}/join`
- `POST /api/circle/{id}/leave`
- `GET /api/circle/{id}/logs`
- `POST /api/circle/{id}/logs`
- `POST /api/circle/{logId}/like`
- `GET /api/circle/{logId}/comments`
- `POST /api/circle/{logId}/comments`

### 文件上传

- `POST /api/upload`

请求类型：`multipart/form-data`

字段：

- `file`：图片文件
- `scene`：上传场景，可选，例如 `log`、`circle-log`、`circle-cover`

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "url": "https://tourwise.oss-cn-beijing.aliyuncs.com/uploads/log/...",
    "imageUrl": "https://tourwise.oss-cn-beijing.aliyuncs.com/uploads/log/...",
    "fileName": "example.png"
  }
}
```

## 6. 路线算法说明

路线模块使用 `route_edges` 表构建图。

图节点：

- POI，来自 `pois`

图边：

- 路线边，来自 `route_edges`
- 每条边包含距离、耗时、交通方式、拥挤系数、是否室内等信息

最短路径：

- 使用 Dijkstra
- 默认权重：`distance_m * congestion_factor`
- 如果偏好包含 `fastest`，权重改为 `duration_min * congestion_factor`

多点路线：

- 按传入点顺序串联多段 Dijkstra
- 每一段都返回实际经过的 POI 节点
- 最终合并为一条总路线

室内路线：

- 第一版筛选 `is_indoor = 1` 或 `transport_type = 'indoor'`
- 如果没有可用室内边，返回明确错误或空路线提示

## 7. 验收清单

后端基础：

```bash
mvn test
```

数据库只读：

```bash
curl http://localhost:8080/api/search/types
curl http://localhost:8080/api/recommend/hot-top10
curl http://localhost:8080/api/food/list
```

路线规划：

```bash
curl -X POST http://localhost:8080/api/route/shortest \
  -H 'Content-Type: application/json' \
  -d '{"start":"沙河校区图书馆","end":"北区食堂","preferences":["fastest"]}'
```

未登录保护：

```bash
curl -X POST http://localhost:8080/api/upload
```

应返回：

```json
{"code":401,"message":"未登录或登录已过期","data":null}
```

写入验收：

1. 注册用户
2. 登录拿 token
3. 带 `Authorization` 请求头访问：
   - `POST /api/log/create`
   - `POST /api/log/{id}/like`
   - `POST /api/circle/{id}/join`
   - `POST /api/circle/{id}/logs`
   - `POST /api/circle/{logId}/comments`

前端联调：

1. 后端启动在 `8080`
2. 前端启动在 `5173`
3. 登录
4. 验证首页、搜索、详情、美食、路线、游记、圈子页面都能加载真实数据
5. 验证头像、游记配图、圈子日志配图、圈子封面能上传到 OSS

## 8. 当前边界

- 没有引入完整 Spring Security，使用轻量 JWT 拦截器。
- 游记删除为软删除，不物理删除数据。
- 头像恢复默认头像时不删除 OSS 物理文件。
- 推荐算法为规则推荐，不是机器学习。
- 路线前端第一版是 SVG 示意图，不是第三方地图 SDK。
- 如果 OSS bucket 是私有读，前端直接访问图片 URL 会失败；第一版建议 bucket 设置公共读，或者后续改成签名 URL。
