# 成员二基础配置接口

更新日期：2026-07-20

本文件记录成员二可独立运行的费用和礼包基础配置后端接口。它们只读写成员二负责的 `fee_item`、`fee_amount_option`、`gift_item` 及其引用关系；不查询学生、组织、批次或审核数据。

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
