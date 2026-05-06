package com.example.packetprofiler;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class PacketProfilerEvents {
    private MinecraftServer currentServer;
    private long tickCounter;
    private long secondsCounter;

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ConnectionInjector.inject(player);
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        currentServer = event.getServer();
        injectAll(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        PacketReportWriter.dumpAsync(event.getServer(), "server_stopping");
        currentServer = null;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tickCounter++;
        if (tickCounter % 100 == 0 && currentServer != null) {
            injectAll(currentServer);
        }
        if (tickCounter % 20 != 0) {
            return;
        }
        secondsCounter++;
        PacketStats.get().rotateSecond();
        MinecraftServer server = currentServer;
        int logInterval = ProfilerConfig.LOG_INTERVAL_SECONDS.get();
        if (server != null && logInterval > 0 && secondsCounter % logInterval == 0) {
            logTop(ProfilerConfig.TOP_N.get());
        }
        int reportInterval = ProfilerConfig.REPORT_INTERVAL_SECONDS.get();
        if (server != null && reportInterval > 0 && secondsCounter % reportInterval == 0) {
            PacketReportWriter.dumpAsync(server, "periodic");
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("packetprofiler")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("top")
                    .executes(context -> sendTop(context.getSource(), ProfilerConfig.TOP_N.get()))
                    .then(Commands.argument("limit", IntegerArgumentType.integer(1, 100))
                        .executes(context -> sendTop(context.getSource(), IntegerArgumentType.getInteger(context, "limit")))))
                .then(Commands.literal("reset")
                    .executes(context -> {
                        PacketStats.get().reset();
                        context.getSource().sendSuccess(() -> Component.literal("Packet profiler counters reset."), true);
                        return 1;
                    }))
                .then(Commands.literal("dump")
                    .executes(context -> {
                        PacketReportWriter.dumpAsync(context.getSource().getServer(), "command");
                        context.getSource().sendSuccess(
                            () -> Component.literal("Packet profiler dump scheduled under "
                                + PacketReportWriter.reportDirectory(context.getSource().getServer()).toAbsolutePath()),
                            false
                        );
                        return 1;
                    }))
        );
    }

    private static int sendTop(CommandSourceStack source, int limit) {
        source.sendSuccess(() -> Component.literal("Packet profiler Top " + limit + " by current bytes/s:"), false);
        int index = 1;
        for (PacketStats.Row row : PacketStats.get().top(limit)) {
            PacketStatKey key = row.key();
            int rank = index++;
            source.sendSuccess(() -> Component.literal(formatRow(rank, row, key)).withStyle(styleFor(row.risk())), false);
        }
        return Math.max(index - 1, 1);
    }

    private static void logTop(int limit) {
        PacketProfilerMod.LOGGER.info("Packet profiler Top {} by current bytes/s", limit);
        int index = 1;
        for (PacketStats.Row row : PacketStats.get().top(limit)) {
            PacketProfilerMod.LOGGER.info(formatRow(index++, row, row.key()));
        }
    }

    private static String formatRow(int rank, PacketStats.Row row, PacketStatKey key) {
        return String.format(
            "#%d [%s/%s] %s %s %s channel=%s pps=%d bps=%d peak=%dpps/%dBps total=%dp/%dB risk=%s reason=%s",
            rank,
            key.direction(),
            key.modId(),
            key.playerName(),
            key.playerUuid(),
            simpleClassName(key.packetClass()),
            key.channel().isBlank() ? "-" : key.channel(),
            row.lastPacketsPerSecond(),
            row.lastBytesPerSecond(),
            row.peakPacketsPerSecond(),
            row.peakBytesPerSecond(),
            row.totalPackets(),
            row.totalBytes(),
            row.risk(),
            row.reason()
        );
    }

    private static String simpleClassName(String className) {
        int dot = className.lastIndexOf('.');
        return dot >= 0 ? className.substring(dot + 1) : className;
    }

    private static ChatFormatting styleFor(RiskLevel risk) {
        return switch (risk) {
            case RED -> ChatFormatting.RED;
            case YELLOW -> ChatFormatting.YELLOW;
            case GREEN -> ChatFormatting.GREEN;
        };
    }

    private static void injectAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ConnectionInjector.inject(player);
        }
    }
}
