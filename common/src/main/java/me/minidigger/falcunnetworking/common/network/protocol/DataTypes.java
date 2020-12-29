package me.minidigger.falcunnetworking.common.network.protocol;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import me.minidigger.falcunnetworking.common.Constants;
import me.minidigger.falcunnetworking.common.api.FalcunGroup;
import me.minidigger.falcunnetworking.common.api.FalcunUser;
import me.minidigger.falcunnetworking.common.api.Invite;
import me.minidigger.falcunnetworking.common.api.InviteType;
import me.minidigger.falcunnetworking.common.api.Message;

public class DataTypes {

    public static String readString(ByteBuf buf) {
        int length = buf.readInt();

        byte[] bytes = new byte[length];
        buf.readBytes(bytes);

        return new String(bytes, Constants.CHARSET);
    }

    public static void writeString(String string, ByteBuf buf) {
        if (string == null) {
            buf.writeInt(0);
            return;
        }
        byte[] bytes = string.getBytes(Constants.CHARSET);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    public static byte[] readByteArray(ByteBuf buf) {
        int len = buf.readInt();
        byte[] data = new byte[len];
        buf.readBytes(data);
        return data;
    }

    public static void writeByteArray(byte[] arr, ByteBuf buf) {
        buf.writeInt(arr.length);
        buf.writeBytes(arr);
    }

    public static FalcunUser readUser(ByteBuf buf) {
        long id = buf.readLong();
        UUID uuid = UUID.fromString(readString(buf));
        String name = readString(buf);
        FalcunUser user = new FalcunUser(uuid, name);
        user.setInternalId(id);
        return user;
    }

    public static void writeUser(FalcunUser user, ByteBuf buf) {
        buf.writeLong(user.getInternalId());
        writeString(user.getId().toString(), buf);
        writeString(user.getName(), buf);
    }

    public static Invite readInvite(ByteBuf buf) {
        InviteType type = InviteType.values()[buf.readInt()];
        long id = buf.readLong();
        FalcunUser user = readUser(buf);
        FalcunUser inviter = readUser(buf);
        if (type == InviteType.GROUP) {
            FalcunGroup group = readGroup(buf);
            Invite invite = new Invite(user, group, inviter);
            invite.setInternalId(id);
            return invite;
        } else {
            Invite invite = new Invite(user, inviter);
            invite.setInternalId(id);
            return invite;
        }
    }

    public static void writeInvite(Invite invite, ByteBuf buf) {
        buf.writeInt(invite.getType().ordinal());
        buf.writeLong(invite.getInternalId());
        writeUser(invite.getUser(), buf);
        writeUser(invite.getInviter(), buf);
        if (invite.getType() == InviteType.GROUP) {
            writeGroup(invite.getGroup(), buf);
        }
    }

    public static FalcunGroup readGroup(ByteBuf buf) {
        FalcunGroup group = new FalcunGroup();
        group.setOwner(readUser(buf));
        group.setName(readString(buf));
        group.setInternalId(buf.readLong());

        int size = buf.readInt();
        if (size > 0) {
            Set<FalcunUser> users = new HashSet<>();
            for (int i = 0; i < size; i++) {
                users.add(readUser(buf));
            }
            group.setUsers(users);
        }
        return group;
    }

    public static void writeGroup(FalcunGroup group, ByteBuf buf) {
        writeUser(group.getOwner(), buf);
        writeString(group.getName(), buf);
        buf.writeLong(group.getInternalId());

        if (group.getUsers() != null) {
            buf.writeInt(group.getUsers().size());
            for (FalcunUser user : group.getUsers()) {
                writeUser(user, buf);
            }
        } else {
            buf.writeInt(0);
        }
    }

    public static Message readMessage(ByteBuf buf) {
        Message message = new Message();
        message.setInternalId(buf.readLong());
        message.setSender(readUser(buf));
        message.setGroup(readGroup(buf));
        message.setTimestamp(new Date(buf.readLong()));
        message.setMessage(readString(buf));
        return message;
    }

    public static void writeMessage(Message message, ByteBuf buf) {
        buf.writeLong(message.getInternalId());
        writeUser(message.getSender(), buf);
        writeGroup(message.getGroup(), buf);
        buf.writeLong(message.getTimestamp().getTime());
        writeString(message.getMessage(), buf);
    }
}
