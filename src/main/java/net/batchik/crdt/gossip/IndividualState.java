package net.batchik.crdt.gossip;

import java.net.InetSocketAddress;
import java.text.NumberFormat;
import java.util.*;

import net.batchik.crdt.gossip.datatypes.GCounterUtil;
import net.batchik.crdt.gossip.datatypes.PNCounterUtil;
import org.apache.log4j.Logger;


public class IndividualState {
    private static Logger log = Logger.getLogger(IndividualState.class.getName());
    private NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
    private HashMap<String, Tuple<Object, Long>> state;
    private String id;
    private long maxVersion;

    public IndividualState(String id) {
        this.id = id;
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

    public synchronized void incrementCounter(String key) {
        Tuple<Object, Long> tuple = state.get(key);
        if (tuple == null) {
            state.put(key, new Tuple<Object, Long>(GCounterUtil.newGCounter(id), ++maxVersion));
        } else if (tuple.fst instanceof GCounter) {
            GCounter counter = (GCounter) tuple.fst;
            GCounterUtil.increment(counter, id);
            tuple.snd = ++maxVersion;
        }
    }

    public synchronized void merge(String key, Object value, long version) {
        Tuple<Object, Long> tuple = state.get(key);

        if (tuple == null) {
            if (version > maxVersion) {
                maxVersion = version;
                state.put(key, new Tuple<>(value, maxVersion));
            } else {
                state.put(key, new Tuple<>(value, ++maxVersion));
            }
        } else {
            long newVersion = Math.max(tuple.snd, version);
            if (tuple.fst instanceof GCounter) {
                tuple.fst = GCounterUtil.merge((GCounter) tuple.fst, (GCounter) value);
            } else if (tuple.fst instanceof PNCounter) {
                tuple.fst = PNCounterUtil.merge((PNCounter) tuple.fst, (PNCounter) value);
            }
            tuple.snd = newVersion;
            if (tuple.snd > maxVersion) {
                maxVersion = tuple.snd;
            }
        }
    }

    /**
     * Generates the delta for this particular individual state. This is used in sending
     * updates to other nodes in the system
     * @param qMaxVersion the max version number that p knows that q has received
     * @param selfAddress the ID of r
     * @return the list of digests for this individual state
     */
    public synchronized List<Digest> getDeltaScuttle(long qMaxVersion, String selfAddress) {
        List<Digest> digests = new ArrayList<>();

        for (Map.Entry<String, Tuple<Object, Long>> entry : state.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue().fst;
            long version = entry.getValue().snd;
            if (version > qMaxVersion) {
                Digest digest = new Digest()
                        .setR(selfAddress)
                        .setK(key)
                        .setN(version);
                if (value instanceof GCounter) {
                    digest.setGCounter((GCounter) value);
                    digest.setType(Type.GCCOUNTER);
                } else if (value instanceof PNCounter) {
                    digest.setPNCounter((PNCounter) value);
                    digest.setType(Type.PNCOUNTER);
                }
                digests.add(digest);
            }
        }
        return digests;
    }

    public synchronized String getAllResponse() {

        StringBuilder builder = new StringBuilder();
        builder.append("response: (").append(state.size()).append(" elements)\n");
        for (Map.Entry<String, Tuple<Object, Long>> entry : state.entrySet()) {
            builder.append(entry.getKey())
                    .append(" : ")
                    .append(format.format(GCounterUtil.value((GCounter) entry.getValue().fst)))
                    .append("\n");
        }
        return builder.toString();
    }
}
