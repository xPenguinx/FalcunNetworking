package me.minidigger.falcunnetworking.server.api;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import me.minidigger.falcunnetworking.common.api.FalcunGroup;
import me.minidigger.falcunnetworking.common.api.FalcunUser;
import me.minidigger.falcunnetworking.common.api.Invite;
import me.minidigger.falcunnetworking.common.api.InviteType;
import me.minidigger.falcunnetworking.common.api.Message;
import me.minidigger.falcunnetworking.common.network.FalcunConnection;
import me.minidigger.falcunnetworking.common.network.protocol.server.ServerChatPacket;
import me.minidigger.falcunnetworking.server.FalcunServer;
import me.minidigger.falcunnetworking.server.db.GroupDao;
import me.minidigger.falcunnetworking.server.db.InviteDao;
import me.minidigger.falcunnetworking.server.db.MessageDao;
import me.minidigger.falcunnetworking.server.db.UserDao;
import me.minidigger.falcunnetworking.server.db.tables.GroupTable;
import me.minidigger.falcunnetworking.server.db.tables.InviteTable;
import me.minidigger.falcunnetworking.server.db.tables.MessageTable;
import me.minidigger.falcunnetworking.server.db.tables.UsersTable;

public class GroupsHandler {

    private final FalcunServer server;

