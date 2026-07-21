# 绿色通道系统前端

Vue 3 + Vite 前端。当前已接入的成员二页面包括：

- `/application-config`：批次与申请配置；已联调欠费项目、金额档位和礼包物品接口；
- `/student-center`：学生申请中心；已联调草稿与欠费明细接口。

成员三审核页面当前包括：

- `/counselor/approvals`：辅导员审核工作台；
- `/college/approvals`：学院审核工作台；
- `/school/approvals`：学校审核工作台。

## 本地运行

```bash
npm ci
npm run dev
```

默认启用状态化 Mock，支持筛选、逐条审核、批量上报和学校取消演示。接入真实后端时复制 `.env.example` 并设置：

```text
VITE_USE_MOCK=false
VITE_API_BASE_URL=/api
```

构建验证：

```bash
npm run build
```

审核页面只提交审核动作、意见、版本和请求号，不由前端指定目标状态；身份、角色和数据范围必须由后端可信登录上下文校验。

学生申请页在后端尚未接入可信身份上下文时，需要在本机未提交的 `.env` 文件中配置：

```text
VITE_DEV_STUDENT_ID=1
VITE_DEV_USER_ID=1
```

上述变量仅服务于后端当前临时开发身份头；接入 JWT 当前用户能力后必须删除。
