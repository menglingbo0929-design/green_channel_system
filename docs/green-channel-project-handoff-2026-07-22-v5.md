# 高校绿色通道系统功能对齐、真实能力盘点与四人 TODO v5

## 0. v5 增量结论（2026-07-22）

本版完整保留 v4 的 Service、Port、Adapter、Mapper、Controller、前端和数据库文件分类索引，并补入成员二更新合并后的真实运行结论。

1. 新生 Excel 导入失败的直接原因已经确认：`student.class_id` 为必填字段，而 `StudentImportServiceImpl` 在班级编码为空或查不到班级时仍继续插入学生。现在导入会先按“班级编码 + 学院 + 专业 + 年级 + 启用状态”定位班级，定位失败只跳过该行并返回明确原因，不会再产生 `Field 'class_id' doesn't have a default value`。
2. 基础数据导入只建立 `sys_user`、`sys_user_role` 和 `student`，不会凭空建立绿色通道申请。统计报表严格统计 `application` 中已经进入 `APPROVED`、`CONFIRM_PENDING`、`COMPLETED` 的正式申请，因此“导入了七名学生但统计只有一条申请”本身不是统计缺数；其他学生完成申请及审核后才应进入报表。
3. 已修复一处真实统计串行：旧 `database/04_test_data.sql` 直接向 `arrears_confirmation` 写入 1 至 100 的裸 `application_id`，真实生活补助申请复用 ID 后，被错误关联为欠费确认金额。旧确认样例已禁用；看板、历史批次和报表明细只读取“存在有效欠费申请且由学校管理员确认”的记录。
4. 已新增迁移 `database/migrations/V20260722_005__deactivate_invalid_arrears_confirmation_seed.sql`，将数据库中不满足真实关联条件的旧确认样例逻辑删除，释放正式欠费确认唯一键。没有自动迁移工具时，应在 ODC 选中 `green_channel` 后只执行该文件一次。
5. 页面出现多少统计记录由正式业务数据决定，不应为填满图表而把新生基础数据伪装成申请，也不应重新引入前端测试数组。

### v5 本次变更文件

- 导入校验：`backend/src/main/java/com/example/backend/service/impl/StudentImportServiceImpl.java`
- 看板聚合：`backend/src/main/java/com/example/backend/service/adapter/ApplicationStatisticsQueryPortAdapter.java`
- 报表明细：`backend/src/main/java/com/example/backend/service/adapter/StatisticsReportQueryPortAdapter.java`
- 旧测试数据入口：`database/04_test_data.sql`
- 存量清理迁移：`database/migrations/V20260722_005__deactivate_invalid_arrears_confirmation_seed.sql`
- 本交接文档：`docs/green-channel-project-handoff-2026-07-22-v5.md`


更新时间：2026-07-22  
核对分支：`feature/confirmation-statistics`  
代码基线：已同步 `origin/main` 后的当前本地工作区  
功能基线：本项目正式功能要求截图（新生信息入库、绿色通道入口与配置、申请、三级审核、欠费确认、流程查看、线下补录、统计）  
用途：以正式功能要求为准，重新识别全仓已有能力、真实缺口和四名成员各自 TODO；前端为了排版或演示自行增加的内容不反向生成后端任务。

> 本次只进行只读盘点并生成本文件，没有修改项目代码，没有删除任何文件或文件夹。项目内旧进度文档可能同时保留“未接入”和“已实现”的历史段落，结论必须以当前代码、Bean、Mapper 和 SQL 为准。

## 1. 判定规则

1. 正式功能要求高于前端示意页。示意页多出的卡片、按钮、筛选项或提示文案，不自动成为后端 TODO。
2. 接口名称或包路径不同不等于缺失。只要已经有可注入 Bean，并能通过适配器完成同一职责，就判为“已有”。
3. 只有接口文件、旧 TODO 注释或条件 Bean，而没有可注入实现，才判为“真实缺失”。
4. 数据为空不等于接口缺失。必须先判断是否存在满足业务状态的数据，例如欠费确认要求申请进入 `CONFIRM_PENDING`，统计要求申请已经学校审核通过或完成。
5. 不为前端冗余内容新增表、Mapper、Service 或接口。

## 2. 全仓真实结构盘点

当前后端不是按成员或功能整齐分包，而是同时存在以下四组路径：

| 层次 | 主要路径 | 实际职责 |
|---|---|---|
| 成员二申请域 | `application/domain`、`application/mapper`、`application/service`、`application/web` | 统一申请主表、欠费/礼包/补助明细、草稿、附件、正式提交、配置目录、资源预占与释放 |
| 成员三审核域 | `approval/api`、`approval/port`、`approval/service`、`approval/persistence`、`approval/web`、`approval/integration` | 三级审核、批量上报、状态流转、流程查询、消息、取消以及跨模块适配 |
| 成员一基础域 | `mapper`、`service/impl`、`web/controller`、`security`、`model/domain` | 用户与 JWT、学生与组织、导入、批次、数据范围、政策规则、当前用户 |
| 成员四业务编排 | `service/impl`、`service/adapter`、`service/port`、`web/controller`、统计/确认相关 DTO/VO | 欠费确认、单据、学校代申请、线下补录、统计、筛选、报表、打印 |

### 2.1 已存在的关键 Service / Port / Adapter

下列能力在旧文档中曾被写为“等待”或“缺失”，但当前代码中已经存在，不能重复实现：

