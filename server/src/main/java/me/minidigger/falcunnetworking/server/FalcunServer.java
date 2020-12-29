package me.minidigger.falcunnetworking.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import me.minidigger.falcunnetworking.common.network.pipeline.FalcunPipeline;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacketHandler;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacketRegistry;
import me.minidigger.falcunnetworking.common.properties.Property;
import me.minidigger.falcunnetworking.server.api.UsersHandler;
import me.minidigger.falcunnetworking.server.console.FalcunConsole;
import me.minidigger.falcunnetworking.server.db.DBHandler;
import me.minidigger.falcunnetworking.server.api.FriendsHandler;
import me.minidigger.falcunnetworking.server.api.GroupsHandler;

public class FalcunServer {

    public static final Property<String> DB_USER = new Property<>("db.user", String.class, "falcun");
    public static final Property<String> DB_PASS = new Property<>("db.pass", String.class, "falcun");
    public static final Property<String> DB_NAME = new Property<>("db.name", String.class, "falcun");
    public static final Property<String> DB_HOST = new Property<>("db.host", String.class, "localhost");
    public static final Property<Integer> DB_PORT = new Property<>("db.port", Integer.class, 3306);

    private final int port;

    private FalcunConsole console;
    private FalcunPacketRegistry packetRegistry;
    private FalcunPacketHandler packetHandler;
    private ServerFalcunHandler handler;
    private DBHandler dbHandler;
    private AuthHandler authHandler;
    private FriendsHandler friendsHandler;
    private GroupsHandler groupsHandler;
    private UsersHandler usersHandler;

    public FalcunServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        FalcunServer server = new FalcunServer(1337);
        server.start();
    }

    public void start() {
        console = new FalcunConsole(this);
        console.start();

        handler = new ServerFalcunHandler(this);

        packetRegistry = new FalcunPacketRegistry();
        packetRegistry.init();

        packetHandler = new FalcunServerPacketHandler(handler);
        packetHandler.init();

        dbHandler = new DBHandler();
        dbHandler.setup();
        dbHandler.createTables();

        authHandler = new AuthHandler(this);

        friendsHandler = new FriendsHandler(this);
        groupsHandler = new GroupsHandler(this);
        usersHandler = new UsersHandler(this);

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new FalcunPipeline(packetRegistry, packetHandler, handler));

            bootstrap.bind(port).sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public ServerFalcunHandler getHandler() {
        return handler;
    }

    public FalcunPacketRegistry getPacketRegistry() {
        return packetRegistry;
    }

    public FalcunPacketHandler getPacketHandler() {
        return packetHandler;
    }

    public DBHandler getDbHandler() {
        return dbHandler;
    }

    public AuthHandler getAuthHandler() {
        return authHandler;
    }

    public FriendsHandler getFriendsHandler() {
        return friendsHandler;
    }

    public GroupsHandler getGroupsHandler() {
        return groupsHandler;
    }

    public UsersHandler getUsersHandler() {
        return usersHandler;
    }
}
