package me.minidigger.falcunnetworking.common.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class Invite {

    private FalcunUser user;
    private FalcunGroup group;
    private FalcunUser inviter;
    private InviteType type;
    private long internalId;

    public Invite(FalcunUser user, FalcunUser inviter) {
        this.user = user;
        this.inviter = inviter;
        this.type = InviteType.FRIEND;
    }

    public Invite(FalcunUser user, FalcunGroup group, FalcunUser inviter) {
        this.user = user;
        this.group = group;
        this.inviter = inviter;
        this.type = InviteType.GROUP;
    }

    public void setInternalId(long internalId) {
        this.internalId = internalId;
    }

    public long getInternalId() {
        return internalId;
    }

    public FalcunUser getUser() {
        return user;
    }

    public void setUser(FalcunUser user) {
        this.user = user;
    }

    public FalcunGroup getGroup() {
        return group;
    }

    public void setGroup(FalcunGroup group) {
        this.group = group;
    }

    public FalcunUser getInviter() {
        return inviter;
    }

    public void setInviter(FalcunUser inviter) {
        this.inviter = inviter;
    }

    public InviteType getType() {
        return type;
    }

    public void setType(InviteType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Invite invite = (Invite) o;
        return internalId == invite.internalId &&
               Objects.equal(user, invite.user) &&
               Objects.equal(group, invite.group) &&
               Objects.equal(inviter, invite.inviter) &&
               type == invite.type;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(user, group, inviter, type, internalId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("user", user)
                .add("group", group)
                .add("inviter", inviter)
                .add("type", type)
                .add("internalId", internalId)
                .toString();
    }
}
