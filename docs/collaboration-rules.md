# 高校绿色通道系统协作与共享资源操作规范

> 文件位置：`docs/collaboration-rules.md`  
> 本文件是数据库权限、共享文件修改和 Git 同步的强制规范。

---

## 1. 文档权威顺序

执行任何跨模块操作前，按以下顺序读取：

1. `docs/collaboration-rules.md`：谁能改、怎么改、怎么同步；
2. `docs/change-log.md`：最近有哪些共享结构、接口、状态发生变化；
3. `docs/database-design.md`：数据库表、字段、约束、表负责人；
4. `docs/api-document.md`：接口路径、参数、返回值、错误码；
5. `docs/status-flow.md`：申请状态、审核状态和允许的状态变化；
6. `docs/decisions/<模块>.md`：模块负责人做出的特殊设计决定；
7. 对应代码和 SQL。

根目录 `README.md` 只做项目总览，不作为字段、接口或状态的最终依据。

任何共享结构变化，必须在同一个提交中同时修改代码和对应文档；只改代码、不改文档的变化视为无效约定。

---

## 2. 四人的数据库表所有权

### 2.1 成员一：基础用户与批次

分支：

```text
feature/base-user-batch
```

拥有以下表的结构维护权：

```text
sys_user
sys_role
sys_user_role
college
major
grade
class_info
student
counselor_student
green_channel_batch
batch_eligible_grade
subsidy_batch
subsidy_batch_eligible_grade
student_tag
policy_rule
```

其他成员可以查询这些表，但不能直接修改表结构。

---

### 2.2 成员二：申请、礼包、补助额度

分支：

```text
feature/application-config
```

拥有以下表的结构维护权：

```text
fee_item
fee_amount_option
gift_item
batch_gift_item
college_gift_quota
grade_gift_quota
application
arrears_application
gift_application
gift_application_item
application_attachment
application_operation_record
college_subsidy_quota
grade_subsidy_quota
subsidy_application
student_recommendation
```

其中 `application` 是共享主表：

- 成员二负责维护 DDL、Entity 和 Mapper；
- 成员三、成员四可以读取和通过成员二提供的 Service 修改业务数据；
- 任何字段增加、删除、改名、改类型，必须由成员二、三、四共同确认；
- 其他成员禁止自行建立第二张申请主表。

---

### 2.3 成员三：审核与上报

分支：

```text
feature/approval-flow
```

拥有以下表的结构维护权：

```text
approval_record
approval_submission_record
system_message
message_read_record
```

其他成员可以查询审核结果，但不能直接修改审核记录或跳过审核 Service 改状态。

`application.status` 和 `application.current_level` 的状态变化由成员三负责业务逻辑，但字段结构仍由成员二维护。

---

### 2.4 成员四：确认与统计

分支：

```text
feature/confirmation-statistics
```

拥有以下表的结构维护权：

```text
arrears_confirmation
```

统计功能默认只执行查询和聚合，不建立重复业务表，不直接修改其他成员负责的表。

成员四需要改变申请状态时，必须调用成员三提供的状态流转 Service；需要创建申请时，必须调用成员二提供的申请 Service。

---

## 3. 数据库操作权限

### 3.1 查询权限

所有成员可以根据业务需要读取其他成员负责的表，但必须先阅读：

```text
docs/database-design.md
docs/change-log.md
docs/decisions/<表负责人模块>.md
```

禁止根据猜测使用字段。

### 3.2 数据写入权限

每张表只允许表负责人模块直接执行 `INSERT`、`UPDATE`、`DELETE`。

其他模块需要写入时：

1. 调用表负责人提供的 Service；
2. 或由负责人新增专用 Service 方法；
3. 禁止其他成员新建 Mapper，直接修改别人负责的表。

### 3.3 表结构修改权限

只有表负责人可以提交以下操作：

```text
CREATE TABLE
ALTER TABLE
DROP TABLE
RENAME TABLE
新增或删除索引
修改唯一约束或外键
修改字段类型、名称和可空性
```

