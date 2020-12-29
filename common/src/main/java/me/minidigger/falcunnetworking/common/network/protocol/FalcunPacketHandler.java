package me.minidigger.falcunnetworking.common.network.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.minidigger.falcunnetworking.common.network.FalcunConnection;
import me.minidigger.falcunnetworking.common.network.protocol.server.ServerResponsePacket;
import me.minidigger.falcunnetworking.common.network.protocol.server.ServerResponsePacket.ResponseType;

public abstract class FalcunPacketHandler {

    private static final Logger log = LoggerFactory.getLogger(FalcunPacketHandler.class);

    private final FalcunHandler handler;

    public FalcunPacketHandler(FalcunHandler handler) {
        this.handler = handler;
    }

    public <T extends FalcunPacket> void handle(FalcunConnection connection, T packet) {
        if (log.isDebugEnabled()) {
            log.debug("Got packet {} by {}", packet, connection);
        }

        if (handler != null) {
            try {
                handler.handle(connection, packet);
            } catch (Exception ex) {
                log.error("Internal error while processing packet {} from {}", packet, connection, ex);
                if (packet.getDirection() == PacketDirection.TO_SERVER) {
                    // if on server, send the client an error. server doesn't care about client errors.
                    connection.sendPacket(new ServerResponsePacket("Internal Error", ResponseType.ERROR));
                }
            }
        }
    }

    public abstract PacketDirection getDirection();

    public abstract void init();
}
