package me.minidigger.falcunnetworking.server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.minidigger.falcunnetworking.server.FalcunServer;
import me.minidigger.falcunnetworking.server.db.tables.FriendTable;


public class DBHandler {

    private static final Logger log = LoggerFactory.getLogger(DBHandler.class);
    private static final Logger sqlLogger = LoggerFactory.getLogger(log.getName() + ".sql");

    private Jdbi jdbi;

    public void setup() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + FalcunServer.DB_HOST.get() + ":" + FalcunServer.DB_PORT.get() + "/" + FalcunServer.DB_NAME.get());
        config.setUsername(FalcunServer.DB_USER.get());
        config.setPassword(FalcunServer.DB_PASS.get());
        config.addDataSourceProperty("dataSourceClassName", "com.mysql.cj.jdbc.MysqlDataSource");
        config.addDataSourceProperty("autoCommit", "false");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("cachePrepStmts", "true");

        HikariDataSource ds = new HikariDataSource(config);
        jdbi = Jdbi.create(ds);
        jdbi.installPlugin(new SqlObjectPlugin());

        if (sqlLogger.isDebugEnabled()) {
            SqlLogger debugLogger = new SqlLogger() {
                @Override
                public void logAfterExecution(StatementContext context) {
                    sqlLogger.debug("sql: {}", context.getRenderedSql());
                }
            };
            jdbi.setSqlLogger(debugLogger);
        }
    }

    public void createTables() {
        jdbi.withExtension(UserDao.class, userDao -> {
            userDao.createTable();
            return null;
        });
        jdbi.withExtension(FriendDao.class, friendDao -> {
            friendDao.createTable();
            return null;
        });
        jdbi.withExtension(InviteDao.class, inviteDao -> {
            inviteDao.createTable();
            return null;
        });
        jdbi.withExtension(GroupDao.class, groupDao -> {
            groupDao.createTable();
            groupDao.createMembershipTable();
            return null;
        });
        jdbi.withExtension(MessageDao.class, messageDao -> {
            messageDao.createTable();
            return null;
        });
    }

    public Jdbi jdbi() {
        return jdbi;
    }
}
