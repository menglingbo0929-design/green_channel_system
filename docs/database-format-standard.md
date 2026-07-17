# 数据库格式与迁移规范

> 状态：`PROPOSED`  
> 适用范围：高校绿色通道系统全体开发成员。  
> 目标：规定项目最终数据库结构应遵守的通用格式、约束、所有权和迁移方式。  
> 生效条件：受影响成员确认，并在 `docs/change-log.md` 将对应记录更新为 `APPROVED` 后生效。

## 1. 权威顺序与适用边界

本规范不改变 `docs/collaboration-rules.md` 规定的权威顺序。

执行数据库操作时按以下规则处理：

1. 表所有权、修改权限和协作流程以 `docs/collaboration-rules.md` 为准；
2. 最近共享变化以 `docs/change-log.md` 为准；
3. 已批准的具体表结构以 `docs/database-design.md` 为准；
4. 本文件规定未在具体表设计中单独说明的默认格式；
5. 接口字段和状态分别以 `docs/api-document.md`、`docs/status-flow.md` 为准；
6. 模块特殊实现以对应 `docs/decisions/*.md` 为准；
7. 代码、Entity、Mapper 和 SQL 必须与上述已批准文档一致。

发生冲突时，不得自行选择某一版本实施。必须先在 `docs/change-log.md` 登记 `PROPOSED`，由受影响成员确认，再由对应表负责人修改公共文档和代码。

根目录 `README.md` 和外部需求说明只用于项目总览与需求追溯，不替代已经批准的数据库设计。

## 2. 数据库运行环境

- 数据库名称：`green_channel`；
- 数据库：MySQL 8.x；
- 最低版本：MySQL 8.0.16；
- 集成测试和演示环境必须统一到同一 MySQL 大版本和补丁版本；
- 字符集：`utf8mb4`；
- 排序规则：`utf8mb4_unicode_ci`；
- 存储引擎：`InnoDB`；
- 项目业务时区：`Asia/Shanghai`；
- 数据库连接、JVM 和 JSON 序列化必须使用一致时区；
- 禁止零日期，例如 `0000-00-00` 或 `0000-00-00 00:00:00`；
- 集成环境必须启用严格 SQL 模式，非法值不得被静默截断。

使用 `CHECK` 约束的原因是 MySQL 从 8.0.16 开始才正式执行该约束，因此项目最低版本不得低于 8.0.16。

## 3. 命名规范

### 3.1 表和字段

- 表名、字段名全部使用小写下划线；
- 表名使用有业务含义的单数或集合名称，项目内保持一致；
- 主键统一为 `id`；
- 关联字段统一为 `xxx_id`；
- 布尔字段使用肯定语义，例如 `enabled`、`required`、`read_flag`；
- 时间字段统一使用 `xxx_time`，不混用 `xxx_at`；
- 禁止使用拼音、无意义缩写和数据库保留字；
- 名称发生变化时必须通过迁移完成，不能只修改 Entity。

示例：

```text
green_channel_batch
application_id
create_time
update_time
college_submit_deadline
```

### 3.2 约束和索引

| 对象 | 命名格式 | 示例 |
|---|---|---|
| 普通索引 | `idx_表名_字段组合` | `idx_application_batch_status` |
| 唯一索引 | `uk_表名_字段组合` | `uk_application_student_batch_type_deleted` |
| 外键 | `fk_子表_父表_字段` | `fk_approval_record_application_id` |
| CHECK | `chk_表名_字段` | `chk_sys_user_enabled` |

索引和约束名称不得超过 MySQL 标识符长度限制。字段较多时可以缩短名称，但必须保持可识别且不能使用 `idx_1`、`test_index` 等无意义名称。

## 4. 类型规范

| 业务含义 | MySQL 类型 | Java 类型 | 说明 |
|---|---|---|---|
| 主键、关联 ID | `BIGINT` | `Long` | 使用有符号类型，避免超出 Java `Long` 范围 |
| 普通计数、版本号 | `INT` | `Integer` | 非负规则由业务和 CHECK 保证 |
| 大数量统计 | `BIGINT` | `Long` | 仅确有需要时使用 |
| 金额 | `DECIMAL(12,2)` | `BigDecimal` | 禁止使用 `float`、`double` |
| 布尔/开关 | `TINYINT` | `Integer` 或 `Boolean` | 必须限制为 0 或 1 |
| 日期时间 | `DATETIME` | `LocalDateTime` | 按 `Asia/Shanghai` 解释 |
| 仅日期 | `DATE` | `LocalDate` | 不附带时间 |
| 状态和类型 | `VARCHAR(32)` | Java Enum | 保存英文大写值 |
| 短文本 | `VARCHAR(n)` | `String` | 根据业务设置长度 |
| 审核意见、长说明 | `VARCHAR(1000)` 或 `TEXT` | `String` | 超过 1000 字符或无需索引时使用 TEXT |
| 非结构化快照 | `JSON` | 对应 DTO/String | 只保存不参与核心查询的快照 |

