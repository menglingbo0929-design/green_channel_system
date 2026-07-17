# 数据库设计

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
| `confirmed_at` | `DATETIME` | 否 | 最终确认时间 |
| `created_at` | `DATETIME` | 否 | 创建时间 |
| `updated_at` | `DATETIME` | 否 | 更新时间 |
| `deleted` | `BIGINT UNSIGNED` | 否 | 逻辑删除标记，0 表示有效；删除时写入本行 `id` |

约束和索引：

- `uk_arrears_confirmation_application_id_deleted(application_id, deleted)`：同一申请只能存在一条有效确认记录，作为重复确认的数据库最终防线；
- `uk_arrears_confirmation_voucher_no(voucher_no)`：单据编号唯一；
- `idx_arrears_confirmation_confirm_user_id(confirm_user_id)`：按确认人查询或审计时使用。

业务规则：

- 实际确认金额必须大于 `0.00`，且不得超过学生申报金额；
- 保存确认记录后，必须由成员三状态流转 Service 将申请从 `CONFIRM_PENDING` 流转为 `COMPLETED`；
- 若状态流转失败，确认记录必须随事务回滚；
- 欠费单据的查询、学生查看、打印属于 6.1.2，后续独立实现，不在本表增加展示状态字段。
