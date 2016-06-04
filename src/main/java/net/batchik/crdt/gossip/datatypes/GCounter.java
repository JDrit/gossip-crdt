package net.batchik.crdt.gossip.datatypes;

import java.nio.ByteBuffer;

public class GCounter extends Type<Integer, GCounter> {
    private int size;
    private int id;
    private int[] P;

    public GCounter(int size, int id) {
        super(id);
        this.size = size;
        this.id = id;
        P = new int[size];
    }

    private GCounter(int id, int size, int[] P) {
        super(id);
        this.size = size;
        this.id = id;
        this.P = P;
    }

    public void increment(int amount) {
        P[id] += amount;
    }

    @Override
    public Integer value() {
        int sum = 0;
        for (int i = 0 ; i < size ; i++) {
            sum += P[i];
        }
        return sum;
    }

    @Override
    public ByteBuffer serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 4 + P.length * 4);
        buffer.putInt(0);
        buffer.putInt(id);
        buffer.putInt(P.length);
        for (int i = 0 ; i < size ; i++) {
            buffer.putInt(P[i]);
        }
        return buffer;
    }


    public static GCounter deserialze(ByteBuffer buffer) {
        int id = buffer.getInt();
        int size = buffer.getInt();
        int[] P = new int[size];

        for (int i = 0 ; i < size ; i++) {
            P[i] = buffer.getInt();
        }
        return new GCounter(id, size, P);
    }

    @Override
    public void merge(GCounter other) {
        for (int i = 0 ; i < size ; i++) {
            P[i] = (this.P[i] > other.P[i]) ? this.P[i] : other.P[i];
        }
    }
}