| 能力 | 当前正式实现 |
|---|---|
| 当前登录用户 | `security.CurrentUserProvider implements ICurrentUserProvider`；JWT 已包含 `userId`、`studentId`、`collegeId` |
| 学生资料快照 | `StudentProfileQueryServiceImpl` |
| 学生数据范围 | `StudentScopeServiceImpl` + `ApprovalStudentScopeServiceAdapter` |
| 学校代申请学生查询 | `SchoolProxyStudentQueryServiceImpl implements SchoolProxyStudentQueryPort` |
| 组织结构查询 | `OrganizationQueryServiceImpl` |
| 批次查询 | `BatchQueryServiceImpl`、`ApprovalBatchQueryServiceImpl` |
| 政策读取 | `PolicyRuleQueryServiceImpl` |
| 申请创建、状态读写、详情 | `application.service.ApplicationService` 同时实现创建、状态查询、状态写入和详情 Service |
| 申请资源生命周期 | `ApplicationResourceServiceImpl implements ApprovalResourceService` |
| 成员二与成员三状态适配 | `approval.integration.ApplicationStateServiceAdapter` |
| 审核完成与流转 | `ApprovalWorkflowService implements ApprovalCompletionService` 等审核 API |
| 欠费确认申请读取 | `application.service.ArrearsConfirmationApplicationService` 及成员四读适配器 |
| 欠费单据访问 | `ArrearsVoucherAccessPortAdapter`、`ArrearsDocumentServiceImpl` |
| 统计权限 | `StatisticsAccessServiceImpl` / `StatisticsAccessPortAdapter` |
| 统计汇总真实查询 | `ApplicationStatisticsQueryPortAdapter` |
| 统计明细真实查询 | `StatisticsReportQueryPortAdapter` |
| 学校代申请写入 | `application.service.SchoolProxyApplicationService` 与成员四适配器 |
| 线下补录写入 | `application.service.SupplementApplicationService` 与成员四适配器 |
| 系统消息 | `DefaultSystemMessageService`、`SystemMessageController`、消息 Mapper |

### 2.2 已存在的主要 Mapper 与表

- 成员一：`UserMapper`、`RoleMapper`、`StudentMapper`、学院/专业/年级/班级 Mapper、辅导员学生关系 Mapper、绿色通道批次 Mapper、政策规则 Mapper。
- 成员二：`ApplicationMapper`、`ArrearsApplicationMapper`、`ApplicationCatalogMapper`、`ApplicationResourceMapper`、`ApplicationResourceConfigMapper`、`ApplicationOperationMapper`。
- 成员三：`ApprovalRecordMapper`、`ApprovalSubmissionRecordMapper`、`SystemMessageMapper`、`MessageReadRecordMapper`。
- 成员四：`ArrearsConfirmationMapper`，其余统计和跨域只读通过正式 Port/Adapter 或集合 SQL 完成，没有再建第二套申请表。

数据库已经包含学生与组织、批次、统一 `application`、欠费/礼包/补助明细、附件、资源配额、审核记录、消息、欠费确认等主链表。不能因为类位于不同包就再建同义表。

## 3. 只有两个确认缺失的跨模块查询 Bean

全仓搜索后，以下两个接口只有声明和调用方，没有任何实现类或 Spring Bean：

| 真正缺失的 Bean | 已有调用方 | 最小正确处理 |
|---|---|---|
| `approval.port.ApprovalApplicationQueryPort` | `DefaultApprovalWorkbenchQueryService` | 成员二基于现有 `application`、学生组织、明细和附件表提供一个读适配器，完成待审分页、按 ID 分页、详情、看板计数、范围内 ID 查询。不要再建申请表或第二套 Service。 |
| `approval.port.ApprovalSubmissionApplicationQueryService` | `ApprovalBatchSubmissionService` | 成员二基于现有 `ApplicationMapper` 增加或复用按 `batchType + batchId` 查询申请状态快照的方法，注册一个轻量适配 Bean。 |

其余成员三依赖的 `CurrentUserProvider`、`StudentScopeService`、`ApprovalBatchQueryService`、`ApplicationStateWriteService`、`ApplicationDetailService` 和 `ApprovalResourceService` 都已经有正式实现，不再列 TODO。

## 4. 按正式功能要求逐项对齐

### 4.1 新生信息入库

正式要求：学校导入姓名、学号、学院、专业、年级、班级等基础数据，并支持学生使用学号登录。

当前状态：

- 学生表、组织表、用户表已经存在。
- `StudentImportServiceImpl`、导入模板、批量导入、学生列表、新增、编辑、启停接口已经存在。
- 导入数据包含生源地贷款、校园地贷款、困难等级、资料完整标志，并可关联登录用户。

真实缺口：学生本人还没有“读取自己的资料并完成允许字段”的专用 GET/PUT 闭环；当前管理端按 ID 编辑不能替代学生本人完善资料。

结论：主体已完成，成员一只补本人资料闭环，不重建学生模块。

### 4.2 绿色通道入口

正式要求：学生完善资料后，根据生源地/校园地贷款情况判断是否允许进入，并展示相关政策。

当前状态：

- `GreenChannelEligibilityService` 已完成资格判断。
- `StudentProfileQueryServiceImpl` 已提供贷款和资料完整状态。
- JWT 已经携带当前学生 ID。

真实缺口：学生申请 Controller 和资格 Controller 仍读取 `X-Student-Id`、`X-User-Id`，没有使用现成 JWT 当前用户；学生资料闭环缺失也会导致 `PROFILE_INCOMPLETE`。

结论：资格逻辑已有，不再另写一个资格 Service；只需要成员一补资料闭环、成员二把学生申请入口切到 JWT。

### 4.3 绿色通道信息设置

正式要求：申请时间、学院提交截止时间、适用年级、资金来源、欠费项目及金额档位、可修改/重新开放、爱心礼包物品、库存和学院/年级名额。

当前已有：

- 批次开始、结束、学院截止时间、适用年级：`green_channel_batch` + `GreenChannelBatchServiceImpl`。
- 欠费项目和金额档位：`fee_item`、`fee_amount_option` + `ApplicationCatalogService`。
- 礼包批次库存与单人上限：`batch_gift_item`。
- 学院和年级礼包名额：`college_gift_quota`、`grade_gift_quota` + `ApplicationResourceConfigService`。
- 跨批次复用物品的基本结构：独立 `gift_item` 再关联 `batch_gift_item`。

真实缺口：

1. `green_channel_batch` 没有资金来源配置。
2. 当前批次状态接口只切换 `enabled`，没有真正维护 `DRAFT / OPEN / CLOSED`，因此“结束后修改并重新开放”没有闭环。
3. `gift_item` 只有名称与启用状态，缺正式要求中的图片、类型、尺寸、简介、单价、性别限制、是否必选等属性。
4. `policy_rule` 只有读取 Service，没有学校管理员维护自定义引导文案的写接口。

结论：已有时间、年级、欠费档位、库存和配额不重写；只补上面四项正式缺口。

