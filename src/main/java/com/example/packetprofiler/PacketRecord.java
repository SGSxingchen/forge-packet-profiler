package com.example.packetprofiler;

import java.util.UUID;

public record PacketRecord(
    PacketDirection direction,
    UUID playerUuid,
    String playerName,
    String packetClass,
    String channel,
    String modId,
    int estimatedBytes,
    boolean sizeEstimated
) {}
