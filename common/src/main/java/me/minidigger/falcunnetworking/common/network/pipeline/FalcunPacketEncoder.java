package me.minidigger.falcunnetworking.common.network.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacket;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacketRegistry;

public class FalcunPacketEncoder extends MessageToByteEncoder<FalcunPacket> {

    private static final Logger log = LoggerFactory.getLogger(FalcunPacketEncoder.class);

    private final FalcunPacketRegistry packetRegistry;

    public FalcunPacketEncoder(FalcunPacketRegistry packetRegistry) {
        this.packetRegistry = packetRegistry;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, FalcunPacket packet, ByteBuf out) {
        packetRegistry.fillInfo(packet);
        if (log.isDebugEnabled()) {
            log.debug("Writing packet {}: {}", packet.getId(), packet);
        }

        out.writeInt(packet.getId());

        try {
            packet.toWire(out);
        } catch (Exception ex) {
            log.warn("Error while encoding packet {}", packet, ex);
            ctx.close();
        }
    }
}
