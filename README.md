# 高校绿色通道系统

高校绿色通道系统面向新生、辅导员、学院管理员和学校管理员，用于完成新生信息入库、绿色通道申请、爱心礼包申请、三级审核、欠费确认、申请补录和统计报表等业务。

本项目采用前后端分离架构，由四名成员并行开发。所有成员必须遵守本文档中的目录、分支、接口、数据库和合并规则。

---

## 1. 技术栈

### 前端

- Vue 3
- Vite
- Vue Router
- Pinia
- Axios
- Element Plus
- ECharts

### 后端

- Spring Boot
- MyBatis
- MySQL
- Maven
- JWT
- Validation
- Lombok

### 当前基础环境

- 后端：JDK 21
- 前端开发端口：`5175`
- 后端默认端口：`8083`
- 数据库名称：`green_channel`

实际依赖版本以 `backend/pom.xml` 和 `frontend/package.json` 为准。

---

## 2. 仓库结构

```text
green_channel_system/
├── backend/                         Spring Boot 后端
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/greenchannel/backend/
│   │   │   └── resources/
│   │   └── test/
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
│
├── frontend/                        Vue 3 前端
│   ├── public/
│   ├── src/
│   │   ├── api/
│   │   ├── assets/
│   │   ├── components/
│   │   ├── layout/
│   │   ├── router/
│   │   ├── stores/
│   │   ├── utils/
│   │   └── views/
│   │       ├── common/
│   │       ├── student/
│   │       ├── counselor/
│   │       ├── college/
│   │       └── school/
│   ├── package.json
│   ├── package-lock.json
│   └── vite.config.js
│
├── database/
│   ├── 01_create_database.sql
│   ├── 02_create_tables.sql
│   ├── 03_init_data.sql
│   └── 04_test_data.sql
│
├── docs/
│   ├── requirement.md
│   ├── division.md
│   ├── database-design.md
│   ├── api-document.md
│   ├── status-flow.md
│   └── development-guide.md
│
├── .gitignore
└── README.md
```

`uploads/`、`.idea/`、`node_modules/`、`target/`、本地配置文件等内容不提交到 GitHub。

---

## 3. 系统角色

| 角色 | 权限范围 |
|---|---|
| 学生 | 完善本人信息、提交申请、修改退回申请、查看本人审核流程 |
| 辅导员 | 查看本人负责学生、完成第一级审核、提交学院 |
| 学院管理员 | 查看本学院申请、完成第二级审核、提交学校 |
| 学校管理员 | 管理全校基础数据、配置批次、最终审核、欠费确认、补录和统计 |

所有数据权限必须由后端校验，不能只通过前端隐藏菜单实现。

---

## 4. 主要业务流程

```text
学校导入新生信息
        ↓
学校创建并开放绿色通道批次
        ↓
学生登录并完善个人信息
        ↓
学生提交欠费申请和/或爱心礼包申请
        ↓
辅导员审核
        ↓
学院审核
        ↓
学校审核
        ↓
欠费信息最终确认
        ↓
学生查看结果和单据
        ↓
学校统计、导出和打印
```

---

## 5. 项目功能

### 5.1 登录与权限

- 用户登录；
- JWT 身份认证；
- 获取当前登录用户；
- 修改密码；
- 按角色展示菜单；
- 前端路由权限控制；
- 后端接口权限控制；
- 按学生、辅导员、学院和学校限制数据范围。

### 5.2 新生信息管理

- 新生信息新增、修改、删除和分页查询；
- 按学号、姓名、学院、专业、年级、班级筛选；
- Excel 批量导入；
- 下载导入模板；
- 自动创建学生账号；
- 学生完善个人信息。

### 5.3 绿色通道批次

- 批次新增、修改、开启和关闭；
- 设置申请开始时间和结束时间；
- 设置学院提交截止时间；
- 设置可申请年级；
- 设置资金来源、政策说明和页面提示。

### 5.4 欠费申请

- 学费和住宿费项目配置；
- 预设金额档位；
- 学生选择或手动填写金额；
- 总金额不得超过 8000 元；
- 自动生成欠费原因；
- 上传附件；
- 保存草稿；
- 正式提交；
- 退回后修改并重新提交。

### 5.5 爱心礼包

- 礼包物品新增、修改、删除和图片上传；
- 配置物品名称、类型、尺寸、简介、单价和库存；
- 配置是否必选、适用性别和单人选择上限；
- 学校向学院分配名额；
- 学院向年级分配名额；
- 学生选择物品并提交申请；
- 后端校验库存、学院名额和年级名额。

