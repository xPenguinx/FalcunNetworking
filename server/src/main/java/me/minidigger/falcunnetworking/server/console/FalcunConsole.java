package me.minidigger.falcunnetworking.server.console;


import net.minecrell.terminalconsole.SimpleTerminalConsole;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import me.minidigger.falcunnetworking.common.Constants;
import me.minidigger.falcunnetworking.common.api.FalcunUser;
import me.minidigger.falcunnetworking.common.network.FalcunConnection;
import me.minidigger.falcunnetworking.common.network.protocol.server.ServerChatPacket;
import me.minidigger.falcunnetworking.server.FalcunServer;


public class FalcunConsole extends SimpleTerminalConsole {

    private static final Logger log = LoggerFactory.getLogger(FalcunConsole.class);

    private final FalcunServer server;

    public FalcunConsole(FalcunServer server) {
        this.server = server;
    }

    @Override
    public void start() {
        new Thread(super::start, "TerminalThread").start();
    }

    @Override
    protected boolean isRunning() {
        return true;
    }

    @Override
    public void runCommand(String command) {
        try {
            runCommand0(command);
        } catch (Exception ex) {
            log.error("Error while executing command {}", command, ex);
        }
    }

    private void runCommand0(String command) {
        String[] args = command.split(" ");

        switch (args[0]) {
            case "stop":
                shutdown();
                break;
            case "list":
                String users = server.getHandler().getConnections().stream().map(c -> c.getUser().getName()).filter(Objects::nonNull).collect(Collectors.joining(", "));
                log.info("Connected users: {}", users);
                break;
            case "kick":
                if (args.length < 2) {
                    log.warn("Usage: kick <user> [reason]");
                    return;
                }
                Optional<FalcunConnection> conn = getConn(args[1]);
                if (!conn.isPresent()) {
                    log.warn("Unknown user {}", args[1]);
                    return;
                }

                if (args.length > 2) {
                    conn.get().close(command.replace("kick " + args[1] + " ", ""));
                } else {
                    conn.get().close("Kicked by console");
                }
                log.info("Kicked!");
                break;
            case "info":
                if (args.length < 2) {
                    log.warn("Usage: info <user>");
                    return;
                }
                conn = getConn(args[1]);
                if (!conn.isPresent()) {
                    log.warn("Unknown user {}", args[1]);
                } else {
                    log.info("Info: {}", conn.get());
                }
                break;
            case "broadcast":
                if (args.length < 2) {
                    log.warn("Usage: broadcast <message>");
                    return;
                }

                String message = command.replace("broadcast ", "");
                server.getHandler().getConnections().forEach(c -> c.sendPacket(new ServerChatPacket(message)));
                log.info("Send message to {} clients", server.getHandler().getConnections().size());
                break;
            case "listfriends":
                if (args.length < 2) {
                    log.warn("Usage: listfriends <user>");
                    return;
                }
                FalcunUser user = server.getUsersHandler().getUser(args[1]);
                if (user == null) {
                    log.warn("Unknown user {}", args[1]);
                    return;
                }

                String friends = server.getFriendsHandler().getFriends(user).stream().map(FalcunUser::getName).collect(Collectors.joining());
                log.info("User {} has these friends: {}", args[1], friends);

                break;
            case "remfriend":
                if (args.length < 3) {
                    log.warn("Usage: remfriend <user> <user2>");
                    return;
                }
                FalcunUser user1 = server.getUsersHandler().getUser(args[1]);
                if (user1 == null) {
                    log.warn("Unknown user {}", args[1]);
                    return;
                }
                FalcunUser user2 = server.getUsersHandler().getUser(args[2]);
                if (user2 == null) {
                    log.warn("Unknown user2 {}", args[2]);
                    return;
                }

                String result = server.getFriendsHandler().removeUser(user1, user2);
                log.info("Removed: {}", result);

                break;
            case "invfriend":
                if (args.length < 3) {
                    log.warn("Usage: invfriend <user> <user2>");
                    return;
                }
                user1 = server.getUsersHandler().getUser(args[1]);
                if (user1 == null) {
                    log.warn("Unknown user {}", args[1]);
                    return;
                }

                if (server.getFriendsHandler().sendInvite(user1, args[2]) != null) {
                    log.info("Invite send");
                } else {
                    log.warn("Can't send invite to that user");
                }

                break;
            case "showinvites":
                if (args.length < 2) {
                    log.warn("Usage: showinvites <user>");
                    return;
                }
                user = server.getUsersHandler().getUser(args[1]);
                if (user == null) {
                    log.warn("Unknown user {}", args[1]);
                    return;
                }
                server.getFriendsHandler().getIncomingInvites(user);

                break;
            default:
                log.warn("Unknown command");
        }
    }

    private Optional<FalcunConnection> getConn(String username) {
        return server.getHandler().getConnections().stream().filter(c -> username.equals(c.getUser().getName())).findFirst();
    }

    @Override
    protected void shutdown() {
        log.info("Shutting down");
        if (!Constants.TEST_MODE) {
            System.exit(0);
        }
    }

    @Override
    protected LineReader buildReader(LineReaderBuilder builder) {
        return super.buildReader(builder.appName("FalcunNetworking"));
    }
}
