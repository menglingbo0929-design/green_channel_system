# 成员三审核工作台查询：跨成员依赖

更新日期：2026-07-21  
负责人：成员三（工作台编排、审核记录聚合、详情权限裁剪）

## 已由成员三完成

- `GET /api/approvals/pending`
- `GET /api/approvals/processed`
- `GET /api/approvals/{applicationId}`
- `GET /api/approvals/dashboard`

接口由 `ApprovalWorkbenchQueryService` 实现。它不读取 `application`、学生、附件或申请明细表；已审列表和“最新审核结论”只从成员三的 `approval_record` 查询。

## 成员一必须提供

1. 真实 Spring Bean：`com.example.backend.approval.port.StudentScopeService`。
2. 辅导员范围的分页查询能力。现有的单条 `isCounselorResponsibleFor` 只能校验一名学生，无法安全完成待审列表的数据库分页；成员一需提供“按辅导员查询其负责学生 ID 集合”或等价的可分页数据范围条件。
3. 学院范围校验必须基于真实学生与学院关系，不能相信前端传入的 `collegeId`。

## 成员二必须提供

实现并注册 Spring Bean：`com.example.backend.approval.port.ApprovalApplicationQueryPort`。该 Port 由成员三定义，成员二仍独占下列物理读取：`application`、申请明细、附件、学生快照和组织名称。

必须实现的方法：

| 方法 | 要求 |
|---|---|
| `pagePending` | 根据 `ApprovalWorkScope` 与固定待审状态分页；辅导员只见本人负责学生，学院只见本学院，学校见全校。 |
| `pageByApplicationIds` | 只返回成员三传入的已审申请 ID，仍须应用角色范围和全部筛选条件。 |
| `getRequiredApprovalDetail` | 返回申请基础信息、欠费/礼包/补助明细和附件；不写任何数据。 |
| `getDashboard` | 按相同权限与筛选条件聚合待审层级、学院待审数和流程漏斗。 |
| `listScopedApplicationIds` | 返回当前范围和筛选条件下的申请 ID，供成员三只在自己的 `approval_record` 中聚合审核结论。 |

`ApprovalApplicationSnapshot` 必须填充 `reviewRound`、学生/学院/年级展示字段、批次、状态、提交时间和版本号；不能从前端传入这些字段。

## 接线顺序与验收

1. 成员一先注册数据范围 Bean；成员二实现并注册查询 Port。
2. 启动后 `ApprovalWorkbenchQueryService` 会自动注入；缺任一依赖时接口明确返回 `APPROVAL_INTEGRATION_UNAVAILABLE`，不会伪造数据。
3. 分别以辅导员、学院、学校、学生身份验证：范围隔离、分页、状态限制、已审记录、详情权限和 dashboard 统计。
4. 成员二不得为此修改成员三审核表；成员三不得新增 `application` Mapper 或 SQL。
