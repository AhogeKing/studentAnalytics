# 2026-06-27

## 昨晚新增后端能力整理

本节只记录 2026-06-26 晚间新增或修复、且会影响后续前端开发的后端能力。此前前端已经适配的学生查询、学生详情、筛选项和分析图表接口不在这里重复展开。

## 系统用户删除策略判断

`sys_user` 和学生核心数据、模型训练数据之间没有直接强绑定：

- `student` 不依赖 `sys_user`。
- `student_performance` 不依赖 `sys_user`。
- 当前模型训练视图 `v_student_model_dataset` 只读取 `student` 和 `student_performance`。

因此，物理删除系统用户不会直接破坏学生数据和模型训练数据。

但 `sys_user` 在 V0 数据库中仍被以下辅助业务表引用：

- `model_version.created_by`
- `warning_record.handler_user_id`
- `import_batch.imported_by`
- `operation_log.user_id`

这些外键当前都设计为 `ON DELETE SET NULL`，所以物理删除用户不会导致外键错误，但会让历史记录失去操作者信息。

当前实现选择：

- `PATCH /admin/users/{id}/status`：启用或禁用账号。
- `DELETE /admin/users/{id}`：当前也执行禁用语义，即 `status = 0`。

这更接近“账号停用”，不是严格意义上的软删除。因为 `sys_user` 当前没有 `deleted` 字段。

后续如果要区分软删除和物理删除，可以设计成：

| 操作 | 建议接口 | 语义 |
| --- | --- | --- |
| 禁用账号 | `PATCH /admin/users/{id}/status` | 保留账号和历史关联，仅禁止登录 |
| 软删除账号 | 可新增 `deleted` 字段后使用 `DELETE /admin/users/{id}` | 从列表默认隐藏，但保留记录 |
| 物理删除账号 | 可新增 `DELETE /admin/users/{id}/physical` | 真实删除，历史表中的用户外键置空 |

当前阶段建议先保持 `status = 0` 的禁用语义即可。原因是前端用户管理、权限校验和旧 token 失效都已经围绕 `status` 工作，继续推进前端时不需要先改表。

## 权限与鉴权修复

### 已新增能力

- 新增 `@RequireRole` 注解。
- 新增 `RoleInterceptor`，用于拦截管理员专用接口。
- `AuthInterceptor` 在解析 JWT 后会用 `userId` 查询 `sys_user`。
- 如果用户不存在或 `status = 0`，返回 `401`，旧 token 不能继续访问。
- `ForbiddenException` 用于角色不足时返回 `403`。

### 2026-06-27 修复

JJWT 0.12 返回的 `Claims` 是不可变对象，不能执行 `claims.put(...)`。

修复方式：

- `SysUserContext` 不再保存 JWT `Claims`。
- `SysUserContext` 改为保存数据库查询出的 `SysUser`。
- `/me`、权限判断、当前用户 ID 都从数据库用户上下文读取。

这保证了用户状态和角色以数据库为准，也避免修改不可变 `Claims` 的运行时报错。

## 管理员用户管理接口

这些接口供后续前端新增“用户管理”页面使用。全部需要 `ADMIN` token。

基础路径：

```text
/StudentAnalytics/admin/users
```

| 方法 | 路径 | 用途 | 前端状态 |
| --- | --- | --- | --- |
| `GET` | `/admin/users?page_num=1&page_size=20` | 用户分页列表 | 待接入 |
| `GET` | `/admin/users/{id}` | 用户详情 | 待接入 |
| `POST` | `/admin/users` | 新增用户 | 待接入 |
| `PUT` | `/admin/users/{id}` | 修改真实姓名、角色、状态 | 待接入 |
| `PATCH` | `/admin/users/{id}/status` | 启用或禁用用户 | 待接入 |
| `PATCH` | `/admin/users/{id}/password` | 重置密码 | 待接入 |
| `DELETE` | `/admin/users/{id}` | 当前语义为禁用用户 | 待接入 |

分页默认：

- `page_num = 1`
- `page_size = 20`
- 最大 `page_size = 100`

用户列表返回 `PageResultVO<AdminUserVO>`：

- `total`
- `records`

`AdminUserVO` 字段：

- `id`
- `username`
- `real_name`
- `role`
- `status`
- `created_at`
- `updated_at`

前端注意：

- 不返回密码。
- `role` 当前只允许 `ADMIN` 和 `TEACHER`。
- 禁止创建 `STUDENT`。
- 禁止禁用当前登录用户。
- 禁止禁用或降级最后一个启用状态的 `ADMIN`。

