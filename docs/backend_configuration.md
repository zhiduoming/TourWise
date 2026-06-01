# TourWise 后端配置规范

## 配置分层

后端配置分三层：

1. `src/main/resources/application.yaml`
   - 提交到仓库
   - 只放默认值、环境变量占位、非敏感配置

2. `src/main/resources/application-local.yaml`
   - 本机私有配置
   - 已加入 `.gitignore`
   - 可以放本地 MySQL 密码、OSS AccessKey、开发 JWT secret

3. 环境变量
   - 适合部署或临时启动时覆盖配置
   - 例如 `TOURWISE_DB_PASSWORD`、`TOURWISE_OSS_ACCESS_KEY_SECRET`

## application.yaml 负责的内容

当前公共配置包括：

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

mybatis:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: com.tourwise
  configuration:
    map-underscore-to-camel-case: true
```

说明：

- API 统一前缀是 `/api`
- MyBatis XML 统一放在 `src/main/resources/mapper/**/*.xml`
- Java Mapper 接口只保留方法签名，复杂 SQL 放 XML
- `map-underscore-to-camel-case` 开启后，数据库字段如 `created_at` 可以映射为 Java 的 `createdAt`

## 环境变量清单

### 数据库

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `TOURWISE_DB_HOST` | `localhost` | MySQL 地址 |
| `TOURWISE_DB_PORT` | `3306` | MySQL 端口 |
| `TOURWISE_DB_NAME` | `tourist_system` | 数据库名 |
| `TOURWISE_DB_USER` | `root` | 数据库用户名 |
| `TOURWISE_DB_PASSWORD` | 空 | 数据库密码 |

### JWT

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `TOURWISE_JWT_SECRET` | `tourwise-dev-secret-change-me` | JWT HMAC 密钥 |

本地可以用默认值，答辩演示也可以用本地密钥。不要在公开仓库里写真实生产密钥。

### OSS

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `TOURWISE_OSS_ENDPOINT` | `https://oss-cn-beijing.aliyuncs.com` | 北京地域 endpoint |
| `TOURWISE_OSS_BUCKET` | `tourwise` | bucket 名称 |
| `TOURWISE_OSS_ACCESS_KEY_ID` | 空 | AccessKey ID |
| `TOURWISE_OSS_ACCESS_KEY_SECRET` | 空 | AccessKey Secret |
| `TOURWISE_OSS_PUBLIC_BASE_URL` | `https://tourwise.oss-cn-beijing.aliyuncs.com` | 图片访问基础 URL |
| `TOURWISE_OSS_AVATAR_PREFIX` | `avatar` | 头像目录 |
| `TOURWISE_OSS_FILE_PREFIX` | `uploads` | 通用上传目录 |

如果 bucket 是私有读，前端直接访问图片 URL 会失败。当前第一版按公共读 URL 设计；如果你后面要私有读，需要把返回 URL 改成签名 URL。

## application-local.yaml 模板

本机可以创建：

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

启动时使用：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## 前端配置

开发环境：

```text
Frontend/.env.development
```

内容：

```text
VITE_API_BASE_URL=/api
```

Vite 代理把 `/api` 转发到后端 `http://localhost:8080`。

## 当前配置边界

- 不在 `application.yaml` 里写真实密码或 AccessKey。
- 不自动建库，数据库仍然由手动 SQL 导入。
- 不引入 Spring Security，继续使用轻量 JWT 拦截器。
- 本地上传统一走 OSS，不再把游记图片保存到本地目录。
- 头像恢复默认值不删除 OSS 文件。