### 4.4 学生主动申请与学校代申请

正式要求：欠费申请、礼包申请、附件、8000 元上限、按贷款情况自动确定欠费原因、礼包数量限制、提交辅导员审核、学校代申请走同一审核流程；完成后提示生活补助和路费补助入口。

当前已有：

- 统一申请草稿、欠费/礼包/补助明细、更新、删除、提交和状态版本控制。
- 欠费总额 8000 元上限已经在 `ApplicationService.replaceArrearsItems` 校验。
- 礼包批次、单人上限、库存、学院/年级名额的预占、确认和释放已经实现。
- 学生与学校代申请的 PDF/JPG/JPEG/PNG 私有附件写入已经实现。
- 学校代申请使用统一申请表并调用成员三 `submitInitial`，不是单独流程。

真实缺口：

1. 欠费原因目前仍由前端传 `arrearsReasonCode`；正式要求是根据学生贷款信息由后端确定，不能信任客户端自由选择。
2. 附件只有上传和元数据写入，没有受权限保护的读取/预览接口，无法完整满足图片预览。
3. 学生申请 Controller 仍信任开发请求头，需要改为 JWT。
4. `student_recommendation` 虽有表，但未发现生成、查询、已读和跳转闭环；“完成后提示生活补助/路费补助”尚未落地。
5. 礼包完整展示受 4.3 中 `gift_item` 元数据缺失影响。

### 4.5 三级审核

正式要求：辅导员、学院、学校三级审核；支持通过、退回修改、不通过；辅导员/学院按截止时间批量上报；审核人可修改除学生基础信息外的申请字段；学校通过后可取消。

当前已有：

- `ApprovalReviewService`、`ApprovalWorkflowService` 已实现三级状态机、通过/退回/拒绝、重新提交、学校取消。
- 审核记录、版本、轮次、消息、资源预占/确认/释放已经接入。
- 辅导员和学院批量上报编排、批次截止时间校验已经存在。
- 审核时辅导员可填写最终补助金额。

真实缺口：

1. 审核工作台和批量上报缺成员二的两个查询 Bean，见第 3 节；这正是页面显示空或 503 的主要结构性原因。
2. “审核人可修改非基础申请字段”当前只覆盖最终补助金额，没有统一的审核编辑入口。成员三负责角色/状态授权与审核记录，成员二负责对统一申请明细执行允许字段更新，不能由成员三直接写成员二表。

结论：成员三状态机本身不是 TODO；应补查询适配和受控编辑协作，不再建立另一套审核 Service。

### 4.6 欠费信息确认与欠费单据

正式要求：学校审核通过后录入实际欠费金额，最终确认后打印欠费单据。

当前状态：

- `ArrearsConfirmationServiceImpl`、`ArrearsConfirmationApplicationService`、`ArrearsConfirmationCompletionPortAdapter` 已形成确认事务。
- `arrears_confirmation` 保存申报金额快照、实际金额、确认人、时间和单据号。
- `ArrearsVoucherServiceImpl` 已提供学校查询、学生本人查询和打印数据。
- 申请必须进入 `CONFIRM_PENDING` 才会显示在待确认列表；确认后进入 `COMPLETED`。

真实缺口：从正式功能要求看，后端主体没有新的功能缺口。列表为空时先检查是否存在经过学校审核且含欠费明细的 `CONFIRM_PENDING` 申请，不应为零数据再造接口或模拟数据。

### 4.7 查看审核流程

正式要求：学生看本人、辅导员看所管学生、学院看本学院、学校看全校；显示进行到哪一步、退回原因、拒绝原因和通过后的线下提醒。

当前状态：

- 成员三已有流程查询、状态查询、工作台详情、审核记录和消息服务。
- 成员一已有当前用户和数据范围适配。
- 真正阻塞仍是工作台读取申请详情的 `ApprovalApplicationQueryPort` 未实现。

结论：流程模型与权限框架已有，成员二补查询适配后联调；不另建成员四流程接口。

### 4.8 绿色通道申请补录

正式要求：学校按学号定位学生，补录欠费或礼包申请，提交后自动识别为通过。

当前状态：

- `SupplementApplicationService`、`SupplementApplicationPortAdapter` 已支持绿色通道欠费/礼包补录，也支持生活/路费补助补录。
- 学生组织快照、真实统一申请写入和成员三完成流转都已经有 Bean。
- 正式结果为含欠费进入 `CONFIRM_PENDING`，不含欠费进入 `COMPLETED`，符合确认链。

真实缺口：成员四本地正在进行的学校代申请/补录 JWT 对齐仍需最小编译和单链验证后才能判定完成；不能继续使用可伪造 `X-User-Id`。

### 4.9 统计功能

正式要求：只统计最终审核通过或完成的数据；支持总人数/实报人数、欠费原因按人数和金额、礼包物品申请数量、列选择、批量筛选、历史记录、Excel 导出和打印。

当前已有：

- `ApplicationStatisticsQueryPortAdapter` 已按真实申请、学生、欠费原因、欠费确认和礼包表聚合。
- 已返回总申请人数、完成学生人数、各学院/年级人数、欠费原因人数和确认金额、礼包物品数量、历史批次。
- `StatisticsReportQueryPortAdapter` 已提供真实分页明细和固定排序白名单。
- `StatisticsReportServiceImpl`、`StatisticsReportController` 已提供列选择、历史、Excel 和打印数据。

结论：正式统计功能的后端能力已经存在。统计页面为空时，应检查数据库中是否存在 `APPROVED / CONFIRM_PENDING / COMPLETED` 的真实申请以及关联学生、欠费/礼包明细；不要根据空页面再新建统计接口。

## 5. 前端存在但不属于正式功能 TODO 的内容

以下内容不在本次正式功能要求中，不得据此要求后端新增 Service、Mapper 或表：

