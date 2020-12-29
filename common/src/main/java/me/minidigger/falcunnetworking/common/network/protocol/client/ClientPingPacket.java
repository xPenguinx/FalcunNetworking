package me.minidigger.falcunnetworking.common.network.protocol.client;

import com.google.common.base.MoreObjects;

import io.netty.buffer.ByteBuf;
import me.minidigger.falcunnetworking.common.network.protocol.DataTypes;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacket;

public class ClientPingPacket extends FalcunPacket {

    private String payload;

    public ClientPingPacket() {

    }

    public ClientPingPacket(String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }

    @Override
    public void toWire(ByteBuf buf) {
        DataTypes.writeString(payload, buf);
    }

    @Override
    public void fromWire(ByteBuf buf) {
        payload = DataTypes.readString(buf);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("payload", payload)
                .toString();
    }
}
