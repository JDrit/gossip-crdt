package net.batchik.crdt.gossip.datatypes;

import java.nio.ByteBuffer;

public abstract class Type<S, T> implements Mergeable<T> {
    private int id;

    public Type(int id) {
        this.id = id;
    }

    protected int getID() {
        return id;
    }

    public abstract S value();

    public abstract ByteBuffer serialize();

    public static Type deser(ByteBuffer buffer) {
        switch (buffer.getInt()) {
            case 0:
                return GCounter.deserialze(buffer);
            case 1:
                return PNCounter.deserialze(buffer);
            default:
                return null;
        }
    }

}
