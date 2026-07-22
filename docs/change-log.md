
# 共享结构与接口变更记录

## 2026-07-21｜成员三批量上报、工作台、取消与跨模块完成适配

- 状态：IMPLEMENTED（成员三范围）；端到端联调待跨成员依赖就绪
- 提出人：成员三
- 负责人：成员三（审批编排、审核/上报记录、消息和成员三事务适配）
- 影响模块：成员一身份与数据范围、成员二申请/资源、成员三审批、成员四欠费确认与补录
- 影响接口：`/api/approval-submissions/**`、`/api/approvals/pending`、`/processed`、`/dashboard`、`/{applicationId}`、`/{applicationId}/cancel`、`ArrearsConfirmationCompletionPort`、`SupplementCompletionPort`
- 变更内容：完成辅导员/学院首次批量上报、退回补交、工作台分页/详情/统计、学校取消编排，以及欠费确认和补录自动审核的事务适配；前端真实接口统一使用全站 JWT Token，并兼容分页和详情返回结构。
- 当前边界：成员一审批身份/范围/批次/消息收件人 Bean、成员二审核查询和资源生命周期 Bean、成员四取消单据 Bean 未全部就绪时，接口明确返回 `503 APPROVAL_INTEGRATION_UNAVAILABLE`；不使用前端身份参数、直接 SQL 或模拟成功绕过依赖。
- 验证状态：前端生产构建通过；后端全量测试当前被 `SupplementApplicationServiceImpl` 引用缺失的 `ISupplementApplicationService` 阻塞，修复共享编译后需重新执行成员三单元、集成和事务回滚测试。
- 对应文档：`docs/member3-approval-submission-dependencies.md`、`docs/member3-approval-workbench-dependencies.md`、`docs/member3-cancellation-dependencies.md`、`docs/member3-task-d-integration.md`。
- 对应提交：本次成员三合并提交。

## 2026-07-21｜成员二正式申请 Port 接入

- 状态：PARTIALLY_IMPLEMENTED
- 提出人：成员四（依据成员二 `member2-integration-handoff.md`）
- 影响模块：成员一、成员二、成员三、成员四
- 影响接口：`/api/school-proxy/**`、`/api/supplements/**` 及其对应正式 Port
- 变更内容：成员四改为只注入成员二正式 `SchoolProxyApplicationPort` 和 `SupplementApplicationPort`；旧的成员四直接写表适配器取消 Spring Bean 注册。学校代申请与补录前端均补齐 `arrearsReasonCode` 固定枚举；学校代申请补齐附件上传调用，补录历史页补齐真实详情请求；学生快照补充组织 ID 字段。补录创建不再由成员四二次调用成员三自动审核，避免重复推进状态。
- 当前边界：附件存储未接通时固定返回 `501 ATTACHMENT_STORAGE_UNAVAILABLE`；学校代申请提交的附件/资源前置能力未接通时固定返回 `503 SCHOOL_PROXY_SUBMISSION_UNAVAILABLE`；补录历史和详情仍等待成员一按 ID/批量学生组织快照。统计、筛选、报表仍等待成员一权限范围和成员二真实聚合。
- 对应文档：`docs/member2-integration-handoff.md`、`docs/collaboration-rules.md` 第 23 节。
- 对应提交：本次成员四接入提交。

## 2026-07-20｜成员三审核与消息持久层

- 状态：IMPLEMENTED
- 提出人：成员三
- 负责人：成员三
- 影响模块：三级审核、批量上报、审核消息；其他模块仅通过成员三 Service 查询或写入
- 影响表：`approval_record`、`approval_submission_record`、`system_message`、`message_read_record`
- 影响接口：成员三内部 Mapper；不改变已批准的跨模块 Service 签名
- 影响状态：不增加或修改申请状态枚举
- 变更内容：新增四张成员三表的 `V20260720_002__create_approval_and_message_tables.sql`；实现四个 Entity、四个 MyBatis Mapper、枚举映射、H2 测试数据源及 Mapper 集成测试。`approval_submission_record` 按 `batchType + batchId` 契约使用批次类型和两列可空批次 ID，并通过 CHECK 约束保证一致性；新增申请状态 Service 适配器，并将成员二申请状态枚举对齐已批准状态流，确保成员三审核服务调用唯一的申请表写入实现。
- 使用者需要执行的操作：全新或尚未创建成员三表的环境执行本次 migration。已从旧脚本创建过同名表的本地环境应先备份并重建，不得直接重复执行创建 migration。
- 对应提交：待提交

