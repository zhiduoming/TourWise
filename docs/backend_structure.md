# TourWise 后端目录结构说明

当前后端采用显式三层架构，并把请求对象、数据库模型、公共能力分开。

```text
src/main/java/com/tourwise
├── controller   # 接口层：接收 HTTP 请求，做参数校验，返回统一响应
├── service      # 业务层：业务规则、鉴权上下文、路线算法、推荐逻辑
├── mapper       # 数据访问层：MyBatis Mapper 接口，只保留方法签名
├── dto          # 请求 DTO：前端请求体对象，例如登录、注册、路线规划请求
├── vo           # 响应 VO：后端返回给前端看的对象，例如用户信息、路线结果
├── model        # 数据模型：Mapper 插入对象、算法节点和边对象
├── common       # 通用响应、分页、异常、Map 字段处理
├── config       # Spring MVC、OSS 配置属性
└── security     # JWT、登录用户上下文、鉴权拦截器
```

MyBatis XML：

```text
src/main/resources/mapper
├── circle
├── food
├── log
├── recommend
├── route
├── search
└── user
```

## 分层职责

### Controller

Controller 只做三件事：

1. 声明接口路径和 HTTP 方法。
2. 接收请求参数或请求体。
3. 调用 Service 并包装成 `ApiResponse`。

例如：

```java
@PostMapping("/login")
public ApiResponse<LoginVO> login(@RequestBody @Valid LoginRequest request) {
    return ApiResponse.ok(userService.login(request));
}
```

Controller 不直接写 SQL，也不直接处理复杂业务。

### Service

Service 是业务主线。

这里放：

- 用户注册、登录、资料更新
- 推荐打分规则
- 路线 Dijkstra 算法
- 游记点赞、软删除、评论嵌套
- 圈子加入、退出、发帖规则
- OSS 上传前的业务校验

Service 可以调用 Mapper，但不关心 HTTP 细节。

### Mapper

Mapper 接口只保留方法签名：

```java
List<Map<String, Object>> list(...);
```

SQL 统一放在 XML：

```xml
<select id="list" resultType="java.util.Map">
    ...
</select>
```

这样 SQL 更容易阅读、修改和答辩展示。

### DTO

DTO 是前端传进来的请求对象。

例如：

- `LoginRequest`
- `RegisterRequest`
- `RouteRequest`
- `LogCreateRequest`
- `CircleCreateRequest`

DTO 可以带 `@NotBlank`、`@Size`、`@DecimalMin` 等校验注解。

### VO

VO 是后端返回给前端的响应对象。

例如：

- `LoginVO`：登录成功返回 `token` 和 `user`
- `UserVO`：返回前端可见的用户信息，不暴露 `passwordHash`
- `RouteResultVO`：返回路线总距离、总时间、路径点
- `FoodVO`：返回美食列表和详情字段
- `LogVO`、`CommentVO`：返回游记、评论和嵌套回复
- `CircleVO`、`CircleListVO`：返回圈子详情、成员、列表分组

VO 和 DTO 的方向不同：

- DTO：前端 -> 后端
- VO：后端 -> 前端

为了兼容现有 Vue 页面，部分 VO 同时保留了 camelCase 和 snake_case 字段，例如 `createdAt` 与 `created_at`、`likeCount` 与 `like_count`。

### Model

Model 是后端内部使用的数据对象。

例如：

- `UserAccount`
- `LogRecord`
- `CircleRecord`
- `RoutePoi`
- `RouteEdge`

这些对象主要服务于 Mapper 插入、查询映射或路线算法。

## 当前设计取舍

这次整理选择的是“按层分包”：

```text
controller / service / mapper / dto / model
```

优点是：

- 三层架构一眼能看出来。
- 适合课程设计和答辩展示。
- Mapper XML 和 Java 接口职责清楚。

代价是：

- 当项目继续变大时，一个包里会有较多业务类。
- 真实大型项目也常用“按业务模块分包”，例如 `user/controller/service/mapper`。

对当前课设来说，显式三层结构更容易讲清楚，也更符合老师常见的 Java Web 项目预期。

当前实际结构已经扩展为：

```text
controller / service / mapper / dto / vo / model
```

其中 `vo` 让接口返回结构更明确，适合答辩时说明“请求对象”和“响应对象”是分开的。
