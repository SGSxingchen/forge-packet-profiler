package com.example.packetprofiler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.server.level.ServerPlayer;

public final class PacketProfilerHandler extends ChannelDuplexHandler {
    public static final String NAME = "packetprofiler_observer";

    private final ServerPlayer player;

    public PacketProfilerHandler(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        PacketStats.get().record(PacketInspector.inspect(PacketDirection.CLIENTBOUND, player, msg));
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (ProfilerConfig.OBSERVE_SERVERBOUND.get()) {
            PacketStats.get().record(PacketInspector.inspect(PacketDirection.SERVERBOUND, player, msg));
        }
        super.channelRead(ctx, msg);
    }
}
