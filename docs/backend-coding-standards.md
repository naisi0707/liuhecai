# Spring Boot + MyBatis-Plus 后端编码规范

> 适用于本项目（多租户站点：单库 `tenant_id` + Spring Boot API）。  
> 目标：Controller 薄、出参统一 VO、租户不串数据、写法一致可维护。

---

## 1. 技术栈与基本原则

| 项 | 约定 |
|---|---|
| 框架 | Spring Boot 3.x |
| ORM | MyBatis-Plus |
| 校验 | Jakarta Validation |
| 转换 | MapStruct |
| 返回 | 统一 `Result<T>`：`{ code, message, data }` |
| 异常 | `ErrorCode` 枚举 + `BusinessException` + 全局处理 |
| SQL | **不写** `resources/mapper/**/*.xml`，用 MP API / Wrapper / Mapper 注解 |
| 多租户 | 单库 + `tenant_id`，推荐 MP TenantLine 插件自动隔离 |

**基本原则**

1. 按**分层**分包，职责清晰，禁止跨层乱调。
2. Controller **只做编排**：收参 → 校验 → 调 Service → 返回 `Result`。
3. API **禁止**直接暴露 Entity；入参用 DTO，出参用 VO。
4. 业务失败走统一异常与错误码，禁止随意 `return null` 或散落字符串。
5. 站点业务数据必须租户隔离。

---

## 2. 包 / 目录结构

根包：`com.liuhecai`（若调整，全项目保持一致）。

```
com.liuhecai
├── LiuhecaiApplication.java
├── common                 # 公共能力
│   ├── result             # Result
│   ├── exception          # BusinessException、GlobalExceptionHandler
│   ├── enums              # ErrorCode 等
│   └── util
├── config                 # MP、Web、Redis、Jackson 等配置
├── controller             # HTTP 入口
├── service                # 业务接口
│   └── impl               # 业务实现
├── mapper                 # MyBatis-Plus Mapper（仅 Java，无 XML）
├── entity                 # 表实体
├── dto                    # 入参：XxxRequest / XxxQuery
├── vo                     # 出参：XxxVO
├── convert                # MapStruct 转换器
├── tenant                 # 租户解析、TenantContext、拦截器/过滤器
└── job                    # 定时任务（如开奖拉取）

src/main/resources/
├── application.yml
└── application-*.yml
```

**明确不存在**：`src/main/resources/mapper/` 及任何 MyBatis XML。

---

## 3. 分层职责

| 层 | 允许 | 禁止 |
|---|---|---|
| Controller | 定义路由、接收参数、`@Valid`、调用 Service、返回 `Result<VO>` | 写业务逻辑、直接注入/调用 Mapper、返回 Entity、解析租户细节 |
| Service | 业务规则、事务、调用 Mapper、MapStruct 转换、抛 `BusinessException` | 依赖 `HttpServletRequest` 取 Host（租户从 `TenantContext` 取）、拼 HTTP 细节 |
| Mapper | CRUD、Wrapper、`@Select` 等注解 SQL | 业务判断、组装 VO、处理权限 |
| Entity | 与表字段映射 | 作为 Controller 入参/出参、塞业务方法 |

### 3.1 Controller（必须很薄）

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @PostMapping
    public Result<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.ok(userService.create(request));
    }

    @GetMapping("/{id}")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.ok(userService.getDetail(id));
    }

    @GetMapping
    public Result<PageResult<UserVO>> page(@Valid UserQuery query) {
        return Result.ok(userService.page(query));
    }
}
```

### 3.2 Service

```java
public interface UserService {
    UserVO create(UserCreateRequest request);
    UserVO getDetail(Long id);
    PageResult<UserVO> page(UserQuery query);
}
```

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserConvert userConvert;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO create(UserCreateRequest request) {
        // 业务校验 → 转 Entity → insert → 转 VO
        User entity = userConvert.toEntity(request);
        userMapper.insert(entity);
        return userConvert.toVo(entity);
    }

    @Override
    public UserVO getDetail(Long id) {
        User entity = userMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return userConvert.toVo(entity);
    }
}
```

