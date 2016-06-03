package net.batchik.crdt.datatypes;

import java.nio.ByteBuffer;

/**
 * A state-based counter that supports both incrementing and decrementing it
 */
public class PNCounter extends Type<Integer, PNCounter> {

    public int[] P;
    public int[] N;
    private int size;

    public PNCounter(int size, int id) {
        super(id);
        P = new int[size];
        N = new int[size];
        this.size = size;
    }

    public void increment(int amount) {
        int id = getID();
        P[id] += amount;
    }

    public void decrement(int amount) {
        int id = getID();
        N[id] += amount;
    }

    @Override
    public Integer value() {
        int sum = 0;
        for (int i = 0 ; i < size ; i++) {
            sum += P[i];
            sum -= N[i];
        }
        return sum;
    }

    @Override
    public ByteBuffer serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 4 * (P.length + N.length));
        buffer.putInt(P.length);
        for (int i = 0 ; i < size ; i++) {
            buffer.putInt(P[i]);
            buffer.putInt(N[i]);
        }
        return buffer;
    }

    public void merge(PNCounter counter) {
        for (int i = 0 ; i < size ; i++) {
            P[i] = (this.P[i] > counter.P[i]) ? this.P[i] : counter.P[i];
            N[i] = (this.N[i] > counter.N[i]) ? this.N[i] : counter.N[i];
        }
    }
}
