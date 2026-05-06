package com.example.packetprofiler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PacketStats {
    private static final PacketStats INSTANCE = new PacketStats();

    private final ConcurrentHashMap<PacketStatKey, PacketStatEntry> stats = new ConcurrentHashMap<>();

    public static PacketStats get() {
        return INSTANCE;
    }

    public void record(PacketRecord record) {
        PacketStatKey key = PacketStatKey.from(record);
        boolean small = record.estimatedBytes() <= ProfilerConfig.SMALL_PACKET_BYTES.get();
        stats.computeIfAbsent(key, ignored -> new PacketStatEntry()).add(record.estimatedBytes(), small);
    }

    public void rotateSecond() {
        stats.values().forEach(PacketStatEntry::rotateSecond);
    }

    public void reset() {
        stats.clear();
    }

    public List<Row> top(int limit) {
        return snapshot().stream()
            .sorted(Comparator.comparingLong(Row::lastBytesPerSecond).reversed()
                .thenComparing(Comparator.comparingLong(Row::lastPacketsPerSecond).reversed())
                .thenComparing(Comparator.comparingLong(Row::totalBytes).reversed()))
            .limit(limit)
            .toList();
    }

    public List<Row> snapshot() {
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<PacketStatKey, PacketStatEntry> entry : stats.entrySet()) {
            rows.add(Row.from(entry.getKey(), entry.getValue()));
        }
        return rows;
    }

    public record Row(
        PacketStatKey key,
        long totalPackets,
        long totalBytes,
        long lastPacketsPerSecond,
        long lastBytesPerSecond,
        long peakPacketsPerSecond,
        long peakBytesPerSecond,
        long lastSmallPacketsPerSecond,
        RiskLevel risk,
        String reason
    ) {
        static Row from(PacketStatKey key, PacketStatEntry entry) {
            long pps = entry.lastPacketsPerSecond();
            long bps = entry.lastBytesPerSecond();
            long peakPps = entry.peakPacketsPerSecond();
            long peakBps = entry.peakBytesPerSecond();
            long smallPps = entry.lastSmallPacketsPerSecond();

            RiskLevel risk = RiskLevel.GREEN;
            String reason = "normal";
            if (bps >= ProfilerConfig.RED_BYTES_PER_SECOND.get()
                || pps >= ProfilerConfig.RED_PACKETS_PER_SECOND.get()
                || smallPps >= ProfilerConfig.SMALL_PACKET_RATE.get()
                || peakBps >= ProfilerConfig.RED_BYTES_PER_SECOND.get() * 2L
                || peakPps >= ProfilerConfig.RED_PACKETS_PER_SECOND.get() * 2L) {
                risk = RiskLevel.RED;
                reason = reason(bps, pps, smallPps, peakBps, peakPps);
            } else if (bps >= ProfilerConfig.YELLOW_BYTES_PER_SECOND.get()
                || pps >= ProfilerConfig.YELLOW_PACKETS_PER_SECOND.get()
                || smallPps >= ProfilerConfig.SMALL_PACKET_RATE.get() / 2L
                || peakBps >= ProfilerConfig.RED_BYTES_PER_SECOND.get()
                || peakPps >= ProfilerConfig.RED_PACKETS_PER_SECOND.get()) {
                risk = RiskLevel.YELLOW;
                reason = reason(bps, pps, smallPps, peakBps, peakPps);
            }
            return new Row(
                key,
                entry.totalPackets(),
                entry.totalBytes(),
                pps,
                bps,
                peakPps,
                peakBps,
                smallPps,
                risk,
                reason
            );
        }

        private static String reason(long bps, long pps, long smallPps, long peakBps, long peakPps) {
            List<String> parts = new ArrayList<>();
            if (bps >= ProfilerConfig.YELLOW_BYTES_PER_SECOND.get()) parts.add("high_bandwidth");
            if (pps >= ProfilerConfig.YELLOW_PACKETS_PER_SECOND.get()) parts.add("high_frequency");
            if (smallPps >= ProfilerConfig.SMALL_PACKET_RATE.get() / 2L) parts.add("small_packet_flood");
            if (peakBps >= ProfilerConfig.RED_BYTES_PER_SECOND.get() || peakPps >= ProfilerConfig.RED_PACKETS_PER_SECOND.get()) {
                parts.add("burst_peak");
            }
            return String.join("+", parts);
        }
    }
}
