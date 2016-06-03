package net.batchik.crdt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.batchik.crdt.datatypes.Type;

public class IndividualState {
    private HashMap<String, Tuple<Type, Long>> state;
    private long maxVersion = 0;

    public IndividualState() {
        state = new HashMap<>();
    }

    public long getMaxVersion() {
        return maxVersion;
    }

    private class Tuple<T, S> {
        T fst;
        S snd;

        public Tuple(T fst, S snd) {
            this.fst = fst;
            this.snd = snd;
        }
    }

    /**
     * Generates the delta for this particular individual state. This is used in sending
     * updates to other nodes in the system
     * @param qMaxVersion the max version number that p knows that q has received
     * @param selfId the ID of r
     * @return the list of digests for this individual state
     */
    public synchronized List<Digest> getDeltaScuttle(long qMaxVersion, int selfId) {
        List<Digest> digests = new ArrayList<>();
        for (Map.Entry<String, Tuple<Type, Long>> entry : state.entrySet()) {
            String key = entry.getKey();
            Type value = entry.getValue().fst;
            long version = entry.getValue().snd;
            if (version > qMaxVersion) {
                digests.add(new Digest(selfId, key, value.serialize(), version));
            }
        }
        return digests;
    }
}
