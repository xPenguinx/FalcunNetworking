package me.minidigger.falcunnetworking.common.network.protocol;


import io.netty.buffer.ByteBuf;

public abstract class FalcunPacket {

    private PacketDirection direction;
    private int id;

    public FalcunPacket() {
    }

    public PacketDirection getDirection() {
        return direction;
    }

    public int getId() {
        return id;
    }

    public void setDirection(PacketDirection direction) {
        this.direction = direction;
    }

    public void setId(int id) {
        this.id = id;
    }

    public abstract void toWire(ByteBuf buf);

    public abstract void fromWire(ByteBuf buf);

    public abstract String toString();
}
