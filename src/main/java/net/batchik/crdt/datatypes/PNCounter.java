package net.batchik.crdt.datatypes;

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

    public void merge(PNCounter counter) {
        for (int i = 0 ; i < size ; i++) {
            P[i] = (this.P[i] > counter.P[i]) ? this.P[i] : counter.P[i];
            N[i] = (this.N[i] > counter.N[i]) ? this.N[i] : counter.N[i];
        }
    }
}
