# 成员二跨成员对接清单

更新日期：2026-07-21  
适用范围：申请配置、学生申请、学校代申请、线下补录、资源生命周期、欠费确认、统计与报表。

## 1. 成员二当前已提供能力

- 学生申请草稿创建、查询、修改、删除；欠费、礼包、生活补助和路费补助明细维护。
- 欠费项目、金额档位、礼包物品、批次礼包库存/单人上限、学院/年级礼包名额与补助额度配置。
- `SchoolProxyApplicationPort`：学校代申请草稿与欠费/礼包明细物理写入。
- `SupplementApplicationPort`：线下补录创建、欠费/礼包/补助明细写入及自动审核桥接。
- `arrears_application.arrears_reason_code`：已完成前端、DTO、校验、落库和查询贯通。
- H2 集成测试、资源/代申请/补录单元测试；`backend/mvnw.cmd test` 当前共 56 项通过，前端构建通过。

## 2. 成员一对接项

### 2.1 可信身份与数据范围

需要提供可注入的 `CurrentUserProvider` 或等价 JWT 能力，至少返回：

- 当前用户 ID；
- 用户角色（学生、学校管理员等）；
- 学校及组织数据范围。

成员二用途：移除当前仅用于本地联调的 `X-Student-Id`、`X-User-Id` 请求头；校验学生仅操作本人申请、学校管理员仅操作授权范围学生；为代申请、补录、统计和报表提供可信数据范围。

验收：客户端伪造或遗漏身份头时不能越权读取、创建或修改申请。

### 2.2 学校代申请学生查询

需要实现并注入：

```java
SchoolProxyStudentQueryPort.findEnabledStudentByStudentNo(String studentNo)
```

当前学生快照需包含：

- `studentId`、`studentNo`、`studentName`；
- `collegeName`、`majorName`、`gradeName`、`className`；
- **还需补充** `collegeId`、`gradeId`，建议同时提供 `majorId`、`classId`。

成员二需要组织主键来原子预占或确认学院/年级礼包名额和补助额度；仅有组织名称不能安全更新资源表。

验收：真实学号可查询启用学生，停用或不存在学生返回空，且查询受学校数据范围限制。

### 2.3 补录历史和详情的学生快照

当前 Port 仅支持按学号单查，无法将补录申请中的 `studentId` 批量转换为页面所需学生信息。需要新增或扩展：

```java
findEnabledStudentById(Long studentId)
findEnabledStudentsByIds(Collection<Long> studentIds)
```

返回最小脱敏快照：学生 ID、学号、姓名、学院/专业/年级/班级名称及对应 ID、启用状态。

成员二不能直接访问成员一学生、组织表。

验收：补录列表支持分页及学号、申请类型、批次、状态筛选；详情仅返回 `source=SUPPLEMENT` 的记录。

### 2.4 批次、组织与统计能力

需要确认并提供稳定 Spring Bean：

- 批次有效性和批次类型校验；
- 启用批次列表查询，供学生端和配置端下拉使用；
- 学院、年级有效性及批次年级适用范围校验；
- `StatisticsAccessPort` 或等价能力：学校角色校验、可统计数据范围、组织层级隶属关系校验；
- 批量组织名称、批次名称、批次开始时间快照查询。

当前成员二资源配置支持显式输入批次 ID；学生端不能展示真实批次下拉，统计和报表也不能在缺失可信范围时使用模拟数据。

## 3. 成员三对接项

### 3.1 正式提交审核流转

成员三需确认并稳定提供：

```java
ApprovalTransitionService.submitInitial(
    Long applicationId,
    Integer expectedVersion,
    String requestId,
    Long operatorId
)
```

正式提交的事务顺序固定为：

1. 成员二校验附件完整性；
2. 成员二预占礼包库存、学院/年级名额和补助额度；
3. 成员三写 `SUBMIT` 审核记录，并推进到 `COUNSELOR_PENDING`。

需共同确认：`requestId` 幂等、事务传播、预占成功但审核记录写入失败时的整体回滚、退回是否保留预占、拒绝或取消时谁触发资源释放。

当前成员二会拒绝不具备附件与资源预占条件的学校代申请正式提交，错误码为 `SCHOOL_PROXY_SUBMISSION_UNAVAILABLE`，不会产生资源账不一致的半成品状态。

### 3.2 补录自动审核

成员三需确认并提供：

```java
ApprovalTransitionService.completeSupplementReview(
    Long applicationId,
    boolean containsArrears,
    Integer expectedVersion,
    String requestId,
    Long operatorId
)
```

状态约定：

| 补录场景 | 目标状态 | 当前层级 |
|---|---|---|
| 含欠费明细 | `CONFIRM_PENDING` | `CONFIRMATION` |
| 不含欠费明细 | `COMPLETED` | `FINISHED` |

成员二会写申请和真实明细，成员三负责自动通过审核记录与状态流转。三方联调时需确认其与补录资源直接确认处于同一事务。

