# 数据库设计

## 成员二申请配置表（2026-07-20 已提交 migration）

成员二的数据库基线位于 `database/migrations/V20260720_001__create_member2_application_tables.sql`，覆盖：

```text
fee_item, fee_amount_option, gift_item, batch_gift_item,
college_gift_quota, grade_gift_quota, application, arrears_application,
gift_application, gift_application_item, subsidy_application,
college_subsidy_quota, grade_subsidy_quota, application_attachment,
application_operation_record, student_recommendation
```

`application` 使用 `batch_type`、`green_channel_batch_id`、`subsidy_batch_id` 表达批次，并由 `CHECK` 限制两类批次列有且仅有一个非空；生成列 `batch_id` 仅用于数据库唯一约束，避免 MySQL 多列唯一索引对 `NULL` 的重复放行。有效申请唯一约束为 `(student_id, batch_id, application_type, deleted)`。状态更新必须同时匹配 `id`、期望 `status`、期望 `version` 和 `deleted = 0`。

资源表的库存、名额与金额字段均有非负/上下界 `CHECK` 约束；实际预占、确认、释放须使用条件原子更新，并在 `application_operation_record` 以 `request_id` 记录幂等结果。


## arrears_confirmation（成员四）

用途：保存学校对已进入“欠费待确认”状态的绿色通道申请作出的最终确认记录。

表负责人：成员四（`feature/confirmation-statistics`）。

依赖关系：本表以 `application_id` 关联成员二维护的 `application` 主表；为了不跨模块修改表结构，当前不建立外键。成员四只能读取申请信息，申请状态必须由成员三的状态流转 Service 修改。

| 字段 | 类型 | 可空 | 说明 |
|---|---|---:|---|
| `id` | `BIGINT UNSIGNED` | 否 | 主键、自增 |
| `application_id` | `BIGINT UNSIGNED` | 否 | 关联申请主表 ID |
| `voucher_no` | `VARCHAR(32)` | 否 | 唯一欠费单据编号；格式为 `GC + 确认年份 + 申请 ID 六位补零`，例：`GC2026000001` |
| `applied_amount` | `DECIMAL(12,2)` | 否 | 学生申报金额快照；确认时从成员二提供的读取 Service 获取 |
| `confirmed_amount` | `DECIMAL(12,2)` | 否 | 学校最终确认的实际欠费金额 |
| `confirm_user_id` | `BIGINT UNSIGNED` | 否 | 确认学校管理员的用户 ID |
| `request_id` | `VARCHAR(64)` | 否 | 确认写操作的幂等请求号 |
| `confirmed_at` | `DATETIME` | 否 | 最终确认时间 |
| `created_at` | `DATETIME` | 否 | 创建时间 |
| `updated_at` | `DATETIME` | 否 | 更新时间 |
| `deleted` | `BIGINT UNSIGNED` | 否 | 逻辑删除标记，0 表示有效；删除时写入本行 `id` |

约束和索引：

- `uk_arrears_confirmation_application_id_deleted(application_id, deleted)`：同一申请只能存在一条有效确认记录，作为重复确认的数据库最终防线；
- `uk_arrears_confirmation_voucher_no(voucher_no)`：单据编号唯一；
- `uk_arrears_confirmation_request_id(request_id)`：同一确认请求只允许写入一次；
- `idx_arrears_confirmation_confirm_user_id(confirm_user_id)`：按确认人查询或审计时使用。

业务规则：

- 实际确认金额必须大于 `0.00`，且不得超过学生申报金额；
- 保存确认记录后，必须由成员三状态流转 Service 将申请从 `CONFIRM_PENDING` 流转为 `COMPLETED`；
- 若状态流转失败，确认记录必须随事务回滚；
- 欠费单据的查询、学生查看、打印属于 6.1.2，后续独立实现，不在本表增加展示状态字段。

> 本次变更只记录成员三拥有的表：`approval_record`、`approval_submission_record`、`system_message`、`message_read_record`。其他成员负责表的 DDL、字段、索引、Entity 和 Mapper 不在本文档中代为定义。

> 审核流程对共享主表 `application` 的字段需求当前为 `PROPOSED`，只登记在 `docs/change-log.md` 和成员三决策文件中；成员二、成员三、成员四确认前，成员三不得修改 `application` 的 DDL、Entity 或 Mapper。

## 1. 成员三表所有权

成员三可以直接维护：

```text
approval_record
approval_submission_record
system_message
message_read_record
```

成员三不能直接写入：

```text
application
用户、学生、学院和批次表
礼包、库存、名额和补助额度表
arrears_confirmation
```

跨模块写入必须调用对应表负责人提供的 Service。

## 2. 对 `application` 的只读依赖