但提交前必须：

1. 在 `docs/change-log.md` 写一条 `PROPOSED` 记录；
2. 通知受影响成员；
3. 修改 `docs/database-design.md`；
4. 得到受影响成员确认；
5. 再修改 SQL、Entity、DTO/VO、Mapper 和接口文档；
6. 完成后把记录状态改为 `IMPLEMENTED`。

### 3.4 共享集成数据库权限

若四人共用一个测试数据库：

- 仓库负责人拥有 DDL 权限；
- 其他成员只执行本模块测试所需的 DML；
- 所有 DDL 由仓库负责人按照已合并到 `main` 的 SQL 统一执行；
- 禁止任何成员直接在共享数据库里手工改表后不提交 SQL；
- 本地数据库可以自行重建，但最终结构必须以仓库 SQL 为准。

---

## 4. 数据库文件的唯一职责

```text
database/01_create_database.sql
```

只负责创建数据库。

```text
database/02_create_tables.sql
```

保存当前完整表结构，用于全新环境一次性建表。

```text
database/03_init_data.sql
```

保存角色、字典、默认配置等必要初始化数据。

```text
database/04_test_data.sql
```

只保存开发测试数据。

```text
database/migrations/
```

保存已经建库后的增量修改，例如：

```text
V20260717_01__add_application_source.sql
V20260717_02__add_subsidy_quota.sql
```

数据库结构发生变化时，必须同时修改：

```text
docs/database-design.md
database/02_create_tables.sql
database/migrations/对应增量SQL
docs/change-log.md
```

---

## 5. 特殊设定写在哪里

每个成员必须维护自己的设计决策文件：

```text
docs/decisions/base-user-batch.md
docs/decisions/application-config.md
docs/decisions/approval-flow.md
docs/decisions/confirmation-statistics.md
```

以下内容必须写入本人决策文件：

- 特殊字段含义；
- 特殊校验规则；
- 事务和锁的使用方式；
- 文件保存方式；
- 某个状态为什么这样流转；
- 某个接口为什么这样设计；
- 与通用规则不同的实现；
- 对其他成员可能产生影响的限制。

统一记录格式：

```markdown
## 决策标题

- 日期：
- 负责人：
- 涉及表：
- 涉及接口：
- 涉及文件：
- 具体规定：
- 做出原因：
- 对其他模块的影响：
- 是否需要其他成员确认：
```

个人决策如果影响共享表、接口或状态，不能只写个人决策文件，还必须同步更新对应公共文档。

---

## 6. 每次开始开发前的固定操作

### 6.1 切换到自己的分支

```bash
git switch feature/自己的分支
```

### 6.2 拉取自己的远程分支

```bash
git pull --ff-only origin feature/自己的分支
```

### 6.3 获取远程最新状态

```bash
git fetch origin
```

### 6.4 查看共享规则是否变化

必须先看：

```text
docs/change-log.md
```

如果 `main` 中的公共文档有更新，先把 `main` 合入自己的分支：

```bash
git merge origin/main
```

解决冲突并确认文档后，才能继续开发。

---

## 7. 执行不同操作前必须阅读的文件

| 准备执行的操作 | 必须先读 |
|---|---|
| 使用或修改数据库表 | `database-design.md`、`change-log.md`、表负责人决策文件 |
| 调用别人接口 | `api-document.md`、`change-log.md`、接口负责人决策文件 |
| 修改申请状态 | `status-flow.md`、`change-log.md`、`approval-flow.md` |
| 修改公共 Axios、路由、权限 | `development-guide.md`、`change-log.md`、`base-user-batch.md` |
| 修改 `application` 主表 | `database-design.md`、`status-flow.md`、成员二/三/四决策文件 |
| 使用别人的 SQL | `database-design.md`、对应 migration、表负责人决策文件 |
| 使用别人分支里的代码 | `change-log.md`、对应提交说明、负责人决策文件 |
| 修改 `pom.xml` 或 `package.json` | `development-guide.md`、`change-log.md` |

