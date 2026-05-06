package com.example.packetprofiler;

import io.netty.buffer.Unpooled;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class PacketInspector {
    private PacketInspector() {}

    public static PacketRecord inspect(PacketDirection direction, ServerPlayer player, Object message) {
        String packetClass = message == null ? "null" : message.getClass().getName();
        String channel = findChannel(message);
        String modId = inferModId(packetClass, channel);
        SizeEstimate size = estimateSize(message);
        UUID uuid = player == null ? new UUID(0L, 0L) : player.getUUID();
        String name = player == null ? "<unknown>" : player.getGameProfile().getName();
        return new PacketRecord(direction, uuid, name, packetClass, channel, modId, size.bytes(), size.estimated());
    }

    private static SizeEstimate estimateSize(Object message) {
        if (message instanceof io.netty.buffer.ByteBuf byteBuf) {
            return new SizeEstimate(byteBuf.readableBytes(), false);
        }
        if (message instanceof Packet<?> packet) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer(128));
            try {
                packet.write(buffer);
                return new SizeEstimate(buffer.readableBytes(), true);
            } catch (RuntimeException ex) {
                int fallback = fallbackCustomPayloadBytes(message);
                return new SizeEstimate(Math.max(fallback, 0), true);
            } finally {
                buffer.release();
            }
        }
        return new SizeEstimate(0, true);
    }

    private static int fallbackCustomPayloadBytes(Object message) {
        if (message == null) {
            return 0;
        }
        for (Field field : message.getClass().getDeclaredFields()) {
            if (FriendlyByteBuf.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    FriendlyByteBuf data = (FriendlyByteBuf) field.get(message);
                    return data == null ? 0 : data.readableBytes();
                } catch (ReflectiveOperationException ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private static String findChannel(Object message) {
        ResourceLocation fromMethod = findChannelByMethod(message);
        if (fromMethod != null) {
            return fromMethod.toString();
        }
        ResourceLocation fromField = findChannelByField(message);
        if (fromField != null) {
            return fromField.toString();
        }
        return "";
    }

    private static ResourceLocation findChannelByMethod(Object message) {
        if (message == null) {
            return null;
        }
        for (String methodName : new String[] {"getIdentifier", "identifier", "getChannel", "channel", "id", "getId"}) {
            try {
                Method method = message.getClass().getMethod(methodName);
                if (method.getParameterCount() == 0 && ResourceLocation.class.isAssignableFrom(method.getReturnType())) {
                    return (ResourceLocation) method.invoke(message);
                }
            } catch (ReflectiveOperationException ignored) {
                // Try the next common accessor name.
            }
        }
        return null;
    }

    private static ResourceLocation findChannelByField(Object message) {
        if (message == null) {
            return null;
        }
        Class<?> type = message.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (ResourceLocation.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        return (ResourceLocation) field.get(message);
                    } catch (ReflectiveOperationException ignored) {
                        return null;
                    }
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static String inferModId(String packetClass, String channel) {
        if (channel != null && !channel.isBlank()) {
            int colon = channel.indexOf(':');
            if (colon > 0) {
                return channel.substring(0, colon).toLowerCase(Locale.ROOT);
            }
        }
        if (packetClass.startsWith("net.minecraft.")) {
            return "minecraft";
        }
        if (packetClass.startsWith("net.minecraftforge.")) {
            return "forge";
        }
        return "unknown";
    }

    private record SizeEstimate(int bytes, boolean estimated) {}
}
