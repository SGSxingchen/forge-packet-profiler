package com.example.packetprofiler;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ProfilerConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue LOG_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.IntValue TOP_N;
    public static final ForgeConfigSpec.IntValue REPORT_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.IntValue YELLOW_BYTES_PER_SECOND;
    public static final ForgeConfigSpec.IntValue RED_BYTES_PER_SECOND;
    public static final ForgeConfigSpec.IntValue YELLOW_PACKETS_PER_SECOND;
    public static final ForgeConfigSpec.IntValue RED_PACKETS_PER_SECOND;
    public static final ForgeConfigSpec.IntValue SMALL_PACKET_BYTES;
    public static final ForgeConfigSpec.IntValue SMALL_PACKET_RATE;
    public static final ForgeConfigSpec.BooleanValue OBSERVE_SERVERBOUND;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("packetprofiler");
        LOG_INTERVAL_SECONDS = builder.comment("Seconds between console Top-N summaries. 0 disables periodic logging.")
            .defineInRange("logIntervalSeconds", 10, 0, 3600);
        REPORT_INTERVAL_SECONDS = builder.comment("Seconds between automatic JSON/CSV report writes. 0 disables automatic dumps.")
            .defineInRange("reportIntervalSeconds", 60, 0, 3600);
        TOP_N = builder.comment("Rows to show in logs and /packetprofiler top.")
            .defineInRange("topN", 10, 1, 100);
        OBSERVE_SERVERBOUND = builder.comment("Also count packets received from clients after the player has logged in.")
            .define("observeServerbound", true);
        YELLOW_BYTES_PER_SECOND = builder.defineInRange("yellowBytesPerSecond", 64 * 1024, 1, Integer.MAX_VALUE);
        RED_BYTES_PER_SECOND = builder.defineInRange("redBytesPerSecond", 256 * 1024, 1, Integer.MAX_VALUE);
        YELLOW_PACKETS_PER_SECOND = builder.defineInRange("yellowPacketsPerSecond", 100, 1, Integer.MAX_VALUE);
        RED_PACKETS_PER_SECOND = builder.defineInRange("redPacketsPerSecond", 400, 1, Integer.MAX_VALUE);
        SMALL_PACKET_BYTES = builder.defineInRange("smallPacketBytes", 32, 1, 1024);
        SMALL_PACKET_RATE = builder.defineInRange("smallPacketPacketsPerSecond", 200, 1, Integer.MAX_VALUE);
        builder.pop();
        SPEC = builder.build();
    }

    private ProfilerConfig() {}
}
