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
  4. 学校管理员 ID 统一由成员一 `ICurrentUserProvider` 从 JWT 登录上下文取得；正式接口和前端不传递 `X-User-Id`。
  5. 成员四不直接查询或更新 `application`、`arrears_application`、`student`。读取待确认申请由成员二提供；成员三负责 `CONFIRM_PENDING -> COMPLETED`。演示前保证相关 Bean 已合入。
- 做出原因：遵守表所有权、统一字段格式和状态流转权限，避免成员四根据未定字段编写跨模块 SQL。
- 对其他模块的影响：成员二、三需要在合入 main 后实现 `ArrearsConfirmationApplicationPort` 的读取与状态流转契约；状态流转实现必须加入成员四确认事务。
- 是否需要其他成员确认：需要。接口集成前由成员二、成员三确认并在公共接口文档中登记。

## 6.1.3 学校代申请：接口与跨模块契约

- 日期：2026-07-17
- 负责人：成员四
- 涉及表：成员四不直接写表；申请、明细、附件由成员二维护，审核记录与状态由成员三维护。
- 涉及接口：GET /api/school-proxy/students；POST /api/school-proxy/applications/drafts；POST /api/school-proxy/applications/{applicationId}/attachments；POST /api/school-proxy/applications/{applicationId}/submit。
- 具体规定：
  1. 仅学校管理员可调用；当前 Controller 从 JWT 登录上下文取得用户身份，前端不传递用户 ID 或角色请求头。
  2. 创建草稿必须包含 studentNo、batchType=GREEN_CHANNEL、batchId、requestId；可填写 applicationReason、欠费项目列表（feeItemId + declaredAmount）和礼包物品列表（giftItemId + quantity）。
  3. 成员四先调用成员一学生查询能力确认学号存在，再调用成员二创建来源为 SCHOOL_PROXY 的 DRAFT；成员二负责重复校验和申请/明细物理写入。
  4. 附件以 multipart/form-data 上传，字段名 file，另带 requestId；成员二负责类型、大小、受控存储和附件表写入。
  5. 附件完成后才允许正式提交；成员二发起外层事务预占资源，成员三在同一事务写 SUBMIT 记录并推进为 COUNSELOR_PENDING。
  6. 创建、上传、提交均携带唯一 requestId。依赖实现未合入时不进行完整演示。
- 做出原因：满足 V2 的学校帮助学生申请、上传附件并进入普通三级审核，同时遵守表所有权。
- 对其他模块的影响：成员一实现学生查询 Port；成员二实现草稿、附件、提交 Port；成员三实现提交状态流转。
- 是否需要其他成员确认：需要；当前可先开发成员四 Controller、DTO、临时页面和 Port 调用外壳。

## 6.1.4 绿色通道线下补录：自动审核和完成边界

- 日期：2026-07-20
- 负责人：成员四
- 涉及表：成员四不新建补录表；成员二负责 `application`、欠费/礼包/补助明细、资源和操作记录，成员三负责 `approval_record`。
- 涉及接口：`GET /api/supplements/students`、`GET /api/supplements`、`GET /api/supplements/{applicationId}`、`POST /api/supplements`、`SupplementApplicationPort`、`SupplementCompletionPort`。
- 涉及文件：`SupplementApplicationController`、`SupplementApplicationServiceImpl`、补录 DTO/VO、两个补录 Port、`frontend/src/views/school/supplement/`、`docs/collaboration-rules.md` 第 17 节。
- 具体规定：
  1. 线下补录来源只能为 `SUPPLEMENT`；客户端只提交学号、申请类型、批次、真实业务明细、补录原因、线下办理时间和 requestId，不提交来源、状态、审核层级或操作人。
  2. `GREEN_CHANNEL` 由后端映射为绿色通道批次，至少有欠费或礼包一项；两种补助由后端映射为补助批次，只允许填写大于零的补助金额。
  3. `SupplementApplicationServiceImpl` 使用外层事务，直接调用成员二创建 `DRAFT` 及明细，再调用成员三写校级自动 `APPROVE` 记录并流转状态；成员一、二、三实现须在演示前合入。
  4. 含欠费时目标为 `CONFIRM_PENDING/CONFIRMATION`，不含欠费时目标为 `COMPLETED/SYSTEM`；成员四不增加额外异常包装。
  5. 历史记录只查询 `source=SUPPLEMENT`，支持学号、申请类型、批次 ID、最终状态筛选，每页最多 100 条；不返回模拟记录。
  6. Controller 和 Service 沿用项目视频中的字段 `@Autowired` 和分层写法；演示前保证成员二、三实现已提供。
