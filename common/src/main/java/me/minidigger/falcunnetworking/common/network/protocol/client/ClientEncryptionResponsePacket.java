package me.minidigger.falcunnetworking.common.network.protocol.client;

import com.google.common.base.MoreObjects;

import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.netty.buffer.ByteBuf;
import me.minidigger.falcunnetworking.common.Constants;
import me.minidigger.falcunnetworking.common.network.CryptUtil;
import me.minidigger.falcunnetworking.common.network.protocol.DataTypes;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacket;

public class ClientEncryptionResponsePacket extends FalcunPacket {

    private SecretKey sharedSecret;
    private byte[] verificationToken;
    private final PublicKey key; // public key, send from server to client in encryption request
    private final PrivateKey serverKey; // servers private key, stored on the server and the server only

    public ClientEncryptionResponsePacket() {
        this.key = null;
        this.serverKey = Constants.getPrivateKey();
    }

    public ClientEncryptionResponsePacket(SecretKey sharedSecret, byte[] verificationToken, PublicKey key) {
        this.sharedSecret = sharedSecret;
        this.verificationToken = verificationToken;
        this.key = key;
        this.serverKey = null;
    }

    public SecretKey getSharedSecret() {
        return sharedSecret;
    }

    public byte[] getVerificationToken() {
        return verificationToken;
    }

    @Override
    public void toWire(ByteBuf buf) {
        DataTypes.writeByteArray(CryptUtil.encrypt(key, sharedSecret.getEncoded()), buf);
        DataTypes.writeByteArray(CryptUtil.encrypt(key, verificationToken), buf);
    }

    @Override
    public void fromWire(ByteBuf buf) {
        sharedSecret = new SecretKeySpec(CryptUtil.decrypt(serverKey, DataTypes.readByteArray(buf)), "AES");
        verificationToken = CryptUtil.decrypt(serverKey, DataTypes.readByteArray(buf));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("sharedSecret", sharedSecret)
                .add("verificationToken", verificationToken)
                .toString();
    }
}
