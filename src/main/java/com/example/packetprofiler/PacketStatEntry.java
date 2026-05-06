package com.example.packetprofiler;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public final class PacketStatEntry {
    private final LongAdder totalPackets = new LongAdder();
    private final LongAdder totalBytes = new LongAdder();
    private final LongAdder currentPackets = new LongAdder();
    private final LongAdder currentBytes = new LongAdder();
    private final LongAdder currentSmallPackets = new LongAdder();
    private final AtomicLong lastPacketsPerSecond = new AtomicLong();
    private final AtomicLong lastBytesPerSecond = new AtomicLong();
    private final AtomicLong peakPacketsPerSecond = new AtomicLong();
    private final AtomicLong peakBytesPerSecond = new AtomicLong();
    private final AtomicLong lastSmallPacketsPerSecond = new AtomicLong();

    public void add(int bytes, boolean small) {
        int safeBytes = Math.max(bytes, 0);
        totalPackets.increment();
        totalBytes.add(safeBytes);
        currentPackets.increment();
        currentBytes.add(safeBytes);
        if (small) {
            currentSmallPackets.increment();
        }
    }

    public void rotateSecond() {
        long pps = currentPackets.sumThenReset();
        long bps = currentBytes.sumThenReset();
        long small = currentSmallPackets.sumThenReset();
        lastPacketsPerSecond.set(pps);
        lastBytesPerSecond.set(bps);
        lastSmallPacketsPerSecond.set(small);
        updatePeak(peakPacketsPerSecond, pps);
        updatePeak(peakBytesPerSecond, bps);
    }

    private static void updatePeak(AtomicLong target, long value) {
        long current;
        do {
            current = target.get();
            if (value <= current) {
                return;
            }
        } while (!target.compareAndSet(current, value));
    }

    public long totalPackets() { return totalPackets.sum(); }
    public long totalBytes() { return totalBytes.sum(); }
    public long lastPacketsPerSecond() { return lastPacketsPerSecond.get(); }
    public long lastBytesPerSecond() { return lastBytesPerSecond.get(); }
    public long peakPacketsPerSecond() { return peakPacketsPerSecond.get(); }
    public long peakBytesPerSecond() { return peakBytesPerSecond.get(); }
    public long lastSmallPacketsPerSecond() { return lastSmallPacketsPerSecond.get(); }
}
