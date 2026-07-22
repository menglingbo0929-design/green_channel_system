# 基础用户与批次模块设计决策

本文件由成员一维护。涉及共享表、公共接口或公共状态的决定，还必须同步更新对应公共文档和 `docs/change-log.md`。

## JWT 认证与会话模型

- 日期：2026-07-17
- 负责人：成员一
- 涉及表：`sys_user`、`sys_role`、`sys_user_role`
- 涉及接口：`POST /api/user/login`、所有需要认证的接口
- 涉及文件：`backend/.../security/JwtTokenProvider.java`、`JwtAuthenticationFilter.java`、`CurrentUserProvider.java`
- 具体规定：
  1. 登录成功后签发 JWT Token，载荷包含 `userId`、`loginName`、`roles`（逗号分隔）、`studentId`、`collegeId`。
  2. `JwtAuthenticationFilter` 从 `Authorization: Bearer <token>` 提取并校验 Token，将用户信息写入 `SecurityContext`。
  3. `CurrentUserProvider` 从 `SecurityContext` 读取当前用户，返回 `LoginUser` 记录（userId, loginName, roles, studentId, collegeId）。
  4. 后端所有权限判断必须从 `CurrentUserProvider` 获取身份，不接受前端传入的用户 ID、角色或学院 ID。
  5. Token 过期时间默认 24 小时，密钥通过 `application.yml` 的 `jwt.secret` 配置。
- 做出原因：统一身份来源，避免前端篡改身份字段；JWT 无状态设计兼容水平扩展。
- 对其他模块的影响：成员二/三/四的 Controller 注入 `CurrentUserProvider` 获取当前用户，不从请求参数取身份。
- 是否需要其他成员确认：否，已实现。

## 用户-角色多对多模型

- 日期：2026-07-17
- 负责人：成员一
- 涉及表：`sys_user`、`sys_role`、`sys_user_role`
- 涉及接口：用户管理 CRUD、登录
- 涉及文件：`database/02_create_tables.sql`、`database/03_create_database.sql`
- 具体规定：
  1. 系统角色固定为 4 种：`STUDENT`（学生）、`COUNSELOR`（辅导员）、`COLLEGE`（学院管理员）、`SCHOOL`（学校管理员）。
  2. 用户与角色通过 `sys_user_role` 多对多关联，一个用户可同时拥有多个角色。
  3. 角色编码为固定枚举，不在前端或接口中动态创建/删除。
  4. 登录时查询用户角色列表写入 JWT，前端菜单和后端权限按角色判断。
- 做出原因：支持学生兼任辅导员等灵活场景；固定角色简化权限模型。
- 对其他模块的影响：其他模块从 JWT 的 `roles` 字段判断权限，不直接查 `sys_user_role` 表。
- 是否需要其他成员确认：否，已实现。

## 组织架构层级模型

- 日期：2026-07-17
- 负责人：成员一
- 涉及表：`college`、`major`、`grade`、`class_info`
- 涉及接口：`OrganizationController` CRUD
- 涉及文件：`database/02_create_tables.sql`、`database/03_create_database.sql`
- 具体规定：
  1. 四张表形成学院 → 专业 → 班级的树形层级，年级独立存在。
  2. `major.college_id` 关联学院，`class_info.major_id` + `class_info.grade_id` + `class_info.college_id`（冗余加速查询）。
  3. 每张表使用 `college_code`/`major_code`/`grade_code`/`class_code` 作为业务唯一标识。
  4. 启用/停用通过 `enabled` 字段控制，不影响历史数据引用。
- 做出原因：高校资助系统标准组织模型；冗余 `college_id` 避免多表 JOIN。
- 对其他模块的影响：成员二通过 `OrganizationQueryService` 获取下拉选项；成员四统计按学院/年级聚合。
- 是否需要其他成员确认：否，已实现。

## 学生信息模型

- 日期：2026-07-17
- 负责人：成员一
- 涉及表：`student`
- 涉及接口：`StudentController` CRUD、`StudentProfileQueryService`
- 涉及文件：`database/02_create_tables.sql`、`backend/.../model/domain/Student.java`
- 具体规定：
  1. 学生通过 `student_no`（学号）唯一标识。
  2. 学生必须关联 `college_id`、`major_id`、`grade_id`、`class_id` 四个组织维度。
  3. `user_id` 关联 `sys_user`，导入学生时自动创建登录账号并绑定 `STUDENT` 角色。
  4. `counselor_id` 关联辅导员用户，配合 `counselor_student` 表实现辅导员-学生多对多管理。
  5. `origin_loan`（生源地贷款）、`campus_loan`（校园地贷款）、`subsidy_level`、`difficulty_level` 记录资助相关信息。
  6. `info_complete` 标记学生是否完善了个人信息。
- 做出原因：学生是申请流程的主体，完整的基础信息是成员二申请校验和成员三数据范围判断的前提。
- 对其他模块的影响：成员二通过 `StudentProfileQueryService` 获取学生信息写入申请；成员三通过 `StudentScopeService` 校验辅导员/学院数据范围。
- 是否需要其他成员确认：字段已确认，`StudentProfileQueryService` 实现待完成。

## 辅导员-学生关联模型