## 学生新增接口

该接口供后续前端新增“新增学生”表单或按钮使用。需要 `ADMIN` token。

```text
POST /StudentAnalytics/students
```

请求体：

```json
{
  "student_no": 3001,
  "name": "New Student",
  "age": 16,
  "gender": 0,
  "ethnicity": 1,
  "parental_education": 2,
  "class_name": "1-3"
}
```

规则：

- `student_no` 必填且唯一。
- `name` 可空；为空时后端生成 `Student {studentNo}`。
- `age` 按数据库约束为 `0-30`。
- `gender` 为 `0/1`。
- `ethnicity` 为 `0-3`。
- `parental_education` 为 `0-4`。
- `class_name` 支持 `1-3` 或 `高一 3 班`。
- `grade_level` 由后端从 `class_name` 反推。
- 新增学生只写 `student` 表，不自动创建 `student_performance`。

返回：

```text
Result<StudentDetailVO>
```

前端保存成功后可以直接跳转详情页或刷新详情页。

## 学业表现 Upsert 接口

该接口供后续前端在学生详情页补充或编辑学习表现使用。需要 `ADMIN` token。

```text
PUT /StudentAnalytics/students/performance/{studentNo}
```

请求体：

```json
{
  "study_time_weekly": 12.5,
  "absences": 3,
  "tutoring": true,
  "parental_support": 3,
  "extracurricular": true,
  "sports": false,
  "music": true,
  "volunteering": false,
  "gpa": 3.7
}
```

语义：

- 如果学生已有 `student_performance`，则更新。
- 如果学生没有 `student_performance`，则新增。
- 学生不存在或已删除时返回业务错误。

规则：

- 不允许前端提交 `grade_class`。
- `grade_class` 由后端根据 GPA 自动计算。
- `data_source = MANUAL`。
- `data_quality_status = 0`。
- 更新已有记录时会清空 `quality_issue`。

返回：

```text
Result<StudentDetailVO>
```

前端保存成功后直接用返回的详情数据刷新页面即可。

## 现有写操作权限变化

以下接口昨晚新增了 `ADMIN` 权限限制：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `PUT` | `/students/overview/{studentNo}` | 修改学生概览字段 |
| `DELETE` | `/students/overview/{studentNo}` | 软删除学生 |

`TEACHER` 仍保留已有查看能力：

- 学生列表和筛选。
- 学生详情。
- 分析图表。
- 当前用户信息。

## 前端下一步边界

下一步写前端时，不需要重新做已经适配的后端接口：

- `GET /students/list`
- `GET /students/page`
- `GET /students/filter-options`
- `GET /students/detail/{studentNo}`
- `GET /analytics/gpa-distribution`
- `GET /analytics/grade-class-distribution`
- `GET /analytics/performance-points`

前端下一步应优先接入：

1. 管理员用户管理页面，对接 `/admin/users` 系列接口。
2. 学生新增入口，对接 `POST /students`。
3. 学生详情页的学习表现新增 / 编辑入口，对接 `PUT /students/performance/{studentNo}`。
4. 基于当前用户 `role` 控制按钮显示：`ADMIN` 显示新增、编辑、删除、用户管理；`TEACHER` 只显示查看。
5. 统一处理 `401` 和 `403`：`401` 走重新登录，`403` 给出无权限提示。

## 本次建议验证

后端提交或继续前端开发前，建议保留以下验证结果：

```bash
mvn -q -DskipTests compile
xmllint --noout src/main/resources/mapper/StudentMapper.xml src/main/resources/mapper/AnalysisMapper.xml
git diff --check -- src/main/java src/main/resources
```

`doc/backend-log.md` 被 `.gitignore` 的 `doc/*` 忽略。如果需要把这份日志提交到 Git，需要使用：

```bash
git add -f doc/backend-log.md
```

# 2026-06-26

## 文档用途

这份文档作为后端开发日志使用。后续后端相关的新增接口、Service 规则、Mapper 调整、数据库设计变化和验证结果，都继续追加到本文件中，并以当天日期作为一级标题。

本次记录基于当前 Spring Boot 后端实际代码，不包含前端实现细节。

## 当前后端总体状态

后端当前已形成以下可用能力：