---

## 8. 使用其他成员成果的规则

### 8.1 只需要接口或数据结构

不要拉取对方分支。

直接按照以下文档开发：

```text
docs/api-document.md
docs/database-design.md
docs/status-flow.md
```

接口未完成时，前端使用模拟数据。

### 8.2 只需要一个独立工具、组件或修复

由原负责人提交一个独立提交，并把提交号写入 `docs/change-log.md`。

使用者执行：

```bash
git fetch origin
```

```bash
git cherry-pick <提交编号>
```

### 8.3 需要对方完整模块

只有以下条件全部满足才允许使用：

- 对方模块已完成；
- 对方已推送；
- 对方已更新相关文档；
- 对方已通过基本测试；
- 对方已创建 Pull Request 并合并到 `main`。

使用者随后执行：

```bash
git fetch origin
```

```bash
git merge origin/main
```

禁止为了拿代码直接把另一个成员的整个开发分支合进自己的分支。

---

## 9. 共享结构变更流程

任何共享表、公共接口、公共状态或公共配置变化，必须按以下顺序：

```text
提出变更
→ 写入 docs/change-log.md，状态为 PROPOSED
→ 相关成员确认
→ 负责人修改公共文档
→ 负责人修改代码和 SQL
→ 本模块测试
→ 状态改为 IMPLEMENTED
→ Commit and Push
→ 创建 Pull Request
→ 合并到 main
→ 其他成员 fetch 并 merge origin/main
```

没有写入 `docs/change-log.md` 的共享变化，其他成员有权不接受。

---

## 10. `docs/change-log.md` 固定格式

```markdown
## 2026-07-17｜变更标题

- 状态：PROPOSED / APPROVED / IMPLEMENTED
- 提出人：
- 负责人：
- 影响模块：
- 影响表：
- 影响接口：
- 影响状态：
- 变更内容：
- 使用者需要执行的操作：
- 对应提交：
```

最新记录放在文件最上方。

---

## 11. 提交前检查

提交前必须确认：

```bash
git status
```

并检查：

- 当前在自己的分支；
- 修改了对应公共文档；
- SQL 与数据库设计一致；
- 接口与接口文档一致；
- 状态与状态文档一致；
- 没有修改别人负责的 Mapper；
- 没有提交 `.idea`、`*.iml`、`node_modules`、`target`；
- 没有提交密码、Token 和本地数据库配置。

之后再在 IDEA 中执行：

```text
Commit and Push
```

---

## 12. 当前尚未存在、必须新建的文件

以下文件如果仓库中还没有，必须建立：

```text
docs/collaboration-rules.md
docs/change-log.md
docs/decisions/base-user-batch.md
docs/decisions/application-config.md
docs/decisions/approval-flow.md
docs/decisions/confirmation-statistics.md
database/migrations/
```

现有 README 已经规定了独立分支、公共文件修改需提前说明，以及接口、数据库、状态文档作为协作依据；但没有明确表级权限、固定阅读顺序、共享变更记录格式和具体同步流程。本文件补齐这些规则。

## 13. 6.1.2 欠费单据接口固定约定

本节补齐 V2 已明确功能但尚未命名的接口与数据契约；不改变表所有权、状态规则或事务边界。

### 13.1 对外路径和权限

GET /api/arrears-vouchers：学校管理员分页查询确认单。

GET /api/arrears-vouchers/{voucherNo}：学校管理员查看或预览单据。

GET /api/arrears-vouchers/{voucherNo}/print：学校管理员获取打印数据。

GET /api/student/arrears-vouchers/{voucherNo}：学生查看本人单据。