禁止把身份证号、学号、手机号等标识当作数值参与计算；其具体类型由表负责人根据长度、前导零和脱敏需求确定。

布尔字段示例：

```sql
enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
CONSTRAINT chk_xxx_enabled CHECK (enabled IN (0, 1))
```

`TINYINT(1)` 中的 `(1)` 只是显示宽度，不能保证值只能是 0 或 1，因此本项目不依赖该写法表达约束。

## 5. 公共字段

### 5.1 可变业务表

可新增、修改且需要保留历史删除记录的业务表默认包含：

```sql
id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
            ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
create_by   BIGINT NULL COMMENT '创建人',
update_by   BIGINT NULL COMMENT '更新人',
deleted     BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0 为有效，删除后写本行 id'
```

规则：

- 有效数据统一使用 `deleted = 0`；
- 逻辑删除统一执行 `deleted = id`；
- Java 中 `deleted` 使用 `Long`，不能按 Boolean 处理；
- 所有普通查询必须显式过滤 `deleted = 0`；
- 删除操作必须由表负责人 Service 执行；
- 已被其他业务引用的数据不得物理删除；
- 唯一约束需要允许删除后重建时，将 `deleted` 放入唯一键。

### 5.2 不可变审计和流水表

以下类型的表默认只允许新增，不允许更新或删除：

```text
审核记录
上报记录
操作日志
状态变化历史
金额确认历史
```

此类表至少包含：

```sql
id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
create_by   BIGINT NULL COMMENT '创建人'
```

不可变审计表通常不增加 `update_time` 和 `deleted`。确有冲正需求时新增一条冲正记录，不覆盖原记录。

### 5.3 纯关联表

纯关联表是否需要逻辑删除、操作人和更新时间，由表负责人根据以下规则决定：

- 需要保留分配历史：采用可变业务表公共字段；
- 只表示当前关系且可安全重建：至少包含主键和创建时间；
- 任何例外必须写入对应模块决策文件。

## 6. 并发和乐观锁

所有可能被并发修改且不能被覆盖的状态表都必须考虑乐观锁或条件原子更新，不限于库存、名额和额度表。

典型范围：

```text
application
礼包库存
学院和年级名额
学院和年级补助额度
批次状态
欠费确认状态
```

统一版本字段：

```sql
version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本'
```

条件更新示例：

```sql
UPDATE application
SET status = ?,
    current_level = ?,
    version = version + 1,
    update_time = CURRENT_TIMESTAMP,
    update_by = ?
WHERE id = ?
  AND status = ?
  AND version = ?
  AND deleted = 0;
```

受影响行数不是 1 时，Service 必须返回明确的状态冲突或版本冲突错误，不能继续写审核记录或扣减资源。

资源扣减优先使用带余额条件的原子更新、行锁或乐观锁，禁止“先查余额，再无条件更新”。

## 7. 枚举和状态

- 状态、类型、动作使用 `VARCHAR(32)`；
- 数据库存英文大写值；
- 不使用 MySQL `ENUM`；
- Java 使用 Enum，不在业务代码中散落字符串；
- 前端不得提交目标状态，只提交业务动作；
- 后端 Service 根据当前状态、用户权限和动作计算新状态；
- 公共枚举变化必须更新状态文档、接口文档、数据库设计和变更日志。

申请类型：

```text
GREEN_CHANNEL
LIVING_SUBSIDY
TRAVEL_SUBSIDY
```

申请来源：

```text
STUDENT
SCHOOL_PROXY
SUPPLEMENT
```

申请状态：

```text
DRAFT
COUNSELOR_PENDING
COUNSELOR_RETURNED
COLLEGE_PENDING
COLLEGE_RETURNED
SCHOOL_PENDING
SCHOOL_RETURNED
REJECTED
APPROVED
CONFIRM_PENDING
COMPLETED
CANCELLED
```

状态含义和允许流转以 `docs/status-flow.md` 为准，本规范不重复决定状态机。

## 8. 统一申请模型

系统只保留一张申请主表 `application`。

| 申请场景 | `application_type` | 对应详情 |
|---|---|---|
| 绿色通道，可包含欠费和礼包 | `GREEN_CHANNEL` | `arrears_application`、`gift_application` |
| 生活补助 | `LIVING_SUBSIDY` | `subsidy_application` |
| 路费补助 | `TRAVEL_SUBSIDY` | `subsidy_application` |

关系模型：

```text
student
  └─ application
      ├─ arrears_application       0..1
      ├─ gift_application          0..1
      │   └─ gift_application_item 0..N
      ├─ subsidy_application       0..1
      ├─ application_attachment    0..N
      └─ approval_record           0..N
```

