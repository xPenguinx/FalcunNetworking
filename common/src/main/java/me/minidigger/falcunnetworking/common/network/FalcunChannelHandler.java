package me.minidigger.falcunnetworking.common.network;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunHandler;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacket;

public class FalcunChannelHandler extends SimpleChannelInboundHandler<FalcunPacket> {

    private static final Logger log = LoggerFactory.getLogger(FalcunChannelHandler.class);

    private FalcunConnection connection;

    private final FalcunHandler handler;

    public FalcunChannelHandler(FalcunHandler handler) {
        this.handler = handler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("[+] Channel connected: {}", ctx.channel().remoteAddress());

        this.connection = new FalcunConnection(ctx);
        if (handler != null) {
            handler.join(connection);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("[-] Channel disconnected: {}", ctx.channel().remoteAddress());

        if (handler != null) {
            handler.leave(connection);
        }
        this.connection = null;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if ("Connection reset".equals(cause.getMessage())) {
            log.error("{}: Connection reset.", this.connection.getRemoteAddress());
        } else if ("An established connection was aborted by the software in your host machine".equals(cause.getMessage()) ||
                   "An existing connection was forcibly closed by the remote host".equals(cause.getMessage())) {
            log.error("{}: Disconnected.", this.connection.getRemoteAddress());
        } else {
            log.error("{}: Exception caught, closing channel.", this.connection.getRemoteAddress(), cause);
        }

        if (handler != null) {
            handler.leave(connection);
        }
        this.connection = null;

        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FalcunPacket msg) throws Exception {
        // unused
    }

    public FalcunConnection getConnection() {
        return this.connection;
    }
}
