package me.minidigger.falcunnetworking.client;

import me.minidigger.falcunnetworking.common.network.protocol.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.minidigger.falcunnetworking.common.api.FalcunGroup;
import me.minidigger.falcunnetworking.common.api.FalcunUser;
import me.minidigger.falcunnetworking.common.api.Invite;
import me.minidigger.falcunnetworking.common.api.Message;
import me.minidigger.falcunnetworking.common.network.FalcunConnection;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunHandler;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacket;
import me.minidigger.falcunnetworking.common.network.protocol.client.ClientListRequestPacket;
import me.minidigger.falcunnetworking.common.network.protocol.client.ClientListRequestPacket.ListType;
import me.minidigger.falcunnetworking.common.network.protocol.client.ClientLoginStartPacket;
import me.minidigger.falcunnetworking.common.network.protocol.client.ClientPingPacket;

public class ClientFalcunHandler extends FalcunHandler {

    private static final Logger log = LoggerFactory.getLogger(ClientFalcunHandler.class);

    private final FalcunClient client;
    private FalcunConnection connection;

    // ideally you save them somewhere more accessible to your UI, where you can also have logic for invalidating the lists and stuff
    private List<FalcunUser> users = new ArrayList<FalcunUser>();
    private List<FalcunUser> friends = new ArrayList<FalcunUser>();
    private List<FalcunGroup> groups = new ArrayList<FalcunGroup>();
    private List<Invite> incomingFriendInvites = new ArrayList<Invite>();
    private List<Invite> outgoingFriendInvites = new ArrayList<Invite>();
    private Map<String, List<Invite>> incomingGroupInvites = new HashMap<String, List<Invite>>();
    private Map<String, List<Invite>> outgoingGroupInvites = new HashMap<String, List<Invite>>();
    private Map<String, List<Message>> messages = new HashMap<String, List<Message>>();

    public ClientFalcunHandler(FalcunClient client) {
        this.client = client;
    }

    @Override
    public void join(FalcunConnection connection) {
        this.connection = connection;
        connection.sendPacket(new ClientLoginStartPacket(client.getUser().getName()));
    }

    @Override
    public void leave(FalcunConnection connection) {
        this.connection = null;
        client.getConsole().shutdown();
    }

    public FalcunConnection getConnection() {
        return connection;
    }

