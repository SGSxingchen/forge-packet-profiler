package com.example.packetprofiler;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(PacketProfilerMod.MODID)
public final class PacketProfilerMod {
    public static final String MODID = "packetprofiler";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PacketProfilerMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ProfilerConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new PacketProfilerEvents());
    }
}
