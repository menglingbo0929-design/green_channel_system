# 成员二跨成员对接清单（成员四执行版）

> 来源：成员二 `member2-integration-handoff.md`（2026-07-21）。
> 本文件记录成员四的接入方式、已接通内容和仍受其他模块制约的精确边界；它不替代成员二对其表、Mapper 和事务的所有权。

## 1. 成员二已提供、成员四必须直接调用的正式能力

| 场景 | 成员二正式 Port / Service | 成员四接入规则 |
|---|---|---|
| 学校代申请 | `SchoolProxyApplicationPort` / `application.service.SchoolProxyApplicationService` | 成员四的 Controller/Service 只调用 Port；不得再写 `application`、明细、附件或资源表。旧 `service.adapter.SchoolProxyApplicationPortAdapter` 仅保留历史源码，不注册为 Spring Bean。 |
| 线下补录 | `SupplementApplicationPort` / `application.service.SupplementApplicationService` | 成员四的 `SupplementApplicationServiceImpl` 只调用 Port。正式 Service 已在事务内写明细、占用资源并调用成员三自动审核，成员四不得再次调用审核完成接口。 |
| 欠费确认与统计 | 成员二对应读取/聚合 Port | 成员四只通过 Port 读取，不新建跨模块 Mapper 或直接查询成员二表。 |

成员二相关迁移必须按顺序执行：

```text
database/migrations/V20260720_001__create_member2_application_tables.sql
database/migrations/V20260721_001__add_member2_supplement_and_arrears_reason.sql
```

## 2. 6.1.3 学校代申请

对外路径固定为：

```http
GET  /api/school-proxy/students?studentNo={studentNo}
POST /api/school-proxy/applications/drafts
POST /api/school-proxy/applications/{applicationId}/attachments?requestId={requestId}
POST /api/school-proxy/applications/{applicationId}/submit?version={version}&requestId={requestId}
```

- 草稿创建已经接入成员二正式 Port，可创建真实申请及明细。
- 前端欠费项必须传 `arrearsReasonCode`，只能使用 `FAMILY_FINANCIAL_DIFFICULTY`、`FAMILY_EMERGENCY`、`MAJOR_ILLNESS`、`DISASTER_ACCIDENT`、`OTHER` 之一；不得再以自由文本替代编码。
- 附件请求使用 `multipart/form-data`，文件字段固定为 `file`。
- 当前附件存储尚未接通时，上传端点固定返回 `501 ATTACHMENT_STORAGE_UNAVAILABLE`；这不是成员四可以伪造成功的步骤。
- 当前资源预占/附件前置条件尚未满足时，提交端点固定返回 `503 SCHOOL_PROXY_SUBMISSION_UNAVAILABLE`；前端必须保留“先上传附件、后提交”的顺序，不得把草稿误显示为已提交。

## 3. 6.1.4 线下补录

对外路径固定为：

```http
GET  /api/supplements/students?studentNo={studentNo}
GET  /api/supplements?pageNo={pageNo}&pageSize={pageSize}
GET  /api/supplements/{applicationId}
POST /api/supplements
```

- `POST /api/supplements` 已委托成员二正式 `SupplementApplicationPort`；成员二 Service 在其事务中调用成员三的 `ApprovalTransitionService.completeSupplementReview`。成员四不再做第二次自动审核推进。
- `GET /api/supplements/students` 现可按学号读取学生基础快照，返回同时包含 `collegeId`、`majorId`、`gradeId`、`classId` 及名称字段。
- 补录历史分页和详情仍依赖成员一提供“按学生 ID / 批量学生组织快照”能力；该能力未合入前，成员二正式 Service 会明确返回其不可用状态，成员四不返回模拟历史或详情。

## 4. 仍待其他成员完成的事项

| 负责人 | 缺口 | 成员四的处理 |
|---|---|---|
| 成员一 | 学校数据范围校验；按学生 ID 和批量学生组织快照查询 | 不直接查成员一维护的学生/组织表；列表和详情保持真实失败状态。 |
| 成员二 | 代申请附件对象存储、资源预占和正式提交；补录历史/详情在学生批量快照接通后的真实组装；统计/报表聚合 | 前端保留固定接口与参数，不伪造成功、统计数字或报表数据。 |
| 成员三 | 无新增调用入口；其补录自动审核由成员二正式 Service 调用 | 成员四不二次调用，避免重复审核记录和状态推进。 |

## 5. 成员四验收顺序

1. 执行上述成员二迁移并准备其测试数据。
2. 调用学生查询，再创建学校代申请草稿；附件和提交分别按 501 / 503 当前边界验证，待成员二能力接通后再验证成功路径。
3. 调用补录学生查询，再创建补录；检查返回状态：有欠费为 `CONFIRM_PENDING`，无欠费为 `COMPLETED`。
4. 待成员一批量快照接通后，再验证补录列表/详情；待成员二统计/报表聚合接通后，再验证页面九统计、打印和导出。
