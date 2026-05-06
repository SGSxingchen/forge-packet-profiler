package com.example.packetprofiler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public final class PacketReportWriter {
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "packetprofiler-report-writer");
        thread.setDaemon(true);
        return thread;
    });

    private PacketReportWriter() {}

    public static void dumpAsync(MinecraftServer server, String reason) {
        Path directory = reportDirectory(server);
        List<PacketStats.Row> rows = PacketStats.get().snapshot().stream()
            .sorted(Comparator.comparingLong(PacketStats.Row::totalBytes).reversed())
            .toList();
        String stamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(':', '-');
        WRITER.execute(() -> write(directory, rows, reason, stamp));
    }

    public static Path reportDirectory(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("packetprofiler");
    }

    private static void write(Path directory, List<PacketStats.Row> rows, String reason, String stamp) {
        try {
            Files.createDirectories(directory);
            Files.writeString(directory.resolve("packet-profiler-latest.csv"), toCsv(rows), StandardCharsets.UTF_8);
            Files.writeString(directory.resolve("packet-profiler-latest.json"), toJson(rows, reason), StandardCharsets.UTF_8);
            Files.writeString(directory.resolve("packet-profiler-" + stamp + ".csv"), toCsv(rows), StandardCharsets.UTF_8);
            Files.writeString(directory.resolve("packet-profiler-" + stamp + ".json"), toJson(rows, reason), StandardCharsets.UTF_8);
            PacketProfilerMod.LOGGER.info("Packet profiler wrote {} rows to {}", rows.size(), directory.toAbsolutePath());
        } catch (IOException ex) {
            PacketProfilerMod.LOGGER.warn("Failed to write packet profiler report", ex);
        }
    }

    private static String toCsv(List<PacketStats.Row> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append("direction,player_uuid,player_name,modid,channel,packet_class,last_pps,last_bps,peak_pps,peak_bps,total_packets,total_bytes,small_pps,risk,reason\n");
        for (PacketStats.Row row : rows) {
            PacketStatKey key = row.key();
            builder.append(key.direction()).append(',')
                .append(key.playerUuid()).append(',')
                .append(csv(key.playerName())).append(',')
                .append(csv(key.modId())).append(',')
                .append(csv(key.channel())).append(',')
                .append(csv(key.packetClass())).append(',')
                .append(row.lastPacketsPerSecond()).append(',')
                .append(row.lastBytesPerSecond()).append(',')
                .append(row.peakPacketsPerSecond()).append(',')
                .append(row.peakBytesPerSecond()).append(',')
                .append(row.totalPackets()).append(',')
                .append(row.totalBytes()).append(',')
                .append(row.lastSmallPacketsPerSecond()).append(',')
                .append(row.risk()).append(',')
                .append(csv(row.reason())).append('\n');
        }
        return builder.toString();
    }

    private static String toJson(List<PacketStats.Row> rows, String reason) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n  \"generatedAt\": \"").append(json(Instant.now().toString())).append("\",\n");
        builder.append("  \"reason\": \"").append(json(reason)).append("\",\n");
        builder.append("  \"rows\": [\n");
        for (int i = 0; i < rows.size(); i++) {
            PacketStats.Row row = rows.get(i);
            PacketStatKey key = row.key();
            builder.append("    {")
                .append("\"direction\":\"").append(key.direction()).append("\",")
                .append("\"playerUuid\":\"").append(key.playerUuid()).append("\",")
                .append("\"playerName\":\"").append(json(key.playerName())).append("\",")
                .append("\"modId\":\"").append(json(key.modId())).append("\",")
                .append("\"channel\":\"").append(json(key.channel())).append("\",")
                .append("\"packetClass\":\"").append(json(key.packetClass())).append("\",")
                .append("\"lastPps\":").append(row.lastPacketsPerSecond()).append(',')
                .append("\"lastBps\":").append(row.lastBytesPerSecond()).append(',')
                .append("\"peakPps\":").append(row.peakPacketsPerSecond()).append(',')
                .append("\"peakBps\":").append(row.peakBytesPerSecond()).append(',')
                .append("\"totalPackets\":").append(row.totalPackets()).append(',')
                .append("\"totalBytes\":").append(row.totalBytes()).append(',')
                .append("\"smallPps\":").append(row.lastSmallPacketsPerSecond()).append(',')
                .append("\"risk\":\"").append(row.risk()).append("\",")
                .append("\"reason\":\"").append(json(row.reason())).append("\"")
                .append('}');
            if (i + 1 < rows.size()) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("  ]\n}\n");
        return builder.toString();
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        return '"' + safe.replace("\"", "\"\"") + '"';
    }

    private static String json(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
