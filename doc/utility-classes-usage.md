# 项目工具类使用说明

本文档记录当前 `studentAnalytics` 后端项目中已经存在的工具类、上下文类和异常辅助类的用途及推荐用法。文档基于当前源码整理，后续如果工具类字段或包名调整，应同步更新这里。

## 1. Result

位置：

```text
src/main/java/com/ahogeking/studentanalytics/common/Result.java
```

用途：

`Result<T>` 是统一接口返回对象，用于让 controller 返回稳定的数据结构。

当前返回字段：

```java
private Integer code;
private String message;
private T data;
private LocalDateTime timestamp;
```

约定：

- `code = 0` 表示成功。
- 非 `0` 表示失败。
- `message` 是给前端或调用方看的提示信息。
- `data` 是接口业务数据。
- `timestamp` 由静态工厂方法自动生成。

常用写法：

```java
return Result.success();
```

```java
return Result.success(user);
```

```java
return Result.error("用户名已存在");
```

```java
return Result.error(401, "登录状态已失效");
```

当前项目示例：

```java
@PostMapping("/register")
public Result<Void> register(@RequestBody @Valid RegisterRequest registerRequest) {
    sysUserService.register(registerRequest);
    return Result.success();
}
```

注意事项：

- Controller 层优先返回 `Result<T>`，不要一会儿返回字符串、一会儿返回对象。
- 如果后续增加全局异常处理器，异常也应该统一转换成 `Result.error(...)`。

## 2. PasswordUtil

位置：

```text
src/main/java/com/ahogeking/studentanalytics/common/PasswordUtil.java
```

用途：

`PasswordUtil` 是密码加密和校验工具类，内部使用 `BCryptPasswordEncoder`。

当前方法：

```java
public static String encode(String rawPassword)
```

```java
public static boolean matches(String rawPassword, String encodedPassword)
```

注册时加密密码：

```java
String encodedPassword = PasswordUtil.encode(registerRequest.getPassword());
user.setPassword(encodedPassword);
```

登录时校验密码：

```java
if (!PasswordUtil.matches(request.getPassword(), user.getPassword())) {
    throw new RuntimeException("用户名或密码错误");
}
```

当前项目示例：

```java
user.setPassword(PasswordUtil.encode(registerRequest.getPassword()));
```

注意事项：

- `PasswordUtil` 是 `final` 工具类，构造器是 `private`，不能注入，也不能 `new PasswordUtil()`。
- 正确用法是直接调用静态方法：`PasswordUtil.encode(...)`、`PasswordUtil.matches(...)`。
- 不要把原始密码保存到数据库；写入 `sys_user.password` 前必须先加密。

## 3. JwtUtil

位置：

```text
src/main/java/com/ahogeking/studentanalytics/common/JwtUtil.java
```

用途：

`JwtUtil` 用于生成和解析 JWT 登录令牌。它是 Spring 管理的 Bean，使用 `@Component` 和 `@ConfigurationProperties(prefix = "app.jwt")` 读取配置。

当前配置字段：

```yaml
app:
  jwt:
    secret: your-local-secret-at-least-32-bytes
    expiration-hours: 24
```

当前项目通过 `application.yml` 导入本地 JWT 配置：

```yaml
spring:
  config:
    import: optional:classpath:jwt.yml
```

推荐做法：

- 把真实密钥放在本地 `src/main/resources/jwt.yml`。
- 不要把真实密钥写进文档、代码注释或提交记录。
- 当前 `.gitignore` 已忽略 `src/main/resources/jwt.yml`。

### 生成登录令牌

在登录成功后注入 `JwtUtil`，并调用：

```java
String token = jwtUtil.createLoginToken(
        user.getId(),
        user.getUsername(),
        user.getRole()
);
```

生成的 token 包含：

- `subject`：用户 id 字符串。
- `userId`：用户 id。
- `username`：用户名。
- `role`：用户角色。
- `issuedAt`：签发时间。
- `expiration`：过期时间。

### 解析令牌

```java
Claims claims = jwtUtil.parseToken(token);
```

`parseToken` 支持两种传入格式：

```text
Bearer xxxxxx
```

或：

```text
xxxxxx
```

读取用户信息：

```java
Integer userId = jwtUtil.getUserId(token);
String username = jwtUtil.getUsername(token);
String role = jwtUtil.getRole(token);
```

