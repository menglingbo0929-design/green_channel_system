# 成员二基础配置接口

更新日期：2026-07-21

本文件记录成员二可独立运行的费用、礼包与资源配置后端接口。它们只读写成员二负责的配置、库存和配额表；批次、学院、年级仅通过成员一公开 Service 查询和校验，不直接访问成员一的数据表。

> 当前项目尚未接入成员一的可信身份上下文。因此这些接口的管理员鉴权不是已交付能力；在接入前仅限开发环境使用。

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/fee-items?includeDisabled=false` | 查询欠费项目 |
| `POST` | `/api/fee-items` | 新增欠费项目 |
| `PUT` | `/api/fee-items/{id}` | 修改名称或启用状态 |
| `DELETE` | `/api/fee-items/{id}` | 逻辑删除未被申请引用的项目 |
| `GET` | `/api/fee-amount-options?feeItemId={id}&includeDisabled=false` | 查询金额档位 |
| `POST` | `/api/fee-amount-options` | 新增金额档位 |
| `PUT` | `/api/fee-amount-options/{id}` | 修改金额档位 |
| `DELETE` | `/api/fee-amount-options/{id}` | 逻辑删除金额档位 |
| `GET` | `/api/gift-items?includeDisabled=false` | 查询礼包物品 |
| `POST` | `/api/gift-items` | 新增礼包物品 |
| `PUT` | `/api/gift-items/{id}` | 修改名称或启用状态 |
| `DELETE` | `/api/gift-items/{id}` | 逻辑删除未被批次配置引用的物品 |
| `GET` | `/api/application-resources/batch-gift-items?batchId={id}` | 查询批次礼包库存和单人上限 |
| `POST` | `/api/application-resources/batch-gift-items` | 新增批次礼包物品关联 |
| `PUT` / `DELETE` | `/api/application-resources/batch-gift-items/{id}` | 乐观锁更新 / 删除未使用的关联 |
| `GET` | `/api/application-resources/gift-quotas?batchId={id}&scope=COLLEGE|GRADE` | 查询学院或年级礼包名额 |
| `POST` | `/api/application-resources/gift-quotas` | 新增礼包名额分配 |
| `PUT` / `DELETE` | `/api/application-resources/gift-quotas/{id}?scope=...` | 乐观锁更新 / 删除未占用名额 |
| `GET` | `/api/application-resources/subsidy-quotas?batchId={id}&scope=COLLEGE|GRADE` | 查询学院或年级补助额度 |
| `POST` | `/api/application-resources/subsidy-quotas` | 新增补助额度分配 |
| `PUT` / `DELETE` | `/api/application-resources/subsidy-quotas/{id}?scope=...` | 乐观锁更新 / 删除未占用额度 |
| `GET` | `/api/application-resources/colleges`、`/grades` | 成员一 Service 提供的启用学院/年级选项 |

## 请求与校验

新增或修改欠费项目、礼包物品：

```json
{
  "name": "学费",
  "enabled": true
}
```

新增或修改金额档位：

```json
{
  "feeItemId": 1,
  "amount": 1200.00,
  "enabled": true
}
```

- 名称会去除首尾空白，活动记录中不可重复。
- 金额必须大于 `0.00`；同一欠费项目下的活动金额不可重复。
- 已停用的欠费项目不能新增或迁入金额档位。
- 已被 `arrears_application` 使用的欠费项目不可删除；已被 `batch_gift_item` 使用的礼包物品不可删除。
- `includeDisabled=false` 时只返回启用记录。

批次礼包物品请求：

```json
{ "batchId": 1, "giftItemId": 2, "stockTotal": 100, "perStudentLimit": 1 }
```

礼包名额请求（补助额度将 `quotaTotal` 改为金额字段 `quotaAmount`）：

```json
{ "batchId": 1, "scope": "COLLEGE", "targetId": 8, "quotaTotal": 30 }
```

- 更新库存、名额或额度必须携带当前 `version`，且不能低于已预占量。
- 删除库存关联前不能有礼包申请明细；删除配额前不得有预占或已使用量。
- 年级维度还会通过成员一 `isGradeEligible(batchId, gradeId)` 校验其属于该批次。
- 当前成员一仅提供单批次查询 Service，管理端以显式 `batchId` 加载资源；批次下拉列表需待成员一提供列表 Service 后接入。

## 错误码

| 错误码 | HTTP 状态 | 含义 |
|---|---:|---|
| `FEE_ITEM_NOT_FOUND` | 404 | 欠费项目不存在或已删除 |
| `FEE_ITEM_NAME_EXISTS` | 409 | 欠费项目名称重复 |
| `FEE_ITEM_DISABLED` | 400 | 已停用的欠费项目不能配置金额档位 |
| `FEE_ITEM_IN_USE` | 409 | 欠费项目已被申请使用 |
| `FEE_AMOUNT_OPTION_NOT_FOUND` | 404 | 金额档位不存在或已删除 |
| `FEE_AMOUNT_OPTION_EXISTS` | 409 | 同项目的金额档位重复 |
| `GIFT_ITEM_NOT_FOUND` | 404 | 礼包物品不存在或已删除 |
| `GIFT_ITEM_NAME_EXISTS` | 409 | 礼包物品名称重复 |
| `GIFT_ITEM_IN_USE` | 409 | 礼包物品已在批次配置中使用 |
| `BATCH_NOT_FOUND` | 400 | 批次不存在或不可用 |
| `BATCH_GIFT_ITEM_EXISTS` / `BATCH_GIFT_ITEM_VERSION_CONFLICT` | 409 | 批次物品重复，或库存/版本冲突 |
| `GIFT_QUOTA_EXISTS` / `GIFT_QUOTA_VERSION_CONFLICT` | 409 | 礼包名额重复，或名额/版本冲突 |
| `SUBSIDY_QUOTA_EXISTS` / `SUBSIDY_QUOTA_VERSION_CONFLICT` | 409 | 补助额度重复，或额度/版本冲突 |
| `QUOTA_TARGET_NOT_FOUND` / `GRADE_NOT_ELIGIBLE_FOR_BATCH` | 400 | 分配对象无效，或年级不适用于批次 |