- 单据对外唯一标识为 voucherNo，不暴露确认记录数据库主键作为 URL 标识。
- 学校接口要求 SCHOOL 权限；学生接口要求 STUDENT 且只能读取本人申请。
- `X-User-Id` 仅允许本地手工调试时临时携带用户 ID，不能作为权限成功验证依据；成员一 `CurrentUserProvider`（或等价 JWT 实现）合入后，后端必须从该可信上下文取得用户 ID、角色和数据范围。前端传入的学生 ID、角色或学院 ID 一律不作为权限依据。
- 只允许读取 arrears_confirmation.deleted = 0 的记录；已作废或已取消单据第一阶段不返回。

### 13.2 单据返回格式

四个接口返回同一 ArrearsVoucherVO，字段固定为：

voucherNo, applicationId, studentNo, studentName, collegeName, majorName, gradeName, className, arrearsItems[{feeItemName, declaredAmount}], appliedAmount, confirmedAmount, confirmedTime, confirmUserId, confirmUserName, printTitle, printTime。

- arrearsItems 是欠费项目明细；appliedAmount 为确认时快照，confirmedAmount 为实际确认金额。
- 查询和预览接口 printTime 为 null；打印接口由后端填入本次请求时间，前端只负责浏览器打印，不生成第二张单据。
- 金额 JSON 使用数值、后端使用 BigDecimal；时间按 ISO 8601 输出。

### 13.3 跨模块批量查询和访问校验

成员四直接从 arrears_confirmation 查询单据编号、申请 ID、金额、确认信息和时间；不得直接创建 application/student 写 Mapper。成员二必须提供：

Map<Long, ArrearsVoucherApplicantSnapshot> findVoucherApplicantsByApplicationIds(Collection<Long> applicationIds)。

快照字段必须覆盖学生/组织信息与 arrearsItems。成员一必须提供：

void checkSchoolUser(Long userId);
void checkStudentOwnsApplication(Long userId, Long applicationId)。

成员四列表一次最多取 100 条确认记录后批量请求成员二快照，禁止逐条跨模块查询。任一依赖未合入时接口返回 503，不返回不完整或伪造单据。

### 13.4 6.1.2 的完成边界与当前阻塞项

成员四已可独立完成的范围仅包括：读取 `arrears_confirmation`、按 `voucherNo` 定位确认记录、分页、组装单据响应、学校/学生接口路由以及浏览器打印触发。该范围不依赖任何临时表、模拟数据或跨表 JOIN。

以下内容在成员一、成员二实现并合入第 13.3 节规定的 Port 前，明确视为**未完成且不得伪造实现**：

- 单据中的学号、姓名、学院、专业、年级、班级和欠费项目明细；
- 学校管理员和学生身份校验；
- 学生仅查看本人单据的归属校验；
- 使用真实数据完成的接口联调、Apifox 成功响应验证和前端完整展示验证。

阻塞解除条件是：成员二提供能返回完整 `ArrearsVoucherApplicantSnapshot` 的批量查询实现，成员一提供学校角色、学生归属和确认人姓名查询实现，并写明其实现类、可调用方式和最小测试数据。成员四收到上述已合入成果后，只补接入代码和联调，不另行创建临时字段、假数据、重复表或越权 Mapper。

---

## 14. 外部依赖缺失时的强制处理规则

当功能依赖其他成员负责的表、SQL、Service、接口、权限或测试数据，而该依赖尚未合入 `main`、未在权威文档中声明，或尚不能提供真实数据时，实施者必须同时做到：

1. 停止实现该依赖对应的业务闭环；不得用内存数据、硬编码数据、假 SQL、猜测字段、临时接口或直接访问他人 Mapper 替代。
2. 在本人决策文件和 `docs/change-log.md` 中写明：已完成范围、缺失依赖的负责人和准确名称、受影响功能、当前不能验证的接口、解除条件。
3. 如已有路由骨架，接口必须明确返回 `503 Service Unavailable`，错误信息指出缺失的依赖；不得返回字段缺失的 200 成功响应。
4. 前端可以保留仅用于路由连通性验证的空态页面，但必须显示“依赖未合入，当前不可验证”，不得展示伪造业务数据或声称功能已完成。