## 2026-07-20｜汇总 6.1.1—6.1.7 跨成员依赖和解除条件

- 状态：APPROVED
- 提出人：成员四
- 影响模块：成员一、成员二、成员三、成员四
- 变更内容：在 `collaboration-rules.md` 第 20 节集中列出七项任务所需的准确 Port/Service、负责人、当前实现状态、真实数据要求和解除条件；同时将 6.1.4 外层事务声明与当前普通 `@Transactional` 实现对齐。
- 协作要求：只存在接口或 TODO 不算依赖完成；实现方须提供 Spring Bean、实现类位置和最小真实 SQL/测试数据，成员四完成 Apifox 与前端联调后才能更新为 `IMPLEMENTED`。
- 对应提交：本次公共文档提交

## 2026-07-20｜演示版本移除成员四自定义异常处理

- 状态：PROPOSED
- 提出人：成员四
- 影响模块：成员二、成员四及公共异常包
- 影响文件：删除 `BusinessException`、`GlobalExceptionHandler`、`ApplicationException`、`ApplicationExceptionHandler`；成员四 Controller、Service、测试页及被成员四调用的本地申请服务不再使用显式 `try/catch` 或主动 `throw`。
- 保留内容：DTO 基础校验注解、数据库约束和事务；它们继续保证正常演示输入下的数据格式与写入一致性。
- 协作要求：缺少跨成员实现时在协作文档中记录阻塞，不使用临时异常响应、模拟数据或第二套接口补齐。
- 对应提交：本地待决定，未提交

## 2026-07-20｜成员四 6.1.7 报表明细、Excel 导出和打印

- 状态：PROPOSED
- 提出人：成员四
- 负责人：成员四（查询编排、字段白名单、Excel 和打印）、成员一（学校权限与数据范围）、成员二（真实报表明细分页查询）
- 影响模块：成员一、成员二、成员四
- 影响表：只读成员一组织/学生、成员二申请/明细/批次/资源和成员四确认表；不新增报表业务表
- 影响接口：`GET /api/statistics/reports/details`、`/history`、`/print`、`/export`、`StatisticsReportQueryPort`
- 影响状态：只读 `APPROVED`、`COMPLETED`，不改变申请状态
- 变更内容：固定 22 个自定义列、统一筛选 DTO、历史批次查询和 SXSSF 每批 500 行的流式导出；演示版本不设置打印/导出总数拦截；`pom.xml` 新增 `poi-ooxml:5.3.0`。
- 当前实现范围：成员四 Controller、Service、DTO/VO、列映射、Excel 与打印生成已经完成；不保留自定义异常包装。
- 当前阻塞项：成员一 `StatisticsAccessPort` 与成员二 `StatisticsReportQueryPort` 真实实现尚未合入，不能用真实数据验证成功下载和打印。
- 使用者需要执行的操作：成员一、二确认第 18 节及新 Port；合入实现后由成员四完成 Apifox、Excel 文件打开和打印联调。
- 对应提交：本地待决定，未提交

## 2026-07-20｜成员四 6.1.4 绿色通道线下补录接口与编排