说明：

- `application` 是唯一流程主表；
- 欠费和礼包是 `GREEN_CHANNEL` 的可选详情；
- `subsidy_application` 是补助详情表，不是第二张申请主表；
- 一个 `GREEN_CHANNEL` 至少包含欠费或礼包中的一项；
- `LIVING_SUBSIDY`、`TRAVEL_SUBSIDY` 必须存在对应补助详情；
- 退回后只能修改原申请，不能创建第二条有效申请。

同一学生、同一批次、同一申请类型只能有一条有效申请。采用“删除标记写本行 ID”的逻辑删除方案时，唯一约束为：

```sql
UNIQUE KEY uk_application_student_batch_type_deleted
    (student_id, batch_id, application_type, deleted)
```

`application` 的最终字段、DDL、Entity 和 Mapper 由成员二维护。成员三可以提出状态字段需求并负责状态流转逻辑，但不能直接修改该表结构或建立写 Mapper。

## 9. 表所有权和跨模块写入

表所有权以 `docs/collaboration-rules.md` 的完整名单为准。核心边界如下：

| 成员 | 直接维护范围 |
|---|---|
| 成员一 | 用户、角色、学院、专业、年级、班级、学生、辅导员关系、批次和政策基础数据 |
| 成员二 | 申请主表、申请详情、附件、礼包、库存、名额和补助额度 |
| 成员三 | 审核记录、审核上报、系统消息和消息已读记录 |
| 成员四 | 欠费确认；统计默认只查询和聚合 |

数据库权限规则：

1. 只有表负责人可以创建或修改该表的 DDL、Entity 和 Mapper；
2. 只有表负责人模块可以直接执行该表的 `INSERT`、`UPDATE`、`DELETE`；
3. 其他模块需要写入时必须调用表负责人提供的 Service；
4. 其他成员不得为同一张表建立第二个写 Mapper；
5. 跨模块查询也应优先使用负责人提供的稳定查询 Service 或已确认 DTO；
6. 共享字段变化必须由受影响成员确认。

`application.status` 和 `application.current_level` 的业务规则由成员三负责，但物理写入由成员二提供的 Service 完成。成员四改变申请状态时调用成员三的状态流转 Service，不能直接更新 `application`。

## 10. 索引规范

### 10.1 基本规则

- 主键自动建立聚簇索引；
- 外键或逻辑关联列必须有可用索引；
- 索引必须根据真实查询条件、排序和分页设计；
- 优先设计有业务意义的联合索引；
- 联合索引已覆盖左前缀时，不重复建立相同单列索引；
- 低区分度状态字段通常不单独建立索引，应与批次、学院或时间组合；
- 不为很少查询、频繁更新或超长文本字段盲目建立索引；
- 新增索引前必须检查现有索引是否重复或被覆盖。

审核常见索引示例：

```sql
KEY idx_application_batch_status (batch_id, status),
KEY idx_application_student_status (student_id, status),
KEY idx_approval_record_application_time (application_id, create_time, id)
```

索引列顺序应依据主要查询确定，示例不能直接复制到所有表。

### 10.2 唯一约束

业务唯一性应尽量由数据库唯一约束保证，不能只在 Service 中先查询再插入。

建立唯一约束前必须明确：

- 是否只约束有效数据；
- `NULL` 是否允许重复；
- 逻辑删除后是否允许重新创建；
- 并发插入冲突对应的业务错误码。

## 11. 外键规范

本项目对稳定的主从关系使用物理外键，默认规则为：

```text
ON UPDATE RESTRICT
ON DELETE RESTRICT
```

禁止对申请、审核、确认和审计记录使用 `ON DELETE CASCADE`。

规则：

- 父表必须先创建，子表负责人负责提交子表外键；
- 跨模块外键在父表结构合并并稳定后新增；
- 允许为空的可选关系可以使用 `ON DELETE SET NULL`，但必须在数据库设计中说明；
- 不建立物理外键的特殊情况必须在模块决策文件中写明原因、数据校验方式和孤儿数据检查方案；
- 逻辑删除不会触发外键动作，Service 仍需校验业务引用关系。

## 12. SQL 文件职责

```text
database/01_create_database.sql
```

只创建数据库、字符集和排序规则。

```text
database/02_create_tables.sql
```

保存当前最新、可用于全新环境一次性建表的完整结构。每次批准的结构变化都必须同步更新该文件。

```text
database/03_init_data.sql
```

保存角色、字典、默认配置等必要初始化数据。初始化数据变化时同步更新该文件和对应数据 migration。

```text
database/04_test_data.sql
```

只保存可公开提交的本地开发和演示测试数据，不得包含真实个人信息。

```text
database/migrations/
```

保存已经建库后的增量变化。历史 migration 一旦合并到 `main` 或应用到共享数据库，就不得修改、覆盖或删除。

