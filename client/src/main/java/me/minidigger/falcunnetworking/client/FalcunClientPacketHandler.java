package me.minidigger.falcunnetworking.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.minidigger.falcunnetworking.common.network.protocol.FalcunHandler;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacketHandler;
import me.minidigger.falcunnetworking.common.network.protocol.PacketDirection;

public class FalcunClientPacketHandler extends FalcunPacketHandler {

    private static final Logger log = LoggerFactory.getLogger(FalcunClientPacketHandler.class);

    public FalcunClientPacketHandler(FalcunHandler handler) {
        super(handler);
    }

    @Override
    public PacketDirection getDirection() {
        return PacketDirection.TO_CLIENT;
    }

    @Override
    public void init() {
        log.info("Init FalcunClientPacketHandler");
    }
}
