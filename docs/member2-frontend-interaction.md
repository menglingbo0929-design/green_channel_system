# 成员二前端交互逻辑说明

更新日期：2026-07-21  
负责人：成员二  
负责页面：`批次与申请配置`、`学生申请中心`  
相关前端文件：

- `frontend/src/views/ApplicationConfig.vue`
- `frontend/src/views/StudentApplicationCenter.vue`
- `frontend/src/components/application/AllocationDialog.vue`
- `frontend/src/components/application/SubmitConfirmDialog.vue`
- `frontend/src/api/application.js`

> 本文只描述成员二页面的前端交互、接口调用与联调边界。成员一负责可信登录、批次和组织基础数据；成员三负责审核流转；成员四负责欠费确认、补录与统计编排。

## 1. 统一交互规则

- 页面首次进入和点击“刷新”时调用对应列表接口，并显示加载状态。
- 新增、编辑和删除失败时，展示后端返回的 `message`；没有返回时展示通用失败提示。
- 删除操作必须先弹出二次确认。
- 提交给后端的金额均使用两位小数；前端校验仅改善体验，后端校验是最终依据。
- 草稿类写操作携带 `version`，后端发生乐观锁冲突时提示用户刷新后重试。
- 当前学生申请接口仍使用本地开发身份头 `VITE_DEV_STUDENT_ID`、`VITE_DEV_USER_ID`；接入成员一可信 JWT 身份后必须删除该临时逻辑。

## 2. 页面：批次与申请配置

路由：`/application-config`  
可见角色：学校管理员、学院管理员。

### 2.1 欠费配置（已实现）

| 交互 | 页面行为 | 接口 |
|---|---|---|
| 查询欠费项目 | 进入页面、点击刷新时加载项目列表 | `GET /api/fee-items` |
| 新增/编辑欠费项目 | 输入名称、设置启用状态，保存后关闭弹窗并刷新列表 | `POST/PUT /api/fee-items` |
| 删除欠费项目 | 二次确认；已被申请使用时展示后端拒绝原因 | `DELETE /api/fee-items/{id}` |
| 查询金额档位 | 与欠费项目列表同时加载 | `GET /api/fee-amount-options` |
| 新增/编辑金额档位 | 选择启用的欠费项目、输入正金额并保存 | `POST/PUT /api/fee-amount-options` |
| 删除金额档位 | 二次确认后删除并刷新 | `DELETE /api/fee-amount-options/{id}` |

前端限制：金额必须大于 0；新增金额档位只能选择启用的欠费项目。

### 2.2 礼包物品（基础配置已实现）

| 交互 | 页面行为 | 接口 |
|---|---|---|
| 查询礼包物品 | 进入页面、点击刷新时加载 | `GET /api/gift-items` |
| 新增/编辑物品 | 输入物品名称、设置启用状态，保存后刷新列表 | `POST/PUT /api/gift-items` |
| 删除物品 | 二次确认；已被批次资源引用时由后端拒绝删除 | `DELETE /api/gift-items/{id}` |

当前仅维护物品名称与启用状态。库存、单人上限、批次关联尚未实现真实配置接口。

### 2.3 批次、库存、名额和补助额度（待对接）

当前页面明确显示空状态，不展示模拟数据。

- 批次设置：批次查询和维护属于成员一；成员二页面只调用其查询能力，不直接写批次表。
- 礼包库存/单人上限：成员二需补齐 `batch_gift_item` 的配置接口和列表/编辑交互。
- 学院、年级礼包名额：成员二需补齐 `college_gift_quota`、`grade_gift_quota` 的配置与分配交互。
- 学院、年级补助额度：成员二需补齐 `college_subsidy_quota`、`grade_subsidy_quota` 的配置与分配交互。
- `AllocationDialog.vue` 当前仅为提示外壳；真实分配时应展示总量、预占量、已使用量、剩余量、新分配量和保存结果。