### 3.3 Mapper

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT COUNT(1) FROM user WHERE tenant_id = #{tenantId} AND username = #{username}")
    long countByUsername(@Param("tenantId") Long tenantId, @Param("username") String username);
}
```

优先使用 `BaseMapper` + `LambdaQueryWrapper`；仅当 Wrapper 表达困难时再用注解 SQL。

---

## 4. 模型约定（Entity / DTO / VO）

| 类型 | 用途 | 命名示例 |
|---|---|---|
| Entity | 数据库表映射 | `User`、`LotteryDraw`、`Post` |
| DTO | API 入参 | `UserCreateRequest`、`UserUpdateRequest`、`UserQuery` |
| VO | API 出参 | `UserVO`、`LotteryDrawVO` |

**硬规则**

1. Entity **不得**出现在 Controller 方法签名（参数或返回值）。
2. VO **不得**包含密码哈希、内部密钥等敏感字段。
3. DTO 只保留接口需要的字段，不照搬整张表。
4. 查询条件用 `XxxQuery`；写操作用 `XxxCreateRequest` / `XxxUpdateRequest`。

```java
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String username;
    private String passwordHash;
    private Integer enabled;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
@Data
public class UserCreateRequest {
    @NotBlank
    @Size(max = 32)
    private String username;

    @NotBlank
    @Size(min = 6, max = 64)
    private String password;
}
```

```java
@Data
public class UserVO {
    private Long id;
    private String username;
    private Integer enabled;
    private LocalDateTime createdAt;
}
```

---

## 5. 统一返回 Result

结构固定为：

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

| 字段 | 说明 |
|---|---|
| `code` | `0` 成功；非 `0` 失败（与 `ErrorCode` 一致） |
| `message` | 提示文案 |
| `data` | 业务数据；失败时一般为 `null` |

```java
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.message = "ok";
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        r.data = null;
        return r;
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }
}
```

Controller 返回类型一律 `Result<T>`。列表分页建议：

```java
@Data
public class PageResult<T> {
    private long total;
    private long page;
    private long size;
    private List<T> records;
}
```

返回：`Result<PageResult<UserVO>>`。

---

## 6. 异常与错误码

### 6.1 ErrorCode 枚举

```java
@Getter
@AllArgsConstructor
public enum ErrorCode {
    SUCCESS(0, "ok"),
    BAD_REQUEST(40000, "请求参数错误"),
    VALIDATION_FAILED(40001, "参数校验失败"),
    UNAUTHORIZED(40100, "未登录"),
    FORBIDDEN(40300, "无权限"),
    USER_NOT_FOUND(10001, "用户不存在"),
    USERNAME_EXISTS(10002, "用户名已存在"),
    TENANT_NOT_FOUND(30001, "租户不存在"),
    SYSTEM_ERROR(50000, "系统繁忙，请稍后重试");

    private final int code;
    private final String message;
}
```

新增业务错误时：**只加枚举项**，禁止在业务代码里写魔法数字。

### 6.2 BusinessException

```java
@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
```

### 6.3 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValid(Exception e) {
        String msg = extractFirstErrorMessage(e);
        return Result.fail(ErrorCode.VALIDATION_FAILED.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        // 记录完整堆栈日志，勿把内部细节直接返回前端
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }
}
```

**约定**

- 可预期的业务失败：抛 `BusinessException`。
- 参数问题：交给校验注解 + 全局处理。
- 未知异常：统一 `SYSTEM_ERROR`，日志留痕。

---

## 7. 参数校验

1. 在 DTO 字段上使用 Jakarta 注解：`@NotNull`、`@NotBlank`、`@Size`、`@Min`、`@Max`、`@Email` 等。
2. Controller 类加 `@Validated`；`@RequestBody` 参数加 `@Valid`。
3. 路径变量、查询对象需要校验时，同样使用 `@Validated`。
4. 跨字段/状态机等业务规则放在 Service，用 `BusinessException`，不要硬塞进注解里。

```java
@GetMapping
public Result<PageResult<UserVO>> page(@Valid UserQuery query) {
    return Result.ok(userService.page(query));
}
```

---

## 8. MapStruct 转换

转换器统一放在 `convert` 包，由 Service 调用。

```java
@Mapper(componentModel = "spring")
public interface UserConvert {

    UserVO toVo(User entity);

    List<UserVO> toVoList(List<User> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    User toEntity(UserCreateRequest request);
}
```

**约定**

- 禁止在 Controller 里手写大段 `set` 做 Entity ↔ VO。
- 密码等需加工的字段：在 Service 中处理后再写入 Entity，或使用 MapStruct `@AfterMapping` / `expression`（复杂逻辑优先放 Service）。

---

## 9. MyBatis-Plus 约定

1. Mapper 继承 `BaseMapper<T>`。
2. 简单条件用 `LambdaQueryWrapper` / `LambdaUpdateWrapper`，避免字符串列名。
3. **禁止**新增 MyBatis XML；复杂 SQL 用 `@Select`、`@Update`、`@Insert`、`@Delete`。
4. 逻辑删除：`@TableLogic`（字段含义与全局配置一致，如 `0` 未删 / `1` 已删）。
5. 创建时间、更新时间：使用 MP 自动填充（`MetaObjectHandler`）。
6. 主键策略与表结构一致（推荐雪花 `ASSIGN_ID` 或按表约定）。
7. 分页：使用 MP `Page<>`，在 Service 转为 `PageResult<VO>` 再返回。
8. 写操作 Service 方法加：`@Transactional(rollbackFor = Exception.class)`。

