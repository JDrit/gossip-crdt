package net.batchik.crdt;

import java.util.HashMap;
import net.batchik.crdt.datatypes.Type;

public class IndividualState {
    private HashMap<String, Tuple<Type, Long>> state;

    public IndividualState() {
        state = new HashMap<>();
    }

    private class Tuple<T, S> {
        T fst;
        S snd;

        public Tuple(T fst, S snd) {
            this.fst = fst;
            this.snd = snd;
        }
    }
}
