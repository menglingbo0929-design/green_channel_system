# 高校绿色通道系统协作与共享资源操作规范

> 文件位置：`docs/collaboration-rules.md`  
> 本文件是数据库权限、共享文件修改和 Git 同步的强制规范。

---

## 1. 文档权威顺序

执行任何跨模块操作前，按以下顺序读取：

1. `docs/collaboration-rules.md`：谁能改、怎么改、怎么同步；
2. `docs/change-log.md`：最近有哪些共享结构、接口、状态发生变化；
3. `docs/database-design.md`：数据库表、字段、约束、表负责人；
4. `docs/api-document.md`：接口路径、参数、返回值、错误码；
5. `docs/status-flow.md`：申请状态、审核状态和允许的状态变化；
6. `docs/decisions/<模块>.md`：模块负责人做出的特殊设计决定；
7. 对应代码和 SQL。

根目录 `README.md` 只做项目总览，不作为字段、接口或状态的最终依据。

任何共享结构变化，必须在同一个提交中同时修改代码和对应文档；只改代码、不改文档的变化视为无效约定。

---

## 2. 四人的数据库表所有权

### 2.1 成员一：基础用户与批次

分支：

```text
feature/base-user-batch
```

拥有以下表的结构维护权：

```text
sys_user
sys_role
sys_user_role
college
major
grade
class_info
student
counselor_student
green_channel_batch
batch_eligible_grade
subsidy_batch
subsidy_batch_eligible_grade
student_tag
policy_rule
```

其他成员可以查询这些表，但不能直接修改表结构。

---

### 2.2 成员二：申请、礼包、补助额度

分支：

```text
feature/application-config
```

拥有以下表的结构维护权：

```text
fee_item
fee_amount_option
gift_item
batch_gift_item
college_gift_quota
grade_gift_quota
application
arrears_application
gift_application
gift_application_item
application_attachment
application_operation_record
college_subsidy_quota
grade_subsidy_quota
subsidy_application
student_recommendation
```

其中 `application` 是共享主表：

- 成员二负责维护 DDL、Entity 和 Mapper；
- 成员三、成员四可以读取和通过成员二提供的 Service 修改业务数据；
- 任何字段增加、删除、改名、改类型，必须由成员二、三、四共同确认；
- 其他成员禁止自行建立第二张申请主表。

---

### 2.3 成员三：审核与上报

分支：

```text
feature/approval-flow
```

拥有以下表的结构维护权：

```text
approval_record
approval_submission_record
system_message
message_read_record
```

其他成员可以查询审核结果，但不能直接修改审核记录或跳过审核 Service 改状态。

`application.status` 和 `application.current_level` 的状态变化由成员三负责业务逻辑，但字段结构仍由成员二维护。

---

### 2.4 成员四：确认与统计

分支：

```text
feature/confirmation-statistics
```

拥有以下表的结构维护权：

```text
arrears_confirmation
```

统计功能默认只执行查询和聚合，不建立重复业务表，不直接修改其他成员负责的表。

成员四需要改变申请状态时，必须调用成员三提供的状态流转 Service；需要创建申请时，必须调用成员二提供的申请 Service。

---

## 3. 数据库操作权限

### 3.1 查询权限

所有成员可以根据业务需要读取其他成员负责的表，但必须先阅读：

```text
docs/database-design.md
docs/change-log.md
docs/decisions/<表负责人模块>.md
```

禁止根据猜测使用字段。

### 3.2 数据写入权限

每张表只允许表负责人模块直接执行 `INSERT`、`UPDATE`、`DELETE`。

其他模块需要写入时：

1. 调用表负责人提供的 Service；
2. 或由负责人新增专用 Service 方法；
3. 禁止其他成员新建 Mapper，直接修改别人负责的表。

### 3.3 表结构修改权限

只有表负责人可以提交以下操作：

```text
CREATE TABLE
ALTER TABLE
DROP TABLE
RENAME TABLE
新增或删除索引
修改唯一约束或外键
修改字段类型、名称和可空性
```

但提交前必须：

1. 在 `docs/change-log.md` 写一条 `PROPOSED` 记录；
2. 通知受影响成员；
3. 修改 `docs/database-design.md`；
4. 得到受影响成员确认；
5. 再修改 SQL、Entity、DTO/VO、Mapper 和接口文档；
6. 完成后把记录状态改为 `IMPLEMENTED`。

### 3.4 共享集成数据库权限

若四人共用一个测试数据库：

- 仓库负责人拥有 DDL 权限；
- 其他成员只执行本模块测试所需的 DML；
- 所有 DDL 由仓库负责人按照已合并到 `main` 的 SQL 统一执行；
- 禁止任何成员直接在共享数据库里手工改表后不提交 SQL；
- 本地数据库可以自行重建，但最终结构必须以仓库 SQL 为准。

---

## 4. 数据库文件的唯一职责

```text
database/01_create_database.sql
```

只负责创建数据库。

```text
database/02_create_tables.sql
```

保存当前完整表结构，用于全新环境一次性建表。

```text
database/03_init_data.sql
```

保存角色、字典、默认配置等必要初始化数据。

```text
database/04_test_data.sql
```

只保存开发测试数据。

```text
database/migrations/
```

保存已经建库后的增量修改，例如：

```text
V20260717_01__add_application_source.sql
V20260717_02__add_subsidy_quota.sql
```

数据库结构发生变化时，必须同时修改：

```text
docs/database-design.md
database/02_create_tables.sql
database/migrations/对应增量SQL
docs/change-log.md
```

