package me.minidigger.falcunnetworking.server.db;

import org.jdbi.v3.core.enums.EnumByOrdinal;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Timestamped;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

import me.minidigger.falcunnetworking.common.api.FalcunUser;
import me.minidigger.falcunnetworking.common.api.InviteType;
import me.minidigger.falcunnetworking.server.db.tables.InviteTable;

@RegisterBeanMapper(InviteTable.class)
public interface InviteDao {

    @SqlUpdate("create table if not exists falcun_invite (" +
               "id bigint primary key auto_increment," +
               "user bigint not null," +
               "inviter bigint not null," +
               "groupId bigint," +
               "type int not null," +
               "timestamp timestamp not null," +
               "active bool not null default true" +
               ")")
    boolean createTable();

    @SqlUpdate("insert into falcun_invite (user, inviter, groupId, type, timestamp) values (:inv.user, :inv.inviter, :inv.groupId, :inv.type, :now)")
    @Timestamped
    @GetGeneratedKeys
    long createInvite(@BindBean("inv") InviteTable inv);

    @SqlUpdate("update falcun_invite set active = false where id = :id")
    void deactivateInviteById(long id);

    @SqlQuery("select * from falcun_invite where id = :id")
    InviteTable getInvite(long id);

    @SqlQuery("select * from falcun_invite where type = :type and user = :user.internalId and active = 1")
    List<InviteTable> getIncomingInvites(@BindBean("user") FalcunUser user, @EnumByOrdinal InviteType type);

    @SqlQuery("select * from falcun_invite where type = :type and inviter = :user.internalId and active = 1")
    List<InviteTable> getOutgoingInvites(@BindBean("user") FalcunUser user, @EnumByOrdinal InviteType type);

    @SqlQuery("select * from falcun_invite where type = :type and inviter = :user.internalId and active = 1 and groupId = :groupId")
    List<InviteTable> getOutgoingInvites(@BindBean("user") FalcunUser user, @EnumByOrdinal InviteType type, long groupId);
}
