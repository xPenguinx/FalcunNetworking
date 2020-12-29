package me.minidigger.falcunnetworking.common.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.List;
import java.util.UUID;

public class FalcunUser {

    private long internalId;
    private UUID id;
    private String name;
    private List<FalcunUser> friends;

    public FalcunUser() {

    }
    public FalcunUser(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getInternalId() {
        return internalId;
    }

    public void setInternalId(long internalId) {
        this.internalId = internalId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FalcunUser user = (FalcunUser) o;
        return internalId == user.internalId &&
               Objects.equal(id, user.id) &&
               Objects.equal(name, user.name) &&
               Objects.equal(friends, user.friends);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(internalId, id, name, friends);
    }
}