### 5.6 三级审核

- 辅导员审核；
- 学院审核；
- 学校最终审核；
- 审核通过；
- 退回修改；
- 审核不通过；
- 填写审核意见；
- 保存完整审核记录；
- 学生查看审核进度。

### 5.7 欠费确认

- 查询学校审核通过的欠费申请；
- 填写实际欠费金额；
- 保存确认人和确认时间；
- 生成欠费单据编号；
- 预览和打印欠费单据。

### 5.8 学校代申请与补录

- 根据学号查询学生；
- 学校帮助学生创建申请；
- 补录线下申请；
- 记录申请来源、补录人和补录时间；
- 防止同一批次重复补录。

### 5.9 统计报表

- 总申请人数和实报人数；
- 各学院、年级申请人数；
- 欠费项目人数和金额；
- 欠费原因按人数和金额统计；
- 爱心礼包物品申请数量；
- 历史批次查询；
- ECharts 图表；
- Excel 导出；
- 报表打印。

---

## 6. 四人分工

### 成员一：基础数据与批次

负责登录、JWT、用户权限、学院/专业/年级/班级、新生信息、Excel 导入、学生个人信息、绿色通道批次、公共布局和权限菜单。

分支：

```text
feature/base-user-batch
```

### 成员二：申请配置与学生申请

负责欠费项目、金额档位、礼包物品、学院和年级名额、绿色通道入口判断、申请主表、欠费申请、礼包申请、附件上传和学生申请页面。

分支：

```text
feature/application-config
```

### 成员三：三级审核与流程查询

负责辅导员审核、学院审核、学校审核、审核状态流转、审核记录、退回修改、学生审核进度和各角色审核页面。

分支：

```text
feature/approval-flow
```

### 成员四：确认、补录与统计

负责欠费最终确认、欠费单据、学校代申请、绿色通道补录、统计查询、ECharts 图表、Excel 导出、报表打印和历史记录。

分支：

```text
feature/confirmation-statistics
```

---

## 7. 分支规则

### 7.1 主分支

```text
main
```

`main` 只保存：

- 已确认的公共项目结构；
- 已统一的公共配置；
- 已通过测试的完整模块；
- 已确认的接口、数据库和状态文档；
- 最终可运行代码。

禁止直接在 `main` 分支编写业务代码。

### 7.2 个人分支

每名成员只在自己的功能分支开发：

```text
feature/base-user-batch
feature/application-config
feature/approval-flow
feature/confirmation-statistics
```

禁止：

- 在其他成员分支直接修改代码；
- 未经沟通强制推送其他成员分支；
- 在 `main` 上直接提交业务代码；
- 为同一功能建立多套重复实体、接口或数据库表。

### 7.3 第一次开始开发

```bash
git clone https://github.com/menglingbo0929-design/green_channel_system.git
```

```bash
cd green_channel_system
```

```bash
git switch -c feature/本人负责的模块
```

```bash
git push -u origin feature/本人负责的模块
```

远程分支已经存在时：

```bash
git fetch origin
```

```bash
git switch feature/本人负责的模块
```

---

## 8. 在哪里编写代码

### 8.1 后端

后端统一放在：

```text
backend/src/main/java/com/greenchannel/backend/
```

推荐目录：

```text
com.greenchannel.backend
├── common
├── config
├── controller
│   ├── auth
│   ├── student
│   ├── batch
│   ├── application
│   ├── approval
│   ├── confirmation
│   └── statistics
├── service
├── service/impl
├── mapper
├── entity
├── dto
├── vo
├── exception
└── utils
```

MyBatis XML 放在：

```text
backend/src/main/resources/mapper/
```

要求：

- Controller 只负责接收参数和返回结果；
- Service 负责业务规则、事务和状态流转；
- Mapper 负责数据库访问；
- Entity 对应数据库表；
- DTO 接收前端参数；
- VO 返回前端展示数据；
- 不允许在 Controller 中直接编写复杂 SQL 或完整业务逻辑。

### 8.2 前端

```text
frontend/src/api/            Axios 接口
frontend/src/components/     公共组件
frontend/src/layout/         公共布局
frontend/src/router/         路由
frontend/src/stores/         Pinia
frontend/src/utils/          工具函数
frontend/src/views/common/   公共页面
frontend/src/views/student/  学生页面
frontend/src/views/counselor/辅导员页面
frontend/src/views/college/  学院页面
frontend/src/views/school/   学校页面
```

