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
  3. 同一申请仅允许一条有效确认记录，通过唯一约束 `(application_id, deleted)` 防止重复确认；并发下由数据库唯一约束作为最终防线。
  4. 成员一登录模块未合入前，学校管理员 ID 暂由 `X-User-Id` 请求头传递，仅用于后端联调。登录模块合入后，只替换 Controller 的当前用户获取方式。
  5. 成员四不直接查询或更新 `application`、`arrears_application`、`student`。读取待确认申请由成员二提供，状态 `CONFIRM_PENDING -> COMPLETED` 由成员三 Service 提供。未合入时，通过 `ObjectProvider` 延迟获取该集成能力；后端可以启动，但相关接口返回 503，不使用模拟数据或猜测字段。
- 做出原因：遵守表所有权、统一字段格式和状态流转权限，避免成员四根据未定字段编写跨模块 SQL。
- 对其他模块的影响：成员二、三需要在合入 main 后实现 `ArrearsConfirmationApplicationPort` 的读取与状态流转契约；状态流转实现必须加入成员四确认事务。
- 是否需要其他成员确认：需要。接口集成前由成员二、成员三确认并在公共接口文档中登记。
