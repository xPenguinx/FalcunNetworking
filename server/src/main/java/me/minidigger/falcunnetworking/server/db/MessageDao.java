package me.minidigger.falcunnetworking.server.db;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Timestamped;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

import me.minidigger.falcunnetworking.server.db.tables.MessageTable;

@RegisterBeanMapper(MessageTable.class)
public interface MessageDao {

    @SqlUpdate("create table if not exists falcun_message (" +
               "id bigint primary key auto_increment," +
               "user bigint not null," +
               "groupId bigint," +
               "message varchar(255) not null, " +
               "timestamp timestamp not null" +
               ")")
    boolean createTable();

    @RegisterBeanMapper(MessageTable.class)
    @SqlQuery("select * from falcun_message where groupId = :groupId order by timestamp limit :count")
    List<MessageTable> retrieveMessages(long groupId, long count);

    @Timestamped
    @SqlUpdate("insert into falcun_message (user, groupId, message, timestamp) values (:message.user, :message.groupId, :message.message, :now)")
    void saveMessage(@BindBean("message") MessageTable message);
}
