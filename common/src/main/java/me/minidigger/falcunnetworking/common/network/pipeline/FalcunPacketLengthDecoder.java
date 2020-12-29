package me.minidigger.falcunnetworking.common.network.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class FalcunPacketLengthDecoder extends ByteToMessageDecoder {

    private static final Logger log = LoggerFactory.getLogger(FalcunPacketLengthDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        in.markReaderIndex();

        if (in.readableBytes() < 4) {
            log.warn("Can't decode incoming packet length, it only contains {} bytes!", in.readableBytes());
            in.skipBytes(in.readableBytes());
            return;
        }

        int packetLength = in.readInt();

        if (in.readableBytes() < packetLength) {
            in.resetReaderIndex();
        } else {
            out.add(in.readBytes(packetLength));
        }
    }
}
