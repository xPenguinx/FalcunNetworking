package me.minidigger.falcunnetworking.common.network.protocol.server;

import com.google.common.base.MoreObjects;
import io.netty.buffer.ByteBuf;
import me.minidigger.falcunnetworking.common.api.FalcunUser;
import me.minidigger.falcunnetworking.common.network.protocol.DataTypes;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacket;

public class ServerEmoteStopPacket  extends FalcunPacket {

    private FalcunUser user;

    public ServerEmoteStopPacket() {

    }

    public ServerEmoteStopPacket(FalcunUser user) {
        this.user = user;
    }

    public FalcunUser getUser() {
        return user;
    }

    @Override
    public void toWire(ByteBuf buf) {
        DataTypes.writeUser(user, buf);
    }

    @Override
    public void fromWire(ByteBuf buf) {
        user = DataTypes.readUser(buf);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("user", user)
                .toString();
    }

}
