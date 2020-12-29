package me.minidigger.falcunnetworking.server;

import me.minidigger.falcunnetworking.common.network.protocol.client.*;
import me.minidigger.falcunnetworking.common.network.protocol.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import me.minidigger.falcunnetworking.common.api.FalcunUser;
import me.minidigger.falcunnetworking.common.api.Invite;
import me.minidigger.falcunnetworking.common.network.FalcunConnection;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunHandler;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacket;
import me.minidigger.falcunnetworking.common.network.protocol.client.ClientChatPacket.MessageType;

import static me.minidigger.falcunnetworking.common.network.protocol.server.ServerResponsePacket.ResponseType;

public class ServerFalcunHandler extends FalcunHandler {

    private static final Logger log = LoggerFactory.getLogger(ServerFalcunHandler.class);

    private final Queue<FalcunConnection> connections = new ConcurrentLinkedQueue<>();

    private final FalcunServer server;

    public ServerFalcunHandler(FalcunServer server) {
        this.server = server;
    }

    public Queue<FalcunConnection> getConnections() {
        return connections;
    }

    public FalcunConnection getConnection(FalcunUser user) {
        return connections.stream().filter(c -> c.getUser().getInternalId() == user.getInternalId()).findFirst().orElse(null);
    }

    public List<FalcunUser> getUsers() {
        return connections.stream().filter(FalcunConnection::isAuthFinished).map(FalcunConnection::getUser).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void join(FalcunConnection connection) {
        connections.add(connection);
        log.info("Now handling {} connections", connections.size());
    }

    @Override
    public void leave(FalcunConnection connection) {
        connections.remove(connection);
        connections.stream().filter(FalcunConnection::isAuthFinished).forEach(x -> x.sendPacket(new ServerListResponsePacket(getUsers(), true)));
        log.info("Now handling {} connections", connections.size());
    }

    @Override
    public void handle(FalcunConnection connection, FalcunPacket msg) {
        if (msg instanceof ClientLoginStartPacket) {
            ClientLoginStartPacket packet = (ClientLoginStartPacket) msg;
            for (FalcunConnection con : connections) {
                if (con.getUser() != null && con.getUser().getName() != null && con.getUser().getName().equals(packet.getUsername())) {
                    connection.close("Already connected from a different connection!");
                    return;
                }
            }
            connection.initUser(packet.getUsername());
            connection.sendPacket(new ServerEncryptionRequestPacket(server.getAuthHandler().getPublicKey(), server.getAuthHandler().genVerificationToken(packet.getUsername())));
        } else if (msg instanceof ClientEncryptionResponsePacket) {
            ClientEncryptionResponsePacket packet = (ClientEncryptionResponsePacket) msg;
            server.getAuthHandler().auth(packet, connection);
        } else if (!connection.isAuthFinished()) {
            // everything below requires auth, so stop if not authed
            log.warn("Can't accept packet {} from {} without being logged in!", msg, connection);
            connection.close("Sending " + msg.getClass().getName() + " without being logged in");
        } else if (msg instanceof ClientPingPacket) {
            ClientPingPacket pingPacket = (ClientPingPacket) msg;
            connection.sendPacket(new ServerPongPacket(pingPacket.getPayload()));
        } else if (msg instanceof ClientChatPacket) {
            ClientChatPacket packet = (ClientChatPacket) msg;

            if (packet.getMessageType() == MessageType.USER) {
                FalcunUser user = server.getUsersHandler().getUser(packet.getUser());
                if (user == null) {
                    connection.sendPacket(new ServerResponsePacket("Unknown user", ResponseType.WARNING));
                } else {
                    String response = server.getFriendsHandler().chat(connection.getUser(), user, packet.getMessage());
                    connection.sendPacket(new ServerResponsePacket(response, "Send".equals(response) ? ResponseType.OK : ResponseType.WARNING));
                }
            } else if (packet.getMessageType() == MessageType.GROUP) {
                String response = server.getGroupsHandler().chat(packet.getName(), connection.getUser(), packet.getMessage());
                connection.sendPacket(new ServerResponsePacket(response, "Send".equals(response) ? ResponseType.OK : ResponseType.WARNING));
            } else {
                connection.sendPacket(new ServerResponsePacket("Unknown message type", ResponseType.ERROR));
            }
        } else if (msg instanceof ClientListRequestPacket) {
            ClientListRequestPacket packet = (ClientListRequestPacket) msg;
            switch (packet.getType()) {
                case FALCUN_USERS:
                    connection.sendPacket(new ServerListResponsePacket(getUsers(), true));
                    break;
                case INCOMING_FRIEND_INVITES:
                    connection.sendPacket(new ServerListResponsePacket(packet.getType(), server.getFriendsHandler().getIncomingInvites(connection.getUser())));
                    break;
                case OUTGOING_FRIEND_INVITES:
                    connection.sendPacket(new ServerListResponsePacket(packet.getType(), server.getFriendsHandler().getOutgoingInvites(connection.getUser())));
                    break;
                case INCOMING_GROUP_INVITES:
                    connection.sendPacket(new ServerListResponsePacket(packet.getType(), server.getGroupsHandler().getIncomingInvites(connection.getUser())));
                    break;
                case OUTGOING_GROUP_INVITES:
                    connection.sendPacket(new ServerListResponsePacket(packet.getGroupName(), server.getGroupsHandler().getOutgoingInvites(packet.getGroupName(), connection.getUser())));
                    break;
                case FRIENDS:
                    connection.sendPacket(new ServerListResponsePacket(server.getFriendsHandler().getFriends(connection.getUser())));
                    break;
                case GROUPS:
                    connection.sendPacket(new ServerListResponsePacket(server.getGroupsHandler().getGroups(connection.getUser(), true), 1337));
                    break;
                case MESSAGES:
                    connection.sendPacket(new ServerListResponsePacket(server.getGroupsHandler().getMessages(packet.getGroupName(), connection.getUser()), packet.getGroupName()));
                    break;
                default:
                    connection.sendPacket(new ServerResponsePacket("Unknown list request type", ResponseType.ERROR));
                    break;
            }
        } else if (msg instanceof ClientActionPacket) {
            ClientActionPacket packet = (ClientActionPacket) msg;

            String status;
            switch (packet.getAction()) {
                case SEND_FRIEND_INVITE:
                case SEND_GROUP_INVITE:
                    Invite invite;
                    if (packet.getAction() == ClientActionPacket.Action.SEND_FRIEND_INVITE) {
                        invite = server.getFriendsHandler().sendInvite(connection.getUser(), packet.getUsername());
                    } else {
                        invite = server.getGroupsHandler().sendInvite(packet.getGroup(), connection.getUser(), packet.getUsername());
                    }
                    if (invite != null) {
                        FalcunConnection con = server.getHandler().getConnection(invite.getUser());
                        if (con != null) {
                            con.sendPacket(new ServerInviteStatusPacket(invite, ServerInviteStatusPacket.InviteStatus.NEW));
                        }
                        connection.sendPacket(new ServerResponsePacket("OK", ResponseType.OK));
                    } else {
                        connection.sendPacket(new ServerResponsePacket("Error", ResponseType.ERROR));
                    }
                    return;
                case ACCEPT_FRIEND_INVITE:
                case ACCEPT_GROUP_INVITE:
                    if (packet.getAction() == ClientActionPacket.Action.ACCEPT_FRIEND_INVITE) {
                        status = server.getFriendsHandler().acceptInvite(connection.getUser(), packet.getInvite());
                    } else {
                        status = server.getGroupsHandler().acceptInvite(connection.getUser(), packet.getInvite());
                    }
                    if ("OK".equals(status)) {
                        FalcunConnection con = server.getHandler().getConnection(packet.getInvite().getInviter());
                        if (con != null) {
                            con.sendPacket(new ServerInviteStatusPacket(packet.getInvite(), ServerInviteStatusPacket.InviteStatus.ACCEPTED));
                        }
                    }
                    break;
                case DECLINE_FRIEND_INVITE:
                case DECLINE_GROUP_INVITE:
                    if (packet.getAction() == ClientActionPacket.Action.DECLINE_FRIEND_INVITE) {
                        status = server.getFriendsHandler().declineInvite(connection.getUser(), packet.getInvite());
                    } else {
                        status = server.getGroupsHandler().declineInvite(connection.getUser(), packet.getInvite());
                    }
                    if ("OK".equals(status)) {
                        FalcunConnection con = server.getHandler().getConnection(packet.getInvite().getInviter());
                        if (con != null) {
                            con.sendPacket(new ServerInviteStatusPacket(packet.getInvite(), ServerInviteStatusPacket.InviteStatus.DECLINED));
                        }
                    }
                    break;
                case REVOKE_FRIEND_INVITE:
                case REVOKE_GROUP_INVITE:
                    if (packet.getAction() == ClientActionPacket.Action.REVOKE_FRIEND_INVITE) {
                        status = server.getFriendsHandler().revokeInvite(connection.getUser(), packet.getInvite());
                    } else {
                        status = server.getGroupsHandler().revokeInvite(connection.getUser(), packet.getInvite());
                    }
                    if ("OK".equals(status)) {
                        FalcunConnection con = server.getHandler().getConnection(packet.getInvite().getUser());
                        if (con != null) {
                            con.sendPacket(new ServerInviteStatusPacket(packet.getInvite(), ServerInviteStatusPacket.InviteStatus.REVOKED));
                        }
                    }
                    break;
                case REMOVE_FRIEND:
                    status = server.getFriendsHandler().removeUser(connection.getUser(), packet.getUser());
                    break;
                case KICK_USER:
                    status = server.getGroupsHandler().kick(packet.getGroup(), connection.getUser(), packet.getUser());
                    break;
                case CREATE_GROUP:
                    status = server.getGroupsHandler().createGroup(packet.getGroupName(), connection.getUser());
                    break;
                case DELETE_GROUP:
                    status = server.getGroupsHandler().deleteGroup(packet.getGroupName(), connection.getUser());
                    break;
                default:
                    connection.sendPacket(new ServerResponsePacket("Unknown action", ResponseType.ERROR));
                    return;
            }
            connection.sendPacket(new ServerResponsePacket(status, "OK".equals(status) ? ResponseType.OK : ResponseType.ERROR));
        } else if (msg instanceof ClientEmoteBroadcastPacket) {
            ClientEmoteBroadcastPacket packet = (ClientEmoteBroadcastPacket) msg;
            server.getHandler().getConnections().stream().filter(FalcunConnection::isAuthFinished).forEach(c -> c.sendPacket(new ServerEmoteBroadcastPacket(packet.getUser(), packet.getEmote())));
        } else if (msg instanceof ClientEmoteStopPacket) {
            ClientEmoteStopPacket packet = (ClientEmoteStopPacket) msg;
            server.getHandler().getConnections().stream().filter(FalcunConnection::isAuthFinished).forEach(c -> c.sendPacket(new ServerEmoteStopPacket(packet.getUser())));
        }
    }
}