- “个人列方案”；正式要求只有自定义/选择导出列，不需要再保存一套个人方案。
- “学院覆盖数量”“历史批次数量”等额外统计卡片；已有历史记录查询即可，不要求单独指标接口。
- 学校业务处理页顶部“今日补录”“作废申请”等装饰性汇总卡片。
- 快速入口、首页图标、面包屑、重置按钮、说明文字等纯 UI 内容。
- 单独的“政策与说明页面”本身；正式要求是申请入口能展示且学校可维护政策文案，不等于必须新增一个独立后端业务模块。
- 前端自行增加的任意筛选项；只有批次、学院、专业、年级、班级、申请类型、申请状态、欠费项目、申请时间属于已经确认的统计筛选范围。

消息中心和未读角标已有真实模块，但它是审核通知配套能力，不应因为某张前端图多画了一张卡片而扩大业务范围。

## 6. 四名成员的准确 TODO

### 6.1 成员一 TODO

| 优先级 | TODO | 最小交付 |
|---|---|---|
| P0 | 学生本人资料完善闭环 | 基于 JWT `studentId` 提供本人资料 GET/PUT；禁止修改学号和组织主键；满足完整度后更新 `info_complete=1`。 |
| P0 | 绿色通道批次生命周期 | 在现有批次 Service 上正式维护 `DRAFT / OPEN / CLOSED`，支持结束后修改并重新开放；不能仅切换 `enabled`。 |
| P1 | 资金来源设置 | 作为批次配置维护中央财政、学校事业经费、社会捐赠等可选来源；使用批次关联结构，不复用申请来源 `application.source`。 |
| P1 | 政策引导文案维护 | 在现有 `policy_rule` 与 Mapper 上补学校管理员 CRUD/启停，不再新建政策表。 |

成员一不再承担：当前用户、学生范围、组织快照、批次读取、消息接收人的重新实现，这些能力已存在。

### 6.2 成员二 TODO

| 优先级 | TODO | 最小交付 |
|---|---|---|
| P0 | 审核工作台查询适配 | 实现 `ApprovalApplicationQueryPort` Bean，复用现有申请 Mapper/表完成分页、详情、看板、范围 ID 查询。 |
| P0 | 批量上报候选查询适配 | 实现 `ApprovalSubmissionApplicationQueryService` Bean，按批次返回申请状态快照。 |
| P0 | 学生申请可信身份 | `ApplicationController`、`GreenChannelEligibilityController` 改用 `ICurrentUserProvider` 的 `userId/studentId`，移除 `X-Student-Id`、`X-User-Id`。 |
| P0 | 审核允许字段写入口 | 提供受成员三授权调用的申请非基础字段更新 Service；成员二负责物理写入和版本控制，成员三负责角色、状态和审计。 |
| P1 | 欠费原因后端推导 | 根据成员一学生贷款信息生成固定原因码，不再接受客户端自由指定；同时保留固定枚举用于统计。 |
| P1 | 礼包完整物品属性 | 在现有 `gift_item` 上补图片、类型、尺寸、简介、单价、性别限制、是否必选等字段、DTO、Mapper 和配置接口。 |
| P1 | 附件读取/预览 | 在现有私有存储和 `application_attachment` 上补受身份与申请归属校验的读取接口。 |
| P1 | 补助推荐闭环 | 使用现有 `student_recommendation` 表，实现申请完成后的生活/路费补助推荐生成、查询、已读和前端跳转数据。 |

成员二不再承担：重新实现申请状态 Service、资源预占 Service、附件上传、学校代申请主表写入；这些能力已经存在。

### 6.3 成员三 TODO

| 优先级 | TODO | 最小交付 |
|---|---|---|
| P0 | 配合成员二查询 Bean 联调 | 在两个成员二 Bean 合入后验证辅导员/学院/学校待审、已审、详情和批量上报；成员三不直接读成员二表。 |
| P0 | 审核编辑授权与审计 | 为“修改非基础申请字段”定义各审核级允许字段、状态和角色校验，调用成员二写 Service，并把变更字段写入现有审核记录。 |

成员三的三级状态机、通过/退回/拒绝、重新提交、取消、流程查询、批量上报编排和消息服务均已存在，不列为重写任务。

### 6.4 成员四 TODO

| 优先级 | TODO | 最小交付 |
|---|---|---|
| 已完成（代码） | 学校代申请与补录统一 JWT | 两个 Controller 和前端 API 已使用统一 JWT 当前用户，不再依赖 `X-User-Id`；JDK 21 后端编译与前端生产构建通过。 |
| 待真实数据联调 | 两页面按正式数据状态联调 | 仍需用一条真实申请跑到 `CONFIRM_PENDING` 验证欠费确认，再跑到最终状态验证统计/报表；禁止用前端模拟数据掩盖前序状态缺失。 |
| 已完成（代码） | 前端删减冗余并补正式指标 | 已删除个人列方案、重复批次框和无正式需求卡片；补齐组织筛选、欠费原因人数/金额、礼包物品数量及真实空状态说明。 |

成员四不再承担：申请工作台查询 Bean、学生表/组织 Mapper、三级审核状态机、礼包资源账或申请主表的重复实现。

## 7. 当前主链的真实阻塞关系

| 链路 | 当前判断 | 真正责任 |
|---|---|---|
| 导入学生 → 学生完善资料 | 后半段未闭环 | 成员一本人资料 GET/PUT |
| 资料完成 → 资格判断 | 判断已有，入口身份未收口 | 成员一资料 + 成员二 JWT 接入 |
| 学生创建并提交申请 | 主体已有 | 成员二可信身份、自动原因、预览/推荐收尾 |
| 辅导员 → 学院 → 学校审核 | 状态机已有，真实工作台/批量候选读取断开 | 成员二两个查询 Bean；成员三负责调用与联调 |
| 学校通过 → 欠费确认 → 单据 | 后端主体已有 | 需要前序产生真实 `CONFIRM_PENDING` 数据 |
| 线下补录/代申请 | 主体已有 | 成员四统一 JWT 并验证 |
| 完成申请 → 统计/报表 | 查询已存在 | 需要前序真实终态数据，不是再造统计接口 |

## 8. 最短完成顺序

1. 成员二先实现两个真正缺失的查询 Bean，使三级审核工作台和批量上报读取真实申请。
2. 成员一补学生本人资料完善，并把批次状态真正改为 `DRAFT / OPEN / CLOSED` 生命周期。
3. 成员二将学生申请入口切换 JWT，并补自动欠费原因；先跑通一条欠费申请 happy path。
4. 成员三接入两个查询 Bean，跑“辅导员 → 学院 → 学校”三级审核。
5. 成员四验证“`CONFIRM_PENDING` → 确认 → `COMPLETED` → 单据 → 统计/报表”。
6. 主链跑通后再补资金来源、礼包完整属性、附件预览、推荐和审核编辑。

