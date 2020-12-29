package me.minidigger.falcunnetworking.server.db;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Timestamped;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import me.minidigger.falcunnetworking.server.db.tables.UsersTable;

@RegisterBeanMapper(UsersTable.class)
public interface UserDao {

    @SqlUpdate("create table if not exists falcun_user(" +
               "id bigint primary key not null auto_increment," +
               "uuid varchar(36) unique," +
               "name varchar(255) not null ," +
               "timestamp timestamp not null " +
               ")")
    boolean createTable();

    @SqlQuery("select * from falcun_user where id = :id")
    UsersTable getById(long id);

    @SqlQuery("select * from falcun_user where uuid = :uuid")
    UsersTable getByUUID(String uuid);

    @SqlQuery("select * from falcun_user where name = :name")
    UsersTable getByName(String name);

    @SqlUpdate("insert into falcun_user (uuid, name, timestamp) values (:uuid, :name, :now)")
    @Timestamped
    boolean create(@BindBean UsersTable user);
}