- 状态：PROPOSED
- 提出人：成员四
- 负责人：成员四（学校端校验、事务编排与页面）、成员一（可信学校身份和学生查询）、成员二（补录申请/明细/资源/历史查询）、成员三（自动审核记录与状态流转）
- 影响模块：成员一、成员二、成员三、成员四
- 影响表：成员四不新增或直接写业务表；成员二写 `application` 及欠费/礼包/补助明细和资源，成员三写 `approval_record`
- 影响接口：`GET /api/supplements/students`、`GET /api/supplements`、`GET /api/supplements/{applicationId}`、`POST /api/supplements`、`SupplementApplicationPort`、`SupplementCompletionPort`
- 影响状态：补录先创建 `DRAFT`；含欠费固定进入 `CONFIRM_PENDING/CONFIRMATION`，不含欠费固定进入 `COMPLETED/SYSTEM`
- 变更内容：固定 6.1.4 请求字段、批次类型映射、明细互斥规则、幂等号、返回字段、分页筛选、外层事务和跨模块写入边界；完成成员四 Controller、DTO/VO、Service 编排、Port 和前端验证页。
- 当前阻塞项：成员一可信身份/学生快照适配、成员二补录详情/历史读模型和真实数据联调；成员三 `completeSupplementReview` 已完成，不再是阻塞项。
- 使用者需要执行的操作：成员一补齐学生快照，成员二补齐详情/历史读模型；成员三适配已完成。依赖合入后由成员四使用真实 SQL 数据完成 Apifox 和前端联调，再将状态更新为 `IMPLEMENTED`。
- 对应提交：协作边界 `e17e951`；代码本地待提交

## 2026-07-20｜成员四确认表 SQL 与成员三完成接口对齐

- 状态：IMPLEMENTED
- 提出人：成员四
- 负责人：成员四（`arrears_confirmation` 基线、migration、测试数据及本地确认适配）、成员三（正式 `ApprovalCompletionService` 实现）
- 影响模块：成员三、成员四
- 影响表：`arrears_confirmation`；不修改成员三审核表、成员一角色表或成员二申请表
- 影响接口：成员四本地 `ArrearsConfirmationCompletionPort.completeAfterConfirmation` 对齐成员三 `ApprovalCompletionService.completeAfterConfirmation`
- 影响状态：确认成功后的目标状态仍为 `CONFIRM_PENDING -> COMPLETED`，不新增状态值
- 变更内容：`database/02_create_tables.sql` 补齐 `request_id`、有效确认记录唯一约束、单据号唯一约束和请求号唯一约束；`database/04_test_data.sql` 的 100 条确认测试数据补齐唯一 `request_id`；成员四 migration、Entity、数据库设计与基线 SQL 保持一致。
- 当前阻塞项：成员二待确认申请真实读取、成员一可信身份/数据范围和端到端事务联调；成员三正式 `ApprovalCompletionService` 及成员四完成 Port 适配已实现，不再是阻塞项。
- 使用者需要执行的操作：新环境按 `01_create_database.sql`、`02_create_tables.sql`、`04_test_data.sql` 顺序建库和导入测试数据；已有数据库继续按成员四 migration 处理，不回改已执行 migration。
- 对应提交：待提交

## 2026-07-19｜成员四统计功能与统计筛选接口契约

- 状态：PROPOSED
- 提出人：成员四
- 负责人：成员四（学校端统计编排）、成员一（当前用户与学校权限）、成员二（集合统计查询、申请/组织/批次/欠费/礼包数据与欠费原因字段）
- 影响模块：成员一、成员二、成员四
- 影响表：只读 `application`、学生组织、批次、`arrears_application`、`fee_item`、礼包表和 `arrears_confirmation`；提议成员二新增 `arrears_application.arrears_reason_code`
- 影响接口：`GET /api/statistics/applications/summary`、成员二 `ApplicationStatisticsQueryService`
- 影响状态：只读 `APPROVED`、`COMPLETED`；不改变申请状态
- 变更内容：固定统计口径、人数/金额计算方式、八个筛选参数、返回字段、排序、历史批次规则和欠费原因编码。
- 当前阻塞项：成员一学校统计权限、成员二统计聚合 Service、申请全量真实数据和欠费原因编码字段均未合入；成员四不得用确认表或假数据补算。
- 使用者需要执行的操作：成员一、成员二确认并实现第 16 节契约；成员四在依赖合入后完成真实联调并更新本记录状态。
- 对应提交：待提交