## 9. 本地工作区说明

- 本地已有若干未提交修改，其中包括成员四学校代申请/补录 JWT 对齐代码；本次没有继续改动、没有回退，也没有将其判定为已完成。
- 本机 `application.properties`、`vite.config.js` 中的 8083 配置属于个人环境，不应提交共享分支。
- 后续提交必须逐文件选择；不得用删除文件、硬重置或覆盖整个目录的方式解决冲突。
- 本 v4 位于桌面，不属于项目仓库，除非用户明确要求，否则不得提交。

## 10. 项目结构与文件分类索引（供最终目录整理直接使用）

本节记录截至 2026-07-22 的真实文件分布。后续整理目录时应先按本节确认文件职责和调用边界，不要重新仅凭文件名判断“重复”，也不要在没有完成引用检索和编译验证前删除文件。

### 10.1 后端顶层结构与职责

| 目录 | 当前职责 | 整理时的处理原则 |
|---|---|---|
| `backend/src/main/java/com/example/backend/application` | 成员二维护的申请领域：申请主表、资源配置、申请状态、申请提交和申请侧查询 | 申请数据和资源的事实来源；不要与审核域合并成一个包 |
| `backend/src/main/java/com/example/backend/approval` | 成员三维护的审核领域：三级审核、上报、取消、消息和审核工作台 | 只通过 Port/Adapter 读取或修改申请域，不直接侵入申请 Mapper |
| `backend/src/main/java/com/example/backend/model` | 成员一基础数据及成员四接口 DTO/VO：用户、学生、组织、批次、确认、代申请、补录、统计、单据 | 后续可按业务域拆包，但当前不要搬动类路径 |
| `backend/src/main/java/com/example/backend/mapper` | 基础数据和成员四自有表 Mapper | 与 `model/domain` 一一对应；申请表 Mapper 不应移入这里复制一份 |
| `backend/src/main/java/com/example/backend/service` | 基础数据 Service、成员四业务 Service、跨域 Port 与 Adapter | `service/port` 是成员四所需能力契约；`service/adapter` 是正式跨域桥接或历史适配器 |
| `backend/src/main/java/com/example/backend/web/controller` | 基础数据和成员四 REST Controller | 只负责 HTTP 参数与 Service 调用，不直接访问 Mapper |
| `backend/src/main/java/com/example/backend/security` | JWT、当前用户和鉴权过滤 | 全项目身份事实来源；禁止各模块继续自定义 `X-User-Id` |
| `backend/src/main/java/com/example/backend/config` | Spring Security、JWT 属性、验证码、密码迁移 | 只放全局配置，不放业务 Service |
| `backend/src/main/java/com/example/backend/common` | 统一响应和 MyBatis-Plus 配置 | 保持轻量，不把业务 DTO 塞入 common |

### 10.2 申请领域 `application` 文件索引

#### 领域对象与枚举

- `application/domain/Application.java`
- `application/domain/ApplicationSource.java`
- `application/domain/ApplicationStatus.java`
- `application/domain/ApplicationType.java`
- `application/domain/ApprovalLevel.java`
- `application/domain/BatchType.java`
- `application/domain/QuotaScope.java`

这些类是申请主数据、申请类型、状态与额度范围的申请域定义。整理时优先保留它们作为申请侧事实类型，不要因为审核域存在同名枚举就直接删除。

#### 对外 Port / Service 接口

- `application/port/ApplicationCreationService.java`：创建申请。
- `application/port/ApplicationDetailService.java`：申请详情读取。
- `application/port/ApplicationStateQueryService.java`：申请状态读取。
- `application/port/ApplicationStateWriteService.java`：申请状态写入。
- `application/port/ArrearsConfirmationApplicationPort.java`：欠费确认读取申请。
- `application/port/ArrearsVoucherApplicantQueryPort.java`：欠费单据读取申请人快照。
- `application/port/StudentOrganizationSnapshotQuery.java`：学生组织快照查询。

#### Service 实现

- `application/service/ApplicationCatalogService.java`
- `application/service/ApplicationResourceConfigService.java`
- `application/service/ApplicationResourceServiceImpl.java`
- `application/service/ApplicationService.java`
- `application/service/ArrearsConfirmationApplicationService.java`
- `application/service/GreenChannelEligibilityService.java`
- `application/service/SchoolProxyApplicationService.java`
- `application/service/StudentApplicationSubmissionService.java`
- `application/service/SupplementApplicationService.java`

其中 `SchoolProxyApplicationService` 和 `SupplementApplicationService` 已分别实现成员四的正式 Port，是当前应使用的正式 Bean；对应的旧 Adapter 只是兼容存根，见 10.7。

#### Mapper 与 SQL Provider

- `application/mapper/ApplicationCatalogMapper.java`
- `application/mapper/ApplicationMapper.java`
- `application/mapper/ApplicationOperationMapper.java`
- `application/mapper/ApplicationResourceConfigMapper.java`
- `application/mapper/ApplicationResourceMapper.java`
- `application/mapper/ArrearsApplicationMapper.java`
- `application/mapper/ArrearsApplicationSql.java`

#### Web Controller

- `application/web/ApplicationCatalogController.java`
- `application/web/ApplicationController.java`
- `application/web/ApplicationResourceConfigController.java`
- `application/web/GreenChannelEligibilityController.java`

#### 申请域 DTO / Snapshot

