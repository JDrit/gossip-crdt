package net.batchik.crdt.gossip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.batchik.crdt.gossip.datatypes.PNCounter;
import net.batchik.crdt.gossip.datatypes.Type;


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

    public synchronized void incrementCounter(String key, int id) {
        Tuple<Type, Long> tuple = state.get(key);
        if (tuple == null) {
            state.put(key, new Tuple<>(new PNCounter(2, id), 0L));
        } else {
            if (tuple.fst instanceof PNCounter) {
                PNCounter counter = (PNCounter) tuple.fst;
                counter.increment(1);
                tuple.snd++;
            }
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

    public synchronized String getAllResponse() {
        StringBuilder builder = new StringBuilder();
        builder.append("response: (").append(state.size()).append(" elements)\n");
        for (Map.Entry<String, Tuple<Type, Long>> entry : state.entrySet()) {
            builder.append(entry.getKey())
                    .append(" : ")
                    .append(entry.getValue().fst.value())
                    .append("\n");
        }
        return builder.toString();
    }
}
