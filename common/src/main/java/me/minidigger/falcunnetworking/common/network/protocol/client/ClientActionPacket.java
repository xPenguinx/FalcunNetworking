package me.minidigger.falcunnetworking.common.network.protocol.client;

import com.google.common.base.MoreObjects;

import io.netty.buffer.ByteBuf;
import me.minidigger.falcunnetworking.common.api.FalcunGroup;
import me.minidigger.falcunnetworking.common.api.FalcunUser;
import me.minidigger.falcunnetworking.common.api.Invite;
import me.minidigger.falcunnetworking.common.network.protocol.DataTypes;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacket;

public class ClientActionPacket extends FalcunPacket {

    private Action action;
    private Invite invite;
    private FalcunUser user;
    private String username;
    private FalcunGroup group;
    private String groupName;

    public ClientActionPacket() {

    }

    public ClientActionPacket(Action action, Invite invite) {
        this.action = action;
        this.invite = invite;
    }

    public ClientActionPacket(FalcunUser user) {
        this.action = Action.REMOVE_FRIEND;
        this.user = user;
    }

    public ClientActionPacket(String username) {
        this.action = Action.SEND_FRIEND_INVITE;
        this.username = username;
    }

    public ClientActionPacket(FalcunUser user, FalcunGroup group) {
        this.action = Action.KICK_USER;
        this.user = user;
        this.group = group;
    }

    public ClientActionPacket(String groupName, Action action) {
        this.groupName = groupName;
        this.action = action;
    }

    public ClientActionPacket(FalcunGroup group, String username, Action action) {
        this.group = group;
        this.username = username;
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public FalcunUser getUser() {
        return user;
    }

    public Invite getInvite() {
        return invite;
    }

    public FalcunGroup getGroup() {
        return group;
    }

    public String getUsername() {
        return username;
    }

    public String getGroupName() {
        return groupName;
    }

    @Override
    public void toWire(ByteBuf buf) {
        buf.writeInt(action.ordinal());

        if (action == Action.KICK_USER) {
            DataTypes.writeGroup(group, buf);
            DataTypes.writeUser(user, buf);
        } else if (action == Action.REMOVE_FRIEND) {
            DataTypes.writeUser(user, buf);
        } else if (action == Action.SEND_FRIEND_INVITE || action == Action.SEND_GROUP_INVITE) {
            if (action == Action.SEND_GROUP_INVITE) {
                DataTypes.writeGroup(group, buf);
            }
            DataTypes.writeString(username, buf);
        } else if (action == Action.CREATE_GROUP || action == Action.DELETE_GROUP) {
            DataTypes.writeString(groupName, buf);
        } else {
            DataTypes.writeInvite(invite, buf);
        }
    }

    @Override
    public void fromWire(ByteBuf buf) {
        action = Action.values()[buf.readInt()];
        if (action == Action.KICK_USER) {
            group = DataTypes.readGroup(buf);
            user = DataTypes.readUser(buf);
        } else if (action == Action.REMOVE_FRIEND) {
            user = DataTypes.readUser(buf);
        } else if (action == Action.SEND_FRIEND_INVITE || action == Action.SEND_GROUP_INVITE) {
            if (action == Action.SEND_GROUP_INVITE) {
                group = DataTypes.readGroup(buf);
            }
            username = DataTypes.readString(buf);
        } else if (action == Action.CREATE_GROUP || action == Action.DELETE_GROUP) {
            groupName = DataTypes.readString(buf);
        } else {
            invite = DataTypes.readInvite(buf);
        }
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper string = MoreObjects.toStringHelper(this)
                .add("action", this.action);
        if (action == Action.KICK_USER) {
            string.add("user", user);
            string.add("group", group);
        } else if (action == Action.REMOVE_FRIEND) {
            string.add("user", user);
        } else if (action == Action.SEND_FRIEND_INVITE || action == Action.SEND_GROUP_INVITE) {
            if (action == Action.SEND_GROUP_INVITE) {
                string.add("group", group);
            }
            string.add("username", username);
        } else if (action == Action.CREATE_GROUP || action == Action.DELETE_GROUP) {
            string.add("groupName", groupName);
        } else {
            string.add("invite", invite);
        }
        return string.toString();
    }

    public enum Action {
        ACCEPT_FRIEND_INVITE, DECLINE_FRIEND_INVITE, REVOKE_FRIEND_INVITE, SEND_FRIEND_INVITE, REMOVE_FRIEND,
        ACCEPT_GROUP_INVITE, DECLINE_GROUP_INVITE, REVOKE_GROUP_INVITE, SEND_GROUP_INVITE, KICK_USER, CREATE_GROUP, DELETE_GROUP
    }
}
