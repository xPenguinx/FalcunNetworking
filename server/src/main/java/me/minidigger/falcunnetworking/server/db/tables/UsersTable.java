package me.minidigger.falcunnetworking.server.db.tables;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.time.OffsetDateTime;
import java.util.UUID;

import me.minidigger.falcunnetworking.common.api.FalcunUser;

public class UsersTable {

    private long id;
    private String uuid;
    private String name;
    private OffsetDateTime timestamp;

    public UsersTable() {

    }

    public UsersTable(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public static UsersTable of(FalcunUser user) {
        UsersTable usersTable = new UsersTable(user.getId().toString(), user.getName());
        usersTable.setId(user.getInternalId());
        return usersTable;
    }

    public FalcunUser convert() {
        FalcunUser falcunUser = new FalcunUser(UUID.fromString(uuid), name);
        falcunUser.setInternalId(id);
        return falcunUser;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsersTable that = (UsersTable) o;
        return id == that.id &&
               Objects.equal(uuid, that.uuid) &&
               Objects.equal(name, that.name) &&
               Objects.equal(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, uuid, name, timestamp);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("uuid", uuid)
                .add("name", name)
                .add("timestamp", timestamp)
                .toString();
    }
}