- 通用申请：`ApplicationDraftCommand`、`ApplicationStateSnapshot`、`ApplicationSummary`、`PageResult`。
- 欠费：`ArrearsItemCommand`、`ArrearsItemSnapshot`、`PendingArrearsApplication`、`PendingArrearsQuery`、`UpdateArrearsRequest`。
- 学生/单据快照：`ArrearsVoucherApplicantSnapshot`、`StudentOrganizationSnapshot`。
- 礼包：`BatchGiftItemCommand`、`BatchGiftItemView`、`GiftApplicationItemCommand`、`GiftApplicationItemSnapshot`、`GiftQuotaCommand`、`GiftQuotaView`、`UpdateBatchGiftItemCommand`、`UpdateGiftQuotaCommand`、`UpdateGiftRequest`。
- 目录/欠费金额：`CatalogItemCommand`、`CatalogItemView`、`FeeAmountOptionCommand`、`FeeAmountOptionView`。
- 补助：`SubsidyApplicationSnapshot`、`SubsidyQuotaCommand`、`SubsidyQuotaView`、`UpdateSubsidyQuotaCommand`、`UpdateSubsidyRequest`。
- 资格：`GreenChannelEligibility`。

### 10.3 审核领域 `approval` 文件索引

#### 对外 API

- 工作台与详情：`ApprovalWorkbenchQueryService`、`ApprovalDashboard`、`ApprovalListQuery`、`ApprovalListItem`、`ApprovalPage`、`ApprovalDetailView`。
- 流程查询：`ApprovalFlowQueryService`、`ApprovalFlowSnapshot`、`ApprovalRecordSnapshot`、`ApplicationStatusResult`。
- 审核流转：`ApprovalTransitionService`、`ApprovalCompletionService`。
- 批量上报：`ApprovalSubmissionService`、`ApprovalSubmissionResult`、`ApprovalSubmissionStatus`。
- 消息：`SystemMessageService`、`SystemMessageItem`、`SystemMessagePage`。
- 统计投影：`ApprovalCollegeCount`、`ApprovalDecisionCount`、`ApprovalFunnelCount`、`ApprovalLevelCount`。

#### 审核域内部 Port

- 申请读取：`ApprovalApplicationQueryPort`、`ApprovalApplicationSnapshot`、`ApprovalApplicationDetail`。
- 批量候选读取：`ApprovalSubmissionApplicationQueryService`。
- 申请状态桥：`ApplicationStateQueryService`、`ApplicationStateWriteService`、`ApplicationStateSnapshot`。
- 批次与资源：`ApprovalBatchQueryService`、`ApprovalResourceService`。
- 身份与范围：`CurrentUserProvider`、`LoginUser`、`UserRole`、`StudentScopeService`、`ApprovalWorkScope`。
- 消息接收人：`ApprovalMessageRecipientResolver`。
- 欠费单据取消：`ArrearsDocumentService`。

目前只有 `ApprovalApplicationQueryPort` 和 `ApprovalSubmissionApplicationQueryService` 没有可注入实现；其余 Port 不能再根据名字重复新建。

#### 正式跨域 Adapter

- `approval/integration/ApplicationStateServiceAdapter.java`：审核域调用申请域状态接口。
- `approval/integration/ApprovalCurrentUserProviderAdapter.java`：审核域调用统一 JWT 当前用户。
- `approval/integration/ApprovalStudentScopeServiceAdapter.java`：审核域调用学生范围服务。
- `approval/integration/ArrearsConfirmationCompletionPortAdapter.java`：欠费确认完成后调用审核完成能力。
- `approval/integration/SupplementCompletionPortAdapter.java`：补录完成后调用审核完成能力。

这些 Adapter 是明确的域间边界，不属于“重复类”，整理时应保留。

#### Service、持久层与 Controller

- Service：`ApprovalBatchSubmissionService`、`ApprovalCancellationService`、`ApprovalReviewService`、`ApprovalWorkflowService`、`DefaultApprovalWorkbenchQueryService`、`DefaultSystemMessageService`、`ApprovalServiceConfiguration`。
- Entity：`ApprovalRecordEntity`、`ApprovalSubmissionRecordEntity`、`MessageReadRecordEntity`、`SystemMessageEntity`。
- Mapper：`ApprovalRecordMapper`、`ApprovalSubmissionRecordMapper`、`MessageReadRecordMapper`、`SystemMessageMapper`。
- Controller：`ApprovalController`、`ApprovalSubmissionController`、`SystemMessageController`。
- 异常入口：`ApprovalExceptionHandler`、`ApprovalIntegrationUnavailableException`。
- 审核域核心类型：`ApprovalAction`、`ApprovalErrorCode`、`ApprovalException`、`ApprovalStateException`、`ApprovalStateMachine`、`ApprovalTransition`，以及 persistence/type 下的批次、消息和上报枚举。

### 10.4 基础数据和成员四通用后端索引

#### `model/domain` 与对应 Mapper

| 领域对象 | Mapper |
|---|---|
| `ArrearsConfirmation` | `ArrearsConfirmationMapper` |
| `BatchEligibleGrade` | `BatchEligibleGradeMapper` |
| `ClassInfo` | `ClassInfoMapper` |
| `College` | `CollegeMapper` |
| `CounselorStudent` | `CounselorStudentMapper` |
| `Grade` | `GradeMapper` |
| `GreenChannelBatch` | `GreenChannelBatchMapper` |
| `Major` | `MajorMapper` |
| `PolicyRule` | `PolicyRuleMapper` |
| `Role` | `RoleMapper` |
| `Student` | `StudentMapper` |
| `SubsidyBatch` | `SubsidyBatchMapper` |
| `User` | `UserMapper` |
| `UserRole` | `UserRoleMapper` |

#### Service 接口

- 基础数据：`IUserService`、`StudentProfileQueryService`、`StudentImportService`、`OrganizationQueryService`、`StudentScopeService`、`CounselorStudentService`、`PolicyRuleQueryService`。
- 批次：`GreenChannelBatchService`、`BatchQueryService`。
- 成员四业务：`IArrearsConfirmationService`、`IArrearsVoucherService`、`ISchoolProxyApplicationService`、`ISupplementApplicationService`、`IApplicationStatisticsService`、`IStatisticsReportService`。

#### Service 实现

