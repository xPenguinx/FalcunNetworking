package me.minidigger.falcunnetworking.server;

import com.google.common.primitives.Longs;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import me.minidigger.falcunnetworking.common.Constants;
import me.minidigger.falcunnetworking.common.api.FalcunUser;
import me.minidigger.falcunnetworking.common.network.CryptUtil;
import me.minidigger.falcunnetworking.common.network.FalcunConnection;
import me.minidigger.falcunnetworking.common.network.protocol.client.ClientEncryptionResponsePacket;
import me.minidigger.falcunnetworking.common.network.protocol.server.ServerLoginSuccessPacket;

public class AuthHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);
    private static final SecureRandom random = new SecureRandom();

    private final FalcunServer server;
    private final Gson gson = new Gson();

    private final Map<String, byte[]> verificationTokens = new ConcurrentHashMap<>();

    private final KeyPair keypair;

    public AuthHandler(FalcunServer server) {
        this.server = server;
        this.keypair = generateKey();
        Constants.setPrivateKey(getPrivateKey());
    }

    public PublicKey getPublicKey() {
        return keypair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return keypair.getPrivate();
    }

    public byte[] genVerificationToken(String user) {
        byte[] token = new byte[4];
        random.nextBytes(token);
        verificationTokens.put(user, token);
        return token;
    }

    public static KeyPair generateKey() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean auth(ClientEncryptionResponsePacket packet, FalcunConnection connection) {
        if (!verificationTokens.containsKey(connection.getUser().getName())) {
            connection.close("Timed out");
            return false;
        }

        byte[] token = verificationTokens.get(connection.getUser().getName());
        if (!Arrays.equals(token, packet.getVerificationToken())) {
            connection.close("Access denied");
            return false;
        }

        String serverHash = CryptUtil.genServerHash(packet.getSharedSecret(), getPublicKey());
        UUID id = authToMojang(connection.getUser().getName(), serverHash);

        if (id == null) {
            log.warn("Auth failed for {}", connection.getUser());
            connection.close("Auth failed");
            return false;
        }

        FalcunUser dbUser = server.getUsersHandler().createOrLoadUser(id, connection.getUser().getName());
        if (dbUser == null) {
            log.warn("Error while loading user {}", connection.getUser());
            connection.close("Error while loading your data");
            return false;
        }

        connection.setUser(dbUser);
        connection.setAuthFinished(true);
        connection.enableEncryption(packet.getSharedSecret());
        connection.sendPacket(new ServerLoginSuccessPacket(dbUser.getName(), dbUser.getId()));
        log.info("{} logged in", dbUser);
        return true;
    }

    private UUID authToMojang(String username, String serverHash) {
        if (Constants.DEBUG_OFFLINE_MODE) {
            return UUID.nameUUIDFromBytes(username.getBytes());
        }

        URI uri;
        try {
            uri = new URIBuilder().setScheme("https").setHost("sessionserver.mojang.com").setPath("/session/minecraft/hasJoined")
                    .setParameter("username", username)
                    .setParameter("serverId", serverHash)
                    .build();
        } catch (URISyntaxException e) {
            log.warn("Error creating auth url: ", e);
            return null;
        }

        HttpGet httpGet = new HttpGet(uri);
        httpGet.setHeader("Accept", "application/json");
        httpGet.setHeader("Content-type", "application/json");

        CloseableHttpResponse response;
        CloseableHttpClient client = null;
        try {
            client = HttpClients.createDefault();
            response = client.execute(httpGet);

            if (response.getStatusLine().getStatusCode() < 299 && response.getEntity() != null) {
                try {
                    String content = EntityUtils.toString(response.getEntity());
                    JsonObject object = gson.fromJson(content, JsonObject.class);

                    byte[] bytes = Hex.decodeHex(object.get("id").getAsString().toCharArray());
                    long msb = Longs.fromBytes(bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7]);
                    long lsb = Longs.fromBytes(bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15]);
                    return new UUID(msb, lsb);
                } catch (IOException | DecoderException e) {
                    log.warn("Error parsing auth response: ", e);
                    return null;
                }
            } else {
                return null;
            }
        } catch (IOException e) {
            log.warn("Error making auth call: ", e);
            return null;
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    log.warn("Error closing http client: ", e);
                    return null;
                }
            }
        }
    }
}
