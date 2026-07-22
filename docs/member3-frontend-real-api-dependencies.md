# 成员三前端真实接口联调依赖

更新日期：2026-07-22

## 启用方式

前端默认使用状态化 Mock，以便页面开发和演示不依赖后端启动。真实联调时在 `frontend/.env.local` 配置：

```dotenv
VITE_USE_MOCK=false
# 前后端分离部署时再配置：VITE_API_BASE_URL=http://localhost:8080/api
```

审批页面会使用与全站一致的 `localStorage.token` 作为 Bearer Token；旧的
`green-channel-token` 仅兼容已有本地会话。不得为当前申请人临时补传
`X-Student-Id`、`X-User-Id` 等身份请求头。

## 成员一依赖

1. `CurrentUserProvider` 必须是可注入 Spring Bean，并从可信登录上下文返回用户、角色、学生和学院范围。
2. `StudentScopeService` 必须实现辅导员负责学生校验和学院学生范围校验。
3. 批量上报需提供可注入的批次查询能力，至少校验 `batchType + batchId`、批次状态和截止时间。

未满足时，审批后端应按现有约定返回 `503` 和
`APPROVAL_INTEGRATION_UNAVAILABLE`，前端会展示后端返回的可读原因。

## 成员二依赖

1. `GET /api/applications/mine`：改为基于可信登录身份识别学生，不能继续要求前端传 `X-Student-Id`。目前该接口返回摘要列表，前端已在本地完成分类、关键字筛选与分页；后续可升级为后端分页而不破坏页面。
2. 审核列表、已审列表、工作台和详情需提供真实数据：
   `GET /api/approvals/pending`、`/processed`、`/dashboard`、`/{applicationId}`。
   返回字段遵循 `docs/api-document.md` 的分页和详情契约。
3. 逐条审核、批量上报、退回补交和取消流程依赖申请状态写入、详情读取及资源预留/释放 Service；这些能力须由成员二拥有的 `application` 和资源模块实现，成员三不直接写其表。

## 成员四依赖

学校取消申请时，成员四需提供关联欠费单据是否可取消以及作废单据的 Service；其结果与成员二资源释放、成员三审核记录和状态变化必须处于同一外层事务。

## 成员三后端实现状态

截至 2026-07-21，当前工作树已经完成以下成员三业务编排，不再属于“待实现接口”：

- 批量上报：状态查询、辅导员/学院首次上报、退回补交；
- 审核查询：待审核、已审核、详情和工作台统计；
- 学校取消；
- 欠费确认完成、线下补录自动审核和学校代申请正式提交所需的成员三状态流转；
- 成员四 `ArrearsConfirmationCompletionPort`、`SupplementCompletionPort` 到成员三正式 Service 的事务适配。

上述接口在成员一、二、四的必要 Spring Bean 缺失时仍会返回 `503
APPROVAL_INTEGRATION_UNAVAILABLE`。该响应表示跨成员依赖尚未接入，不再表示成员三业务编排尚未实现。准确依赖分别见：

- `docs/member3-approval-submission-dependencies.md`；
- `docs/member3-approval-workbench-dependencies.md`；
- `docs/member3-cancellation-dependencies.md`；
- `docs/member3-task-d-integration.md`。

成员三仍需完成端到端联调验收：401/403、503 依赖不可用、版本冲突、重复及并发
`requestId`、退回原因、批量失败回滚、跨模块事务回滚，以及消息已读后的未读数刷新。

## 前端收尾状态

- 已移除审核工作台中未接真实导出接口却直接提示成功的按钮；
- 已取消固定 `GREEN_CHANNEL + batchId=1` 的上报默认值，必须显式填写真实批次类型和批次 ID；
- 已移除审核弹窗中的硬编码礼包名额和补助额度；后端未返回资源快照时只提示“提交时最终校验”；
- 学校取消后端接口已经实现，但学校工作台的取消原因弹窗和操作入口尚未接入；
- 学生退回申请的业务内容编辑仍由成员二申请中心负责，成员三重新提交接口只处理最终状态流转和 `SUBMIT` 记录。