审核模块需要从成员二提供的查询 Service 获取以下状态快照，但不在本文档定义字段类型和物理结构：

```java
public record ApplicationStateSnapshot(
    Long applicationId,
    Long studentId,
    Long batchId,
    ApplicationType applicationType,
    ApplicationStatus status,
    ApprovalLevel currentLevel,
    Integer reviewRound,
    Integer version
) {}
```

成员三负责判断目标状态，成员二负责 `application` 的直接写入。建议的跨模块接口记录在 `docs/member-code-contracts.md`，确认后由成员二实现。

## 3. 审核记录表 `approval_record`

用途：保存逐条审核、退回、拒绝、修改、提交、取消及状态变化历史。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | `BIGINT` | PK | 主键 |
| `application_id` | `BIGINT` | NOT NULL | 申请 ID |
| `review_round` | `INT` | NOT NULL | 操作发生时的审核轮次 |
| `approval_level` | `VARCHAR(32)` | NOT NULL | `STUDENT/COUNSELOR/COLLEGE/SCHOOL/CONFIRMATION/SYSTEM` |
| `approver_id` | `BIGINT` | NULL | 操作人 ID；系统动作可为空 |
| `approver_name_snapshot` | `VARCHAR(100)` | NULL | 操作人姓名快照 |
| `action` | `VARCHAR(32)` | NOT NULL | `APPROVE/RETURN/REJECT/MODIFY/SUBMIT/CANCEL` |
| `comment` | `VARCHAR(1000)` | NULL | 审核意见或原因 |
| `old_status` | `VARCHAR(32)` | NOT NULL | 操作前状态 |
| `new_status` | `VARCHAR(32)` | NOT NULL | 操作后状态 |
| `modified_fields` | `JSON` | NULL | `MODIFY` 动作的修改摘要 |
| `request_id` | `VARCHAR(64)` | NOT NULL | 幂等请求号 |
| `create_time` | `DATETIME` | NOT NULL | 操作时间 |

成员三负责的约束和索引：

```text
uk_approval_record_request(request_id) UNIQUE
idx_approval_record_application(application_id, create_time, id)
idx_approval_record_level(approval_level, approver_id, create_time)
idx_approval_record_round(application_id, review_round, approval_level, action)
```

规则：

- 审核记录只能新增，不能提供更新和删除接口；
- 逐条 `APPROVE` 未推进状态时允许 `old_status = new_status`；
- 当前层级审核结论只使用当前 `review_round` 的记录；
- `modified_fields` 不保存身份证号、完整手机号或附件内容。

## 4. 审核上报记录表 `approval_submission_record`

用途：记录辅导员、学院首次批量上报以及退回后的单条补交。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | `BIGINT` | PK | 主键 |
| `batch_id` | `BIGINT` | NOT NULL | 批次 ID |
| `submission_level` | `VARCHAR(32)` | NOT NULL | `COUNSELOR/COLLEGE` |
| `submission_type` | `VARCHAR(32)` | NOT NULL | `INITIAL_BATCH/RETURN_RESUBMIT` |
| `scope_type` | `VARCHAR(32)` | NOT NULL | `COUNSELOR/COLLEGE` |
| `scope_id` | `BIGINT` | NOT NULL | 辅导员用户 ID 或学院 ID |
| `application_id` | `BIGINT` | NOT NULL DEFAULT 0 | 首次上报为 0，退回补交为申请 ID |
| `review_round` | `INT` | NOT NULL DEFAULT 0 | 首次上报为 0，补交为当前审核轮次 |
| `submitter_id` | `BIGINT` | NOT NULL | 上报人 ID |
| `submitted_count` | `INT` | NOT NULL DEFAULT 0 | 成功推进数量 |
| `status` | `VARCHAR(32)` | NOT NULL | 第一阶段使用 `SUBMITTED` |
| `request_id` | `VARCHAR(64)` | NOT NULL | 幂等请求号 |
| `submit_time` | `DATETIME` | NOT NULL | 上报时间 |
| `create_time` | `DATETIME` | NOT NULL | 创建时间 |

成员三负责的约束和索引：

```text
uk_submission_request(request_id) UNIQUE
uk_submission_scope(batch_id, submission_level, scope_id, submission_type, application_id, review_round) UNIQUE
idx_submission_batch(batch_id, submission_level, submit_time)
```

规则：

- `INITIAL_BATCH` 的 `application_id` 和 `review_round` 固定为 0；
- `RETURN_RESUBMIT` 使用真实申请 ID 和当前审核轮次；
- 同一范围只能首次上报一次，同一申请在同一轮次只能补交一次。

## 5. 系统消息表 `system_message`

