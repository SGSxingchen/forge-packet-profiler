package com.example.packetprofiler;

import java.util.UUID;

public record PacketStatKey(
    PacketDirection direction,
    UUID playerUuid,
    String playerName,
    String packetClass,
    String channel,
    String modId
) {
    public static PacketStatKey from(PacketRecord record) {
        return new PacketStatKey(
            record.direction(),
            record.playerUuid(),
            record.playerName(),
            record.packetClass(),
            record.channel(),
            record.modId()
        );
    }
}
