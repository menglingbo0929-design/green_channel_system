# 绿色通道系统前端

Vue 3 + Vite 前端。当前已接入的成员二页面包括：

- `/application-config`：绿色通道信息设置；已联调批次时间、学年、资金来源、适用年级、欠费项目、金额档位和礼包物品接口；
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

默认连接真实后端；仅在明确设置 `VITE_USE_MOCK=true` 时启用演示 Mock。可复制 `.env.example` 设置：

```text
VITE_USE_MOCK=false
VITE_API_BASE_URL=/api
```

构建验证：

```bash
npm run build
```

审核页面只提交审核动作、意见、版本和请求号，不由前端指定目标状态；身份、角色和数据范围必须由后端可信登录上下文校验。

学生申请、批次、审核与统计接口统一使用登录 JWT，不再接受本地开发身份头。
