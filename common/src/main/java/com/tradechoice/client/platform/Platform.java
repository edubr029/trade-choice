package com.tradechoice.client.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.nio.file.Path;

public interface Platform {

    String loaderName();

    boolean isModLoaded(String modId);

    Path getConfigDir();

    void sendPayloadToServer(CustomPacketPayload payload);
}
