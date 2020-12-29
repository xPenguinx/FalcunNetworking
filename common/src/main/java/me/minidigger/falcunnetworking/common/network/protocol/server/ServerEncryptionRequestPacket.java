package me.minidigger.falcunnetworking.common.network.protocol.server;

import com.google.common.base.MoreObjects;

import java.security.PublicKey;

import io.netty.buffer.ByteBuf;
import me.minidigger.falcunnetworking.common.network.CryptUtil;
import me.minidigger.falcunnetworking.common.network.protocol.DataTypes;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacket;

public class ServerEncryptionRequestPacket extends FalcunPacket {

    private PublicKey publicKey;
    private byte[] verifyToken;

    public ServerEncryptionRequestPacket() {

    }

    public ServerEncryptionRequestPacket(PublicKey publicKey, byte[] verifyToken) {
        this.publicKey = publicKey;
        this.verifyToken = verifyToken;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public byte[] getVerifyToken() {
        return verifyToken;
    }

    @Override
    public void toWire(ByteBuf buf) {
        DataTypes.writeByteArray(publicKey.getEncoded(), buf);
        DataTypes.writeByteArray(verifyToken, buf);
    }

    @Override
    public void fromWire(ByteBuf buf) {
        publicKey = CryptUtil.decode(DataTypes.readByteArray(buf));
        verifyToken = DataTypes.readByteArray(buf);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("publicKey", publicKey)
                .add("verifyToken", verifyToken)
                .toString();
    }
}
