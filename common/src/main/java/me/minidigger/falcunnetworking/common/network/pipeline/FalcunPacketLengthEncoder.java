package me.minidigger.falcunnetworking.common.network.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class FalcunPacketLengthEncoder extends MessageToByteEncoder<ByteBuf> {

    private static final Logger log = LoggerFactory.getLogger(FalcunPacketLengthEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        out.writeInt(msg.readableBytes());
        if (log.isDebugEnabled()) {
            log.debug("Wrote packet with size {}", msg.readableBytes());
        }
        out.writeBytes(msg);
    }
}
