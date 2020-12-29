package me.minidigger.falcunnetworking.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.minidigger.falcunnetworking.common.network.protocol.FalcunHandler;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacketHandler;
import me.minidigger.falcunnetworking.common.network.protocol.PacketDirection;

public class FalcunServerPacketHandler extends FalcunPacketHandler {

    private static final Logger log = LoggerFactory.getLogger(FalcunServerPacketHandler.class);

    public FalcunServerPacketHandler(FalcunHandler handler) {
        super(handler);
    }

    @Override
    public PacketDirection getDirection() {
        return PacketDirection.TO_SERVER;
    }

    @Override
    public void init() {
        log.info("Init FalcunServerPacketHandler");
    }
}
