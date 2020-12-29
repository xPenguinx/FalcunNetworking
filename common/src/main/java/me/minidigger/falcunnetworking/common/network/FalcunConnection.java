package me.minidigger.falcunnetworking.common.network;

import com.google.common.base.MoreObjects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import io.netty.channel.ChannelHandlerContext;
import me.minidigger.falcunnetworking.common.api.FalcunUser;
import me.minidigger.falcunnetworking.common.network.pipeline.FalcunPacketDecrypter;
import me.minidigger.falcunnetworking.common.network.pipeline.FalcunPacketEncrypter;
import me.minidigger.falcunnetworking.common.network.protocol.FalcunPacket;
import me.minidigger.falcunnetworking.common.network.protocol.server.ServerDisconnectPacket;

public class FalcunConnection {

    private static final Logger log = LoggerFactory.getLogger(FalcunConnection.class);

    private final ChannelHandlerContext ctx;
    private boolean authFinished = false;

    private FalcunUser user = new FalcunUser();

    public FalcunConnection(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public SocketAddress getRemoteAddress() {
        return ctx.channel().remoteAddress();
    }

    public void sendPacket(FalcunPacket packet) {
        ctx.channel().writeAndFlush(packet);
    }

    public void setUser(FalcunUser user) {
        this.user = user;
    }

    public FalcunUser getUser() {
        return user;
    }

    public void initUser(String username) {
        user.setName(username);
    }

    public void close(String reason) {
        sendPacket(new ServerDisconnectPacket(reason));
        ctx.channel().close();
    }

    public void clientClose() {
        ctx.channel().close();
    }

    public void setAuthFinished(boolean authFinished) {
        this.authFinished = authFinished;
    }

    public boolean isAuthFinished() {
        return authFinished;
    }

    public void enableEncryption(SecretKey key) {
        log.info("Enabling encryption");
        ctx.channel().pipeline().addBefore("lengthDecoder", "decrypter", new FalcunPacketDecrypter(CryptUtil.createContinuousCipher(Cipher.DECRYPT_MODE, key)));
        ctx.channel().pipeline().addBefore("lengthEncoder", "encrypter", new FalcunPacketEncrypter(CryptUtil.createContinuousCipher(Cipher.ENCRYPT_MODE, key)));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("addr", getRemoteAddress())
                .add("user", user)
                .add("authFinished", authFinished)
                .toString();
    }
}
