package me.minidigger.falcunnetworking.common.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.UUID;

public class FalcunMinecraftUser {

    private final String accessToken;
    private final UUID uuid;
    private final String username;

    public FalcunMinecraftUser(String accessToken, UUID uuid, String name) {
        this.accessToken = accessToken;
        this.uuid = uuid;
        this.username = name;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FalcunMinecraftUser that = (FalcunMinecraftUser) o;
        return Objects.equal(accessToken, that.accessToken) &&
               Objects.equal(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(accessToken, uuid);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("accessToken", accessToken)
                .add("uuid", uuid)
                .add("username", username)
                .toString();
    }
}