注意事项：

- `app.jwt.secret` 不能为空。
- `app.jwt.secret` 的 UTF-8 字节长度不能小于 32。
- 令牌无效、过期或格式错误时，`JwtUtil` 会抛出 `JwtAuthenticationException`。
- `JwtUtil` 是 Spring Bean，应该通过构造器注入，不要手动 `new JwtUtil()`。

推荐注入方式：

```java
@Service
public class LoginService {
    private final JwtUtil jwtUtil;

    public LoginService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
}
```

## 4. SysUserContext

位置：

```text
src/main/java/com/ahogeking/studentanalytics/context/SysUserContext.java
```

用途：

`SysUserContext` 是请求线程内的当前登录用户上下文。它内部使用 `ThreadLocal<Claims>` 保存当前请求解析出来的 JWT claims。

当前方法：

```java
public static void set(Claims claims)
public static Claims get()
public static Integer getUserId()
public static String getUsername()
public static String getRole()
public static boolean isAdmin()
public static boolean isTeacher()
public static boolean isStudent()
public static void remove()
```

推荐使用流程：

1. 拦截器从请求头读取 `Authorization`。
2. 使用 `JwtUtil.parseToken(...)` 解析 token。
3. 调用 `SysUserContext.set(claims)` 写入当前线程。
4. Controller 或 Service 使用 `SysUserContext.getUserId()` 等方法读取当前用户。
5. 请求结束时调用 `SysUserContext.remove()` 清理线程变量。

拦截器伪代码：

```java
String token = request.getHeader("Authorization");
Claims claims = jwtUtil.parseToken(token);
SysUserContext.set(claims);
```

请求结束时必须清理：

```java
SysUserContext.remove();
```

业务代码读取当前用户：

```java
Integer currentUserId = SysUserContext.getUserId();
String currentUsername = SysUserContext.getUsername();
String role = SysUserContext.getRole();
```

角色判断：

```java
if (!SysUserContext.isAdmin()) {
    throw new RuntimeException("无权限");
}
```

注意事项：

- `SysUserContext` 只在当前请求线程有效。
- 如果没有先 `set(claims)`，`getUserId()`、`getUsername()`、`getRole()` 会返回 `null`。
- 必须在请求结束时调用 `remove()`，否则线程池复用时可能出现用户上下文污染。
- 当前项目还没有完整登录拦截器时，不要假设 `SysUserContext` 一定有值。

## 5. JwtAuthenticationException

位置：

```text
src/main/java/com/ahogeking/studentanalytics/exception/JwtAuthenticationException.java
```

用途：

`JwtAuthenticationException` 是 JWT 认证失败时使用的运行时异常。

当前使用位置：

```java
throw new JwtAuthenticationException("登录状态已失效，请重新登录", e);
```

```java
throw new JwtAuthenticationException("未登录");
```

推荐处理方式：

后续可以增加全局异常处理器，把该异常统一转换成接口返回：

```java
Result.error(401, ex.getMessage());
```

注意事项：

- 它目前只是异常类型本身，不会自动改变 HTTP 状态码。
- 如果没有全局异常处理器，异常会按 Spring Boot 默认错误响应返回。

## 6. 当前推荐调用关系

注册流程：

```text
SysUserController.register
  -> SysUserService.register
  -> PasswordUtil.encode
  -> SysUserMapper.insert
  -> Result.success
```

登录流程建议：

```text
LoginController.login
  -> 查询 sys_user
  -> PasswordUtil.matches
  -> JwtUtil.createLoginToken
  -> Result.success(token)
```

认证流程建议：

```text
AuthInterceptor.preHandle
  -> 读取 Authorization
  -> JwtUtil.parseToken
  -> SysUserContext.set

Controller / Service
  -> SysUserContext.getUserId / getRole

AuthInterceptor.afterCompletion
  -> SysUserContext.remove
```

## 7. 维护约定

- 工具类如果是 `static` 方法风格，例如 `PasswordUtil`，就不要声明成 Spring Bean，也不要构造注入。
- 需要读取配置、维护初始化状态的类，例如 `JwtUtil`，应该交给 Spring 管理并使用构造器注入。
- `ThreadLocal` 上下文类必须提供 `remove()`，并且拦截器必须在请求结束时调用。
- 文档里不要写真实密钥、数据库密码、token 样例中的真实生产值。