- 做出原因：线下补录同时涉及申请、资源和审核三类共享数据，必须保持表所有权、状态机和事务原子性，不能由成员四另建临时表绕开其他模块。
- 对其他模块的影响：成员一实现学生/学校身份适配；成员二实现补录创建、详情和历史查询 Port；成员三实现补录自动审核 Port，并全部加入成员四外层事务。
- 是否需要其他成员确认：共享方向已由 `requirement.md` 批准；第 17 节的精确字段和适配方法需要成员一、二、三按负责人范围实现后联调。

## 6.1.2 欠费单据：实现边界与依赖阻塞声明

- 日期：2026-07-18
- 负责人：成员四
- 涉及表：成员四直接读取 `arrears_confirmation`；学生、组织、申请和欠费项目数据仍归成员一、成员二所有。
- 涉及接口：`GET /api/arrears-vouchers`、`GET /api/arrears-vouchers/{voucherNo}`、`GET /api/arrears-vouchers/{voucherNo}/print`、`GET /api/student/arrears-vouchers/{voucherNo}`。
- 涉及文件：`ArrearsVoucherController`、`ArrearsVoucherServiceImpl`、`ArrearsVoucherApplicantQueryPort`、`ArrearsVoucherAccessPort`、`docs/collaboration-rules.md` 第 13 节。
- 具体规定：
  1. 成员四仅从确认表取得单据编号、申请 ID、金额和确认信息；不新建学生/申请的临时表，不跨模块写 Mapper，也不硬编码学生或欠费项目。
  2. 成员二的 `findVoucherApplicantsByApplicationIds(Collection<Long>)`、成员一学生组织快照，以及成员四学校权限、学生本人归属和确认人姓名适配已经接线。
  3. 单据列表、详情、学生本人查看和打印均使用真实确认记录与批量申请快照，不返回模拟数据。
  4. 当前代码链已经完整；整体完成状态仍取决于前序流程产生真实 `COMPLETED` 确认记录并完成页面联调。
- 做出原因：欠费单据要求展示的数据分属三位成员；在依赖没有真实实现和测试数据前，越权访问或模拟数据会造成错误接口契约和后续合并冲突。
- 对其他模块的影响：成员一、成员二合入真实实现后，需提供实现类位置、调用方式和最小测试数据；成员四收到后只进行接入和联调。
- 是否需要其他成员确认：依赖 Bean 已存在；需用真实确认记录完成最终联调后再把 6.1.2 整体标记为 `IMPLEMENTED`。

## 学校取消申请：欠费单据检查与作废

- 日期：2026-07-22
- 负责人：成员四
- 涉及表：`arrears_confirmation`
- 涉及接口：成员三 `ArrearsDocumentService`
- 具体规定：
  1. 成员三取消 `APPROVED`、`CONFIRM_PENDING` 或 `COMPLETED` 申请时，只能通过成员四
     `ArrearsDocumentService` 查询和作废单据，不能直接修改 `arrears_confirmation`。
  2. 当前确认表没有线下领取、发放或履约字段，也没有对应的外部履约表；因此第一阶段
     `hasIrreversibleOfflineProcessing(applicationId)` 固定返回 `false`，不凭空阻止取消。
     后续履约模块合入后，应由其负责人提供真实不可逆状态判断。
  3. `voidDocumentForCancellation` 只处理 `deleted = 0` 的有效确认记录，将其 `deleted`
     更新为本行 `id`，即逻辑作废；不物理删除，也不新增单据状态或取消原因字段。
  4. 不存在单据或单据已作废时不更新任何数据，保证取消重试时幂等；方法使用
     `Propagation.MANDATORY` 加入成员三外层取消事务，更新失败必须使申请状态、资源、
     审核记录和消息全部回滚。
- 做出原因：成员三负责取消编排，成员四独占确认单据表。复用既有逻辑删除字段即可满足
  “作废但保留历史”的需求，避免未获批准地扩展确认表。

