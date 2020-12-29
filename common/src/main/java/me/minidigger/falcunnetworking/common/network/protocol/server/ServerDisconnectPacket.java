package me.minidigger.falcunnetworking.common.network.protocol.server;

import com.google.common.base.MoreObjects;

import io.netty.buffer.ByteBuf;
import me.minidigger.falcunnetworking.common.network.protocol.DataTypes;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacket;

public class ServerDisconnectPacket extends FalcunPacket {

    private String reason;

    public ServerDisconnectPacket() {

    }

    public ServerDisconnectPacket(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public void toWire(ByteBuf buf) {
        DataTypes.writeString(reason, buf);
    }

    @Override
    public void fromWire(ByteBuf buf) {
        reason = DataTypes.readString(buf);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("reason", reason)
                .toString();
    }
}
