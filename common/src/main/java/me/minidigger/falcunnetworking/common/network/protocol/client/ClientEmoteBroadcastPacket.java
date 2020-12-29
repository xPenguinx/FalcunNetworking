package me.minidigger.falcunnetworking.common.network.protocol.client;

import com.google.common.base.MoreObjects;
import io.netty.buffer.ByteBuf;
import me.minidigger.falcunnetworking.common.api.FalcunUser;
import me.minidigger.falcunnetworking.common.network.protocol.DataTypes;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacket;

public class ClientEmoteBroadcastPacket extends FalcunPacket {

    private FalcunUser user;
    private String emote;

    public ClientEmoteBroadcastPacket() {

    }

    public ClientEmoteBroadcastPacket(FalcunUser user, String emote) {
        this.user = user;
        this.emote = emote;
    }

    public FalcunUser getUser() {
        return user;
    }

    public String getEmote() {
        return emote;
    }

    @Override
    public void toWire(ByteBuf buf) {
        DataTypes.writeUser(user, buf);
        DataTypes.writeString(emote, buf);
    }

    @Override
    public void fromWire(ByteBuf buf) {
        user = DataTypes.readUser(buf);
        emote = DataTypes.readString(buf);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("user", user)
                .add("emote", emote)
                .toString();
    }
}
