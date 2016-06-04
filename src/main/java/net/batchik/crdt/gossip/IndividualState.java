package net.batchik.crdt.gossip;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.batchik.crdt.gossip.datatypes.GCounter;
import net.batchik.crdt.gossip.datatypes.PNCounter;
import net.batchik.crdt.gossip.datatypes.Type;
import org.apache.log4j.Logger;


public class IndividualState {
    static Logger log = Logger.getLogger(IndividualState.class.getName());
    private HashMap<String, Tuple<Type, Long>> state;
    private long maxVersion;

    public IndividualState() {
        state = new HashMap<>();
        maxVersion = 0L;
    }

    public synchronized long getMaxVersion() {
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
            state.put(key, new Tuple<>(new GCounter(2, id), 1L));
            if (maxVersion == 0) {
                maxVersion = 1;
            }
        } else if (tuple.fst instanceof PNCounter) {
            PNCounter counter = (PNCounter) tuple.fst;
            counter.increment(1);
            tuple.snd++;
            if (tuple.snd > maxVersion) {
                maxVersion = tuple.snd;
            }
        }
    }

    public synchronized void merge(int id, String key, Type value, long version) {
        Tuple<Type, Long> tuple = state.get(key);
        if (tuple == null) {
            state.put(key, new Tuple<>(value, version));
            log.debug("put new tuple into peer: " + id + ", key: " + key);
        } else if (tuple.snd < version) {
            log.debug("mering key: " + key);
            tuple.fst.merge(value);
        }
        if (version > maxVersion) {
            maxVersion = version;
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