要求：

- 页面不能直接写完整请求地址；
- 所有请求统一封装在 `src/api`；
- Token 通过 Axios 请求拦截器统一携带；
- 公共表格、弹窗、上传和流程组件放入 `components`；
- 不允许多个成员分别实现重复的登录、Axios 实例或权限判断。

### 8.3 数据库

数据库脚本统一放在：

```text
database/
```

| 文件 | 内容 |
|---|---|
| `01_create_database.sql` | 创建数据库，只由仓库负责人维护 |
| `02_create_tables.sql` | 正式表结构 |
| `03_init_data.sql` | 角色、字典等必要初始化数据 |
| `04_test_data.sql` | 本地开发和演示测试数据 |

修改数据库时必须同步更新：

```text
database/02_create_tables.sql
docs/database-design.md
```

数据库规则：

- 表名和字段名统一使用小写下划线；
- 主键统一为 `id`；
- 外键统一使用 `xxx_id`；
- 状态字段使用约定的英文枚举值；
- 禁止同一业务建立多张重复主表；
- 修改共享表前必须在小组内说明；
- 删除字段或改变字段类型前必须确认其他模块没有使用。

### 8.4 文档

```text
docs/requirement.md
docs/division.md
docs/database-design.md
docs/api-document.md
docs/status-flow.md
docs/development-guide.md
```

接口、字段或状态发生变化时，代码和文档必须一起修改。

---

## 9. 接口协作规则

前后端并行开发时，以：

```text
docs/api-document.md
```

作为唯一接口约定。

每个接口至少写明：

- 请求方法；
- 请求路径；
- 使用角色；
- 请求参数；
- 返回数据；
- 错误情况；
- 对应状态变化。

统一返回格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

