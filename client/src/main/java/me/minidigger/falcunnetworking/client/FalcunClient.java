package me.minidigger.falcunnetworking.client;

import java.util.UUID;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import me.minidigger.falcunnetworking.common.api.FalcunMinecraftUser;
import me.minidigger.falcunnetworking.common.api.FalcunUser;
import me.minidigger.falcunnetworking.common.network.pipeline.FalcunPipeline;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacketHandler;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacketRegistry;

public class FalcunClient {

    private FalcunConsole console;
    private FalcunPacketRegistry packetRegistry;
    private FalcunPacketHandler packetHandler;
    private ClientFalcunHandler handler;
    private FalcunUser user;
    private FalcunMinecraftUser minecraftUser;
    private AuthHandler authHandler;

    public static void main(String[] args) {
        FalcunClient client = new FalcunClient();
        client.init();
        client.setAuth(args[0], args[1], args[2]);
        client.connect("localhost", 1337);
    }

    public void init() {
        console = new FalcunConsole(this);
        console.start();

        handler = new ClientFalcunHandler(this);

        packetRegistry = new FalcunPacketRegistry();
        packetRegistry.init();

        packetHandler = new FalcunClientPacketHandler(handler);
        packetHandler.init();

        user = new FalcunUser();

        authHandler = new AuthHandler();
    }

    public void setAuth(String user, String uuid, String accessToken) {
        UUID parsedUUID = UUID.fromString(uuid);
        getUser().setName(user);
        getUser().setId(parsedUUID);

        minecraftUser = new FalcunMinecraftUser(accessToken, parsedUUID, user);
    }

    public void connect(final String hostname, final int port) {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new FalcunPipeline(packetRegistry, packetHandler, handler));

            // wait till connection should be closed
            bootstrap.connect(hostname, port).sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    public FalcunConsole getConsole() {
        return console;
    }

    public ClientFalcunHandler getHandler() {
        return handler;
    }

    public FalcunPacketRegistry getPacketRegistry() {
        return packetRegistry;
    }

    public FalcunPacketHandler getPacketHandler() {
        return packetHandler;
    }

    public AuthHandler getAuthHandler() {
        return authHandler;
    }

    public FalcunUser getUser() {
        return user;
    }

    public void setUser(FalcunUser user) {
        this.user = user;
    }

    public FalcunMinecraftUser getMinecraftUser() {
        return minecraftUser;
    }
}