---

## 5. 特殊设定写在哪里

每个成员必须维护自己的设计决策文件：

```text
docs/decisions/base-user-batch.md
docs/decisions/application-config.md
docs/decisions/approval-flow.md
docs/decisions/confirmation-statistics.md
```

以下内容必须写入本人决策文件：

- 特殊字段含义；
- 特殊校验规则；
- 事务和锁的使用方式；
- 文件保存方式；
- 某个状态为什么这样流转；
- 某个接口为什么这样设计；
- 与通用规则不同的实现；
- 对其他成员可能产生影响的限制。

统一记录格式：

```markdown
## 决策标题

- 日期：
- 负责人：
- 涉及表：
- 涉及接口：
- 涉及文件：
- 具体规定：
- 做出原因：
- 对其他模块的影响：
- 是否需要其他成员确认：
```

个人决策如果影响共享表、接口或状态，不能只写个人决策文件，还必须同步更新对应公共文档。

---

## 6. 每次开始开发前的固定操作

### 6.1 切换到自己的分支

```bash
git switch feature/自己的分支
```

### 6.2 拉取自己的远程分支

```bash
git pull --ff-only origin feature/自己的分支
```

### 6.3 获取远程最新状态

```bash
git fetch origin
```

### 6.4 查看共享规则是否变化

必须先看：

```text
docs/change-log.md
```

如果 `main` 中的公共文档有更新，先把 `main` 合入自己的分支：

```bash
git merge origin/main
```

解决冲突并确认文档后，才能继续开发。

---

## 7. 执行不同操作前必须阅读的文件

| 准备执行的操作 | 必须先读 |
|---|---|
| 使用或修改数据库表 | `database-design.md`、`change-log.md`、表负责人决策文件 |
| 调用别人接口 | `api-document.md`、`change-log.md`、接口负责人决策文件 |
| 修改申请状态 | `status-flow.md`、`change-log.md`、`approval-flow.md` |
| 修改公共 Axios、路由、权限 | `development-guide.md`、`change-log.md`、`base-user-batch.md` |
| 修改 `application` 主表 | `database-design.md`、`status-flow.md`、成员二/三/四决策文件 |
| 使用别人的 SQL | `database-design.md`、对应 migration、表负责人决策文件 |
| 使用别人分支里的代码 | `change-log.md`、对应提交说明、负责人决策文件 |
| 修改 `pom.xml` 或 `package.json` | `development-guide.md`、`change-log.md` |

---

## 8. 使用其他成员成果的规则

### 8.1 只需要接口或数据结构

不要拉取对方分支。

直接按照以下文档开发：

```text
docs/api-document.md
docs/database-design.md
docs/status-flow.md
```

接口未完成时，前端使用模拟数据。

### 8.2 只需要一个独立工具、组件或修复

由原负责人提交一个独立提交，并把提交号写入 `docs/change-log.md`。

使用者执行：

```bash
git fetch origin
```

```bash
git cherry-pick <提交编号>
```

### 8.3 需要对方完整模块

只有以下条件全部满足才允许使用：

- 对方模块已完成；
- 对方已推送；
- 对方已更新相关文档；
- 对方已通过基本测试；
- 对方已创建 Pull Request 并合并到 `main`。

使用者随后执行：

```bash
git fetch origin
```

```bash
git merge origin/main
```

禁止为了拿代码直接把另一个成员的整个开发分支合进自己的分支。

---

## 9. 共享结构变更流程

任何共享表、公共接口、公共状态或公共配置变化，必须按以下顺序：

```text
提出变更
→ 写入 docs/change-log.md，状态为 PROPOSED
→ 相关成员确认
→ 负责人修改公共文档
→ 负责人修改代码和 SQL
→ 本模块测试
→ 状态改为 IMPLEMENTED
→ Commit and Push
→ 创建 Pull Request
→ 合并到 main
→ 其他成员 fetch 并 merge origin/main
```

没有写入 `docs/change-log.md` 的共享变化，其他成员有权不接受。

---

## 10. `docs/change-log.md` 固定格式

```markdown
## 2026-07-17｜变更标题

- 状态：PROPOSED / APPROVED / IMPLEMENTED
- 提出人：
- 负责人：
- 影响模块：
- 影响表：
- 影响接口：
- 影响状态：
- 变更内容：
- 使用者需要执行的操作：
- 对应提交：
```

最新记录放在文件最上方。

---

## 11. 提交前检查

提交前必须确认：

```bash
git status
```

并检查：

- 当前在自己的分支；
- 修改了对应公共文档；
- SQL 与数据库设计一致；
- 接口与接口文档一致；
- 状态与状态文档一致；
- 没有修改别人负责的 Mapper；
- 没有提交 `.idea`、`*.iml`、`node_modules`、`target`；
- 没有提交密码、Token 和本地数据库配置。

之后再在 IDEA 中执行：

```text
Commit and Push
```

---

## 12. 当前尚未存在、必须新建的文件

以下文件如果仓库中还没有，必须建立：

```text
docs/collaboration-rules.md
docs/change-log.md
docs/decisions/base-user-batch.md
docs/decisions/application-config.md
docs/decisions/approval-flow.md
docs/decisions/confirmation-statistics.md
database/migrations/
```

现有 README 已经规定了独立分支、公共文件修改需提前说明，以及接口、数据库、状态文档作为协作依据；但没有明确表级权限、固定阅读顺序、共享变更记录格式和具体同步流程。本文件补齐这些规则。
