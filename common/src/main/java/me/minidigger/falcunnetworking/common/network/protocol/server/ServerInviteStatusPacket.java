package me.minidigger.falcunnetworking.common.network.protocol.server;

import com.google.common.base.MoreObjects;

import io.netty.buffer.ByteBuf;
import me.minidigger.falcunnetworking.common.api.Invite;
import me.minidigger.falcunnetworking.common.network.protocol.DataTypes;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacket;

public class ServerInviteStatusPacket extends FalcunPacket {

    private Invite invite;
    private InviteStatus status;

    public ServerInviteStatusPacket() {

    }

    public ServerInviteStatusPacket(Invite invite, InviteStatus status) {
        this.invite = invite;
        this.status = status;
    }

    public Invite getInvite() {
        return invite;
    }

    public InviteStatus getStatus() {
        return status;
    }

    @Override
    public void toWire(ByteBuf buf) {
        DataTypes.writeInvite(invite, buf);
        buf.writeInt(status.ordinal());
    }

    @Override
    public void fromWire(ByteBuf buf) {
        invite = DataTypes.readInvite(buf);
        status = InviteStatus.values()[buf.readInt()];
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("invite", invite)
                .add("status", status)
                .toString();
    }

    public enum InviteStatus {
        ACCEPTED, DECLINED, REVOKED, NEW
    }
}