用途：保存退回、拒绝、通过、截止时间和线下办理等审核消息。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | `BIGINT` | PK | 主键 |
| `receiver_user_id` | `BIGINT` | NOT NULL | 接收用户 ID |
| `message_type` | `VARCHAR(32)` | NOT NULL | 消息类型 |
| `business_type` | `VARCHAR(32)` | NOT NULL | `APPLICATION/BATCH` |
| `business_id` | `BIGINT` | NOT NULL | 申请 ID 或批次 ID |
| `title` | `VARCHAR(200)` | NOT NULL | 标题 |
| `content` | `VARCHAR(2000)` | NOT NULL | 内容 |
| `create_by` | `BIGINT` | NULL | 创建人；系统消息可为空 |
| `create_time` | `DATETIME` | NOT NULL | 创建时间 |

成员三负责的索引：

```text
idx_system_message_receiver(receiver_user_id, create_time)
idx_system_message_business(business_type, business_id)
```

## 6. 消息已读记录表 `message_read_record`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | `BIGINT` | PK | 主键 |
| `message_id` | `BIGINT` | NOT NULL | 消息 ID |
| `user_id` | `BIGINT` | NOT NULL | 阅读人 ID |
| `read_time` | `DATETIME` | NOT NULL | 阅读时间 |
| `create_time` | `DATETIME` | NOT NULL | 创建时间 |

成员三负责的约束和索引：

```text
uk_message_read(message_id, user_id) UNIQUE
idx_message_read_user(user_id, read_time)
```

重复标记已读时，根据唯一约束返回第一次处理结果，不重复插入。

## 7. 成员三事务边界

成员三负责状态机判断和事务编排，但所有跨表写入通过负责人 Service 完成：

```text
校验当前用户、数据范围、旧状态和版本
-> 调用成员二 Service 更新 application 或资源
-> 成员三写 approval_record / approval_submission_record
-> 成员三写 system_message
-> 任一步失败则整个 Spring 事务回滚
```

取消申请时，成员三还需要调用成员四提供的单据检查和作废 Service。成员三不得直接修改 `arrears_confirmation`。

## 8. V2 全表总览（待四位成员确认）

本节只补充全项目的表范围、关键字段和关系，不替代前述成员三表的详细字段定义，也不授权任何成员修改其他成员的表。

| 负责人 | 表 | 关键字段/数据格式 | 必须满足的关系或约束 |
|---|---|---|---|
| 成员一 | sys_user、sys_role、sys_user_role | 用户名、密码哈希、角色编码；ID 为 BIGINT | 用户名有效数据唯一；角色固定为 STUDENT/COUNSELOR/COLLEGE/SCHOOL |
| 成员一 | college、major、grade、class_info | 编码、名称、enabled TINYINT | 学院→专业→班级；学生引用的组织数据必须一致 |
| 成员一 | student、counselor_student | 学号、姓名、学院/专业/年级/班级 ID、贷款情况 | 学号有效数据唯一；辅导员只可处理其关联学生 |
| 成员一 | green_channel_batch、batch_eligible_grade | 批次编号、开始/结束/学院上报截止时间、状态 | 批次与适用年级多对多；时间顺序合法 |
| 成员一 | subsidy_batch、subsidy_batch_eligible_grade | 批次编号、开始/结束时间、状态 | 补助批次与适用年级多对多 |
| 成员二 | fee_item、fee_amount_option | 欠费项目、可选金额 DECIMAL(12,2) | 金额大于 0；选项归属一个欠费项目 |
| 成员二 | gift_item、batch_gift_item、college_gift_quota、grade_gift_quota | 礼包物品、库存、名额、reserved_count、used_count、version | 0 ≤ used_count ≤ reserved_count ≤ 总量；原子扣减 |
| 成员二 | application | student_id、application_type、source、batch_type、批次 ID、status、current_level、review_round、version | 唯一申请主表；同学生同批次同类型仅一条有效记录 |
| 成员二 | arrears_application、gift_application、gift_application_item | application_id、申报金额/物品数量 | 仅归属 GREEN_CHANNEL；欠费和礼包均为可选明细 |
| 成员二 | subsidy_application、college_subsidy_quota、grade_subsidy_quota | application_id、申请/核定金额、额度金额 | 仅归属 LIVING_SUBSIDY/TRAVEL_SUBSIDY；金额使用 DECIMAL(12,2) |
| 成员二 | application_attachment、student_recommendation | application_id、受控 file_id；推荐快照 JSON | 不保存可猜测公开路径；敏感字段不写 JSON |
| 成员三 | approval_record、approval_submission_record | application_id、审核轮次、动作、旧/新状态、request_id | 不可变流水；request_id 唯一，避免重复审核/上报 |
| 成员三 | system_message、message_read_record | 接收用户、业务类型/ID、已读时间 | 消息已读以 (message_id,user_id) 唯一 |
| 成员四 | arrears_confirmation | application_id、单据号、申报/确认金额、确认人/时间、deleted | 一条有效申请仅一条有效确认；确认金额大于 0 且不高于申报金额 |