“代码可以编译”不等于“功能已完成”。只有依赖实现、真实 SQL/测试数据和成功联调均具备时，才能在 `docs/change-log.md` 将对应功能标记为 `IMPLEMENTED`。

---

## 15. 2026-07-19｜成员四 6.1.1—6.1.3 实现核对后的固定契约

本节以已批准的 `docs/requirement.md`、当前 `database-design.md` 和当前成员四代码为依据，消除已经能够确定的命名分歧。它不把未合入的跨模块能力假定为已存在。

### 15.1 6.1.1 欠费最终确认：字段、状态和持久化

对外接口固定为：

```http
GET  /api/confirm/list
GET  /api/confirm/app/{applicationId}
POST /api/confirm/{applicationId}
```

`POST /api/confirm/{applicationId}` 的 JSON 请求体**只能**使用以下字段名：

```json
{
  "confirmedAmount": 1000.00,
  "version": 1,
  "requestId": "唯一请求号"
}
```

- `version` 是统一申请主表 `application.version`，用于成员三在 `CONFIRM_PENDING -> COMPLETED` 时做乐观锁校验；字段名不得写成 `applicationVersion`。
- `confirmedAmount` 必须大于 `0.00`，且不大于成员二返回的 `appliedAmount`；确认人、确认时间、单据编号和申报金额快照均由后端产生或读取，前端不得传入。
- 确认成功返回 `confirmationId`、`applicationId`、`appliedAmount`、`confirmedAmount`、`voucherNo`、`confirmUserId`、`confirmedAt`、`applicationStatus`；其中 `applicationStatus` 固定为 `COMPLETED`。
- `voucherNo` 固定为 `GC + 四位确认年份 + applicationId 六位补零`，例如申请 `1` 于 2026 年确认时为 `GC2026000001`。同一 `applicationId` 的有效确认记录只能有一条。
- `arrears_confirmation` 的最终物理字段包括 `request_id VARCHAR(64) NOT NULL`，并具有 `uk_arrears_confirmation_application_id_deleted`、`uk_arrears_confirmation_voucher_no`、`uk_arrears_confirmation_request_id` 三个唯一约束。`database/02_create_database.sql` 当前尚未包含 `request_id` 及后两个约束，与本条、`database-design.md` 和成员四 migration 冲突；在该基线 SQL 同步前，禁止把 6.1.1 标记为可在全新数据库完整验证。
- 成员三的正式能力名为 `ApprovalCompletionService`；成员四代码中的 `ArrearsConfirmationCompletionPort` 只是调用该能力的本地适配边界，不要求成员三另建第二套状态机。

### 15.2 6.1.2 欠费单据：保持现有固定规则，不新增临时字段

第 13 节的四个单据路径、`ArrearsVoucherVO` 字段、分页上限和批量查询规则继续有效。本次核对没有发现能以成员四自有表补全学生、组织、欠费项目或用户姓名的数据来源，因此这些字段不得从确认表猜测、拼接或设置默认值。

当前唯一允许的实现状态是：成员四读取 `arrears_confirmation` 并编排查询；成员二提供完整 `ArrearsVoucherApplicantSnapshot`，成员一提供权限、归属和姓名查询后，才可返回完整的 200 单据响应。依赖未合入时必须返回 503。

### 15.3 6.1.3 学校代申请：路径、载荷和状态

对外接口固定为：

```http
GET  /api/school-proxy/students?studentNo={studentNo}
POST /api/school-proxy/applications/drafts
POST /api/school-proxy/applications/{applicationId}/attachments?requestId={requestId}
POST /api/school-proxy/applications/{applicationId}/submit?version={version}&requestId={requestId}
```