    @Override
    public void handle(FalcunConnection connection, FalcunPacket msg) {
        if (msg instanceof ServerEncryptionRequestPacket) {
            ServerEncryptionRequestPacket packet = (ServerEncryptionRequestPacket) msg;
            client.getAuthHandler().auth(packet, connection, client.getMinecraftUser());
        } else if (msg instanceof ServerLoginSuccessPacket) {
            ServerLoginSuccessPacket packet = (ServerLoginSuccessPacket) msg;
            client.setUser(new FalcunUser(packet.getUuid(), packet.getUsername()));
            connection.setAuthFinished(true);
            log.info("Logged in as {}", client.getUser());
            connection.sendPacket(new ClientPingPacket("This is a test"));
            // request all data
            for (ListType listType : ListType.values()) {
                if (listType == ListType.MESSAGES || listType == ListType.OUTGOING_GROUP_INVITES) continue;
                connection.sendPacket(new ClientListRequestPacket(listType));
            }
        } else if (msg instanceof ServerDisconnectPacket) {
            ServerDisconnectPacket packet = (ServerDisconnectPacket) msg;
            log.warn("Disconnected: {}", packet.getReason());
        } else if (!connection.isAuthFinished()) {
            // everything below requires auth, so stop if not authed
            throw new RuntimeException("Can't accept packet " + msg + " without being logged in!");
        } else if (msg instanceof ServerPongPacket) {
            ServerPongPacket pongPacket = (ServerPongPacket) msg;
            log.info("PONG! {}", pongPacket.getPayload());
        } else if (msg instanceof ServerResponsePacket) {
            ServerResponsePacket packet = (ServerResponsePacket) msg;
            log.info("Got response: {} {}", packet.getType(), packet.getMessage());
        } else if (msg instanceof ServerChatPacket) {
            ServerChatPacket packet = (ServerChatPacket) msg;
            log.info("Incoming chat: {} ({})", packet.getMessage(), packet);
        } else if (msg instanceof ServerListResponsePacket) {
            ServerListResponsePacket packet = (ServerListResponsePacket) msg;
            // this is a lazy impl, ideally your UI does this
            switch (packet.getType()) {
                case FALCUN_USERS:
                    users = packet.getUsers();
                    break;
                case INCOMING_FRIEND_INVITES:
                    incomingFriendInvites = packet.getInvites();
                    break;
                case OUTGOING_FRIEND_INVITES:
                    outgoingFriendInvites = packet.getInvites();
                    break;
                case FRIENDS:
                    friends = packet.getFriends();
                    break;
                case INCOMING_GROUP_INVITES:
                    incomingGroupInvites = new HashMap<String, List<Invite>>();
                    for (Invite invite : packet.getInvites()) {
                        List<Invite> invites = incomingGroupInvites.get(invite.getGroup().getName());
                        if (invites == null) {
                            invites = new ArrayList<Invite>();
                        }
                        invites.add(invite);
                        incomingGroupInvites.put(invite.getGroup().getName(), invites);
                    }
                    break;
                case OUTGOING_GROUP_INVITES:
                    outgoingGroupInvites = new HashMap<String, List<Invite>>();
                    for (Invite invite : packet.getInvites()) {
                        List<Invite> invites = outgoingGroupInvites.get(invite.getGroup().getName());
                        if (invites == null) {
                            invites = new ArrayList<Invite>();
                        }
                        invites.add(invite);
                        outgoingGroupInvites.put(invite.getGroup().getName(), invites);
                    }
                    break;
                case MESSAGES:
                    this.messages.put(packet.getGroupName(), packet.getMessages());
                    log.info("Got {} messages for group {}", packet.getMessages().size(), packet.getGroupName());
                    break;
                case GROUPS:
                    groups = packet.getGroups();
                    break;
            }
        } else if (msg instanceof ServerInviteStatusPacket) {
            ServerInviteStatusPacket packet = (ServerInviteStatusPacket) msg;
            log.info("INVITE: " + packet);
            // this is a lazy impl, ideally your UI does this
            // some invite stuff changed, better invalidate all cached lists
            for (ListType listType : ListType.values()) {
                if (listType == ListType.MESSAGES || listType == ListType.OUTGOING_GROUP_INVITES) continue;
                connection.sendPacket(new ClientListRequestPacket(listType));
            }
        } else if(msg instanceof ServerEmoteBroadcastPacket) {
            ServerEmoteBroadcastPacket packet = (ServerEmoteBroadcastPacket) msg;
            //handle emote for user here
        } else if(msg instanceof ServerEmoteStopPacket) {
            ServerEmoteStopPacket packet = (ServerEmoteStopPacket) msg;
            //stop emote for user here
        }
    }

    public List<FalcunUser> getFriends() {
        return friends;
    }

    public List<FalcunGroup> getGroups() {
        return groups;
    }

    public List<Invite> getIncomingFriendInvites() {
        return incomingFriendInvites;
    }

    public List<Invite> getOutgoingFriendInvites() {
        return outgoingFriendInvites;
    }

    public Map<String, List<Invite>> getIncomingGroupInvites() {
        return incomingGroupInvites;
    }

    public Map<String, List<Invite>> getOutgoingGroupInvites() {
        return outgoingGroupInvites;
    }

    public FalcunGroup getGroup(String name) {
        for (FalcunGroup group : groups) {
            if (group.getName().equals(name)) {
                return group;
            }
        }
        return null;
    }
}