- 基础数据：`UserServiceImpl`、`StudentProfileQueryServiceImpl`、`StudentImportServiceImpl`、`OrganizationQueryServiceImpl`、`StudentScopeServiceImpl`、`CounselorStudentServiceImpl`、`PolicyRuleQueryServiceImpl`。
- 批次：`GreenChannelBatchServiceImpl`、`BatchQueryServiceImpl`、`ApprovalBatchQueryServiceImpl`。
- 成员四业务：`ArrearsConfirmationServiceImpl`、`ArrearsVoucherServiceImpl`、`SchoolProxyApplicationServiceImpl`、`SupplementApplicationServiceImpl`、`ApplicationStatisticsServiceImpl`、`StatisticsReportServiceImpl`。
- 跨域查询：`SchoolProxyStudentQueryServiceImpl`、`StatisticsAccessServiceImpl`、`ApprovalMessageRecipientResolverImpl`、`ArrearsDocumentServiceImpl`。

#### 成员四 Port

- `ApplicationStatisticsQueryPort`
- `ArrearsConfirmationApplicationPort`
- `ArrearsConfirmationCompletionPort`
- `ArrearsVoucherAccessPort`
- `ArrearsVoucherApplicantQueryPort`
- `SchoolProxyApplicationPort`
- `SchoolProxyStudentQueryPort`
- `StatisticsAccessPort`
- `StatisticsReportQueryPort`
- `SupplementApplicationPort`
- `SupplementCompletionPort`

#### 正式成员四 Adapter

- `ApplicationReadPortAdapter`：把成员二 Application 能力适配到成员四读取 Port。
- `ApplicationStatisticsQueryPortAdapter`：从真实申请、学生、欠费和礼包数据聚合统计。
- `ArrearsVoucherAccessPortAdapter`：单据访问范围与申请归属校验。
- `StatisticsReportQueryPortAdapter`：统计明细、筛选与导出数据查询。
- `StudentOrganizationSnapshotQueryAdapter`：学生组织快照适配。

#### Controller

- 基础数据：`UserController`、`StudentController`、`OrganizationController`、`GreenChannelBatchController`、`VerificationCodeController`。
- 成员四业务：`ArrearsConfirmationController`、`ArrearsVoucherController`、`SchoolProxyApplicationController`、`SupplementApplicationController`、`ApplicationStatisticsController`、`StatisticsReportController`。

#### 成员四 DTO / VO

- 欠费确认：`ArrearsConfirmationQueryDTO`、`ConfirmArrearsDTO`、`PendingArrearsApplicationVO`、`ConfirmResultVO`。
- 学校代申请：`SchoolProxyArrearsItemDTO`、`SchoolProxyDraftDTO`、`SchoolProxyGiftItemDTO`、`SchoolProxyApplicationVO`、`SchoolProxyStudentVO`。
- 线下补录：`SupplementCreateDTO`、`SupplementQueryDTO`、`SupplementApplicationVO`、`SupplementCompletionResultVO`。
- 欠费单据：`ArrearsVoucherQueryDTO`、`ArrearsVoucherApplicantSnapshot`、`ArrearsVoucherItemVO`、`ArrearsVoucherVO`。
- 统计：`StatisticsFilterDTO`、`StatisticsReportQueryDTO`、`StatisticsReportColumn`、`ApplicationStatisticsVO`、`ArrearsReasonStatisticsVO`、`BatchHistoryStatisticsVO`、`CollegeApplicantCountVO`、`GradeApplicantCountVO`、`GiftItemApplicationCountVO`、`StatisticsExportFileVO`、`StatisticsReportColumnVO`、`StatisticsReportPageVO`、`StatisticsReportPrintVO`、`StatisticsReportRowVO`。

### 10.5 安全、公共与配置文件索引

- JWT 与身份：`security/JwtTokenProvider.java`、`security/JwtAuthenticationFilter.java`、`security/ICurrentUserProvider.java`、`security/CurrentUserProvider.java`。
- 全局配置：`config/SecurityConfig.java`、`config/JwtProperties.java`、`config/VerificationCodeStore.java`、`config/PasswordMigrationRunner.java`。
- 公共响应/分页：`common/JsonResponse.java`、`model/dto/PageDTO.java`。
- MyBatis-Plus：`common/config/MybatisPlusConfig.java`。
- 登录与用户 DTO：`LoginRequest`、`LoginByCodeRequest`、`LoginResponse`、`LoginUser`、`PasswordChangeRequest`、`CreateUserRequest`、`UpdateUserRequest`、`UserVO`。

`security.ICurrentUserProvider` 与 `approval.port.CurrentUserProvider` 是两个域的契约，由 `ApprovalCurrentUserProviderAdapter` 桥接；它们不是可以直接删除的重复接口。

### 10.6 前端结构索引

#### API 文件

| 文件 | 业务 |
|---|---|
| `frontend/src/api/application.js` | 学生申请、申请明细与资源配置 |
| `frontend/src/api/approval.js` | 三级审核、流程、上报和消息 |
| `frontend/src/api/confirmation.js` | 欠费待确认、详情与最终确认 |
| `frontend/src/api/schoolProxy.js` | 学校代申请 |
| `frontend/src/api/supplement.js` | 线下补录与历史详情 |
| `frontend/src/api/voucher.js` | 欠费单据查询、打印和学生查看 |
| `frontend/src/api/statistics.js` | 统计汇总、报表、历史、Excel 和打印 |
| `frontend/src/api/memberFourClient.js` | 成员四页面统一 HTTP 客户端/响应解包 |
| `frontend/src/api/index.js` | 全局 API 客户端入口 |

#### 核心 View

- 公共页面：`Login.vue`、`Layout.vue`、`Home.vue`、`BaseData.vue`、`ApplicationConfig.vue`、`Error403.vue`、`Error404.vue`。
- 学生申请：`StudentApplicationCenter.vue`、`approval/MyApplications.vue`、`student/confirmation/MyArrearsVoucher.vue`。
- 三级审核：`approval/ApprovalWorkbench.vue`、`approval/MessageCenter.vue`。
- 学校业务总页：`school/SchoolBusinessProcessing.vue`。
- 欠费确认/单据：`school/confirmation/ArrearsConfirmationList.vue`、`school/confirmation/ArrearsVoucher.vue`。
- 学校代申请/补录：`school/supplement/SchoolProxyApplication.vue`、`SupplementApplication.vue`、`SupplementHistory.vue`。
- 统计：`school/statistics/StatisticsDashboard.vue`、`ApplicationStatistics.vue`、`StatisticsReport.vue`。