- 用户认证与权限：注册、登录、退出、当前登录用户信息、基于角色的接口访问控制。
- 管理员用户管理：用户分页查询、详情、新增、修改、启用/禁用、重置密码、删除即禁用。
- 学生管理：学生概览列表、筛选、搜索、排序、新增、更新、软删除、详情聚合。
- 学业表现管理：查看学生表现，并支持按学生新增或更新表现记录。
- 分析统计：GPA 区间分布、成绩等级分布、学习表现散点数据，并支持按年级和班级限定分析范围。

运行配置：

- 服务端口：`8081`
- 上下文路径：`/StudentAnalytics`
- 数据库：`student_analytics`
- 数据源配置来自 `application.yml`，支持通过 `MYSQL_HOST`、`MYSQL_DATABASE`、`MYSQL_USER`、`MYSQL_PASSWORD`、`MYSQL_URL` 覆盖。
- MyBatis XML 位置：`classpath:mapper/*.xml`
- MyBatis 开启下划线到驼峰映射：`map-underscore-to-camel-case: true`

统一返回结构使用 `Result<T>`。业务异常通过 `BusinessException` 返回普通错误结果，JWT 鉴权异常通过 `JwtAuthenticationException` 返回 `401`，角色权限不足通过 `ForbiddenException` 返回 `403`。

## 鉴权与用户模块

### 已实现接口

| 方法 | 路径 | 功能 |
| --- | --- | --- |
| `POST` | `/register` | 注册系统用户 |
| `POST` | `/login` | 登录并返回 JWT |
| `POST` | `/logout` | 清理当前线程中的用户上下文 |
| `GET` | `/me` | 返回当前 JWT 中解析出的用户信息 |
| `GET` | `/hello` | 简单连通性测试 |

在完整访问地址中，需要带上上下文路径，例如：

```text
POST /StudentAnalytics/login
GET /StudentAnalytics/me
```

### 业务规则

- 注册时根据用户名查重，重复则抛出 `用户名已存在`。
- 密码通过 `PasswordUtil.encode()` 加密保存。
- 公开注册接口只允许创建 `TEACHER` 账号；未显式提交角色时，默认角色为 `TEACHER`。
- 公开注册接口不允许创建 `ADMIN` 或 `STUDENT`，管理员账号应通过管理员用户管理接口或数据库初始化准备。
- 登录时用户名不存在或密码不匹配，统一返回 `用户名或密码错误`。
- 登录成功后通过 `JwtUtil.createLoginToken()` 写入 `userId`、`username`、`realName`、`role`。

### 拦截器

`WebConfig` 注册 `AuthInterceptor`：

- 拦截路径：`/**`
- 放行路径：`/login`、`/register`

这意味着除注册和登录外，其它接口都应携带有效 `Authorization` 请求头。

`AuthInterceptor` 当前会在每次请求中：

- 读取 `Authorization` 请求头。
- 解析 JWT，取得 `userId`。
- 通过 `sys_user.id` 查询当前用户。
- 如果用户不存在或 `status = 0`，抛出 `JwtAuthenticationException`，旧 token 不能继续访问。
- 将当前数据库中的 `username`、`realName`、`role` 写回 `SysUserContext`。

`WebConfig` 还注册了 `RoleInterceptor`：

- 读取 Controller 类或方法上的 `@RequireRole`。
- 未标注 `@RequireRole` 的接口，只要求登录，不做角色限制。
- 标注了 `@RequireRole({"ADMIN"})` 的接口只允许管理员访问。
- 当前权限策略是简单角色控制，不引入 `permission`、`role_permission`、`user_role` 等 RBAC 表。

### 权限规则

当前已落地的权限规则：

| 模块 | ADMIN | TEACHER |
| --- | --- | --- |
| 登录 / 退出 / 当前用户 | 是 | 是 |
| 学生查询 / 详情 | 是 | 是 |
| 分析图表 | 是 | 是 |
| 学生新增 | 是 | 否 |
| 学生修改 | 是 | 否 |
| 学生删除 | 是 | 否 |
| 学业表现新增 / 修改 | 是 | 否 |
| 用户管理 | 是 | 否 |
| 操作日志查看 | 暂未实现，仅保留 TODO | 否 |
| 模型训练 | 暂未实现，仅保留 TODO | 否 |
| 单学生预测 | 暂未实现，仅保留 TODO | 否 |
| 风险预警 | 暂未实现，仅保留 TODO | 否 |

## 管理员用户管理模块

### 已实现接口

Controller 基础路径：

```text
/admin/users
```