- 查询学生只接受 `studentNo`；返回 `studentId`、`studentNo`、`studentName`、`collegeName`、`majorName`、`gradeName`、`className`，不得返回身份证号、手机号等敏感字段。
- 创建草稿请求体固定为 `studentNo`、`batchType`、`batchId`、`applicationReason`、`arrearsItems`、`giftItems`、`requestId`。其中 `batchType` 的唯一合法值是 `GREEN_CHANNEL`；`arrearsItems` 的每项为 `feeItemId`、`declaredAmount`，`giftItems` 的每项为 `giftItemId`、`quantity`。
- 附件请求使用 `multipart/form-data`，文件字段名固定为 `file`；提交接口的 `version`、`requestId` 均为必填查询参数。创建、上传和提交不得复用不同业务操作的 `requestId`。
- 创建草稿成功后返回 `applicationId`、`applicationNo`、`source`、`status`、`version`，其中 `source` 固定为 `SCHOOL_PROXY`、初始 `status` 固定为 `DRAFT`；正式提交成功后 `status` 固定为 `COUNSELOR_PENDING`。
- 成员二的正式职责为 `ApplicationCreationService` 及附件/提交写入；成员四代码中的 `SchoolProxyApplicationPort` 仅是本模块调用成员二能力的适配边界，成员四不得以此直接写 `application`、明细、附件或资源表。成员一的学生查询、成员二的创建/附件/提交、成员三的提交审核记录尚未合入时，该流程仍必须返回 503，不得宣布可用。

### 15.4 当前用户身份的统一处理

`X-User-Id` 只可用于本地手工调试，绝不是已确定的生产接口参数或权限方案。第一阶段正式实现必须使用成员一提供的 `CurrentUserProvider`（或其后续等价 JWT 实现）取得用户 ID、角色和数据范围；在该真实实现合入前，6.1.1、6.1.2、6.1.3 的权限成功场景均不可验证，也不得写入 `IMPLEMENTED`。

---

## 16. 6.1.5 统计功能与 6.1.6 统计筛选固定约定

### 16.1 对外接口、权限和统计口径

学校端只使用一个聚合读取接口：

```http
GET /api/statistics/applications/summary
```

- 接口要求 `SCHOOL` 权限；正式实现从成员一 `CurrentUserProvider` 取得当前用户。`X-User-Id` 只能用于本地连通性调试，未接入真实权限时接口必须返回 503，不能返回统计成功结果。
- 只统计 `application.deleted = 0` 且状态为 `APPROVED` 或 `COMPLETED` 的申请；`CONFIRM_PENDING`、所有待审/退回/拒绝状态和 `CANCELLED` 一律排除。
- “申请总人数”固定为筛选范围内拥有最终状态申请的 `DISTINCT student_id` 数；“实报人数”固定为其中状态为 `COMPLETED` 的 `DISTINCT student_id` 数。两项均不以申请行数冒充人数。
- “欠费总金额”固定为筛选范围内、关联有效 `arrears_confirmation.deleted = 0` 且申请状态为 `COMPLETED` 的 `SUM(confirmed_amount)`；没有确认记录时按 `0.00` 返回。
- “欠费项目人数”固定为筛选范围内至少申报该欠费项目的 `DISTINCT student_id` 数；按项目分组时，同一学生对同一项目只能计一次。
- 统计查询必须是一条成员二实现的面向集合的聚合查询（允许只读关联成员四 `arrears_confirmation`）；禁止成员四先逐条取申请再在内存中汇总。

### 16.2 固定筛选参数

所有参数均为可选；不传表示不过滤。参数名、类型和约束固定如下：

| 参数 | 类型 | 约束 |
|---|---|---|
| `batchType` | string | `GREEN_CHANNEL` 或 `SUBSIDY`；传 `batchId` 时必传 |
| `batchId` | long | 大于 0；与 `batchType` 共同定位历史或当前批次 |
| `collegeId` / `majorId` / `gradeId` / `classId` | long | 大于 0；组织层级必须真实存在且相互隶属 |
| `applicationType` | string | `GREEN_CHANNEL`、`LIVING_SUBSIDY`、`TRAVEL_SUBSIDY` 之一 |
| `applicationStatus` | string | 仅允许 `APPROVED` 或 `COMPLETED`；不传时统计两种最终状态 |
| `feeItemId` | long | 大于 0；只筛选含该欠费项目的申请 |
| `applicationStartTime` / `applicationEndTime` | ISO 8601 datetime | 起始时间不得晚于结束时间；按申请创建时间闭区间筛选 |

