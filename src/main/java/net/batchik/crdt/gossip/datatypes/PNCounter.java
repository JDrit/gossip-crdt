package net.batchik.crdt.gossip.datatypes;

import org.apache.http.impl.nio.reactor.IOReactorConfig;

import java.nio.ByteBuffer;

/**
 * A state-based counter that supports both incrementing and decrementing it
 */
public class PNCounter extends Type<Integer, PNCounter> {

    private GCounter P;
    private GCounter N;

    public PNCounter(int size, int id) {
        super(id);
        P = new GCounter(size, id);
        N = new GCounter(size, id);
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
        ByteBuffer finalBuffer = ByteBuffer.allocate(b1.position() + b2.position());
        finalBuffer.put(b1);
        finalBuffer.put(b2);
        return finalBuffer;
    }

    public void merge(PNCounter counter) {
        P.merge(counter.P);
        N.merge(counter.N);
    }
}
