package com.example.packetprofiler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import java.lang.reflect.Field;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;

public final class ConnectionInjector {
    private ConnectionInjector() {}

    public static void inject(ServerPlayer player) {
        Connection connection = findConnection(player);
        if (connection == null) {
            PacketProfilerMod.LOGGER.warn("Could not find network connection for {}", player.getGameProfile().getName());
            return;
        }
        Channel channel = findChannel(connection);
        if (channel == null) {
            PacketProfilerMod.LOGGER.warn("Could not find Netty channel for {}", player.getGameProfile().getName());
            return;
        }
        channel.eventLoop().execute(() -> injectOnEventLoop(channel, player));
    }

    private static void injectOnEventLoop(Channel channel, ServerPlayer player) {
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get(PacketProfilerHandler.NAME) != null) {
            return;
        }
        PacketProfilerHandler handler = new PacketProfilerHandler(player);
        if (pipeline.get("packet_handler") != null) {
            pipeline.addBefore("packet_handler", PacketProfilerHandler.NAME, handler);
        } else {
            pipeline.addLast(PacketProfilerHandler.NAME, handler);
        }
        PacketProfilerMod.LOGGER.info("Packet profiler attached to {}", player.getGameProfile().getName());
    }

    private static Connection findConnection(ServerPlayer player) {
        Object listener = player.connection;
        Class<?> type = listener.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (Connection.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        return (Connection) field.get(listener);
                    } catch (ReflectiveOperationException ignored) {
                        return null;
                    }
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static Channel findChannel(Connection connection) {
        Class<?> type = connection.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (Channel.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        return (Channel) field.get(connection);
                    } catch (ReflectiveOperationException ignored) {
                        return null;
                    }
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }
}