全部接口都标注 `@RequireRole({"ADMIN"})`。

| 方法 | 路径 | 功能 |
| --- | --- | --- |
| `GET` | `/admin/users` | 分页查询用户列表 |
| `GET` | `/admin/users/{id}` | 查询用户详情 |
| `POST` | `/admin/users` | 新增用户 |
| `PUT` | `/admin/users/{id}` | 修改用户信息 |
| `PATCH` | `/admin/users/{id}/status` | 启用或禁用用户 |
| `PATCH` | `/admin/users/{id}/password` | 重置用户密码 |
| `DELETE` | `/admin/users/{id}` | 删除用户，实际语义为禁用 |

### 分页查询

`GET /admin/users` 支持参数：

| 参数 | 默认值 | 含义 |
| --- | --- | --- |
| `page_num` | `1` | 页码，从 1 开始 |
| `page_size` | `20` | 每页数量，最大限制为 100 |

返回结构使用 `PageResultVO<AdminUserVO>`：

- `total`：用户总数。
- `records`：当前页用户列表。

`AdminUserVO` 不返回密码字段，只返回：

- `id`
- `username`
- `real_name`
- `role`
- `status`
- `created_at`
- `updated_at`

分页查询当前由 `SysUserMapper.selectAdminUserPage()` 和 `countAdminUsers()` 显式 SQL 实现，没有依赖 MyBatis-Plus 分页插件。

### 新增用户

`POST /admin/users` 请求体：

```json
{
  "username": "teacher01",
  "password": "123456",
  "real_name": "Teacher 01",
  "role": "TEACHER"
}
```

规则：

- `username` 必填且唯一。
- `password` 必填，长度为 `6-100`，保存前通过 `PasswordUtil.encode()` 加密。
- `real_name` 可空，空白字符串会规范化为 `null`。
- `role` 只允许 `ADMIN` 或 `TEACHER`。
- 不开放创建 `STUDENT`。
- `status` 默认写入 `1`。

### 修改用户

`PUT /admin/users/{id}` 可修改：

- `real_name`
- `role`
- `status`

规则：

- 不支持修改 `username`，避免影响登录标识。
- `role` 只允许 `ADMIN` 或 `TEACHER`。
- `status` 只能为 `0` 或 `1`。
- 不能禁用当前登录用户。
- 不能禁用或降级最后一个启用状态的 `ADMIN`。

### 启用 / 禁用用户

`PATCH /admin/users/{id}/status` 请求体：

```json
{
  "status": 0
}
```

规则：

- `status = 1` 表示启用。
- `status = 0` 表示禁用。
- 不能禁用当前登录用户。
- 不能禁用最后一个启用状态的 `ADMIN`。
- 用户被禁用后，即使持有旧 JWT，也会在 `AuthInterceptor` 中被拒绝访问。

### 重置密码

`PATCH /admin/users/{id}/password` 请求体：

```json
{
  "new_password": "newPassword123"
}
```

规则：

- `new_password` 必填，长度为 `6-100`。
- 保存前通过 `PasswordUtil.encode()` 加密。

### 删除用户

`DELETE /admin/users/{id}` 当前不物理删除 `sys_user`，而是执行禁用语义：

```text
status = 0
```

原因是 `sys_user` 当前没有 `deleted` 字段，且用户可能被 `operation_log`、`model_version` 等表引用。

## 学生概览模块

### 已实现接口

Controller 路径同时兼容：

- `/students`
- `/student`

| 方法 | 路径 | 功能 |
| --- | --- | --- |
| `GET` | `/students/list` | 查询所有未删除学生概览 |
| `GET` | `/students/page` | 查询默认班级、关键词搜索或组合筛选后的学生概览 |
| `GET` | `/students/filter-options` | 返回前端筛选项 |
| `POST` | `/students` | 新增学生基础信息 |
| `PUT` | `/students/overview/{studentNo}` | 更新学生概览字段 |
| `DELETE` | `/students/overview/{studentNo}` | 按学号软删除学生 |
| `GET` | `/students/detail/{studentNo}` | 查询学生详情聚合信息 |

说明：

- 查询类接口仍兼容 `/students` 和 `/student` 两个 Controller 路径。
- 新增学生接口由 `StudentAdminController` 单独挂在 `/students`，不会生成 `/student` 别名。
- `POST /students`、`PUT /students/overview/{studentNo}`、`DELETE /students/overview/{studentNo}` 都要求 `ADMIN`。