### 8.1 两类批次的字段约定

green_channel_batch 与 subsidy_batch 是两张不同表。为避免后续混淆，申请和上报记录统一采用：

- batch_type：GREEN_CHANNEL 或 SUBSIDY；
- green_channel_batch_id：绿色通道批次时非空；
- subsidy_batch_id：补助批次时非空；
- 接口层可统一返回 batchId，但写入时必须同时说明 batchType。

数据库 CHECK 约束保证任一记录只关联其中一类批次。这样保留真实外键，也避免同一个 batch_id 指到不同表的冲突。

### 8.2 统一字段和实现边界

- 新表默认遵循 database-format-standard.md：BIGINT、DECIMAL(12,2)、DATETIME、VARCHAR(32)、create_time/update_time/deleted。
- application 仍由成员二维护；成员三只通过其 Service 修改状态；成员四只通过成员三 Service 完成 CONFIRM_PENDING 到 COMPLETED。
- 本节没有新增表。若需要新增表、字段或索引，必须先在 change-log.md 创建 PROPOSED 记录，再由表负责人实现。

## 9. 角色字典表 `sys_role`（成员一）

用途：存储系统角色的字典数据，角色固定为 4 种，基本不增删改。

表负责人：成员一（`feature/base-user-batch`）。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | `BIGINT` | PK | 主键、自增 |
| `role_code` | `VARCHAR(32)` | NOT NULL | 角色编码，固定值：`STUDENT`/`COUNSELOR`/`COLLEGE`/`SCHOOL` |
| `role_name` | `VARCHAR(32)` | NOT NULL | 角色中文名，如"学生""辅导员""学院管理员""学校管理员" |
| `create_time` | `DATETIME` | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |

成员一负责的约束和索引：

```text
uk_role_code(role_code) UNIQUE
```

初始数据：

```sql
INSERT INTO `sys_role` (`role_code`, `role_name`) VALUES
    ('STUDENT',   '学生'),
    ('COUNSELOR', '辅导员'),
    ('COLLEGE',   '学院管理员'),
    ('SCHOOL',    '学校管理员');
```

规则：

- 角色为全系统权限模型的基础，其他模块从 JWT Token 中获取角色编码做权限判断；
- 角色编码为固定枚举值，不在前端或接口中动态创建；
- 成员一通过 `CurrentUserProvider` 对外暴露当前用户的角色列表。

## 10. 用户-角色关联表 `sys_user_role`（成员一）

用途：实现用户与角色的多对多关联。一个用户可以同时拥有多个角色（如某学生兼任辅导员）。

表负责人：成员一（`feature/base-user-batch`）。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | `BIGINT` | PK | 主键、自增 |
| `user_id` | `BIGINT` | NOT NULL | 用户 ID，关联 `sys_user.id` |
| `role_id` | `BIGINT` | NOT NULL | 角色 ID，关联 `sys_role.id` |
| `create_time` | `DATETIME` | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |

成员一负责的约束和索引：

```text
uk_user_role(user_id, role_id) UNIQUE
idx_user_role_user(user_id)
idx_user_role_role(role_id)
```

规则：

- 同一用户不可重复绑定同一角色；
- 登录时查询用户角色列表，写入 JWT Token 的 `roles` 字段（逗号分隔）；
- 成员一提供 `selectRoleCodesByUserId(Long userId)` 查询方法供 Controller 使用；
- 其他模块通过 JWT Token 获取角色，不直接查此表。

建表 SQL：

```sql
-- 角色字典表
CREATE TABLE `sys_role` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `role_code`   VARCHAR(32)  NOT NULL COMMENT '角色编码：STUDENT/COUNSELOR/COLLEGE/SCHOOL',
    `role_name`   VARCHAR(32)  NOT NULL COMMENT '角色中文名',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户-角色关联表
CREATE TABLE `sys_user_role` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID，关联sys_user.id',
    `role_id`     BIGINT   NOT NULL COMMENT '角色ID，关联sys_role.id',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_role_user` (`user_id`),
    KEY `idx_user_role_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 初始角色数据
INSERT INTO `sys_role` (`role_code`, `role_name`) VALUES
    ('STUDENT',   '学生'),
    ('COUNSELOR', '辅导员'),
    ('COLLEGE',   '学院管理员'),
    ('SCHOOL',    '学校管理员');
```
