package me.minidigger.falcunnetworking.server.db;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Timestamped;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

import me.minidigger.falcunnetworking.common.api.FalcunUser;
import me.minidigger.falcunnetworking.server.db.tables.GroupTable;
import me.minidigger.falcunnetworking.server.db.tables.UsersTable;

@RegisterBeanMapper(GroupTable.class)
public interface GroupDao {

    @SqlUpdate("create table if not exists falcun_group (" +
               "id bigint primary key auto_increment," +
               "name varchar(255) not null," +
               "owner bigint not null," +
               "timestamp timestamp not null" +
               ")")
    boolean createTable();

    @SqlUpdate("create table if not exists falcun_group_members (" +
               "userId bigint not null ," +
               "groupId varchar(255) not null," +
               "timestamp timestamp not null" +
               ")")
    boolean createMembershipTable();

    @SqlQuery("select * from falcun_group where id = :id")
    GroupTable getById(long id);

    @SqlQuery("select * from falcun_group g where name = :name and :user.internalId in (select userId from falcun_group_members gm where g.id = gm.groupId)")
    GroupTable getGroup(String name, @BindBean("user") FalcunUser user);

    @Timestamped
    @GetGeneratedKeys
    @SqlUpdate("insert into falcun_group (name, owner, timestamp) values (:name, :owner.internalId, :now)")
    long createGroup(String name, @BindBean("owner") FalcunUser owner);

    @SqlUpdate("delete from falcun_group where id = :id")
    boolean deleteGroup(long id);

    @Timestamped
    @SqlUpdate("insert into falcun_group_members (userId, groupId, timestamp) values ( :user.internalId, :groupId, :now)")
    void addUser(long groupId, @BindBean("user") FalcunUser user);

    @SqlUpdate("delete from falcun_group_members where groupId = :groupId and userId = :user.internalId")
    void removeUser(long groupId, @BindBean("user") FalcunUser user);

    @RegisterBeanMapper(UsersTable.class)
    @SqlQuery("select * from falcun_user where id in (select userId from falcun_group_members where groupId = :groupId)")
    List<UsersTable> getGroupMembers(long groupId);

    @SqlQuery("select * from falcun_group where id in (select groupId from falcun_group_members where userId = :user.internalId)")
    List<GroupTable> getGroups(@BindBean("user") FalcunUser user);
}