### 查询能力

`GET /students/page` 支持以下参数：

| 参数 | 含义 |
| --- | --- |
| `class_name` | 班级，可重复传参，也兼容逗号分隔字符串 |
| `min_gpa` | GPA 下限 |
| `max_gpa` | GPA 上限 |
| `grade_class` | 成绩等级，`0-4` |
| `grade_level` | 年级，`1=高一`、`2=高二`、`3=高三` |
| `gender` | 性别，`0/1` |
| `sort_field` | 排序字段 |
| `sort_order` | `asc` 或 `desc` |
| `keyword` | 姓名或纯数字学号关键词 |

查询模式规则：

- 没有关键词和筛选条件时，默认查 `1-1` 班。
- 关键词搜索支持姓名模糊匹配；如果关键词为纯数字，也同时匹配 `student_no`。
- 关键词搜索不能与班级、GPA、成绩等级、年级、性别筛选混用。
- GPA 上下限如果传反，后端会自动交换。
- 排序字段白名单：`studentNo`、`name`、`age`、`gpa`、`gradeClass`。
- 排序 SQL 列由后端白名单转换，避免直接使用前端传入值拼接任意列。

### 班级格式

数据库存储班级为原始格式：

```text
1-1
1-2
2-3
```

后端同时支持把展示格式规范化为数据库格式：

```text
高一 1 班 -> 1-1
高二 3 班 -> 2-3
```

返回给前端时使用 `ClassInfoVO` 同时保留：

- `grade_level`：年级数字。
- `raw_class_name`：数据库原始班级值。
- `class_name`：中文展示值，例如 `高一 1 班`。

### 新增学生

`POST /students` 请求体：

```json
{
  "student_no": 3001,
  "name": "New Student",
  "age": 16,
  "gender": 0,
  "ethnicity": 1,
  "parental_education": 2,
  "class_name": "1-3"
}
```

也支持驼峰字段：

- `studentNo`
- `parentalEducation`
- `className`

新增规则：

- `student_no` 必填且唯一。
- `name` 可空；为空时后端自动生成 `Student {studentNo}`。
- `age` 按数据库约束校验为 `0-30`。
- `gender` 只能为 `0` 或 `1`。
- `ethnicity` 只能为 `0-3`。
- `parental_education` 只能为 `0-4`。
- `class_name` 必填，支持 `1-3` 或 `高一 3 班`。
- `grade_level` 不由前端提交，而是从 `class_name` 自动反推。
- `deleted` 默认写入 `0`。
- 新增学生只写入 `student` 表，不创建默认 `student_performance`。

返回：

```text
Result<StudentDetailVO>
```

新增后会立即调用 `selectStudentDetail(studentNo)` 返回详情。因为表现记录尚未创建时，`performance_available = false`。

### 更新能力

`PUT /students/overview/{studentNo}` 当前支持更新：

- `name`
- `age`
- `gender`
- `className`
- `gradeLevel`
- `gpa`

更新规则：

- `studentNo` 不能为空。
- 请求体不能为空，且必须包含至少一个可更新字段。
- `gradeClass` 不能手动提交，成绩等级由 GPA 自动计算。
- `gradeLevel` 不能单独修改；提交班级时后端会从班级推导年级。
- 如果同时提交 `gradeLevel` 和 `className`，二者必须一致。
- 修改 `gpa` 时同步更新 `student_performance.grade_class`。
- 如果学生没有成绩记录，不能修改 GPA。

GPA 到 `grade_class` 的规则：

| GPA 范围 | grade_class |
| --- | --- |
| `3.5 <= GPA <= 4.0` | `0` |
| `3.0 <= GPA < 3.5` | `1` |
| `2.5 <= GPA < 3.0` | `2` |
| `2.0 <= GPA < 2.5` | `3` |
| `GPA < 2.0` | `4` |

### 软删除

`DELETE /students/overview/{studentNo}` 不物理删除学生，而是更新：

```sql
deleted = 1
updated_at = CURRENT_TIMESTAMP
```

后续概览、筛选、详情和分析查询都以 `s.deleted = 0` 作为基础条件。

## 学业表现管理模块

### 已实现接口

| 方法 | 路径 | 功能 |
| --- | --- | --- |
| `PUT` | `/students/performance/{studentNo}` | 新增或更新指定学生的学业表现 |

该接口要求 `ADMIN`。

### 请求体