- 日期：2026-07-20
- 负责人：成员一
- 涉及表：`counselor_student`
- 涉及接口：辅导员分配管理（待实现）
- 涉及文件：`database/02_create_tables.sql`
- 具体规定：
  1. 辅导员与学生的多对多关系通过 `counselor_student` 表维护。
  2. `student` 表的 `counselor_id` 字段作为主要辅导员冗余，配合关联表支持多辅导员场景。
  3. 辅导员只能查看和审核自己负责学生的申请，数据范围由 `StudentScopeService.isCounselorResponsibleFor()` 校验。
- 做出原因：满足辅导员分级管理需求，支持批量分配和调换。
- 对其他模块的影响：成员三审核时必须调用 `StudentScopeService` 校验辅导员数据范围。
- 是否需要其他成员确认：待 `StudentScopeService` 实现后通知成员三对接。

## 批次管理模型

- 日期：2026-07-20
- 负责人：成员一
- 涉及表：`green_channel_batch`、`batch_eligible_grade`、`subsidy_batch`、`subsidy_batch_eligible_grade`
- 涉及接口：批次管理 CRUD（待实现）、`BatchQueryService`
- 涉及文件：`database/02_create_tables.sql`、`database/03_create_database.sql`
- 具体规定：
  1. 绿色通道和补助使用不同的批次表，通过 `application.batch_type` 区分。
  2. `green_channel_batch` 包含 `start_time`（申请开始）、`end_time`（申请截止）、`college_deadline`（学院上报截止）。
  3. `subsidy_batch` 通过 `batch_type`（`LIVING_SUBSIDY`/`TRAVEL_SUBSIDY`）区分补助类型。
  4. 批次通过 `batch_eligible_grade` / `subsidy_batch_eligible_grade` 多对多关联适用年级。
  5. 批次状态使用 `DRAFT`（草稿）/ `OPEN`（开放）/ `CLOSED`（已关闭）生命周期。
- 做出原因：批次是申请的时间范围和适用人群基础，成员二/三/四都需要批次信息做校验和筛选。
- 对其他模块的影响：成员二的申请创建需校验批次状态和年级资格；成员三的上报需校验截止时间；成员四的统计按批次筛选。
- 是否需要其他成员确认：字段和状态枚举需成员二/三/四确认后实现 `BatchQueryService`。

## 密码安全策略

- 日期：2026-07-17
- 负责人：成员一
- 涉及表：`sys_user`
- 涉及接口：登录、修改密码、创建用户
- 涉及文件：`backend/.../config/SecurityConfig.java`、`PasswordMigrationRunner.java`
- 具体规定：
  1. 密码使用 BCrypt 加密存储，`SecurityConfig` 注入 `BCryptPasswordEncoder`。
  2. 修改密码时校验旧密码、确认新密码一致性，加密后更新。
  3. `PasswordMigrationRunner` 在启动时检查是否存在明文密码记录并自动升级为 BCrypt 哈希。
  4. 演示账号使用固定 BCrypt 哈希（密码明文在 `03_create_database.sql` 中不暴露）。
- 做出原因：满足安全审计要求，同时兼容历史明文密码平滑迁移。
- 对其他模块的影响：无，密码验证由成员一独立完成。
- 是否需要其他成员确认：否。

## 逻辑删除约定

- 日期：2026-07-17
- 负责人：成员一
- 涉及表：所有成员一负责的表
- 涉及接口：全部查询/写入接口
- 涉及文件：`database/02_create_tables.sql`
- 具体规定：
  1. 成员一所有表使用 `deleted BIGINT DEFAULT 0` 作为逻辑删除标记。
  2. 删除时写入当前行 `id` 值，查询时附加 `deleted = 0` 条件。
  3. 唯一索引不包含 `deleted` 字段，业务层保证有效数据的唯一性。
- 做出原因：与系统其他模块（成员二/三/四）的 `deleted` 字段约定一致；不使用 BOOL 以便扩展（如记录删除操作人 ID）。
- 对其他模块的影响：其他模块查询成员一的表时需附带 `deleted = 0` 条件。
- 是否需要其他成员确认：否，已与成员二/三/四对齐。

## 跨模块服务接口约定

- 日期：2026-07-20
- 负责人：成员一
- 涉及表：全部成员一负责的表
- 涉及接口：`StudentProfileQueryService`、`StudentScopeService`、`BatchQueryService`、`OrganizationQueryService`、`PolicyRuleQueryService`
- 涉及文件：`backend/.../service/*.java`、`docs/requirement.md`
- 具体规定：
  1. `CurrentUserProvider`：已实现，从 SecurityContext 返回 LoginUser。
  2. `StudentProfileQueryService`：待实现，返回学生基本信息和贷款情况，供成员二写入申请快照。
  3. `StudentScopeService`：待实现，校验学生是否属于当前用户的数据范围，供成员二/三权限判断。
  4. `BatchQueryService`：待实现，查询开放批次、校验年级资格，供成员二/三/四使用。
  5. `OrganizationQueryService`：待实现，返回学院/年级下拉选项，供成员二表单使用。
  6. `PolicyRuleQueryService`：待实现，返回当前批次适用政策规则，供成员二推荐提示。
  7. 所有接口只返回 `deleted = 0` 且 `enabled = 1` 的有效数据。
  8. 接口不暴露数据库主键作为唯一业务标识（如 class_info 对外使用 class_code）。
- 做出原因：满足 `requirement.md` 第 4 节成员一对其他模块的能力承诺。
- 对其他模块的影响：成员二/三/四通过注入这些 Service 获取基础数据，不直接查成员一的 Mapper。
- 是否需要其他成员确认：接口签名需成员二/三确认，实现后更新 `docs/api-document.md`。
