# Forge Packet Profiler

Forge Packet Profiler is a server-side packet profiling mod for Minecraft Forge 1.20.1.

It is designed for pre-event load testing and network diagnostics on modded Minecraft servers. The mod observes packet traffic after player login, aggregates traffic statistics by packet type, player, direction, custom payload channel, and inferred mod id, then exports ranked reports with risk indicators.

The profiler is read-only. It does not block, rewrite, throttle, or redirect packets.

## Purpose

Large modded servers may experience severe network pressure even when CPU, memory, and TPS appear acceptable. In many cases, the cause is excessive packet synchronization from one or more mods, such as high-frequency custom payloads, large serialized state updates, or repeated broadcast traffic.

Forge Packet Profiler helps server operators identify these sources before an official event starts, so the operations team can adjust configuration, replace problematic mods, request upstream fixes, or develop targeted patches.

## Features

- Server-side only packet observation for Forge 1.20.1.
- No client-side installation required.
- Netty pipeline based profiling after player login.
- Clientbound and optional serverbound packet statistics.
- Aggregation by:
  - direction
  - player UUID and name
  - packet class
  - custom payload channel
  - inferred mod id
- Traffic metrics including:
  - total packets
  - estimated total bytes
  - packets per second
  - bytes per second
  - peak packet rate
  - peak bandwidth
  - small packet rate
- Automatic risk classification for high-bandwidth or high-frequency traffic.
- Periodic CSV and JSON report output.
- In-game and console commands for quick inspection.
- GitHub Actions based automated build.

## Installation

1. Build the mod or download the jar from a workflow artifact/release.
2. Place the jar into the `mods/` directory of a Forge 1.20.1 server.
3. Start the server normally.

The generated jar is named similar to:

```text
packetprofiler-forge-1.20.1-0.1.0.jar
```

This mod is intended to run on the server. Clients do not need to install it.

## Commands

```text
/packetprofiler top
/packetprofiler top <limit>
/packetprofiler dump
/packetprofiler reset
```

### `/packetprofiler top`

Displays the current highest traffic entries using the configured default limit.

### `/packetprofiler top <limit>`

Displays a custom number of ranked entries.

Example:

```text
/packetprofiler top 20
```

### `/packetprofiler dump`

Writes the current profiling data to report files immediately.

### `/packetprofiler reset`

Clears the current in-memory statistics.

## Report Output

Reports are written under the active world directory:

```text
world/serverconfig/packetprofiler/packet-profiler-latest.csv
world/serverconfig/packetprofiler/packet-profiler-latest.json
```

Timestamped historical reports are also retained in the same report directory.

Recommended usage:

- Use CSV files for spreadsheet review and manual ranking.
- Use JSON files for dashboards, scripts, or external monitoring integration.

## Risk Classification

Each traffic entry receives a risk level based on simple threshold rules. The current implementation is intended for operational triage rather than precise root-cause analysis.

Typical risk signals include:

- sustained high bandwidth
- high packet rate
- repeated small packets
- large per-player traffic
- suspicious custom payload traffic

The thresholds can be adjusted through the mod configuration.

## Mod Attribution

For Forge custom payload packets, the profiler attempts to infer the source mod from the channel namespace.

For example:

```text
examplemod:sync_state -> examplemod
```

For vanilla and Forge internal packets, the profiler reports `minecraft` or `forge` where possible. If the source cannot be inferred reliably, it is reported as `unknown` together with the packet class name.

## Build

Requirements:

- JDK 17
- Gradle wrapper included in this repository

Build command:

```bash
./gradlew build --no-daemon
```

Build output:

```text
build/libs/packetprofiler-forge-1.20.1-0.1.0.jar
```

## Automated Build

This repository includes a GitHub Actions workflow at:

```text
.github/workflows/build.yml
```

The workflow runs on push, pull request, and manual dispatch. It builds the project with JDK 17 and uploads the generated jar as an artifact.

## Limitations

- Only packets after player login are currently profiled.
- Handshake, status ping, and login-stage traffic are not included.
- Packet size is estimated by serialization or readable payload length and does not represent full TCP, encryption, compression, or Netty framing overhead.
- Some custom or obfuscated packets may only be identified by packet class.
- The profiler does not currently perform mitigation. It only observes and reports.

## Recommended Workflow

1. Install the profiler on a staging or pre-event server.
2. Run a realistic load test with the expected modpack and player count.
3. Use `/packetprofiler top` for immediate inspection.
4. Export reports with `/packetprofiler dump` or wait for scheduled output.
5. Review high-risk entries by mod id, packet type, and player impact.
6. Apply operational fixes such as configuration changes, mod replacement, upstream issue reports, or targeted patches.
7. Repeat the test and compare historical reports.

## License

This project is licensed under the MIT License.
