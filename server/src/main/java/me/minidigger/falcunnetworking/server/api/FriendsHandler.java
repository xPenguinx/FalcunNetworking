package me.minidigger.falcunnetworking.server.api;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import me.minidigger.falcunnetworking.common.api.FalcunUser;
import me.minidigger.falcunnetworking.common.api.Invite;
import me.minidigger.falcunnetworking.common.api.InviteType;
import me.minidigger.falcunnetworking.common.network.FalcunConnection;
import me.minidigger.falcunnetworking.common.network.protocol.server.ServerChatPacket;
import me.minidigger.falcunnetworking.server.FalcunServer;
import me.minidigger.falcunnetworking.server.db.FriendDao;
import me.minidigger.falcunnetworking.server.db.InviteDao;
import me.minidigger.falcunnetworking.server.db.UserDao;
import me.minidigger.falcunnetworking.server.db.tables.InviteTable;
import me.minidigger.falcunnetworking.server.db.tables.UsersTable;

public class FriendsHandler {

    private final FalcunServer server;

    public FriendsHandler(FalcunServer server) {
        this.server = server;
    }

    public Invite sendInvite(FalcunUser sender, String receiverName) {
        // cant invite yourself
        if (sender.getName().equals(receiverName)) {
            return null;
        }

        // check if already on friendlist
        for (FalcunUser friend : getFriends(sender)) {
            if (friend.getName().equals(receiverName)) {
                return null;
            }
        }

        // check existing invite
        for (Invite outgoingInvite : getOutgoingInvites(sender)) {
            if (outgoingInvite.getUser().getName().equals(receiverName)) {
                return null;
            }
        }

        // get user
        UsersTable receiver = server.getDbHandler().jdbi().withExtension(UserDao.class, handle -> handle.getByName(receiverName));
        if (receiver == null) {
            return null;
        }

        // check existing invite the other way around
        for (Invite incomingInvite : getIncomingInvites(receiver.convert())) {
            if (incomingInvite.getUser().getName().equals(receiverName)) {
                return null;
            }
        }

        return server.getDbHandler().jdbi().withExtension(InviteDao.class, handle -> {
            long id = handle.createInvite(new InviteTable(receiver.getId(), sender.getInternalId()));
            return handle.getInvite(id).convert(server.getUsersHandler(), server.getGroupsHandler());
        });
    }

    public String acceptInvite(FalcunUser user, Invite invite) {
        String error = server.getDbHandler().jdbi().withExtension(InviteDao.class, handle -> {
            if (!checkInviteAction(handle, user, invite, true)) {
                return "Invalid";
            }
            handle.deactivateInviteById(invite.getInternalId());
            return "OK";
        });
        if (!"OK".equals(error)) {
            return error;
        }
        return server.getDbHandler().jdbi().withExtension(FriendDao.class, handle -> {
            handle.addFriend(UsersTable.of(user), UsersTable.of(invite.getInviter()));
            return "OK";
        });
    }

    public String declineInvite(FalcunUser user, Invite invite) {
        return server.getDbHandler().jdbi().withExtension(InviteDao.class, handle -> {
            if (!checkInviteAction(handle, user, invite, true)) {
                return "Invalid";
            }
            handle.deactivateInviteById(invite.getInternalId());
            return "OK";
        });
    }

    public String revokeInvite(FalcunUser user, Invite invite) {
        return server.getDbHandler().jdbi().withExtension(InviteDao.class, handle -> {
            if (!checkInviteAction(handle, user, invite, false)) {
                return "Invalid";
            }
            handle.deactivateInviteById(invite.getInternalId());
            return "OK";
        });
    }

    private boolean checkInviteAction(InviteDao dao, FalcunUser user, Invite invite, boolean checkForUserAction) {
        InviteTable table = dao.getInvite(invite.getInternalId());
        boolean valid = table.convert(server.getUsersHandler(), server.getGroupsHandler()).equals(invite);
        if (checkForUserAction) {
            valid = valid && invite.getUser().equals(user);
        } else {
            valid = valid && invite.getInviter().equals(user);
        }
        return valid;
    }

    public List<Invite> getIncomingInvites(FalcunUser user) {
        List<Invite> list = new ArrayList<>();
        for (InviteTable table : server.getDbHandler().jdbi().withExtension(InviteDao.class, handle -> handle.getIncomingInvites(user, InviteType.FRIEND))) {
            Invite convert = table.convert(server.getUsersHandler(), server.getGroupsHandler());
            list.add(convert);
        }
        return list;
    }

    public List<Invite> getOutgoingInvites(FalcunUser user) {
        List<Invite> list = new ArrayList<>();
        for (InviteTable table : server.getDbHandler().jdbi().withExtension(InviteDao.class, handle -> handle.getOutgoingInvites(user, InviteType.FRIEND))) {
            Invite convert = table.convert(server.getUsersHandler(), server.getGroupsHandler());
            list.add(convert);
        }
        return list;
    }

    public List<FalcunUser> getFriends(FalcunUser user) {
        return server.getDbHandler().jdbi().withExtension(FriendDao.class,
                handle -> handle.findFriends(UsersTable.of(user))).
                stream().map(UsersTable::convert)
                .collect(Collectors.toList());
    }

    public String removeUser(FalcunUser sender, FalcunUser removed) {
        Integer affectedRows = server.getDbHandler().jdbi().withExtension(FriendDao.class,
                handle -> handle.removeFriend(UsersTable.of(sender), UsersTable.of(removed)));

        return affectedRows != 0 ? "OK" : "User was not in friendlist!";
    }

    public String chat(FalcunUser sender, FalcunUser receiver, String message) {
        FalcunConnection connection = server.getHandler().getConnection(receiver);
        if (connection == null) {
            return "User is not online or not in friend list!";
        }

        if (getFriends(sender).stream().noneMatch(p -> p.getInternalId() == receiver.getInternalId())) {
            return "User is not online or not in friend list!";
        } else {
            connection.sendPacket(new ServerChatPacket(message, sender.getId()));
            return "Send";
        }
    }
}
