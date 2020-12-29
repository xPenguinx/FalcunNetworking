package me.minidigger.falcunnetworking.client;


import net.minecrell.terminalconsole.SimpleTerminalConsole;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import me.minidigger.falcunnetworking.common.Constants;
import me.minidigger.falcunnetworking.common.api.FalcunGroup;
import me.minidigger.falcunnetworking.common.api.FalcunUser;
import me.minidigger.falcunnetworking.common.api.Invite;
import me.minidigger.falcunnetworking.common.network.protocol.client.ClientActionPacket;
import me.minidigger.falcunnetworking.common.network.protocol.client.ClientActionPacket.Action;
import me.minidigger.falcunnetworking.common.network.protocol.client.ClientChatPacket;
import me.minidigger.falcunnetworking.common.network.protocol.client.ClientListRequestPacket;
import me.minidigger.falcunnetworking.common.network.protocol.client.ClientListRequestPacket.ListType;

public class FalcunConsole extends SimpleTerminalConsole {

    private static final Logger log = LoggerFactory.getLogger(FalcunConsole.class);

    private final FalcunClient client;

    public FalcunConsole(FalcunClient client) {
        this.client = client;
    }

    @Override
    public void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                FalcunConsole.super.start();
            }
        }, "TerminalThread").start();
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

        if ("stop".equals(args[0])) {
            shutdown();
        } else if ("chat".equals(args[0])) {
            if (args.length < 3) {
                log.warn("Usage: chat <target> <message>");
                return;
            }
            String message = command.replace("chat " + args[1], "");
            for (FalcunUser friend : client.getHandler().getFriends()) {
                if (friend.getName().equals(args[1])) {
                    client.getHandler().getConnection().sendPacket(new ClientChatPacket(message, friend.getId()));
                    return;
                }
            }
            log.warn("Unknown friend {}", args[1]);
        } else if ("groupChat".equals(args[0])) {
            if (args.length < 3) {
                log.warn("Usage: groupChat <target> <message>");
                return;
            }
            String message = command.replace("groupChat " + args[1], "");
            client.getHandler().getConnection().sendPacket(new ClientChatPacket(message, args[1]));
        } else if ("list".equals(args[0])) {
            if (args.length < 2) {
                log.warn("Usage: list <listtype> [groupname]");
                return;
            }

            ListType type = ListType.valueOf(args[1]);
            if (type == ListType.OUTGOING_GROUP_INVITES || type == ListType.MESSAGES) {
                if (args.length < 3) {
                    log.warn("Usage: list OUTGOING_GROUP_INVITES/MESSAGES <groupname>");
                    return;
                }
                client.getHandler().getConnection().sendPacket(new ClientListRequestPacket(args[2], type));
            } else {
                client.getHandler().getConnection().sendPacket(new ClientListRequestPacket(type));
            }
        } else if ("action".equals(args[0])) {
            if (args.length < 3) {
                log.warn("Usage: action <action> <user/invite/group>");
                return;
            }

            Action action = Action.valueOf(args[1]);
            if (action == Action.KICK_USER) {
                if (args.length < 4) {
                    log.warn("Usage: action KICK_USER <group> <user>");
                    return;
                }
                FalcunGroup group = client.getHandler().getGroup(args[2]);
                if (group == null) {
                    log.warn("Group not found");
                    return;
                }
                for (FalcunUser user : group.getUsers()) {
                    if (user.getName().equals(args[3])) {
                        client.getHandler().getConnection().sendPacket(new ClientActionPacket(user, group));
                        return;
                    }
                }

                log.warn("User not in that group");
                return;
            } else if (action == Action.REMOVE_FRIEND) {
                for (FalcunUser friend : client.getHandler().getFriends()) {
                    if (friend.getName().equals(args[2])) {
                        client.getHandler().getConnection().sendPacket(new ClientActionPacket(friend));
                        return;
                    }
                }

                log.warn("Unknown friend {}", args[2]);
            } else if (action == Action.SEND_FRIEND_INVITE) {
                client.getHandler().getConnection().sendPacket(new ClientActionPacket(args[2]));
            } else if (action == Action.SEND_GROUP_INVITE) {
                FalcunGroup group = client.getHandler().getGroup(args[2]);
                if (group == null) {
                    log.warn("Group not found");
                    return;
                }

                client.getHandler().getConnection().sendPacket(new ClientActionPacket(group, args[3], action));
            } else if (action == Action.CREATE_GROUP || action == Action.DELETE_GROUP) {
                client.getHandler().getConnection().sendPacket(new ClientActionPacket(args[2], action));
            } else {
                Invite invite = null;
                if (action == Action.ACCEPT_FRIEND_INVITE || action == Action.DECLINE_FRIEND_INVITE) {
                    for (Invite incomingFriendInvite : client.getHandler().getIncomingFriendInvites()) {
                        if (incomingFriendInvite.getInviter().getName().equals(args[2])) {
                            invite = incomingFriendInvite;
                            break;
                        }
                    }
                } else if (action == Action.REVOKE_FRIEND_INVITE) {
                    for (Invite outgoingFriendInvite : client.getHandler().getOutgoingFriendInvites()) {
                        if (outgoingFriendInvite.getUser().getName().equals(args[2])) {
                            invite = outgoingFriendInvite;
                            break;
                        }
                    }
                } else if (action == Action.ACCEPT_GROUP_INVITE || action == Action.DECLINE_GROUP_INVITE) {
                    List<Invite> invites = client.getHandler().getIncomingGroupInvites().get(args[2]);
                    if (invites == null) {
                        log.warn("No invites from that group found");
                        return;
                    }
                    for (Invite incomingGroupInvite : invites) {
                        if (incomingGroupInvite.getUser().getName().equals(client.getUser().getName())) {
                            invite = incomingGroupInvite;
                            break;
                        }
                    }
                } else if (action == Action.REVOKE_GROUP_INVITE) {
                    if (args.length < 4) {
                        log.warn("Usage: action REVOKE_GROUP_INVITE <group> <user>");
                        return;
                    }
                    List<Invite> invites = client.getHandler().getOutgoingGroupInvites().get(args[2]);
                    if (invites == null) {
                        log.warn("No invites from that group found");
                        return;
                    }
                    for (Invite outgoingGroupInvite : invites) {
                        if (outgoingGroupInvite.getUser().getName().equals(args[3])) {
                            invite = outgoingGroupInvite;
                            break;
                        }
                    }
                }
                if (invite == null) {
                    log.warn("No matching invite found {}", args[2]);
                } else {
                    client.getHandler().getConnection().sendPacket(new ClientActionPacket(action, invite));
                }
            }
            // update data
            for (ListType listType : ListType.values()) {
                client.getHandler().getConnection().sendPacket(new ClientListRequestPacket(listType));
            }
        } else {
            log.warn("Unknown command");
        }
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
