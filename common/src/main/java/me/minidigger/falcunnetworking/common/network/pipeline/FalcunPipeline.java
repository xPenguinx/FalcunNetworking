package me.minidigger.falcunnetworking.common.network.pipeline;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import me.minidigger.falcunnetworking.common.network.FalcunChannelHandler;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunHandler;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacketHandler;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacketRegistry;

public class FalcunPipeline extends ChannelInitializer<SocketChannel> {

    private final FalcunPacketRegistry packetRegistry;
    private final FalcunPacketHandler packetHandler;
    private final FalcunHandler handler;

    public FalcunPipeline(FalcunPacketRegistry packetRegistry, FalcunPacketHandler packetHandler, FalcunHandler handler) {
        this.packetRegistry = packetRegistry;
        this.packetHandler = packetHandler;
        this.handler = handler;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();

        pipeline.addLast("lengthDecoder", new FalcunPacketLengthDecoder());
        pipeline.addLast("decoder", new FalcunPacketDecoder(packetRegistry, packetHandler));

        pipeline.addLast("lengthEncoder", new FalcunPacketLengthEncoder());
        pipeline.addLast("encoder", new FalcunPacketEncoder(packetRegistry));

        pipeline.addLast("handler", new FalcunChannelHandler(handler));
    }
}
