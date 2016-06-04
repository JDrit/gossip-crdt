package net.batchik.crdt.gossip.datatypes;

import java.nio.ByteBuffer;

/**
 * A state-based counter that supports both incrementing and decrementing it
 */
public class PNCounter extends Type<Integer, PNCounter> {

    private GCounter P;
    private GCounter N;
    private int id;

    public PNCounter(int size, int id) {
        super(id);
        this.id = id;
        P = new GCounter(size, id);
        N = new GCounter(size, id);
    }

    private PNCounter(int id, GCounter P, GCounter N) {
        super(id);
    }

    public void increment(int amount) {
        P.increment(amount);
    }

    public void decrement(int amount) {
        N.increment(amount);
    }

    @Override
    public Integer value() {
       return P.value() - N.value();
    }

    @Override
    public ByteBuffer serialize() {
        ByteBuffer b1 = P.serialize();
        ByteBuffer b2 = N.serialize();
        ByteBuffer finalBuffer = ByteBuffer.allocate(4 + 4 + b1.position() + b2.position());
        finalBuffer.putInt(1);
        finalBuffer.putInt(id);
        finalBuffer.put(b1);
        finalBuffer.put(b2);
        return finalBuffer;
    }

    public static PNCounter deserialze(ByteBuffer buffer) {
        int id = buffer.getInt();
        GCounter newP = GCounter.deserialze(buffer);
        GCounter newN = GCounter.deserialze(buffer);
        return new PNCounter(id, newP, newN);
    }

    public void merge(PNCounter counter) {
        P.merge(counter.P);
        N.merge(counter.N);
    }
}
