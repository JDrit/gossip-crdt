package net.batchik.crdt.datatypes;

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
}
