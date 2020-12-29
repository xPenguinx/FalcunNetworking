package me.minidigger.falcunnetworking.server.db;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Timestamped;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

import me.minidigger.falcunnetworking.server.db.tables.FriendTable;
import me.minidigger.falcunnetworking.server.db.tables.UsersTable;

@RegisterBeanMapper(FriendTable.class)
public interface FriendDao {

    @SqlUpdate("create table if not exists falcun_friend(" +
               "user1 bigint not null," +
               "user2 bigint not null," +
               "timestamp timestamp not null," +
               "primary key (user1, user2)" +
               ")")
    boolean createTable();

    @SqlQuery("select * from falcun_user " +
              "where id in (select user2 from falcun_friend where user1 = :id) " +
              "or id in (select user1 from falcun_friend where user2 = :id)")
    @RegisterBeanMapper(UsersTable.class)
    List<UsersTable> findFriends(@BindBean UsersTable user);

    @SqlUpdate("insert into falcun_friend (user1, user2, timestamp) values (:user1.id, :user2.id, :now)")
    @Timestamped
    void addFriend(@BindBean("user1") UsersTable user1, @BindBean("user2") UsersTable user2);

    @SqlUpdate("delete from falcun_friend " +
               "where (user1 = :user1.id and user2 = :user2.id) " +
               "or (user1 = :user2.id and user2 = :user1.id)")
    int removeFriend(@BindBean("user1") UsersTable user1, @BindBean("user2") UsersTable user2);
}