## 3. 页面：学生申请中心

路由：`/student-center`  
可见角色：学生。

### 3.1 草稿申请（已实现）

| 交互 | 页面行为 | 接口 |
|---|---|---|
| 查询我的申请 | 页面加载和刷新时查询当前学生的申请 | `GET /api/applications/mine` |
| 新建草稿 | 选择申请类型、输入批次 ID 和申请理由 | `POST /api/applications/drafts` |
| 编辑草稿理由 | 仅草稿或退回状态可编辑，保存时带 `version` | `PUT /api/applications/{id}` |
| 删除草稿 | 仅草稿状态显示删除按钮，二次确认后删除 | `DELETE /api/applications/{id}` |

申请类型与批次类型对应关系：绿色通道使用 `GREEN_CHANNEL` 批次；生活补助、路费补助使用 `SUBSIDY` 批次。

### 3.2 绿色通道欠费明细（已实现）

| 交互 | 页面行为 | 接口 |
|---|---|---|
| 查看明细 | 选中绿色通道申请后加载明细 | `GET /api/applications/{id}/arrears` |
| 新增/移除项目 | 在草稿或退回状态编辑欠费项目和申报金额 | 页面内交互 |
| 保存明细 | 校验项目、金额、总额后整体替换 | `PUT /api/applications/{id}/arrears` |

前端校验：项目不能为空、金额大于 0、合计不超过 8,000 元。后端同时校验项目去重、状态、版本和金额上限。

### 3.3 礼包与补助明细（后端接口已实现，页面表单待接入）

| 能力 | 接口 | 当前状态 |
|---|---|---|
| 查询礼包明细 | `GET /api/applications/{id}/gifts` | 已实现，前端 API 已封装 |
| 保存礼包明细 | `PUT /api/applications/{id}/gifts` | 已实现；校验批次归属、重复项和单人上限 |
| 查询补助金额 | `GET /api/applications/{id}/subsidy` | 已实现，前端 API 已封装 |
| 保存补助金额 | `PUT /api/applications/{id}/subsidy` | 已实现；仅生活/路费补助申请可用 |

待完成页面交互：在 `StudentApplicationCenter.vue` 中展示礼包选项、数量编辑，以及生活补助/路费补助的期望金额编辑和保存按钮。

### 3.4 正式提交（待成员一、三联调）

“正式提交”按钮使用 `SubmitConfirmDialog.vue` 展示确认入口，但当前不可提交。完整交互必须按以下顺序实现：

1. 成员一提供当前学生、真实批次、资格和数据范围；
2. 成员二校验草稿完整性，并预占礼包库存、名额和补助额度；
3. 成员二调用成员三审核流转能力，将状态变为 `COUNSELOR_PENDING` 并写审核记录；
4. 任一步失败时整体回滚资源与申请状态；
5. 成功后页面刷新为审核中状态，隐藏编辑和删除入口。

## 4. 跨成员接口边界

| 依赖方 | 成员二使用的能力 | 禁止事项 |
|---|---|---|
| 成员一 | 当前用户、学生档案、批次、学院/年级等组织查询 | 不直接写用户、学生、组织和批次表 |
| 成员三 | 审核状态流转、审核记录、消息 | 不直接写审核记录与消息表 |
| 成员四 | 欠费确认、代申请、补录、统计编排 | 不直接写欠费确认表 |

成员二向其他成员提供申请主表、申请明细、资源查询和状态读写 Service；跨模块写入必须通过 Service，不得新建其他成员表的写 Mapper。

## 5. 验证清单

- 前端：在 `frontend` 执行 `npm run build`。
- 后端：在 `backend` 执行 `./mvnw.cmd test`。
- 当前已知测试基线：后端 43 项测试通过。
- 联调前确认本地 MySQL 已执行成员二 migration，并通过 `SPRING_DATASOURCE_PASSWORD` 提供数据库密码。
