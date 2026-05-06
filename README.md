# Forge Packet Profiler 1.20.1

Forge Packet Profiler 是一个用于 Minecraft Forge 1.20.1 服务端的网络包观测模组。

它的目标不是修改、拦截或优化数据包，而是在活动服测试阶段先把“谁在喷包”找出来：按方向、玩家、包类型、Forge channel、modid、包频率和估算带宽做统计，并输出排行与风险标记，方便管理组提前定位高流量/高频同步来源。

## 功能

- 服务端侧 Netty pipeline 观测，不要求客户端安装。
- 统计服务端发往客户端和客户端发往服务端的登录后网络包。
- 记录 packet class、custom payload channel、modid、玩家、方向、包数、估算字节数、pps、bps、峰值等信息。
- 自动标记风险等级：绿色/黄色/红色。
- 支持控制台/游戏内命令查看排行。
- 定时输出 CSV 和 JSON 报表。

## 使用方式

下载或自行构建 jar 后，将产物放入 Forge 1.20.1 服务端的 `mods/` 目录。

客户端理论上不需要安装本模组，`mods.toml` 已设置为服务端观测用途。

## 命令

```text
/packetprofiler top
/packetprofiler top <limit>
/packetprofiler dump
/packetprofiler reset
```

- `top`：查看当前流量排行。
- `top <limit>`：指定显示数量，例如 `/packetprofiler top 20`。
- `dump`：立刻写出一次报表。
- `reset`：清空当前统计数据。

## 报表输出

报表会写入服务端世界目录下：

```text
world/serverconfig/packetprofiler/packet-profiler-latest.csv
world/serverconfig/packetprofiler/packet-profiler-latest.json
```

同时会保留带时间戳的历史报表，方便对比压测阶段不同时间段的流量变化。

CSV 适合丢进表格软件看排行，JSON 适合后续接网页面板、脚本分析或 Prometheus/监控系统。

## 构建

需要 JDK 17。

```bash
./gradlew build --no-daemon
```

构建产物位于：

```text
build/libs/packetprofiler-forge-1.20.1-0.1.0.jar
```

## GitHub Actions

仓库内置自动构建流程：每次 push 或 pull request 会运行 Gradle build，并上传 jar 产物。

## 当前限制

- 只统计玩家登录完成后的连接，不覆盖握手、状态 ping、login 阶段。
- 包大小为估算值，主要通过 packet 写入 `FriendlyByteBuf` 或 payload readable bytes 估算，不等同于完整 TCP/Netty 实际链路开销。
- 闭源或非标准模组的归属可能只能识别到 packet class 或 `unknown`。
- 当前版本只做观测和报表，不拦截、不限流、不改包内容。

## 适用场景

这个模组适合在大型活动服正式开始前压测使用：

1. 开服压测。
2. 使用 `/packetprofiler top` 或查看 CSV/JSON 报表。
3. 找出高带宽、高频、小包洪水或疑似广播倍率过高的来源。
4. 管理组再决定调整配置、替换模组、联系作者、二开修复，或后续做旁路转发方案。

## License

MIT