分页返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 0,
    "page": 1,
    "size": 10
  }
}
```

需要使用其他成员尚未完成的接口时：

1. 先在 `docs/api-document.md` 中确定接口；
2. 前端使用模拟数据开发；
3. 后端按照同一文档实现；
4. 双方完成后再真实联调。

不要因为等待接口而停止开发，也不要在自己的模块中复制一套别人的 Controller、Service 或 Mapper。

接口发生变化时，负责人必须同时修改：

```text
后端 DTO/VO
前端 api 文件
相关页面
docs/api-document.md
```

未经沟通，不允许直接修改已经被其他模块使用的请求路径、参数名、返回字段、状态值和数据类型。

---

## 10. 使用其他成员分支内容的规则

### 10.1 优先不合并代码

并行开发阶段需要其他成员的接口、文件或 SQL 时，优先使用：

- `docs/api-document.md`；
- `docs/database-design.md`；
- `docs/status-flow.md`；
- 前端模拟数据；
- 本地临时测试数据。

这样可以避免四个分支反复互相合并。

### 10.2 只需要一个独立提交

经过原作者确认后，可以使用：

```bash
git fetch origin
```

```bash
git cherry-pick <提交编号>
```

适用于独立公共工具、稳定公共组件、独立 DTO/VO 或单独 SQL 修复。

### 10.3 必须依赖完整模块

只有满足以下条件，才允许把其他成员分支合并进自己的分支：

- 对方模块已经完成；
- 对方模块已通过基本测试；
- 双方确认接口和数据库不再大改；
- 双方已经提交并推送当前代码；
- 合并由双方共同检查。

```bash
git fetch origin
```

```bash
git merge origin/feature/其他成员分支
```

不允许未沟通就随意合并其他成员整个分支。

### 10.4 冲突处理

- 不直接选择“全部接受当前”或“全部接受传入”；
- 先确认冲突文件归属；
- 业务文件由原负责人决定；
- 公共文件由相关成员共同确认；
- `pom.xml`、`package.json`、路由、状态枚举和 SQL 必须逐项合并；
- 解决后重新编译和测试。

---

## 11. 日常 Git 操作

```bash
git switch feature/本人分支
```

```bash
git pull
```

```bash
git status
```

```bash
git add .
```

```bash
git commit -m "feat: 完成具体功能"
```

```bash
git push
```

提交前缀：

| 前缀 | 含义 |
|---|---|
| `feat` | 新增功能 |
| `fix` | 修复错误 |
| `refactor` | 重构 |
| `docs` | 修改文档 |
| `test` | 增加测试 |
| `style` | 样式调整 |
| `chore` | 配置、依赖和维护 |

禁止使用“修改”“更新”“第一次”“111”等无法说明内容的提交信息。

---

## 12. 合并到 main 的条件

个人分支必须同时满足：

- 所负责的后端功能已完成；
- 对应前端页面已完成；
- 前后端已经联调；
- 数据库脚本已更新；
- 接口文档已更新；
- 主要流程可以演示；
- 后端可以正常编译；
- 前端可以正常启动和构建；
- 没有提交密码、Token 或数据库账号；
- 没有提交 `.idea`、`*.iml`、`node_modules`、`target` 和上传文件；
- 已检查公共字段和状态；
- 已解决当前分支冲突。

不允许只完成一半页面或只有空接口就合并到 `main`。

---

## 13. 合并流程

### 13.1 合并前同步 main

```bash
git fetch origin
```

```bash
git switch feature/本人分支
```

```bash
git merge origin/main
```

解决冲突并完成测试后：

```bash
git push
```

### 13.2 创建 Pull Request

在 GitHub 创建：

```text
feature/本人分支 → main
```

Pull Request 必须说明：

- 完成了哪些功能；
- 修改了哪些数据库表；
- 新增或修改了哪些接口；
- 如何测试；
- 是否依赖其他模块；
- 是否存在未完成内容。

### 13.3 审核与合并

- 至少由仓库负责人检查；
- 涉及其他模块时由相关成员共同检查；
- 禁止未经检查直接合并；
- 合并后在 `main` 上重新测试。

---

## 14. 推荐最终合并顺序

```text
1. feature/base-user-batch
2. feature/application-config
3. feature/approval-flow
4. feature/confirmation-statistics
```

每次合并后都要检查：

- 后端编译；
- 前端构建；
- 数据库脚本；
- 核心页面；
- 已合并模块的基本流程。

---

## 15. 申请状态约定

```text
DRAFT                   草稿
COUNSELOR_PENDING       待辅导员审核
COUNSELOR_RETURNED      辅导员退回
COLLEGE_PENDING         待学院审核
COLLEGE_RETURNED        学院退回
SCHOOL_PENDING          待学校审核
SCHOOL_RETURNED         学校退回
REJECTED                审核不通过
APPROVED                学校审核通过
CONFIRM_PENDING         欠费待确认
COMPLETED               已完成
CANCELLED               已取消
```

正常流程：

```text
DRAFT
→ COUNSELOR_PENDING
→ COLLEGE_PENDING
→ SCHOOL_PENDING
→ APPROVED 或 CONFIRM_PENDING
→ COMPLETED
```

退回后：

```text
任一级退回
→ 学生修改原申请
→ 重新提交
→ COUNSELOR_PENDING
```

所有状态变化必须由后端 Service 校验，前端不能任意指定新状态。

---

## 16. 本地运行

### 前端

```bash
cd frontend
```

```bash
npm install
```

```bash
npm run dev
```

访问：

```text
http://localhost:5175
```

### 后端

```bash
cd backend
```

Windows：

```bash
mvnw.cmd spring-boot:run
```

或：

```bash
mvn spring-boot:run
```

默认地址：

```text
http://localhost:8083
```

### 数据库

按顺序执行：

```text
database/01_create_database.sql
database/02_create_tables.sql
database/03_init_data.sql
database/04_test_data.sql
```

本地数据库密码只写入本地配置文件，不提交到 GitHub。

---

## 17. 最终验收流程

```text
学校导入新生
→ 创建绿色通道批次
→ 学生登录并完善信息
→ 学生提交欠费和礼包申请
→ 辅导员审核
→ 学院审核
→ 学校审核
→ 学校确认欠费
→ 学生查看结果
→ 学校查询统计并导出
```

还要测试：

- 金额超过 8000 元；
- 重复申请；
- 申请时间已截止；
- 库存不足；
- 学院或年级名额不足；
- 无权限访问其他学院数据；
- 退回后重新提交；
- 审核不通过；
- 已通过申请取消；
- 文件格式错误；
- 当前状态不允许操作。

---

## 18. 核心原则
```text
一人一个分支
只在自己的分支开发
main 只保存稳定代码
接口先写文档再开发
共享表和状态先沟通再修改
优先使用接口约定和模拟数据并行开发
不要随意合并其他成员整个分支
模块完成并通过测试后再申请合并
所有代码最终通过 Pull Request 进入 main
```
