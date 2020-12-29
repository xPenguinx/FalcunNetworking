package me.minidigger.falcunnetworking.server.api;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import me.minidigger.falcunnetworking.common.api.FalcunUser;
import me.minidigger.falcunnetworking.server.FalcunServer;
import me.minidigger.falcunnetworking.server.db.UserDao;
import me.minidigger.falcunnetworking.server.db.tables.UsersTable;

public class UsersHandler {

    private final FalcunServer server;

    private final LoadingCache<Long, FalcunUser> cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<Long, FalcunUser>() {
                @Override
                public FalcunUser load(Long id) {
                    UsersTable table = server.getDbHandler().jdbi().withExtension(UserDao.class, handle -> handle.getById(id));

                    if (table == null) {
                        return null;
                    }

                    return table.convert();
                }
            });

    public UsersHandler(FalcunServer server) {
        this.server = server;
    }

    public FalcunUser createOrLoadUser(UUID id, String name) {
        UsersTable table = server.getDbHandler().jdbi().withExtension(UserDao.class, handle -> {
            UsersTable usersTable = handle.getByUUID(id.toString());
            if (usersTable == null) {
                if (!handle.create(new UsersTable(id.toString(), name))){
                    return null;
                }
                usersTable = handle.getByUUID(id.toString());
            }

            return usersTable;
        });

        if (table == null) {
            return null;
        }

        return table.convert();
    }

    public FalcunUser getUser(String name) {
        UsersTable table = server.getDbHandler().jdbi().withExtension(UserDao.class, handle -> handle.getByName(name));

        if (table == null) {
            return null;
        }

        return table.convert();
    }

    public FalcunUser getUser(UUID id) {
        UsersTable table = server.getDbHandler().jdbi().withExtension(UserDao.class, handle -> handle.getByUUID(id.toString()));

        if (table == null) {
            return null;
        }

        return table.convert();
    }

    public FalcunUser getOrCacheUser(long id) {
        try {
            return cache.get(id);
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }
}