当前路由正式入口是 `/member4/school-business` 和 `/member4/statistics-dashboard`。欠费、补录和统计旧路由通过 redirect 指向这两个入口，因此在整理前端目录前必须先检查 `router/index.js` 和 `stores/user.js`，不能只看 Vue 文件是否被直接引用。

#### 可复用 Component

- 申请：`AllocationDialog.vue`、`SubmitConfirmDialog.vue`。
- 审核：`ApprovalDetailDrawer.vue`、`ReviewDialog.vue`、`StatusBadge.vue`。
- 学校业务：`SchoolWorkspaceShell.vue`、`BusinessConfirmDialog.vue`。
- 公共：`FormDialog.vue`；`HelloWorld.vue` 属于脚手架遗留候选，但删除前仍需做引用检索。

### 10.7 已识别的历史适配器与同名边界

以下文件带 `@Deprecated(forRemoval = false)` 且当前不注册 Spring Bean，是为旧调用保留的兼容类：

- `service/adapter/ApprovalCompletionPortAdapter.java`
- `service/adapter/SchoolProxyApplicationPortAdapter.java`
- `service/adapter/SchoolProxyStudentQueryPortAdapter.java`
- `service/adapter/StatisticsAccessPortAdapter.java`
- `service/adapter/SupplementApplicationPortAdapter.java`

正式替代关系：

| 历史文件 | 当前正式能力 |
|---|---|
| `ApprovalCompletionPortAdapter` | `approval.integration.ArrearsConfirmationCompletionPortAdapter` 与 `SupplementCompletionPortAdapter` |
| `SchoolProxyApplicationPortAdapter` | `application.service.SchoolProxyApplicationService` |
| `SchoolProxyStudentQueryPortAdapter` | `service.impl.SchoolProxyStudentQueryServiceImpl` |
| `StatisticsAccessPortAdapter` | `service.impl.StatisticsAccessServiceImpl` |
| `SupplementApplicationPortAdapter` | `application.service.SupplementApplicationService` |

它们是最终结构整理时的“候选归档/删除项”，不是当前开发阶段可以直接删除的文件。必须先确认无 import、无反射加载、无测试依赖，再由全组统一清理。

下列同名或近似接口属于有意保留的跨域边界：

- `application.port.ApplicationStateQueryService/WriteService` 与 `approval.port.ApplicationStateQueryService/WriteService`：由 `ApplicationStateServiceAdapter` 桥接。
- `security.ICurrentUserProvider` 与 `approval.port.CurrentUserProvider`：由 `ApprovalCurrentUserProviderAdapter` 桥接。
- 申请域 `ApplicationStatus/ApplicationType/ApprovalLevel` 与审核域同名类型：当前分别服务于申请持久化和审核状态机，整理时应通过转换器收口，不能先删任意一侧。
- `application.domain.BatchType` 与 `approval.persistence.type.BatchType`：分别属于申请批次和审核上报持久化；是否合并必须在数据库枚举值完全核对后决定。

### 10.8 数据库文件索引与执行边界

- `database/01_create_database.sql`：创建并选择 `green_channel` 数据库。
- `database/02_create_tables.sql`：当前统一建表入口；成员更新应合入此文件，不再恢复或新增旧命名 `02_create_database.sql`。
- `database/03_init_data.sql`：角色、演示账号和基础初始化数据。
- `database/04_test_data.sql`：本地开发验证数据。
- `database/04_create_database.sql`：旧命名的本地测试数据脚本；与 `04_test_data.sql` 内容重叠，是最终整理候选，当前不要直接删除。
- `database/migrations/V20260717_001__create_arrears_confirmation.sql`：欠费确认表迁移。
- `database/migrations/V20260720_001__create_member2_application_tables.sql`：成员二申请域表迁移。
- `database/migrations/V20260720_002__create_approval_and_message_tables.sql`：审核与消息表迁移。
- `database/migrations/V20260721_001__add_member2_supplement_and_arrears_reason.sql`：补录与欠费原因补充迁移。

开发环境首次建库使用 `01 → 02 → 03`，需要演示数据时再执行 `04_test_data.sql`。migration 用于已存在数据库的增量升级，不应和完整建表脚本在同一个空库上无判断地重复执行。

### 10.9 文档分类索引

- 总规范：`requirement.md`、`division.md`、`development-guide.md`、`collaboration-rules.md`、`member-code-contracts.md`。
- 接口/状态：`api-document.md`、`status-flow.md`。
- 数据库：`database-design.md`、`database-format-standard.md`。
- 变更记录：`change-log.md`。
- 成员二：`member2-catalog-api.md`、`member2-frontend-interaction.md`、`member2-integration-handoff.md`、`member2-progress.md`。
- 成员三：`application-config-member3-review.md`、`member3-approval-submission-dependencies.md`、`member3-approval-workbench-dependencies.md`、`member3-cancellation-dependencies.md`、`member3-frontend-real-api-dependencies.md`、`member3-session-handoff-2026-07-17.md`、`member3-session-handoff-2026-07-20.md`、`member3-task-d-integration.md`。
- 已确认决策：`decisions/application-config.md`、`decisions/approval-flow.md`、`decisions/base-user-batch.md`、`decisions/confirmation-statistics.md`。

最终整理时，`decisions` 应保留为不可随意改写的已确认契约；成员 session handoff 可按日期归档；`collaboration-rules.md` 继续作为新增接口前必须读取的公共规则。

### 10.10 最终结构整理建议顺序

1. 先冻结接口和数据库字段，生成 Controller → Service → Port → Adapter → Mapper → 表的引用清单。
2. 再处理确认无 Bean、无引用的 `@Deprecated` 历史适配器；一次只处理一组并编译。
3. 收口前端正式路由入口，再判断被 redirect 替代的 View 是否仍被组件嵌套使用。
4. 统一 `I*Service` 与非 `I` 命名、DTO/VO 包位置和异常包位置，但不要同时改包路径与业务逻辑。
5. 最后处理重复枚举和旧数据库测试脚本；需要数据库值对照和完整迁移记录，不能通过文件名判断。
6. 每个整理提交只做一种机械调整，禁止夹带业务功能修改，也禁止用整目录覆盖或删除来解决冲突。
