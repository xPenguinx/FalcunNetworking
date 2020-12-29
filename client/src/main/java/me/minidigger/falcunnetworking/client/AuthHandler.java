package me.minidigger.falcunnetworking.client;

import com.google.common.primitives.Longs;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import me.minidigger.falcunnetworking.common.Constants;
import me.minidigger.falcunnetworking.common.api.FalcunMinecraftUser;
import me.minidigger.falcunnetworking.common.network.CryptUtil;
import me.minidigger.falcunnetworking.common.network.FalcunConnection;
import me.minidigger.falcunnetworking.common.network.protocol.client.ClientEncryptionResponsePacket;
import me.minidigger.falcunnetworking.common.network.protocol.server.ServerEncryptionRequestPacket;

public class AuthHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);

    private final Gson gson = new Gson();

    public void auth(ServerEncryptionRequestPacket packet, FalcunConnection connection, FalcunMinecraftUser mcUser) {
        SecretKey sharedSecret = generateSharedKey();

        String serverHash = CryptUtil.genServerHash(sharedSecret, packet.getPublicKey());

        if (!Constants.DEBUG_OFFLINE_MODE && !authToMojang(mcUser.getAccessToken(), mcUser.getUuid().toString().replace("-", ""), serverHash)) {
            log.warn("Auth failed!");
            connection.clientClose();
            return;
        }

        connection.sendPacket(new ClientEncryptionResponsePacket(sharedSecret, packet.getVerifyToken(), packet.getPublicKey()));

        connection.enableEncryption(sharedSecret);
    }

    private boolean authToMojang(String accessToken, String profile, String serverHash) {
        StringEntity entity;
        try {
            JsonObject object = new JsonObject();
            object.add("accessToken", new JsonPrimitive(accessToken));
            object.add("selectedProfile", new JsonPrimitive(profile));
            object.add("serverId", new JsonPrimitive(serverHash));
            String json = gson.toJson(object);

            entity = new StringEntity(json, "UTF-8");
        } catch (Exception e) {
            log.warn("Error generating gson: ", e);
            return false;
        }

        HttpPost httpPost = new HttpPost("https://sessionserver.mojang.com/session/minecraft/join");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        CloseableHttpResponse response;
        CloseableHttpClient client = null;
        try {
            client = HttpClients.createDefault();
            response = client.execute(httpPost);
            client.close();
        } catch (IOException e) {
            log.warn("Error making auth call: ", e);
            return false;
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    log.warn("Error closing http client: ", e);
                    return false;
                }
            }
        }

        return response.getStatusLine().getStatusCode() == 204;
    }

    private SecretKey generateSharedKey() {
        try {
            KeyGenerator gen = KeyGenerator.getInstance("AES");
            return gen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    //
    //  Helper to get access token for test
    //

    public boolean authenticate(String username, String password) {
        StringEntity entity;
        try {
            JsonObject object = new JsonObject();
            object.add("username", new JsonPrimitive(username));
            object.add("password", new JsonPrimitive(password));
            object.add("requestUser", new JsonPrimitive(true));
            object.add("clientToken", new JsonPrimitive(UUID.nameUUIDFromBytes("Test".getBytes()).toString()));
            JsonObject agent = new JsonObject();
            agent.add("name", new JsonPrimitive("Minecraft"));
            agent.add("version", new JsonPrimitive(1));
            object.add("agent", agent);
            String json = gson.toJson(object);

            System.out.println(json);
            entity = new StringEntity(json, "UTF-8");
        } catch (Exception e) {
            log.warn("Error generating gson: ", e);
            return false;
        }

        HttpPost httpPost = new HttpPost("https://authserver.mojang.com/authenticate");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json; charset=UTF-8");

        CloseableHttpResponse response;
        CloseableHttpClient client = null;
        try {
            client = HttpClients.createDefault();
            response = client.execute(httpPost);

            if (response.getStatusLine().getStatusCode() < 299 && response.getEntity() != null) {
                try {
                    String content = EntityUtils.toString(response.getEntity());
                    JsonObject object = gson.fromJson(content, JsonObject.class);

                    byte[] bytes = Hex.decodeHex(object.get("selectedProfile").getAsJsonObject().get("id").getAsString().toCharArray());
                    long msb = Longs.fromBytes(bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7]);
                    long lsb = Longs.fromBytes(bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15]);
                    UUID id = new UUID(msb, lsb);

                    String accessToken = object.get("accessToken").getAsString();

                    log.info("Got UUID {}", id);
                    log.info("Got accessToken {}", accessToken);
                    return true;
                } catch (IOException e) {
                    log.warn("Error parsing auth response: ", e);
                    return false;
                } catch (DecoderException e) {
                    log.warn("Error parsing auth response: ", e);
                    return false;
                }
            } else {
                log.warn("Response: " + response.getStatusLine());
                if (response.getEntity() != null) {
                    try {
                        String content = EntityUtils.toString(response.getEntity());
                        log.warn("Failed with {}", content);
                        return false;
                    } catch (IOException e) {
                        log.warn("Error parsing auth response: ", e);
                        return false;
                    }
                } else {
                    log.warn("Auth failed");
                    return false;
                }
            }
        } catch (IOException e) {
            log.warn("Error making auth call: ", e);
            return false;
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    log.warn("Error closing http client: ", e);
                    return false;
                }
            }
        }
    }

    public static void main(String[] args) {
        new AuthHandler().authenticate(args[0], args[1]);
    }
}
