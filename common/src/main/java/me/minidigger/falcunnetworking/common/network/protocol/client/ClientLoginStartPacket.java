package me.minidigger.falcunnetworking.common.network.protocol.client;

import com.google.common.base.MoreObjects;

import io.netty.buffer.ByteBuf;
import me.minidigger.falcunnetworking.common.network.protocol.DataTypes;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacket;

public class ClientLoginStartPacket extends FalcunPacket {

    private String username;

    public ClientLoginStartPacket() {

    }

    public ClientLoginStartPacket(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void toWire(ByteBuf buf) {
        DataTypes.writeString(username, buf);
    }

    @Override
    public void fromWire(ByteBuf buf) {
        this.username = DataTypes.readString(buf);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("username", username)
                .toString();
    }
}