```json
{
  "study_time_weekly": 12.5,
  "absences": 3,
  "tutoring": true,
  "parental_support": 3,
  "extracurricular": true,
  "sports": false,
  "music": true,
  "volunteering": false,
  "gpa": 3.7
}
```

也支持驼峰字段：

- `studyTimeWeekly`
- `parentalSupport`

### Upsert 语义

后端按 `studentNo` 查找未删除学生：

- 学生不存在或已删除：返回 `学生不存在或已删除`。
- 已有 `student_performance`：更新原表现记录。
- 没有 `student_performance`：插入新表现记录。

写入规则：

- `study_time_weekly` 范围为 `0-60`，Java 使用 `BigDecimal`。
- `absences` 范围为 `0-30`。
- `tutoring`、`extracurricular`、`sports`、`music`、`volunteering` 由 Boolean 转为数据库 `0/1`。
- `parental_support` 范围为 `0-4`。
- `gpa` 范围为 `0-4`，Java 使用 `BigDecimal`。
- 前端不能提交 `grade_class`，成绩等级由 GPA 自动计算。
- 新增或更新后，`data_source` 写入 `MANUAL`。
- 新增或更新后，`data_quality_status` 写入 `0`。
- 更新已有表现记录时，清空 `quality_issue`。

返回：

```text
Result<StudentDetailVO>
```

保存成功后会重新查询并返回完整学生详情。

当前不开放：

```text
DELETE /students/performance/{studentNo}
```

原因是 `student_performance` 是分析统计、模型训练、预测和预警的核心数据，删除会引入较多后续一致性处理。

## 学生详情模块

### 已实现接口

```text
GET /students/detail/{studentNo}
```

### 返回结构

`StudentDetailVO` 当前由以下部分组成：

- `basic_info`：学生基础信息。
- `academic_performance`：GPA、成绩等级、每周学习时间、缺勤次数。
- `support_status`：是否参加辅导、家长支持程度。
- `activity_profile`：课外活动、体育、音乐、志愿活动。
- `performance_available`：是否存在 `student_performance` 记录。

### 数据来源

详情查询通过 `StudentMapper.selectStudentDetailAggregateRow()` 一次性聚合：

- `student` 表基础信息。
- `student_performance` 表学习表现。
- `performance_available` 标记是否存在表现记录。

详情内部使用 `StudentDetailAggregateRow`，再转换为 `StudentDetailVO`。转换时复用已有枚举和 VO：

- `GenderEnum`
- `GradeClassEnum`
- `ParentalSupportEnum`
- `YesNoEnum`
- `ClassInfoVO`
- `OptionVO<T>`

## 分析统计模块

### 已实现接口

Controller 基础路径：

```text
/analytics
```

| 方法 | 路径 | 功能 |
| --- | --- | --- |
| `GET` | `/analytics/gpa-distribution` | GPA 区间人数和占比 |
| `GET` | `/analytics/grade-class-distribution` | 成绩等级人数和占比 |
| `GET` | `/analytics/performance-points` | 学习表现散点数据 |

三类接口都支持相同范围参数：

| 参数 | 含义 |
| --- | --- |
| `grade_level` | 年级，`1-3`，可不传 |
| `class_name` | 班级列表，可重复传参，也兼容逗号分隔 |

范围校验规则：

- `grade_level` 只能为 `1`、`2`、`3`。
- 如果传了 `class_name`，必须同时传 `grade_level`。
- `class_name` 必须匹配 `[1-3]-数字`。
- 班级所属年级必须与 `grade_level` 一致。

### GPA 区间统计

`GET /analytics/gpa-distribution` 返回 `GpaDistributionItemVO`：

- `bucketIndex`
- `label`
- `minGpa`
- `maxGpa`
- `studentCount`
- `percentage`

GPA 区间定义集中在 `GpaBucketEnum`：

| code | label |
| --- | --- |
| `0` | `[0.0, 0.5)` |
| `1` | `[0.5, 1.0)` |
| `2` | `[1.0, 1.5)` |
| `3` | `[1.5, 2.0)` |
| `4` | `[2.0, 2.5)` |
| `5` | `[2.5, 3.0)` |
| `6` | `[3.0, 3.5)` |
| `7` | `[3.5, 4.0]` |

百分比用 `BigDecimal` 计算，保留 2 位小数，`RoundingMode.HALF_UP`。

### 成绩等级统计

`GET /analytics/grade-class-distribution` 返回 `GradeClassDistributionItemVO`：

