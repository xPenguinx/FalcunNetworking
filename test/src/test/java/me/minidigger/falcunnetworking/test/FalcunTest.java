package me.minidigger.falcunnetworking.test;

import org.apache.logging.log4j.Level;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Optional;
import java.util.UUID;

import me.minidigger.falcunnetworking.client.FalcunClient;
import me.minidigger.falcunnetworking.common.Constants;
import me.minidigger.falcunnetworking.server.FalcunServer;

import static org.junit.Assert.fail;

@Ignore // only manually run, needs db
public class FalcunTest {

    private static Thread serverThread;
    private static FalcunServer server;

    @BeforeClass
    public static void setupServer() {
        // setup constant
        Constants.TEST_MODE = true;

        serverThread = new Thread(() -> {
            server = new FalcunServer(1337);
            server.start();
        });
        serverThread.start();

        wait(1000);
    }

    @Test
    public void simpleConnectTest() {
        FalcunClient client = new FalcunClient();
        client.init();
        client.setAuth("User1", UUID.nameUUIDFromBytes("User1".getBytes()).toString(), "offline");

        new Thread(() -> client.connect("localhost", 1337)).start();

        wait(1000);

        server.getHandler().getConnections().forEach(c -> c.close("Bye"));

        wait(500);

        assertNoError();
        assertLogged("PONG! This is a test");
    }

    @Test
    public void friendTest() {
        server.getDbHandler().jdbi().withHandle(handle -> {
            Optional<Long> user1Id = handle.createQuery("select id from falcun_user where name LIKE :name").bind("name", "User1").mapTo(Long.class).findFirst();
            Optional<Long> user2Id = handle.createQuery("select id from falcun_user where name LIKE :name").bind("name", "User2").mapTo(Long.class).findFirst();
            if (!user1Id.isPresent() || !user2Id.isPresent()) {
                return null;
            }
            handle.createUpdate("delete from falcun_friend where user1 = :user or user2 = :user").bind("user", user1Id.get()).execute();
            handle.createUpdate("delete from falcun_friend where user1 = :user or user2 = :user").bind("user", user2Id.get()).execute();
            return null;
        });

        FalcunClient client1 = new FalcunClient();
        client1.init();
        client1.setAuth("User1", UUID.nameUUIDFromBytes("User1".getBytes()).toString(), "offline");

        FalcunClient client2 = new FalcunClient();
        client2.init();
        client2.setAuth("User2", UUID.nameUUIDFromBytes("User2".getBytes()).toString(), "offline");

        new Thread(() -> client1.connect("localhost", 1337)).start();
        new Thread(() -> client2.connect("localhost", 1337)).start();

        wait(1000);
        client1.getConsole().runCommand("action SEND_FRIEND_INVITE User2");
        wait(100);
        client2.getConsole().runCommand("action ACCEPT_FRIEND_INVITE User1");
        wait(100);
        client1.getConsole().runCommand("chat User2 Test message 1");
        wait(100);
        client2.getConsole().runCommand("chat User1 Test message 2");
        wait(100);
        client2.getConsole().runCommand("action REMOVE_FRIEND User1");
        wait(100);
        client2.getConsole().runCommand("action SEND_FRIEND_INVITE User1");
        wait(100);
        client2.getConsole().runCommand("action REVOKE_FRIEND_INVITE User1");
        wait(100);

        server.getHandler().getConnections().forEach(c -> c.close("Bye"));

        wait(100);

        assertNoError();
        assertLogged("PONG! This is a test");
        assertLogged("status=ACCEPTED"); // invite was accepted
        assertLogged("Incoming chat:  Test message 1");
        assertLogged("Incoming chat:  Test message 2");
        assertLogged("status=REVOKED"); // invite was accepted
    }

    @Test
    public void groupTest() {
        server.getDbHandler().jdbi().withHandle(handle -> {
            Optional<Long> groupId = handle.createQuery("select id from falcun_group where name LIKE :name").bind("name", "TestGroup").mapTo(Long.class).findFirst();
            if (!groupId.isPresent()) {
                return null;
            }
            handle.createUpdate("delete from falcun_group where id = :groupId").bind("groupId", groupId.get()).execute();
            handle.createUpdate("delete from falcun_group_members where groupId = :groupId").bind("groupId", groupId.get()).execute();
            handle.createUpdate("delete from falcun_message where groupId = :groupId").bind("groupId", groupId.get()).execute();
            return null;
        });

        FalcunClient client1 = new FalcunClient();
        client1.init();
        client1.setAuth("User1", UUID.nameUUIDFromBytes("User1".getBytes()).toString(), "offline");

        FalcunClient client2 = new FalcunClient();
        client2.init();
        client2.setAuth("User2", UUID.nameUUIDFromBytes("User2".getBytes()).toString(), "offline");

        new Thread(() -> client1.connect("localhost", 1337)).start();
        new Thread(() -> client2.connect("localhost", 1337)).start();

        wait(1000);
        client1.getConsole().runCommand("action CREATE_GROUP TestGroup");
        wait(100);
        client1.getConsole().runCommand("action SEND_GROUP_INVITE TestGroup User2");
        wait(100);
        client2.getConsole().runCommand("action ACCEPT_GROUP_INVITE TestGroup");
        wait(100);
        client2.getConsole().runCommand("groupChat TestGroup This is a test message");
        wait(100);
        client2.getConsole().runCommand("action KICK_USER TestGroup User1");
        wait(100);
        client1.getConsole().runCommand("action KICK_USER TestGroup User2");
        wait(100);
        client2.getConsole().runCommand("groupChat TestGroup I can't send this messsage");
        wait(100);
        client1.getConsole().runCommand("list MESSAGES TestGroup");
        wait(100);
        client1.getConsole().runCommand("action DELETE_GROUP TestGroup");
        wait(100);

        server.getHandler().getConnections().forEach(c -> c.close("Bye"));

        wait(100);

        assertNoError();
        assertLogged("PONG! This is a test");
        assertLogged("status=NEW"); // new invite
        assertLogged("status=ACCEPTED"); // invite accepted
        assertLogged("Incoming chat:  This is a test message");
        assertLogged("ERROR Only owner can kick users!");
        assertLogged("WARNING You are not in a group named like that!");
    }

    private void assertNoError() {
        for (String msg : TestAppender.getMessages()) {
            if (msg.contains("[" + Level.ERROR.name() + "]:")) {
                fail("Encountered error: " + msg);
            }
        }
    }

    private void assertLogged(String message) {
        for (String msg : TestAppender.getMessages()) {
            if (msg.contains(message)) {
                return;
            }
        }

        fail("Didn't encounter log message: " + message);
    }

    private static void wait(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