`batchType=GREEN_CHANNEL` 时只允许 `applicationType=GREEN_CHANNEL`；`batchType=SUBSIDY` 时只允许 `LIVING_SUBSIDY` 或 `TRAVEL_SUBSIDY`。不符合上述组合返回 400，不能静默忽略条件。

### 16.3 固定返回数据 `ApplicationStatisticsVO`

接口的 `data` 固定包含：

```text
finalApplicantCount                 申请总人数
completedStudentCount               实报人数
feeItemApplicantCount               满足 feeItemId 时的欠费项目人数；未传 feeItemId 时为所有欠费项目去重人数
confirmedArrearsAmount              欠费总金额
collegeApplicantCounts[]            {collegeId, collegeName, applicantCount}
gradeApplicantCounts[]              {gradeId, gradeName, applicantCount}
arrearsReasonStatistics[]           {arrearsReasonCode, arrearsReasonName, applicantCount, confirmedAmount}
giftItemApplicationCounts[]         {giftItemId, giftItemName, applicationCount}
batchHistoryStatistics[]            {batchType, batchId, batchName, applicantCount, completedStudentCount, confirmedArrearsAmount}
```

- 所有金额使用 JSON 数值和 Java `BigDecimal`；无数据的金额为 `0.00`，无数据的列表为 `[]`，不得为 `null`。
- `collegeApplicantCounts`、`gradeApplicantCounts`、`arrearsReasonStatistics` 和 `giftItemApplicationCounts` 按人数降序、同数时按 ID 升序；`batchHistoryStatistics` 按批次开始时间降序、同时间按 `batchId` 降序。
- `batchHistoryStatistics` 在不传 `batchId` 时返回当前筛选范围内的历史批次聚合；传入 `batchType + batchId` 时只返回该批次的一项。

### 16.4 跨模块统计查询与欠费原因字段

成员二实现本地适配接口 `ApplicationStatisticsQueryPort.queryFinalStatistics(StatisticsFilterDTO)`，其正式业务能力对应已批准的 `ApplicationStatisticsQueryService`。它负责一次性读取申请、学生组织、欠费项目、礼包和批次数据，并以只读方式关联成员四 `arrears_confirmation`；成员四只负责学校端权限编排、参数校验和接口返回。

截图要求的“欠费原因”在当前 `arrears_application` 结构说明中尚无可统计字段，不能把自由文本 `applicationReason` 当作原因分组。为使该功能可实现，新增以下**待成员二确认的表字段提案**：

```text
arrears_application.arrears_reason_code VARCHAR(32) NOT NULL
```

允许值固定为 `FAMILY_FINANCIAL_DIFFICULTY`、`FAMILY_EMERGENCY`、`MAJOR_ILLNESS`、`DISASTER_ACCIDENT`、`OTHER`；返回名称分别为“家庭经济困难”“家庭突发变故”“重大疾病”“灾害或事故”“其他”。成员二须按共享结构流程更新其 DDL、Entity、DTO、迁移和测试数据。该字段合入前，`arrearsReasonStatistics` 没有真实来源，因此完整统计接口返回 503，不得返回虚构分类或空数组冒充成功。

### 16.5 完成边界

成员四可独立完成 Controller、筛选 DTO、统计 VO、前端筛选页和对成员二集合查询 Service 的调用外壳。以下条件全部满足后，6.1.5 与 6.1.6 才能标记为 `IMPLEMENTED`：成员一合入学校统计权限能力；成员二合入统计聚合 Service、真实批次/组织/申请/礼包测试数据，以及已确认的欠费原因字段；接口使用真实数据在 Apifox 和前端联调成功。
