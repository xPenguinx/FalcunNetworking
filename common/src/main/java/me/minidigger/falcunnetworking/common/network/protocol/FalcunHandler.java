package me.minidigger.falcunnetworking.common.network.protocol;

import me.minidigger.falcunnetworking.common.network.FalcunConnection;

public abstract class FalcunHandler {

    public abstract void join(FalcunConnection connection);

    public abstract void leave(FalcunConnection connection);

    public abstract void handle(FalcunConnection connection, FalcunPacket msg);
}