## 2026-07-18｜成员四欠费单据查询、学生查看与打印接口

- 状态：PROPOSED
- 提出人：成员四
- 负责人：成员四（单据查询/打印）、成员一（当前用户和学生归属校验）、成员二（申请与欠费项目批量快照）
- 影响模块：成员一、成员二、成员四
- 影响表：arrears_confirmation；只读 application、学生组织和欠费项目数据
- 影响接口：学校单据列表/详情/打印、学生本人单据查询及两个跨模块只读 Port
- 影响状态：不改变 CONFIRM_PENDING、COMPLETED 或申请状态
- 变更内容：固定路径、权限、单据字段、批量查询上限、打印时间和 JWT 前临时身份规则。
- 当前实现范围：成员四已实现确认记录读取、分页、路由、单据组装与打印触发；不含任何模拟数据、临时表或跨模块直连。
- 当前实现范围：成员二已在 `application.service.ArrearsConfirmationApplicationService` 实现 `ArrearsVoucherApplicantQueryPort.findVoucherApplicantsByApplicationIds`，一次最多读取 100 个申请的真实申请、欠费项目与金额，并通过成员一的 `StudentOrganizationSnapshotQuery` 批量补齐学生组织快照。
- 当前阻塞项：成员一尚未合入 `StudentOrganizationSnapshotQuery` 和 `ArrearsVoucherAccessPort` 的学校权限、学生归属和确认人姓名实现。因此完整单据字段、权限校验、学生本人查看和成功响应联调目前均不可完成或验证。
- 解除条件：成员一提供上述 Port 实现和最小测试数据；成员四接入成员二 Port 后完成单据联调，不新增临时数据方案。
- 使用者需要执行的操作：成员一实现并合入上述两个 Port；成员四接入 `ArrearsVoucherApplicantQueryPort` 并联调；成员二配合排查真实数据问题。
- 对应提交：本地待提交

## 2026-07-17｜成员四学校代申请接口契约

- 状态：PROPOSED
- 提出人：成员四
- 负责人：成员四（学校端编排）、成员一（学生查询）、成员二（申请/附件/提交）、成员三（提交状态与审核记录）
- 影响模块：成员一、成员二、成员三、成员四
- 影响表：application 及明细/附件表、approval_record；成员四不直接写入
- 影响接口：学校代申请学生查询、创建草稿、上传附件、正式提交
- 影响状态：SCHOOL_PROXY 草稿为 DRAFT；正式提交后进入 COUNSELOR_PENDING
- 变更内容：固定 6.1.3 的路径、请求字段、附件上传方式、requestId、事务发起方和跨模块写入边界。
- 当前阻塞项：成员一 `SchoolProxyStudentQueryPort`/可信学校身份、成员二 `SchoolProxyApplicationPort` 的草稿/明细/附件/资源写入，以及成员三 `SUBMIT -> COUNSELOR_PENDING` 审核流转均未形成可注入的完整实现。
- 解除条件：三方提供 Spring Bean、实现类位置和最小真实数据后，成员四按查学生、建草稿、传附件、正式提交的顺序完成 Apifox 与前端联调。
- 使用者需要执行的操作：成员一、二、三按 confirmation-statistics.md 的 Port 契约提供实现；完成联调后更新状态。
- 对应提交：待提交

## 2026-07-17｜成员二申请配置模块跨模块接口提案

- 状态：APPROVED
- 提出人：成员二
- 负责人：成员二（申请、资源和附件接口）、成员一（已确认身份、学生、批次和双外键方案）、成员三（审核状态、事务和记录接口意见已吸收）、成员四（已确认欠费确认、代申请、补录和统计边界）
- 影响模块：全部模块
- 影响表：`application`、`application_operation_record`、申请详情表、礼包/库存/名额/补助额度表、`approval_record`、`arrears_confirmation` 及基础数据表
- 影响接口：学生申请、申请配置、跨模块申请状态写入、资源生命周期、欠费确认读取、代申请、补录和统计查询
- 影响状态：第一阶段状态与办结边界已经确认，使用现有状态枚举
- 变更内容：新增并修订 `docs/decisions/application-config.md`，整理成员二依赖成员一/三的能力、成员二向成员三/四提供的 Service、成员二 REST 接口、错误码及开发前阻塞项；已吸收成员三提交 `8dc667c` 的评审意见，统一状态接口、事务边界、资源幂等、补助额度调整、批量更新、补录自动审核、批次关联和办结规则。
- 使用者需要执行的操作：四名成员按照 `docs/requirement.md` 的第一阶段共识实现本人模块；不改变表所有权、接口方向、状态规则和事务边界的实现细节由模块负责人自行决定。
- 对应提交：待提交