## 13. Migration 规范

### 13.1 文件名

统一格式：

```text
VyyyyMMdd_HHmmss_MN__short_description.sql
```

其中 `MN` 为成员编号：

```text
01 成员一
02 成员二
03 成员三
04 成员四
```

示例：

```text
V20260717_143000_03__create_approval_record.sql
```

创建 migration 前先在 `docs/change-log.md` 登记并占用版本号。版本号不得重复，描述使用小写下划线英文。

### 13.2 允许的变更

Migration 可以包含经过批准的：

```text
CREATE TABLE
ALTER TABLE ADD/MODIFY/CHANGE/DROP
CREATE/DROP INDEX
ADD/DROP CONSTRAINT
数据回填
必要初始化数据变化
```

普通变更优先采用向后兼容的新增方式。字段改名或删除采用：

```text
新增新字段
-> 双写或迁移数据
-> 修改读取逻辑
-> 完成兼容验证
-> 下一次迁移删除旧字段
```

破坏性变更必须额外说明：

- 受影响接口和模块；
- 数据备份方式；
- 数据迁移或回填 SQL；
- 兼容窗口；
- 失败后的人工恢复方法。

MySQL DDL 可能隐式提交，不能假设一个包含多个 DDL 的 migration 可以整体事务回滚。

### 13.3 每次结构变更必须同步

```text
docs/change-log.md
docs/database-design.md
database/02_create_tables.sql
database/migrations/对应增量SQL
表负责人的 DDL、Entity、Mapper
受影响的 DTO/VO、接口文档和测试
```

初始化数据变化时还必须同步 `database/03_init_data.sql`。

### 13.4 Migration 文件头

```sql
-- 变更编号：CHG-20260717-001
-- 负责人：成员三
-- 影响模块：三级审核
-- 影响表：approval_record
-- 用途：创建审核记录表
-- 前置条件：application 表已经存在
-- 数据处理：无
-- 恢复方式：未写入业务数据时可人工删除新表
```

## 14. 数据安全和审计

- 不提交数据库用户名、密码、真实连接地址和生产数据；
- 不提交真实身份证号、手机号、附件和导出文件；
- 测试数据使用明显虚构内容；
- 密码只保存安全哈希；
- 附件表只保存受控文件标识和必要元数据，不保存可猜测的公开路径；
- 日志和 JSON 快照不得保存密码、JWT、完整身份证号、完整手机号或附件内容；
- 审核、确认、取消、库存和额度变化必须保留操作人、时间、业务编号、原值、新值和处理结果；
- 审计记录不得因业务申请取消或逻辑删除而丢失。

## 15. DDL 和共享数据库权限

- 只有表负责人可以提交该表的 DDL、索引、约束、Entity 和 Mapper 变化；
- 共享集成数据库的 DDL 由仓库负责人根据已经合并到 `main` 的 SQL 统一执行；
- 其他成员只能在本地数据库验证本人模块 migration；
- 禁止在共享数据库中手工改表后不提交 SQL；
- 禁止先修改共享数据库再补文档；
- 本地数据库可以重建，但最终结构必须以仓库已批准 SQL 为准。

## 16. 提交前检查

- [ ] 当前变更由目标表负责人提交；
- [ ] 已在 `docs/change-log.md` 登记并获得必要确认；
- [ ] 表名、字段名、类型和公共字段符合本规范；
- [ ] Java 类型与 MySQL 类型范围一致；
- [ ] 金额使用 `DECIMAL(12,2)` 和 `BigDecimal`；
- [ ] 布尔字段限制为 0 或 1；
- [ ] 时间字段、JVM、数据库连接使用统一时区；
- [ ] 逻辑删除或不可变审计策略已经明确；
- [ ] 并发状态更新包含版本或条件原子更新；
- [ ] 索引服务于实际查询且没有明显重复；
- [ ] 外键及删除规则已经明确；
- [ ] `database/02_create_tables.sql` 已同步；
- [ ] 已新增且未修改历史 migration；
- [ ] 初始化数据变化已同步 `database/03_init_data.sql`；
- [ ] `docs/database-design.md` 已同步；
- [ ] DTO、VO、接口文档和测试已同步；
- [ ] 没有真实个人信息、密码、Token 或本地连接配置；
- [ ] 已说明破坏性变更的数据备份、兼容和恢复方式。

## 17. 参考

- [MySQL 8.0 CHECK Constraints](https://dev.mysql.com/doc/refman/8.0/en/create-table-check-constraints.html)
- [MySQL Numeric Data Type Syntax](https://dev.mysql.com/doc/refman/8.0/en/numeric-type-syntax.html)
- [MySQL Foreign Key Constraints](https://dev.mysql.com/doc/refman/8.0/en/create-table-foreign-keys.html)