```java
LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>()
        .eq(User::getUsername, username)
        .eq(User::getEnabled, 1);
User user = userMapper.selectOne(qw);
```

> 开启 TenantLine 后，站点表的 `tenant_id` 条件由插件自动追加；仍需保证实体含 `tenantId` 字段。

---

## 10. 多租户约定

本项目：**单库 + `tenant_id`**。

### 10.1 租户识别

| 端 | 方式 |
|---|---|
| 前台（站点域名） | 按请求 `Host` 解析域名 → 查租户 → 写入 `TenantContext` |
| 代理后台 | 登录态绑定运营租户 → `TenantContext` |
| 超管后台 | 默认可跨租户；操作某站数据时显式传入并校验权限后的 `tenantId` |

### 10.2 TenantContext

```java
public final class TenantContext {
    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(Long tenantId) {
        HOLDER.set(tenantId);
    }

    public static Long get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
```

Filter / Interceptor 必须在 `finally` 中 `TenantContext.clear()`，防止线程池复用导致串租户。

### 10.3 数据隔离硬规则

1. 站点业务表必须有 `tenant_id` 字段。
2. 普通查询 / 更新 / 删除必须限定当前租户（推荐 **MyBatis-Plus TenantLine 插件** 自动拼接）。
3. 禁止无租户条件的全表 `list()`、禁止只按主键更新却不校验租户归属。
4. 全局共享配置（如开奖源）可不带 `tenant_id`；写入各站结果表时必须带目标 `tenant_id`。
5. Job 批量处理各站时：循环内显式传入/设置 `tenantId`，**不要**依赖「当前 HTTP 请求上下文」。

### 10.4 与分层的关系

- Controller 不负责解析 Host、不直接操作 `TenantContext`（由 `tenant` 包的过滤器/拦截器完成）。
- Service 通过 `TenantContext.get()` 或方法参数获取租户；跨租户接口单独鉴权。

---

## 11. 接口与命名规范

| 对象 | 约定 | 示例 |
|---|---|---|
| Controller | `XxxController` | `UserController` |
| 路由前缀 | `/api/...`，资源名复数 | `/api/users` |
| Service | `XxxService` / `XxxServiceImpl` | |
| Mapper | `XxxMapper` | |
| HTTP 语义 | GET 查、POST 建、PUT/PATCH 改、DELETE 删 | |
| 包名 | 全小写 | `com.liuhecai.controller` |
| 布尔存库 | 优先 `enabled` 等 int/tinyint，慎用 `isXxx` 作 Entity 字段名 | |

方法命名建议：`create` / `update` / `delete` / `getDetail` / `page` / `list`，保持语义一致。

---

## 12. 禁止事项清单

1. Controller 编写业务逻辑，或直接注入 Mapper。
2. API 直接返回或接收 Entity。
3. 不使用 `Result` 包裹；用 `Map`、`JSONObject`、无结构对象当统一出参。
4. `catch` 后吞异常，或仅 `e.printStackTrace()` 不处理。
5. 查询站点业务表时缺少 `tenant_id` 条件（含未走租户插件且手写遗漏）。
6. 新增 `resources/mapper/**/*.xml`。
7. 在 Controller 中手写大段 Entity → VO 赋值。
8. 使用魔法数字作为业务错误码（必须使用 `ErrorCode`）。
9. 在 Service 中直接依赖 `HttpServletRequest` 解析域名/租户。
10. 忘记在请求结束时清理 `TenantContext`。

---

## 13. 提交前自检（Checklist）

- [ ] Controller 方法是否 ≤ 薄编排，无业务分支堆砌？
- [ ] 返回类型是否为 `Result<VO>` / `Result<PageResult<VO>>`？
- [ ] 入参是否为 DTO + `@Valid`？
- [ ] 业务失败是否抛 `BusinessException(ErrorCode)`？
- [ ] Entity ↔ VO 是否走 MapStruct？
- [ ] 是否未新增 MyBatis XML？
- [ ] 站点表操作是否具备租户隔离？
- [ ] 写操作是否有事务注解？

---

## 修订记录

| 日期 | 说明 |
|---|---|
| 2026-07-22 | 初版：分层结构、Result、异常校验、MapStruct、MP 无 XML、多租户约定 |