    private final LoadingCache<Long, FalcunGroup> cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<Long, FalcunGroup>() {
                @Override
                public FalcunGroup load(Long id) {
                    GroupTable table = server.getDbHandler().jdbi().withExtension(GroupDao.class, handle -> handle.getById(id));

                    if (table == null) {
                        return null;
                    }

                    return table.convert(server.getUsersHandler());
                }
            });

    public GroupsHandler(FalcunServer server) {
        this.server = server;
    }

    public String createGroup(String name, FalcunUser owner) {
        if (name == null || name.length() < 4) {
            return "Name must be at least 4 chars long!";
        }

        return server.getDbHandler().jdbi().withExtension(GroupDao.class, handle -> {
            GroupTable group = handle.getGroup(name, owner);
            if (group != null) {
                return "Group already exists!";
            }
            long id = handle.createGroup(name, owner);
            handle.addUser(id, owner);
            return "OK";
        });
    }

    public String deleteGroup(String name, FalcunUser owner) {
        return server.getDbHandler().jdbi().withExtension(GroupDao.class, handle -> {
            GroupTable group = handle.getGroup(name, owner);
            if (group == null) {
                return "Group not found!";
            }
            if (group.getOwner() == owner.getInternalId()) {
                return "Only the owner can delete the group!";
            }
            if (handle.deleteGroup(group.getId())) {
                return "OK";
            } else {
                return "Failed to delete group!";
            }
        });
    }

    public String kick(FalcunGroup group, FalcunUser owner, FalcunUser kicked) {
        if (owner.getName().equals(kicked.getName()) || owner.getInternalId() == kicked.getInternalId()) {
            return "Can't kick yourself!";
        }
        return server.getDbHandler().jdbi().withExtension(GroupDao.class, handle -> {
            GroupTable realGroup = handle.getGroup(group.getName(), owner);

            if (realGroup.getOwner() != owner.getInternalId()) {
                return "Only owner can kick users!";
            }

            handle.removeUser(realGroup.getId(), kicked);
            return "OK";
        });
    }

    public Invite sendInvite(FalcunGroup group, FalcunUser sender, String receiverName) {
        // cant invite yourself
        if (sender.getName().equals(receiverName)) {
            return null;
        }

        // check if user exist
        UsersTable receiver = server.getDbHandler().jdbi().withExtension(UserDao.class, handle -> handle.getByName(receiverName));
        if (receiver == null) {
            return null;
        }

        GroupTable groupTable = server.getDbHandler().jdbi().withExtension(GroupDao.class, handle -> handle.getGroup(group.getName(), sender));
        FalcunGroup realGroup = groupTable.convert(server.getUsersHandler());
        // TODO validate group

        // check if owner
        if (groupTable.getOwner() != sender.getInternalId()) {
            return null;
        }

        // check if already member
        if (getGroupMembers(groupTable.getId()).stream().anyMatch(p -> p.getInternalId() == receiver.getId())) {
            return null;
        }

        // check existing invite
        for (Invite outgoingInvite : getOutgoingInvites(groupTable.getName(), sender)) {
            if (outgoingInvite.getUser().getName().equals(receiverName)) {
                return null;
            }
        }

        return server.getDbHandler().jdbi().withExtension(InviteDao.class, handle -> {
            long id = handle.createInvite(new InviteTable(receiver.getId(), sender.getInternalId(), groupTable.getId()));
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
        return server.getDbHandler().jdbi().withExtension(GroupDao.class, handle -> {
            handle.addUser(invite.getGroup().getInternalId(), invite.getUser());
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

    public List<FalcunGroup> getGroups(FalcunUser user, boolean includeMembers) {
            List<FalcunGroup> groups = server.getDbHandler().jdbi().withExtension(GroupDao.class, handle -> handle.getGroups(user))
                    .stream().map(g -> g.convert(server.getUsersHandler())).collect(Collectors.toList());
            if (includeMembers) {
                server.getDbHandler().jdbi().withExtension(GroupDao.class, handle -> {
                    groups.forEach(g -> g.setUsers(handle.getGroupMembers(g.getInternalId()).stream().map(UsersTable::convert).collect(Collectors.toSet())));
                    return null;
                });
            }

            return groups;
    }

    public List<Invite> getIncomingInvites(FalcunUser user) {
        List<Invite> list = new ArrayList<>();
        for (InviteTable table : server.getDbHandler().jdbi().withExtension(InviteDao.class, handle -> handle.getIncomingInvites(user, InviteType.GROUP))) {
            Invite convert = table.convert(server.getUsersHandler(), server.getGroupsHandler());
            list.add(convert);
        }
        return list;
    }

    public List<Invite> getOutgoingInvites(String name, FalcunUser user) {
        FalcunGroup group = getGroup(name, user);
        if (group == null) {
            return new ArrayList<>();
        }
        long groupId = group.getInternalId();
        List<Invite> list = new ArrayList<>();
        for (InviteTable table : server.getDbHandler().jdbi().withExtension(InviteDao.class, handle -> handle.getOutgoingInvites(user, InviteType.GROUP, groupId))) {
            Invite convert = table.convert(server.getUsersHandler(), server.getGroupsHandler());
            list.add(convert);
        }
        return list;
    }

    public List<FalcunUser> getGroupMembers(long groupId) {
        return server.getDbHandler().jdbi().withExtension(GroupDao.class, handle -> handle.getGroupMembers(groupId))
                .stream().map(UsersTable::convert).collect(Collectors.toList());
    }

    public FalcunGroup getGroup(String name, FalcunUser user) {
        GroupTable groupTable = server.getDbHandler().jdbi().withExtension(GroupDao.class, handle -> handle.getGroup(name, user));
        if (groupTable == null) {
            return null;
        }
        return groupTable.convert(server.getUsersHandler());
    }

    public String chat(String name, FalcunUser user, String message) {
        FalcunGroup group = getGroup(name, user);
        if (group == null) {
            return "You are not in a group named like that!";
        }
        getGroupMembers(group.getInternalId()).forEach(receiver -> {
            FalcunConnection connection = server.getHandler().getConnection(receiver);
            if (connection != null) {
                connection.sendPacket(new ServerChatPacket(message, name, user.getId()));
            }
        });

        MessageTable messageTable = new MessageTable();
        messageTable.setGroupId(group.getInternalId());
        messageTable.setMessage(message);
        messageTable.setUser(user.getInternalId());
        return server.getDbHandler().jdbi().withExtension(MessageDao.class, handle -> {
            handle.saveMessage(messageTable);
            return "Send";
        });
    }

    public List<Message> getMessages(String name, FalcunUser user) {
        FalcunGroup group = getGroup(name, user);
        if (group == null) {
            return new ArrayList<>();
        }

        return server.getDbHandler().jdbi().withExtension(MessageDao.class, handle -> handle.retrieveMessages(group.getInternalId(), 100))
                .stream().map((MessageTable table) -> MessageTable.convert(table, server.getUsersHandler(), server.getGroupsHandler())).collect(Collectors.toList());
    }

    public FalcunGroup getOrCacheGroup(long groupId) {
        try {
            return cache.get(groupId);
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }
}