## 2026-07-17｜新增成员四欠费最终确认记录表

- 状态：IMPLEMENTED
- 提出人：成员四
- 负责人：成员四
- 影响模块：成员二申请模块、成员三审核状态模块、成员四确认模块
- 影响表：新增 `arrears_confirmation`；只读取 `application`，不修改其结构
- 影响接口：成员二需提供待确认欠费申请读取能力；成员三需提供 `CONFIRM_PENDING -> COMPLETED` 状态流转能力
- 影响状态：使用既有 `CONFIRM_PENDING`、`COMPLETED`，不新增状态值
- 变更内容：新增确认金额快照、实际确认金额、确认人、确认时间、单据编号与逻辑删除字段；同一申请的有效确认记录唯一。
- 使用者需要执行的操作：成员二、三合入正式 Service 后，提供 `ArrearsConfirmationApplicationPort` 的实现并同步公共接口文档；成员四执行 `database/migrations/V20260717_001__create_arrears_confirmation.sql`。

# 共享结构变更记录

最新记录必须放在最上方。共享表、公共接口、公共状态或公共配置的变化，只有完成相关成员确认并按规范同步代码和文档后，才能从 `PROPOSED` 更新为 `IMPLEMENTED`。

## 2026-07-17｜数据库格式与迁移规范候选最终稿

- 状态：PROPOSED
- 提出人：成员三
- 负责人：仓库负责人（规范生效）、各表负责人（本人表实现）
- 影响模块：全部模块
- 影响表：全部业务表
- 影响接口：无直接接口变化；后续表结构变化可能影响 DTO/VO
- 影响状态：不修改现有状态枚举
- 变更内容：统一数据库字段命名、Java/MySQL 类型、公共审计字段、逻辑删除、乐观锁、申请关系、表所有权、索引、外键、SQL 基线和 migration 规则；明确历史 migration 不变但 `02_create_tables.sql` 持续更新。
- 使用者需要执行的操作：总负责人确认规范在文档权威顺序中的位置；四名成员确认类型、时间字段、外键和 migration 命名；确认前不据此修改他人表结构。
- 对应提交：待提交

## 2026-07-17｜审核模块公共契约与申请审核字段

- 状态：APPROVED
- 提出人：成员三
- 负责人：成员二（`application` 结构）、成员三（审核状态与审核接口）
- 影响模块：申请配置、三级审核、欠费确认与统计
- 影响表：`application`、`approval_record`、`approval_submission_record`、`system_message`、`message_read_record`
- 影响接口：申请首次提交、退回重提、三级审核、批量上报、取消、欠费确认完成、审核消息查询与已读
- 影响状态：全部申请状态、审核层级和审核动作
- 变更内容：`application` 统一采用 `status`、`current_level`、`review_round`、`version` 等审核字段；逐条通过只记录结论，首次批量上报时推进到下一节点；退回申请通过补交通道重新流转；所有跨表写入通过表负责人 Service 完成。四人第一阶段共识已确认字段方向、枚举、Service 边界、状态终点和事务发起方。
- 使用者需要执行的操作：成员三按照已批准契约实现本人表、状态机、审核接口、审核页面和消息；成员二实现 `application` 状态与资源 Service；成员一提供可信身份和数据范围；成员四通过成员三 Service 完成确认、代申请和补录流转。

- 对应提交：`14a9a9b`（四人第一阶段共识批准依据）