### 3.3 资源生命周期

成员三审核动作需要与成员二资源模块共同定义和实现：

| 业务动作 | 成员二资源动作 |
|---|---|
| 初次提交、退回重提 | 预占或调整资源 |
| 辅导员确定最终补助金额 | 原子调整补助额度预占差额 |
| 学校审核通过 | 预占转已使用 |
| 拒绝、取消 | 释放预占 |
| 退回 | 按已确认规则保留或释放预占 |

## 4. 成员四对接项

### 4.1 学校代申请页面与 Controller

成员四负责学校业务页面及对外 Controller，通过成员二 Port 调用；不得直接创建成员二 Mapper 或写成员二申请、明细、资源表。

固定接口路径：

```http
GET  /api/school-proxy/students?studentNo={studentNo}
POST /api/school-proxy/applications/drafts
POST /api/school-proxy/applications/{applicationId}/attachments?requestId={requestId}
POST /api/school-proxy/applications/{applicationId}/submit?version={version}&requestId={requestId}
```

当前可联调：学生查询外壳、创建学校代申请草稿、欠费/礼包明细写入。

当前不可成功联调：

- 附件上传：成员二返回 `ATTACHMENT_STORAGE_UNAVAILABLE`（501），等待对象存储、文件类型、大小和访问控制方案；
- 正式提交：成员二返回 `SCHOOL_PROXY_SUBMISSION_UNAVAILABLE`（503），等待附件和资源预占条件满足。

### 4.2 线下补录页面与 Controller

固定接口路径：

```http
GET  /api/supplements/students?studentNo={studentNo}
GET  /api/supplements?pageNo={pageNo}&pageSize={pageSize}
GET  /api/supplements/{applicationId}
POST /api/supplements
```

成员四职责：

- 从成员一取得学校身份和学生快照；
- 调用 `SupplementApplicationPort`，不得直接写申请、明细或资源表；
- 将成员二申请/明细写入与成员三自动审核包在外层事务；
- 正确显示成员二 503 依赖错误，不能显示为补录成功。

当前可联调：补录创建、绿色通道欠费/礼包明细、补助金额与自动审核桥接。补录详情和历史仍等待成员一按 `studentId` 的学生快照能力。

### 4.3 欠费确认、统计和报表

成员二已提供欠费确认待办、详情与最多 100 条申请的欠费项目批量快照查询。成员四应通过成员二 Port 读取 `CONFIRM_PENDING` 申请，再通过成员三完成 `CONFIRM_PENDING -> COMPLETED`，不得直接更新 `application` 或 `arrears_application`。

统计与报表需等待以下条件同时满足后再接入真实页面：

- 成员一提供可信统计范围及组织/批次快照；
- 成员四确认已确认金额的只读访问方式和最小联调数据；
- 成员二完成最终状态集合统计和报表分页查询。

禁止使用模拟数据或逐条读取申请后在内存汇总。

## 5. 数据库、接口和部署共同事项

### 5.1 Migration

部署环境需执行成员二 migration：

```text
database/migrations/V20260720_001__create_member2_application_tables.sql
database/migrations/V20260721_001__add_member2_supplement_and_arrears_reason.sql
```

### 5.2 欠费原因码

`arrears_application.arrears_reason_code` 固定允许：

```text
FAMILY_FINANCIAL_DIFFICULTY
FAMILY_EMERGENCY
MAJOR_ILLNESS
DISASTER_ACCIDENT
OTHER
```

请求省略原因码时成员二默认保存 `OTHER`；传入其他值返回 `ARREARS_REASON_CODE_INVALID`（HTTP 400）。不得以自由文本 `applicationReason` 代替统计用原因码。

### 5.3 幂等与表所有权

- 创建草稿、上传附件、正式提交必须使用不同的 `requestId`；
- 跨模块写操作必须保证同一业务事务的回滚语义；
- 成员二不直接读取成员一学生/组织/批次表；
- 成员二不直接修改成员三审核表、成员四欠费确认表；
- 成员四不直接写成员二申请、明细、附件、资源表。

## 6. 联调验收顺序

1. 成员一提供可信身份、按学号学生快照、学院/年级 ID；
2. 成员二实现并验证资源预占；
3. 成员三确认提交、自动审核和资源生命周期事务；
4. 成员四串联“查学生 → 建草稿 → 写明细 → 上传附件 → 提交”；
5. 联调“补录 → 自动审核 → 含欠费确认 / 不含欠费完成”；
6. 成员一、二、四提供统计最小真实数据后实现统计、报表、Excel 与打印；
7. 使用真实数据完成接口和前端验收后，才将整体功能标记为 `IMPLEMENTED`。

## 7. 相关文档

- `docs/member2-progress.md`
- `docs/member2-catalog-api.md`
- `docs/collaboration-rules.md`
- `docs/decisions/confirmation-statistics.md`