- `gradeClass`：`OptionVO<Integer>`
- `studentCount`
- `percentage`

等级标签来自 `GradeClassEnum`，并按 code 升序返回。

### 学习表现散点数据

`GET /analytics/performance-points` 返回 `PerformanceAnalysisPointVO`：

- `studentNo`
- `name`
- `classInfo`
- `studyTimeWeekly`
- `absences`
- `gpa`
- `gradeClass`
- `gpaBucket`

该接口可支撑多种图表：

- 缺勤次数与 GPA。
- 每周学习时间与 GPA。
- 学习时间、缺勤次数、GPA 三变量联合散点图。
- 按 GPA 连续色带、GPA 分桶或成绩等级着色。

## 数据库设计现状

当前主脚本为 `src/main/resources/sql/V0.sql`，已定义以下表：

| 表 | 当前状态 |
| --- | --- |
| `sys_user` | 已被用户认证和管理员用户管理模块使用 |
| `dict_item` | 字典表已建表，当前 Java 侧主要使用枚举类做翻译 |
| `student` | 已被学生新增、概览、详情、分析模块使用 |
| `student_performance` | 已被学业表现 upsert、学生概览、详情、分析模块使用 |
| `model_version` | 表结构已准备，当前未开放 Java 接口 |
| `prediction_result` | 表结构已准备，当前未开放 Java 接口 |
| `warning_record` | 表结构已准备，当前未开放 Java 接口 |
| `import_batch` | 表结构已准备，当前未开放 Java 接口 |
| `operation_log` | 表结构已准备，当前未开放 Java 接口 |

关键字段设计：

- `student.grade_level`：年级，`1=高一`、`2=高二`、`3=高三`。
- `student.class_name`：数据库原始班级，例如 `1-3`。
- `student.deleted`：软删除标记。
- `student.age`：数据库约束为 `0-30`，后端新增和修改都按该范围校验。
- `student.ethnicity`：取值 `0-3`，新增学生时必填。
- `student.parental_education`：取值 `0-4`，新增学生时必填。
- `student_performance.study_time_weekly`：`DECIMAL(7,4)`，Java 使用 `BigDecimal`。
- `student_performance.gpa`：`DECIMAL(5,4)`，Java 使用 `BigDecimal`。
- `student_performance.grade_class`：由 GPA 派生，取值 `0-4`。
- `student_performance.data_source`：新增或人工更新表现记录时写入 `MANUAL`。
- `student_performance.data_quality_status` 和 `quality_issue`：用于记录导入数据质量。

## 枚举与翻译

当前后端通过 VO 枚举集中处理前端展示翻译：

- `GenderEnum`：性别。
- `GradeClassEnum`：成绩等级。
- `ParentalSupportEnum`：家长支持程度。
- `YesNoEnum`：`0/1` 转布尔或选项。
- `GpaBucketEnum`：GPA 分桶。
- `ClassInfoVO`：班级原始值和中文展示值转换。
- `OptionVO<T>`：统一承载 `{ value, label }`。

## Mapper 与查询结构

### StudentMapper

当前覆盖：

- 学生概览行查询。
- 按班级默认查询。
- 按关键词搜索。
- 多条件筛选。
- 班级筛选项查询。
- GPA 最大/最小值查询。
- 学生详情聚合查询。
- 学生概览字段更新。
- 学生成绩字段更新。
- 学生软删除。
- 学生新增依赖 MyBatis-Plus `BaseMapper<Student>.insert()`。
- 学生存在性和唯一性检查依赖 MyBatis-Plus 条件查询。

详情查询使用 `resultMap + association` 聚合 `StudentOverviewRow` 和 `StudentDetailRow`，并遵守 MyBatis DTD 顺序：`result` 在 `association` 前。

### PerformanceMapper

新增 `PerformanceMapper extends BaseMapper<Performance>`，用于学业表现 upsert：

- 通过 `student_id` 查询是否已有表现记录。
- 没有记录时插入 `student_performance`。
- 已有记录时按主键更新 `student_performance`。

### SysUserMapper

当前覆盖：

- 按启用状态用户名查询登录用户。
- 按用户名查询任意状态用户，用于注册和管理员新增用户查重。
- 管理员用户列表分页查询。
- 管理员用户总数查询。
- 其它单条查询、插入、更新依赖 MyBatis-Plus `BaseMapper<SysUser>`。

用户分页查询目前使用显式 SQL：

