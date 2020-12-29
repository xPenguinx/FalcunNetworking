package me.minidigger.falcunnetworking.common.network.protocol;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import me.minidigger.falcunnetworking.common.network.protocol.client.*;
import me.minidigger.falcunnetworking.common.network.protocol.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FalcunPacketRegistry {

    private static final Logger log = LoggerFactory.getLogger(FalcunPacketRegistry.class);

    private final BiMap<Integer, Class<? extends FalcunPacket>> serverRegistry = HashBiMap.create();
    private final BiMap<Integer, Class<? extends FalcunPacket>> clientRegistry = HashBiMap.create();

    public void init() {
        //
        // TO_SERVER
        //

        register(PacketDirection.TO_SERVER, 0, ClientLoginStartPacket.class);
        register(PacketDirection.TO_SERVER, 1, ClientEncryptionResponsePacket.class);
        register(PacketDirection.TO_SERVER, 2, ClientPingPacket.class);
        register(PacketDirection.TO_SERVER, 3, ClientChatPacket.class);
        register(PacketDirection.TO_SERVER, 4, ClientListRequestPacket.class);
        register(PacketDirection.TO_SERVER, 5, ClientActionPacket.class);
        register(PacketDirection.TO_SERVER, 6, ClientEmoteBroadcastPacket.class);
        register(PacketDirection.TO_SERVER, 7, ClientEmoteStopPacket.class);

        //
        // TO_CLIENT
        //
        register(PacketDirection.TO_CLIENT, 0, ServerEncryptionRequestPacket.class);
        register(PacketDirection.TO_CLIENT, 1, ServerLoginSuccessPacket.class);
        register(PacketDirection.TO_CLIENT, 3, ServerDisconnectPacket.class);
        register(PacketDirection.TO_CLIENT, 4, ServerPongPacket.class);
        register(PacketDirection.TO_CLIENT, 5, ServerResponsePacket.class);
        register(PacketDirection.TO_CLIENT, 6, ServerChatPacket.class);
        register(PacketDirection.TO_CLIENT, 7, ServerListResponsePacket.class);
        register(PacketDirection.TO_CLIENT, 8, ServerInviteStatusPacket.class);
        register(PacketDirection.TO_CLIENT, 9, ServerEmoteBroadcastPacket.class);
        register(PacketDirection.TO_CLIENT, 10, ServerEmoteStopPacket.class);

    }

    public void register(PacketDirection direction, int packetId, Class<? extends FalcunPacket> packetClass) {
        if (direction == PacketDirection.TO_SERVER) {
            serverRegistry.put(packetId, packetClass);
        } else {
            clientRegistry.put(packetId, packetClass);
        }
    }

    public Class<? extends FalcunPacket> getPacket(PacketDirection direction, int packetId) {
        if (direction == PacketDirection.TO_SERVER) {
            return serverRegistry.get(packetId);
        } else {
            return clientRegistry.get(packetId);
        }
    }

    public void fillInfo(FalcunPacket packet) {
        Class<? extends FalcunPacket> clazz = packet.getClass();
        Integer id = serverRegistry.inverse().get(clazz);
        if (id == null) {
            id = clientRegistry.inverse().get(clazz);
        }

        if (id == null) {
            log.warn("Could not fill info for packet {}", packet);
        } else {
            packet.setId(id);
        }
    }
}
