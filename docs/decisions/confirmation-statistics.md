# 成员四设计决策

## 6.1.1 欠费信息最终确认的表结构、金额和临时身份方案

- 日期：2026-07-17
- 负责人：成员四
- 涉及表：`arrears_confirmation`
- 涉及接口：`GET /api/confirm/list`、`GET /api/confirm/app/{applicationId}`、`POST /api/confirm/{applicationId}`
- 涉及文件：
  - `database/migrations/V20260717_001__create_arrears_confirmation.sql`
  - `docs/database-design.md`
  - `backend/src/main/java/com/example/backend/service/port/ArrearsConfirmationApplicationPort.java`
- 具体规定：
  1. 单据编号格式为 `GC + 确认年份 + 申请 ID 六位补零`。例如 2026 年确认申请 ID 为 1 时，编号为 `GC2026000001`。
  2. 实际确认金额必须大于 `0.00`，且不得超过学生申报金额；金额字段统一使用 `DECIMAL(12,2)` / Java `BigDecimal`。
  3. 同一申请仅允许一条有效确认记录，通过唯一约束 `(application_id, deleted)` 防止重复确认；确认请求必须携带 requestId，确认表以 request_id 唯一约束识别网络重试；并发下由数据库唯一约束作为最终防线。
  4. 成员一登录模块未合入前，学校管理员 ID 暂由 `X-User-Id` 请求头传递，仅用于后端联调。登录模块合入后，只替换 Controller 的当前用户获取方式。
  5. 成员四不直接查询或更新 `application`、`arrears_application`、`student`。读取待确认申请由成员二提供，状态 `CONFIRM_PENDING -> COMPLETED` 由成员三 Service 提供。未合入时，通过 `ObjectProvider` 延迟获取该集成能力；后端可以启动，但相关接口返回 503，不使用模拟数据或猜测字段。
- 做出原因：遵守表所有权、统一字段格式和状态流转权限，避免成员四根据未定字段编写跨模块 SQL。
- 对其他模块的影响：成员二、三需要在合入 main 后实现 `ArrearsConfirmationApplicationPort` 的读取与状态流转契约；状态流转实现必须加入成员四确认事务。
- 是否需要其他成员确认：需要。接口集成前由成员二、成员三确认并在公共接口文档中登记。

## 6.1.3 学校代申请：接口与跨模块契约

- 日期：2026-07-17
- 负责人：成员四
- 涉及表：成员四不直接写表；申请、明细、附件由成员二维护，审核记录与状态由成员三维护。
- 涉及接口：GET /api/school-proxy/students；POST /api/school-proxy/applications/drafts；POST /api/school-proxy/applications/{applicationId}/attachments；POST /api/school-proxy/applications/{applicationId}/submit。
- 具体规定：
  1. 仅学校管理员可调用；JWT 未合入时临时使用 X-User-Id。
  2. 创建草稿必须包含 studentNo、batchType=GREEN_CHANNEL、batchId、requestId；可填写 applicationReason、欠费项目列表（feeItemId + declaredAmount）和礼包物品列表（giftItemId + quantity）。
  3. 成员四先调用成员一学生查询能力确认学号存在，再调用成员二创建来源为 SCHOOL_PROXY 的 DRAFT；成员二负责重复校验和申请/明细物理写入。
  4. 附件以 multipart/form-data 上传，字段名 file，另带 requestId；成员二负责类型、大小、受控存储和附件表写入。
  5. 附件完成后才允许正式提交；成员二发起外层事务预占资源，成员三在同一事务写 SUBMIT 记录并推进为 COUNSELOR_PENDING。
  6. 创建、上传、提交均携带唯一 requestId。依赖实现未合入时返回 503，不写共享表、不伪造成功结果。
- 做出原因：满足 V2 的学校帮助学生申请、上传附件并进入普通三级审核，同时遵守表所有权。
- 对其他模块的影响：成员一实现学生查询 Port；成员二实现草稿、附件、提交 Port；成员三实现提交状态流转。
- 是否需要其他成员确认：需要；当前可先开发成员四 Controller、DTO、临时页面和 Port 调用外壳。

## 6.1.2 欠费单据：实现边界与依赖阻塞声明

- 日期：2026-07-18
- 负责人：成员四
- 涉及表：成员四直接读取 `arrears_confirmation`；学生、组织、申请和欠费项目数据仍归成员一、成员二所有。
- 涉及接口：`GET /api/arrears-vouchers`、`GET /api/arrears-vouchers/{voucherNo}`、`GET /api/arrears-vouchers/{voucherNo}/print`、`GET /api/student/arrears-vouchers/{voucherNo}`。
- 涉及文件：`ArrearsVoucherController`、`ArrearsVoucherServiceImpl`、`ArrearsVoucherApplicantQueryPort`、`ArrearsVoucherAccessPort`、`docs/collaboration-rules.md` 第 13 节。
- 具体规定：
  1. 成员四仅从确认表取得单据编号、申请 ID、金额和确认信息；不新建学生/申请的临时表，不跨模块写 Mapper，也不硬编码学生或欠费项目。
  2. 成员二必须实现 `findVoucherApplicantsByApplicationIds(Collection<Long>)`，返回学生身份、组织信息和欠费项目快照；成员一必须实现学校权限、学生本人归属及确认人姓名查询。
  3. 上述依赖任何一项未合入时，相关接口返回 503，前端只能展示“依赖未合入，当前不可验证”的空态或错误信息；不得返回字段残缺的单据，也不得以模拟数据声称单据可用。
  4. 当前已完成的是成员四自身的查询编排、路由、分页上限和打印请求触发；完整单据展示、权限校验、学生本人查看及真实数据联调均处于阻塞状态。
- 做出原因：欠费单据要求展示的数据分属三位成员；在依赖没有真实实现和测试数据前，越权访问或模拟数据会造成错误接口契约和后续合并冲突。
- 对其他模块的影响：成员一、成员二合入真实实现后，需提供实现类位置、调用方式和最小测试数据；成员四收到后只进行接入和联调。
- 是否需要其他成员确认：需要，成员一、成员二分别确认自己的 Port 实现后才能将 6.1.2 标记为 `IMPLEMENTED`。

## 6.1.5、6.1.6 统计与筛选：集合聚合边界

- 日期：2026-07-19
- 负责人：成员四
- 涉及表：成员四只读 `arrears_confirmation`；申请、组织、批次、欠费项目、礼包和欠费原因均由成员二/成员一维护。
- 涉及接口：`GET /api/statistics/applications/summary`、`ApplicationStatisticsQueryPort.queryFinalStatistics`。
- 具体规定：
  1. 仅统计有效且为 `APPROVED` 或 `COMPLETED` 的申请，取消、拒绝、退回、待审核、待确认全部排除。
  2. 成员二必须用一次集合聚合查询返回所有统计维度；成员四不得逐条读取申请后在内存汇总。
  3. 筛选字段、返回字段、人数和金额口径以 `collaboration-rules.md` 第 16 节为准；统计无数据时返回 `0.00` 或空数组，不返回模拟数据。
  4. 欠费原因采用成员二 `arrears_application.arrears_reason_code` 的固定枚举；字段未确认或统计/权限 Service 未合入时接口返回 503。
- 做出原因：统计跨越四类成员二表、成员一组织表和成员四确认表，只有集合聚合才能正确保证筛选口径与性能。
- 对其他模块的影响：成员一提供学校统计权限；成员二实现聚合查询、提供真实数据并确认欠费原因字段；成员四提供接口和前端展示。
- 是否需要其他成员确认：需要；当前状态为 `PROPOSED`，不以 Controller 可启动认定功能完成。
