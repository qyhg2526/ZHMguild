# ZHMguild

一款功能完整的 Minecraft **玩家公会插件**，参考 [PlayerGuild](https://ricedoc.handyplus.cn/wiki/PlayerGuild/) 的功能设计，
面向 **Paper 26.2**（Minecraft 26.2，Java 25）开发。

> 服务器版本：Paper / Spigot 26.2+ ｜ 编译目标：Java 25 ｜ 存储：SQLite（默认）/ MySQL

---

## 一、功能一览

| 分类 | 功能 |
| :--- | :--- |
| 公会基础 | 创建、解散、改名、公告、图标、PVP 开关 |
| 成员管理 | 职位体系（会长 / 副会长 / 长老 / 成员）、提升、降职、踢出、转让会长 |
| 加入流程 | 玩家申请、管理员审批、主动邀请、同意 / 拒绝 |
| 全 GUI | 主界面、成员列表、成员管理、入会申请、公会列表、排行榜、设置、确认框 |
| 成长体系 | 公会等级、升级消耗资金与活跃、等级称号、最大成员数随等级提升 |
| 经济体系 | 公会资金、个人贡献、每日签到（连续签到加成）、Vault 金币、PlayerPoints 点券 |
| 活跃体系 | 在线每分钟自动累积、月度活跃、每月 1 日自动重置 |
| 公会主城 | 设置主城、读条传送、冷却、移动打断 |
| 公会聊天 | 独立频道、`!` 快捷前缀、管理員监听 |
| 数据安全 | 异步写入、定时自动保存、自动备份（SQLite 复制 / MySQL 导出 SQL） |
| 扩展支持 | PlaceholderAPI 变量、MiniMessage 富文本、GUI 与语言 100% 可配置 |

---

## 二、安装

1. 将 `ZHMguild-1.0.0.jar` 放入服务端 `plugins/` 目录。
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
| `/zg chat [内容]` | 不带参数切换公会聊天频道；带内容直接发送 |
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
| `/zg confirm` / `/zg cancel` | 确认 / 取消待确认操作 |

### 管理员命令

| 命令 | 说明 |
| :--- | :--- |
| `/zg reload` | 重载 `config.yml` / `messages.yml` / `gui.yml` |
| `/zg view <guild\|player\|list> [名称]` | 查看公会 / 玩家 / 全部公会信息 |
| `/zg give <类型> <名称> <数量>` | 增加（类型：`guildMoney` `guildActive` `guildOre` `player`） |
| `/zg take <类型> <名称> <数量>` | 减少 |
| `/zg set <类型> <名称> <数量>` | 设置 |
| `/zg adminCreate <公会名> [玩家]` | 管理员直接创建公会（不消耗费用，支持离线玩家） |
| `/zg adminEditGuildName <旧名> <新名>` | 管理员改名 |
| `/zg setRole <玩家> <LEADER\|VICE\|ELDER\|MEMBER>` | 设置职位 |
| `/zg adminUp <公会名> [等级]` | 强制升级或指定等级 |
| `/zg join <公会名> <玩家>` | 强制把玩家加入公会 |
| `/zg refresh [公会名]` | 刷新公会数据（修复会长、称号、索引） |
| `/zg clear <application\|guild>` | 清理入会申请 / 清空全部公会 |
| `/zg reward <money\|points\|active\|contribution> <玩家> <数量>` | 发放奖励 |
| `/zg spy` | 监听所有公会聊天 |
| `/zg debug` | 自检：GUI 配置、存储、挂钩状态、公会数据统计 |

`<名称>` 支持内部变量：`${player}`（执行者名）、`${guildName}`（执行者所在公会名）。

---

## 四、权限

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
| `zhmguild.chat` | 公会聊天 | 所有人 |
| `zhmguild.contribute` | 贡献金币 | 所有人 |
| `zhmguild.kick` / `zhmguild.transfer` / `zhmguild.notice` | 成员与公告管理 | 所有人 |
| `zhmguild.view` / `give` / `take` / `set` / `refresh` / `dissolve` | 管理员操作 | OP |
| `zhmguild.adminCreate` / `adminEditGuildName` / `adminUp` / `setRole` / `join` / `clear` / `reward` | 管理员操作 | OP |

> 公会内部还有一层职位校验：`VICE`（副会长）及以上才能审批申请、修改公告/图标/PVP、提升成员；
> 只有 `LEADER`（会长）才能降职、转让、解散。可在 `config.yml → home.set-permission` 调整主城设置所需职位。

---

## 五、配置文件

| 文件 | 说明 |
| :--- | :--- |
| `config.yml` | 存储、创建花费、等级、签到、主城、聊天、活跃、备份等 |
| `messages.yml` | 全部提示文本（MiniMessage 格式，`{xxx}` 占位符） |
| `gui.yml` | 全部界面标题、大小、按钮材质 / 名称 / 描述 / 槽位 |
| `data.yml` | 插件内部状态（月度活跃重置记录），无需手动修改 |
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

切换存储后需要手动迁移数据（`/zg give` 等指令或直接搬库）。

### 等级配置

```yaml
levels:
  1:
    max-members: 10     # 该等级最大成员数
    up-money: 5000.0    # 升到下一级所需公会资金
    up-active: 200      # 升到下一级所需公会活跃
    tag: '<gray>[新芽]</gray>'   # 该等级公会称号
  max-level: 6
```

---

## 六、PlaceholderAPI 变量

| 变量 | 说明 |
| :--- | :--- |
| `%zhmguild_has_guild%` | 是否已加入公会（`true` / `false`） |
| `%zhmguild_name%` | 公会名称 |
| `%zhmguild_tag%` | 公会称号（含等级称号） |
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

## 七、文本格式

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

## 八、从源码构建

环境要求：**JDK 25**、Maven 3.9+

```bash
mvn clean package
```

产物：`target/ZHMguild-1.0.0.jar`（已内置 HikariCP、sqlite-jdbc、mysql-connector-j，开箱即用）。

### 已验证环境

| 项目 | 版本 |
| :--- | :--- |
| 服务端 | Paper 26.2 build 129（`Implementing API version 26.2.build.129-stable`） |
| 运行时 | Zulu OpenJDK 25.0.4 |
| 编译目标 | `--release 25` |
| 存储 | SQLite（默认） / MySQL |

已通过的端到端测试：插件加载、`api-version: 26.2` 校验、SQLite 建表、公会创建 / 改名 / 升级 / 解散、
资金与活跃与矿石与贡献增删改、职位设置、数据跨重启持久化、`/zg debug` GUI 配置自检、控制台与玩家命令分支。

---

## 九、项目结构

```
src/main/java/cn/zhm/guild/
├── ZHMguildPlugin.java          主类
├── command/GuildCommand.java    命令与 Tab 补全
├── config/                      config / messages / gui 读取
├── model/                       Guild / GuildMember / GuildApplication / 枚举
├── storage/                     Database(HikariCP) + GuildStorage(DAO)
├── manager/                     公会缓存、业务动作、聊天、签到、传送、备份
├── gui/                         GUI 框架与全部菜单
├── listener/                    玩家与聊天事件
├── hook/                        Vault / PlayerPoints / PlaceholderAPI 挂钩
└── util/                        文本、物品、时间、占位符工具
```

---

## 十、开源协议

本项目基于 [MIT License](LICENSE) 开源。

> 本项目为独立实现，与 PlayerGuild 无任何代码关联，仅参考了其公开文档所描述的功能设计。