```sql
SELECT id, username, password, real_name AS realName, role, status,
       created_at AS createdAt, updated_at AS updatedAt
FROM sys_user
ORDER BY created_at DESC, id ASC
LIMIT #{pageSize} OFFSET #{offset}
```

### AnalysisMapper

当前覆盖：

- `selectGpaBucketCounts`
- `selectGradeClassCounts`
- `selectPerformanceAnalysisPoints`

三类查询共用 `AnalysisScopeCondition`：

- 按 `grade_level` 过滤。
- 按多个 `class_name` 过滤。

## 当前未完成或仅准备的部分

以下内容在数据库层或结构层已有准备，但当前后端接口尚未真正接入：

- 模型版本管理。
- 成绩等级预测结果写入和查询。
- 学业风险预警。
- 导入批次管理接口。
- 操作日志记录和查询接口。
- 数据字典接口；当前字典翻译主要由 Java 枚举承担。

本次已为暂不实现的模块保留空 Controller 和 TODO 注释：

| Controller | 基础路径 | 当前状态 |
| --- | --- | --- |
| `OperationLogController` | `/admin/operation-logs` | 仅 TODO，后续实现操作日志分页查询 |
| `ModelController` | `/models` | 仅 TODO，后续实现模型训练和模型版本管理 |
| `PredictionController` | `/predictions` | 仅 TODO，后续实现单学生预测 |
| `WarningController` | `/warnings` | 仅 TODO，后续实现风险预警查看和处理 |

这些 Controller 均已标注 `@RequireRole({"ADMIN"})`，但暂时没有具体接口方法。

## 本次新增文件和调整

### 新增核心文件

- `annotation/RequireRole.java`：角色权限注解。
- `interceptor/RoleInterceptor.java`：读取 `@RequireRole` 并校验当前用户角色。
- `exception/ForbiddenException.java`：角色权限不足异常，统一返回 `403`。
- `controller/StudentAdminController.java`：只挂 `/students`，提供学生新增和学业表现 upsert。
- `controller/AdminUserController.java`：管理员用户管理接口。
- `mapper/PerformanceMapper.java`：学业表现表 MyBatis-Plus Mapper。
- `vo/PageResultVO.java`：通用分页返回结构。
- `vo/AdminUserVO.java`：管理员用户列表和详情响应 VO。

### 新增请求 DTO

- `StudentCreateRequest`
- `StudentPerformanceUpsertRequest`
- `UserCreateRequest`
- `UserUpdateRequest`
- `UserStatusUpdateRequest`
- `UserPasswordResetRequest`

### 调整文件

- `WebConfig`：注册 `RoleInterceptor`。
- `AuthInterceptor`：解析 token 后按 `userId` 查库，确保禁用用户无法继续使用旧 token。
- `GlobalExceptionHandler`：新增 `ForbiddenException` 处理。
- `SysUser`：补充 `createdAt`、`updatedAt` 字段，供管理员用户列表返回。
- `SysUserMapper`：补充用户查重和分页 SQL。
- `SysUserService` / `SysUserServiceImpl`：补充管理员用户管理业务。
- `StudentService` / `StudentServiceImpl`：补充学生新增和学业表现 upsert。
- `StudentController`：已有修改和删除接口加 `@RequireRole({"ADMIN"})`。

## 最近后端提交

最近一次后端提交：

```text
0cc8c35 完善分析接口范围筛选
```

主要内容：

- 分析接口支持 `grade_level` 和 `class_name` 范围筛选。
- 新增 `AnalysisScopeQueryRequest`。
- `AnalysisMapper.xml` 新增分析范围 SQL 条件。
- `PerformanceAnalysisPointVO` 增加 `classInfo`。
- 学习表现点返回 `gradeClass` 和 `gpaBucket`。

## 验证记录

本次新增接口和文档更新前，后端已通过：

```bash
mvn -q -DskipTests compile
xmllint --noout src/main/resources/mapper/AnalysisMapper.xml
```

本次新增学生 CRUD 补齐、学业表现 upsert、权限控制、管理员用户管理和本文档更新后，已通过：

```bash
mvn -q -DskipTests compile
xmllint --noout src/main/resources/mapper/StudentMapper.xml src/main/resources/mapper/AnalysisMapper.xml
git diff --check -- src/main/java src/main/resources
```

注意：当前仓库 `.gitignore` 忽略 `doc/*`，如果后续需要把本日志提交到 Git，需要使用：

```bash
git add -f doc/backend-log.md
```