## 欠费原因统计与报表字段对齐

- 日期：2026-07-22；负责人：成员四。
- 统计和报表的欠费原因统一读取成员二的 `arrears_application.arrears_reason_code`，不再将 `fee_item.item_name` 误用为原因。报表保留欠费项目列，同时单独输出原因名称。
- 若一笔申请有多条欠费明细，已确认金额按各明细 `declared_amount` 占该申请欠费申报总额的比例归集到对应原因，防止一笔金额被多个原因重复统计。

## 6.1.5、6.1.6 统计与筛选：集合聚合边界

- 日期：2026-07-19
- 负责人：成员四
- 涉及表：成员四只读 `arrears_confirmation`；申请、组织、批次、欠费项目、礼包和欠费原因均由成员二/成员一维护。
- 涉及接口：`GET /api/statistics/applications/summary`、`ApplicationStatisticsQueryPort.queryFinalStatistics`。
- 具体规定：
  1. 仅统计有效且已经完成学校最终审核的 `APPROVED`、`CONFIRM_PENDING` 或 `COMPLETED` 申请，取消、拒绝、退回和待审核状态全部排除。
  2. `ApplicationStatisticsQueryPortAdapter` 使用集合聚合查询返回所有统计维度；不得逐条读取申请后在内存汇总。
  3. 筛选字段、返回字段、人数和金额口径以 `collaboration-rules.md` 第 16 节为准；统计无数据时返回 `0.00` 或空数组，不返回模拟数据。
  4. 欠费原因采用成员二 `arrears_application.arrears_reason_code` 的固定枚举，并同时统计人数和按明细申报比例分摊的确认金额。
- 做出原因：统计跨越四类成员二表、成员一组织表和成员四确认表，只有集合聚合才能正确保证筛选口径与性能。
- 对其他模块的影响：成员一提供学校统计权限；成员二实现聚合查询、提供真实数据并确认欠费原因字段；成员四提供接口和前端展示。
- 是否需要其他成员确认：需要；当前状态为 `PROPOSED`，不以 Controller 可启动认定功能完成。

## 6.1.7 报表明细、Excel 导出和打印

- 日期：2026-07-20
- 负责人：成员四
- 涉及表：不新增报表表；只读最终状态申请、学生组织、批次、欠费、礼包、补助和确认金额。
- 涉及接口：`GET /api/statistics/reports/details`、`GET /api/statistics/reports/history`、`GET /api/statistics/reports/print`、`GET /api/statistics/reports/export`、`StatisticsReportQueryPort`。
- 涉及文件：`StatisticsReportController`、`StatisticsReportServiceImpl`、统计报表 DTO/VO/字段枚举、`backend/pom.xml`、`docs/collaboration-rules.md` 第 18 节。
- 具体规定：
  1. 明细、历史、Excel 和打印使用完全相同的 `StatisticsReportQueryDTO`，复用 6.1.6 筛选口径，只读取 `APPROVED/CONFIRM_PENDING/COMPLETED`。
  2. 自定义列和排序均采用 `StatisticsReportColumn` 白名单；成员二必须把 key 显式映射到 SQL，禁止前端字段直接拼接 SQL。
  3. 成员二按分页返回 `StatisticsReportRowVO`；成员四动态选择字段并用 `LinkedHashMap` 维持响应、Excel、打印顺序一致。
  4. Excel 使用 `SXSSFWorkbook`，每批读取 500 行、内存窗口 100 行；演示版本不设置导出和打印总数拦截。
  5. Excel 是真正 xlsx，冻结表头并转义公式开头字符；下载响应 `no-store`，不在仓库或服务器长期保存导出文件。
  6. 沿用视频项目的字段 `@Autowired` 和 Controller 调 Service；删除自定义异常类和全局异常处理器，成员四不为报表建立跨模块 Mapper。
- 做出原因：统一查询条件和列白名单才能保证前端、Excel、打印口径一致，同时防止 SQL 注入、公式注入和大数据量内存溢出。
- 对其他模块的影响：成员一实现统计权限；成员二实现报表行分页 Port；公共 `pom.xml` 增加 POI 依赖。
- 是否需要其他成员确认：需要确认公共依赖、新 Port 和字段契约；成员四实现先保持本地，不提交或推送。
