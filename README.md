# Forge Packet Profiler

Forge Packet Profiler 是一个面向 Minecraft Forge 1.20.1 服务端的网络包画像与流量诊断模组。

本项目用于大型模组服、活动服或压测环境，在玩家登录后对服务端网络包进行只读观测，按数据包方向、玩家、Packet 类型、Forge 自定义 Channel、推测 Mod ID、包频率与估算带宽进行聚合统计，并输出排行报表与风险标记。

本模组只负责观测与报表，不会拦截、修改、限流、丢弃或重定向任何数据包。

## 项目背景

在大型 Forge 模组服中，服务器卡顿并不总是由 CPU、内存或 TPS 直接造成。部分情况下，真正的瓶颈来自网络出口：某些模组可能以很高频率同步数据，或将较大的状态数据反复广播给大量玩家，导致百人规模的服务器也能产生数百 Mbps 的持续流量。

这类问题在正式活动开始后很难临时排查，因此更适合在测试阶段提前做网络画像，找出高带宽、高频率或异常广播的数据来源，再由管理组决定是否调整配置、替换模组、联系作者修复，或针对特定模组进行二次开发。

Forge Packet Profiler 的目标就是提供这个“活动前网络体检”能力。

## 功能特性

- 支持 Minecraft Forge 1.20.1 服务端。
- 服务端侧运行，客户端无需安装。
- 基于玩家登录后的 Netty Pipeline 进行只读观测。
- 支持统计服务端到客户端的下行数据包。
- 可选统计客户端到服务端的上行数据包。
- 按以下维度聚合统计：
  - 数据包方向
  - 玩家 UUID 与玩家名
  - Packet Java 类名
  - Forge Custom Payload Channel
  - 推测 Mod ID
- 输出以下指标：
  - 总包数
  - 估算总字节数
  - 每秒包数 pps
  - 每秒字节数 bps
  - 峰值 pps
  - 峰值 bps
  - 小包高频统计
- 自动标记风险等级，用于快速识别高流量或高频率来源。
- 支持命令查看当前 Top 排行。
- 支持 CSV 与 JSON 报表输出。
- 内置 GitHub Actions 自动构建流程。
- main 分支构建成功后自动发布 GitHub Release 并上传 jar。

## 安装方式

1. 从 GitHub Release 下载构建产物，或自行构建 jar。
2. 将 jar 放入 Forge 1.20.1 服务端的 `mods/` 目录。
3. 正常启动服务端。

构建产物名称类似：

```text
packetprofiler-forge-1.20.1-0.1.0.jar
```

本模组设计为服务端诊断工具，客户端不需要安装。

## 使用命令

```text
/packetprofiler top
/packetprofiler top <limit>
/packetprofiler dump
/packetprofiler reset
```

### `/packetprofiler top`

查看当前默认数量的网络流量排行。

### `/packetprofiler top <limit>`

指定显示排行数量。

示例：

```text
/packetprofiler top 20
```

### `/packetprofiler dump`

立即将当前统计结果写出为报表文件。

### `/packetprofiler reset`

清空当前内存中的统计数据，重新开始统计。

## 报表输出位置

报表会写入当前世界目录下：

```text
world/serverconfig/packetprofiler/packet-profiler-latest.csv
world/serverconfig/packetprofiler/packet-profiler-latest.json
```

同时会保留带时间戳的历史报表文件，方便对比不同压测阶段或不同时段的流量变化。

建议用法：

- CSV：适合导入表格软件，按带宽、包频率、Mod ID 等字段排序分析。
- JSON：适合接入脚本、网页面板、监控系统或后续自动分析流程。

## 风险标记说明

每条统计记录都会根据阈值规则生成风险等级。当前风险标记主要用于测试阶段的快速排查，不等同于精确的根因诊断。

常见高风险信号包括：

- 持续高带宽。
- 瞬时峰值过高。
- 包频率异常高。
- 小包数量过多。
- 单个玩家或单个 Packet 类型流量明显异常。
- 某个 Forge Channel 持续产生大量 Custom Payload。

阈值可通过配置项调整。

## Mod 归属推测

对于 Forge Custom Payload 数据包，本模组会尝试从 Channel 命名空间推测来源 Mod。

例如：

```text
examplemod:sync_state -> examplemod
```

对于原版或 Forge 内部数据包，会尽量标记为：

```text
minecraft
forge
```

如果无法可靠推测来源，则会标记为：

```text
unknown
```

并保留 Packet 类名，方便后续人工排查。

## 构建方式

构建环境要求：

- JDK 17
- 使用仓库自带 Gradle Wrapper

执行：

```bash
./gradlew build --no-daemon
```

构建完成后，jar 位于：

```text
build/libs/packetprofiler-forge-1.20.1-0.1.0.jar
```

## GitHub Actions

仓库包含自动构建流程：

```text
.github/workflows/build.yml
```

触发条件：

- push 到 main 分支
- pull request
- 手动 workflow_dispatch

其中 main 分支 push 构建成功后，会自动创建 GitHub Release，并将构建出的 jar 上传到 Release 附件。

## 推荐使用流程

1. 在正式活动前，将本模组安装到测试服或预演服。
2. 使用接近正式活动的模组包、玩家数量与玩法流程进行压测。
3. 使用 `/packetprofiler top` 查看实时排行。
4. 使用 `/packetprofiler dump` 导出报表，或等待定时输出。
5. 根据 CSV/JSON 报表查看高风险来源。
6. 管理组按情况处理：
   - 调整模组配置。
   - 替换高风险模组。
   - 向模组作者反馈。
   - 对开源模组做协议级优化。
   - 对闭源模组做外层兼容或旁路方案评估。
7. 再次压测并对比历史报表。

## 当前限制

- 当前只统计玩家登录完成后的连接。
- 不覆盖握手阶段、状态 ping 阶段和 login 阶段流量。
- 包大小为估算值，不包含完整 TCP、加密、压缩、Netty frame 等链路开销。
- 部分闭源、混淆或非标准模组只能识别到 Packet 类名，无法准确归属到 Mod ID。
- 当前版本只做观测，不提供自动治理能力。

## 许可证

本项目使用 MIT License。