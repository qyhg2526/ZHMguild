# ZHMguild

一款功能完整的 Minecraft **玩家公会插件**，参考 [PlayerGuild](https://ricedoc.handyplus.cn/wiki/PlayerGuild/) 的功能设计，
面向 **Paper 26.2**（Minecraft 26.2，Java 25）开发。

> 服务器版本：Paper / Spigot 26.2+ ｜ 编译目标：Java 25 ｜ 存储：SQLite（默认）/ MySQL
>
> 当前版本：**1.0.1**（[更新记录](#十一更新记录)）

---

## 一、功能一览

| 分类 | 功能 |
| :--- | :--- |
| 公会基础 | 创建、解散、改名、公告、图标、PVP 开关 |
| 成员管理 | 职位体系（会长 / 副会长 / 长老 / 成员）、提升、降职、踢出、转让会长 |
| 加入流程 | 玩家申请、管理员审批、主动邀请、同意 / 拒绝 |
| 全 GUI | 主界面、成员列表、成员管理、入会申请、公会列表、排行榜、设置、公会战、确认框 |
| 成长体系 | 公会等级、升级消耗资金与活跃、最大成员数随等级提升 |
| 经济体系 | 公会资金、个人贡献、每日签到（连续签到加成）、Vault 金币、PlayerPoints 点券 |
| 活跃体系 | 在线每分钟自动累积、月度活跃、每月 1 日自动重置 |
| 公会主城 | 设置主城、读条传送、冷却、移动打断 |
| **匹配公会战** | 公会排队匹配、场地自动分配、准备 / 战斗 / 结算三阶段、淘汰观战、全灭或超时判定、胜负奖励、每日次数限制 |
| 数据安全 | 异步写入、定时自动保存、自动备份（SQLite 复制 / MySQL 导出 SQL） |
| 扩展支持 | PlaceholderAPI 变量、MiniMessage 富文本、GUI 与语言 100% 可配置 |

---

## 二、安装

1. 将 `ZHMguild-1.0.1.jar` 放入服务端 `plugins/` 目录。
2. 启动服务器（需要 **Java 25**）。
3. 编辑 `plugins/ZHMguild/config.yml` 中的存储配置。
4. 执行 `/zg reload` 或重启服务器。

### 前置插件（均为可选）

| 插件 | 用途 |
| :--- | :--- |
| [Vault](https://www.spigotmc.org/resources/vault.34315/) + 经济插件 | 创建公会花费、贡献、签到奖励 |
| [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) | 计分板 / 聊天变量 |
| [PlayerPoints](https://modrinth.com/plugin/playerpoints) | 点券消耗与奖励 |

未安装时相关功能会自动降级，不会报错。

---

## 三、命令

主命令：`/zhmguild`，别名：`/zg`、`/gh`、`/guild`、`/gonghui`

### 玩家命令

| 命令 | 说明 |
| :--- | :--- |
| `/zg` | 打开公会主界面（无公会时打开引导界面） |
| `/zg help` | 查看帮助 |
| `/zg create [公会名]` | 创建公会（不带参数则走 GUI + 聊天栏输入流程） |
| `/zg open [排序]` | 打开公会列表（排序：`LEVEL` `ACTIVE` `MONTH_ACTIVE` `FUNDS` `MEMBERS` `CREATE_TIME`） |
| `/zg me` | 打开我的公会 |
| `/zg join <公会名>` | 申请加入公会（若有邀请则直接加入） |
| `/zg accept [公会名]` | 同意入会申请 / 接受邀请 |
| `/zg deny [公会名]` | 拒绝入会申请 / 拒绝邀请 |
| `/zg invitation <玩家>` | 邀请玩家加入公会 |
| `/zg leave` | 退出公会（需二次确认） |
| `/zg up` | 升级公会 |
| `/zg spawn` | 传送回公会主城 |
| `/zg sethome` | 把当前位置设为公会主城 |
| `/zg signin` | 每日签到 |
| `/zg contribute <数量>` | 向公会贡献金币 |
| `/zg members [公会名]` | 查看成员 |
| `/zg info [公会名]` | 查看公会详细信息 |
| `/zg top [排序]` | 查看公会排行 |
| `/zg notice <内容>` | 修改公会公告 |
| `/zg kick <玩家>` | 踢出成员 |
| `/zg promote <玩家>` / `/zg demote <玩家>` | 提升 / 降低职位 |
| `/zg transfer <玩家>` | 转让会长 |
| `/zg pvp` | 切换公会 PVP |
| `/zg dissolve` | 解散公会（需二次确认） |
| `/zg war` | 打开匹配公会战界面 |
| `/zg war match` | 发起匹配（需要副会长及以上） |
| `/zg war cancel` | 取消匹配（需要副会长及以上） |
| `/zg war leave` | 离开战斗（准备阶段无损失，战斗阶段视为被淘汰） |
| `/zg war status` | 查看公会战状态 |
| `/zg confirm` / `/zg cancel` | 确认 / 取消待确认操作 |

### 管理员命令

| 命令 | 说明 |
| :--- | :--- |
| `/zg reload` | 重载 `config.yml` / `messages.yml` / `gui.yml` |
| `/zg setLocation mate <场地名> <1\|2\|3>` | 设置公会战场地出生点（站在该位置执行）<br>`1` = 红队出生点，`2` = 蓝队出生点，`3` = 观看点（可选） |
| `/zg war list` | 查看全部公会战场地与占用状态 |
| `/zg war start <红队公会> <蓝队公会> [场地名]` | 强制开战 |
| `/zg war stop [all\|战斗ID]` | 强制结束进行中的公会战 |
| `/zg war queue <公会名>` / `unqueue <公会名>` | 强制让公会入队 / 出队 |
| `/zg view <guild\|player\|list> [名称]` | 查看公会 / 玩家 / 全部公会信息 |
| `/zg give <类型> <名称> <数量>` | 增加（类型：`guildMoney` `guildActive` `guildOre` `player`） |
| `/zg take <类型> <名称> <数量>` | 减少 |
| `/zg set <类型> <名称> <数量>` | 设置 |
| `/zg adminCreate <公会名> [玩家]` | 管理员直接创建公会（不消耗费用，支持离线玩家） |
| `/zg adminEditGuildName <旧名> <新名>` | 管理员改名 |
| `/zg setRole <玩家> <LEADER\|VICE\|ELDER\|MEMBER>` | 设置职位 |
| `/zg adminUp <公会名> [等级]` | 强制升级或指定等级 |
| `/zg join <公会名> <玩家>` | 强制把玩家加入公会 |
| `/zg refresh [公会名]` | 刷新公会数据（修复会长、索引） |
| `/zg clear <application\|guild>` | 清理入会申请 / 清空全部公会 |
| `/zg reward <money\|points\|active\|contribution> <玩家> <数量>` | 发放奖励 |
| `/zg debug` | 自检：GUI 配置、存储、挂钩状态、公会数据统计 |

`<名称>` 支持内部变量：`${player}`（执行者名）、`${guildName}`（执行者所在公会名）。

---

## 四、匹配公会战

### 4.1 玩法流程

```
公会长/副会长 /zg war match
        ↓  进入匹配队列
队列中出现 2 个公会 且有「空闲且就绪」的场地
        ↓  自动配对(先入队先匹配)
传送到场 → 准备阶段(默认 30s, 不可互相攻击, 可自由退出)
        ↓
战斗阶段(默认 600s, 双方可互相攻击, 同公会仍然免伤)
        ↓  一方全灭 或 超时
结算 → 发放奖励 → 所有参战(含被淘汰)玩家传送回战前位置
```

- 参战成员为**开战瞬间在线的公会成员**，按职位与贡献排序，每方最多 `max-players-per-side` 人。
- 战斗阶段死亡即**淘汰**：默认保留物品与经验，并在**观看点**复活观战（未设置观看点则回到战前位置）。
- 胜负判定：一方全灭即负；超时则先比存活人数、再比总击杀，仍相同为平局。
- 若无空闲场地，公会会留在队列中等待，场地空出后自动开战。
- 准备与战斗期间**禁止丢弃物品**与**飞行**，可配置**禁用指定指令**（防止 `/spawn` 逃跑，管理员豁免）。
- 每个公会每日可发起次数由 `guild-war.daily-limit` 限制（默认 3 次，0 = 不限）。

### 4.2 场地设置

管理员在场地内站到目标位置执行：

```
/zg setLocation mate 示例场地 1     # 红队出生点
/zg setLocation mate 示例场地 2     # 蓝队出生点
/zg setLocation mate 示例场地 3     # 观看点(可选)
```

可创建多个场地，匹配时自动挑选空闲的一个；场地会保存到 `plugins/ZHMguild/arena.yml`。

### 4.3 奖励配置

```yaml
guild-war:
  rewards:
    win:       { contribution: 500.0, guild-active: 100, guild-funds: 5000.0, commands: [] }
    lose:      { contribution: 150.0, guild-active: 30,  guild-funds: 1000.0, commands: [] }
    draw:      { contribution: 250.0, guild-active: 50,  guild-funds: 2000.0, commands: [] }
    per-kill:  { contribution: 20.0 }   # 每次击杀的额外个人贡献
```

`commands` 中的指令会以控制台身份执行，支持 `{player}` 与 `{guild}` 占位符，例如：

```yaml
      commands:
        - 'give {player} diamond 1'
        - 'broadcast 公会 {guild} 赢得了公会战!'
```

---

## 五、权限

| 权限 | 说明 | 默认 |
| :--- | :--- | :--- |
| `zhmguild.use` | 使用公会基础功能 | 所有人 |
| `zhmguild.admin` | 管理员总权限（含以下全部） | OP |
| `zhmguild.reload` | 重载配置 | OP |
| `zhmguild.create` | 创建公会 | 所有人 |
| `zhmguild.create.color` | 公会名可使用颜色代码 | OP |
| `zhmguild.spawn` / `zhmguild.setSpawn` | 传送 / 设置公会主城 | 所有人 |
| `zhmguild.up` | 升级公会 | 所有人 |
| `zhmguild.leave` | 退出公会 | 所有人 |
| `zhmguild.accept` / `zhmguild.deny` | 同意 / 拒绝申请 | 所有人 |
| `zhmguild.open` / `zhmguild.me` | 打开界面 | 所有人 |
| `zhmguild.invitation` | 邀请玩家 | 所有人 |
| `zhmguild.signIn` | 每日签到 | 所有人 |
| `zhmguild.contribute` | 贡献金币 | 所有人 |
| `zhmguild.kick` / `zhmguild.transfer` / `zhmguild.notice` | 成员与公告管理 | 所有人 |
| `zhmguild.war` | 发起 / 取消匹配公会战 | 所有人 |
| `zhmguild.setLocation` | 设置公会战场地 | OP |
| `zhmguild.warStart` / `zhmguild.warStop` | 强制开战 / 强制结束 | OP |
| `zhmguild.view` / `give` / `take` / `set` / `refresh` / `dissolve` | 管理员操作 | OP |
| `zhmguild.adminCreate` / `adminEditGuildName` / `adminUp` / `setRole` / `join` / `clear` / `reward` | 管理员操作 | OP |

> 公会内部还有一层职位校验：`VICE`（副会长）及以上才能审批申请、修改公告/图标/PVP、提升成员、发起公会战匹配；
> 只有 `LEADER`（会长）才能降职、转让、解散。可在 `config.yml → home.set-permission` 调整主城设置所需职位。

---

## 六、配置文件

| 文件 | 说明 |
| :--- | :--- |
| `config.yml` | 存储、创建花费、等级、签到、主城、活跃、公会战、备份等 |
| `messages.yml` | 全部提示文本（MiniMessage 格式，`{xxx}` 占位符） |
| `gui.yml` | 全部界面标题、大小、按钮材质 / 名称 / 描述 / 槽位 |
| `arena.yml` | 公会战场地出生点（由 `/zg setLocation` 自动生成） |
| `data.yml` | 插件内部状态（月度活跃重置记录、公会战每日次数），无需手动修改 |
| `backUp/` | 自动备份目录 |

### 存储配置

```yaml
storage:
  type: SQLITE          # 或 MYSQL
  table-prefix: zhm_
  auto-save-minutes: 10
  mysql:
    host: 127.0.0.1
    port: 3306
    database: zhmguild
    username: root
    password: ''
    pool-size: 6
```

切换存储后需要手动迁移数据。

### 等级配置

```yaml
levels:
  1:
    max-members: 10     # 该等级最大成员数
    up-money: 5000.0    # 升到下一级所需公会资金
    up-active: 200      # 升到下一级所需公会活跃
  max-level: 6
```

---

## 七、PlaceholderAPI 变量

| 变量 | 说明 |
| :--- | :--- |
| `%zhmguild_has_guild%` | 是否已加入公会（`true` / `false`） |
| `%zhmguild_name%` | 公会名称 |
| `%zhmguild_level%` | 公会等级 |
| `%zhmguild_active%` | 公会活跃 |
| `%zhmguild_month_active%` | 公会月度活跃 |
| `%zhmguild_funds%` | 公会资金 |
| `%zhmguild_ore%` | 公会矿石 |
| `%zhmguild_members%` / `%zhmguild_max_members%` | 当前 / 最大成员数 |
| `%zhmguild_leader%` | 会长名称 |
| `%zhmguild_notice%` | 公会公告 |
| `%zhmguild_create_time%` | 创建时间 |
| `%zhmguild_rank%` | 按等级排名 |
| `%zhmguild_role%` | 玩家在公会中的职位 |
| `%zhmguild_contribution%` | 玩家贡献 |
| `%zhmguild_sign_streak%` | 连续签到天数 |
| `%zhmguild_last_signin%` | 最后签到日期 |
| `%zhmguild_top_<类型>_<名次>%` | 排行榜，例如 `%zhmguild_top_level_1%`、`%zhmguild_top_funds_3%` |

---

## 八、文本格式

所有文本使用 **MiniMessage**，与 PlaceholderAPI 兼容：

```yaml
# 颜色与渐变
'<red>红色</red> <#ff8800>十六进制</#ff8800> <gradient:#00d2ff:#3a7bd5>渐变</gradient>'
# 传统颜色代码同样可用（会自动转换）
'&a绿色 &l加粗'
# 占位符使用 {xxx}
'<gray>欢迎 <yellow>{player}</yellow> 加入 <yellow>{guild}</yellow></gray>'
# 可直接写 PlaceholderAPI 变量
'<gray>你的金币: <yellow>%vault_eco_balance%</yellow></gray>'
```

---

## 九、从源码构建

环境要求：**JDK 25**、Maven 3.9+

```bash
mvn clean package
```

产物：`target/ZHMguild-1.0.1.jar`（已内置 HikariCP、sqlite-jdbc、mysql-connector-j，开箱即用）。

### 已验证环境

| 项目 | 版本 |
| :--- | :--- |
| 服务端 | Paper 26.2 build 129（`Implementing API version 26.2.build.129-stable`） |
| 运行时 | Zulu OpenJDK 25.0.4 |
| 编译目标 | `--release 25` |
| 存储 | SQLite（默认） / MySQL |

已通过的端到端测试：插件加载、`api-version: 26.2` 校验、SQLite 建表、公会创建 / 改名 / 升级 / 解散、
资金与活跃与矿石与贡献增删改、职位设置、数据跨重启持久化、**1.0.0 → 1.0.1 旧库迁移**、
`/zg debug` GUI 配置自检、控制台与玩家命令分支、**公会战场地加载 / 入队 / 自动匹配 / 场地占用与释放 / 失败回滚**。

---

## 十、项目结构

```
src/main/java/cn/zhm/guild/
├── ZHMguildPlugin.java          主类
├── command/GuildCommand.java    命令与 Tab 补全
├── config/                      config / messages / gui 读取
├── model/                       Guild / GuildMember / Arena / GuildWar / 枚举
├── storage/                     Database(HikariCP) + GuildStorage(DAO)
├── manager/                     公会缓存、业务动作、签到、传送、场地、公会战、备份
├── gui/                         GUI 框架与全部菜单
├── listener/                    玩家 / 聊天输入 / 公会战事件
├── hook/                        Vault / PlayerPoints / PlaceholderAPI 挂钩
└── util/                        文本、物品、时间、占位符工具
```

---

## 十一、更新记录

### 1.0.1

**移除**

- 删除 **公会聊天系统**：移除聊天频道、`!` 快捷前缀、管理员监听与 `/zg chat` `/zg spy` 命令，
  聊天栏现在仅用于创建公会 / 修改公告等参数输入。
- 删除 **公会称号系统**：公会仅保留「名称」与「职位」，移除等级称号、`%zhmguild_tag%` 变量与相关配置。
- 删除 GUI 中的 **潜行左键贡献**：箱子界面内无法进入潜行状态，贡献改为左键 / 右键两个档位。

**新增**

- **匹配公会战**：场地管理（`/zg setLocation mate`）、匹配队列、自动配对、准备 / 战斗 / 结算三阶段、
  淘汰与观看点复活、全灭或超时判定、胜负与击杀奖励、每日次数限制、禁止丢弃物品 / 飞行 / 指定指令。
- `/zg war` 系列命令与独立 GUI 菜单。
- `/zg debug` 增加 GUI 配置自检覆盖到公会战菜单。

**变更**

- 数据库表移除 `tag` 列（旧库无需迁移，缺少该列时使用默认值）。
- 版本号提升至 `1.0.1`；配置与语言文件同步更新，旧配置文件可直接覆盖升级。

### 1.0.0

- 首个版本：公会创建 / 解散 / 加入 / 审批 / 邀请、职位与成员管理、全 GUI、贡献 / 资金 / 等级 / 活跃、
  公会主城、每日签到、管理员命令、PlaceholderAPI 变量、SQLite / MySQL 存储。

---

## 十二、开源协议

本项目基于 [MIT License](LICENSE) 开源。

> 本项目为独立实现，与 PlayerGuild 无任何代码关联，仅参考了其公开文档所描述的功能设计。
