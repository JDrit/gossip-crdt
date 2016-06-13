package net.batchik.crdt.gossip;

import java.text.NumberFormat;
import java.util.*;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.concurrent.ReentrantLock;
import net.batchik.crdt.gossip.datatypes.GCounterUtil;
import net.batchik.crdt.gossip.datatypes.PNCounterUtil;
import org.apache.log4j.Logger;


public class IndividualState {
    private static Logger log = Logger.getLogger(IndividualState.class.getName());
    private NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
    private HashMap<String, Tuple<Object, Long>> state;
    private String id;
    private long maxVersion;
    private ReentrantLock lock;

    public IndividualState(String id) {
        this.id = id;
        state = new HashMap<>();
        maxVersion = 0L;
        lock = new ReentrantLock();
    }

    public long getMaxVersion() {
        long m;
        lock.lock();
        m = maxVersion;
        lock.unlock();
        return m;
    }

    private class Tuple<T, S> {
        T fst;
        S snd;

        public Tuple(T fst, S snd) {
            this.fst = fst;
            this.snd = snd;
        }
    }

    public void incrementCounter(String key) throws SuspendExecution {
        lock.lock();
        Tuple<Object, Long> tuple = state.get(key);
        if (tuple == null) {
            state.put(key, new Tuple<>(GCounterUtil.newGCounter(id), ++maxVersion));
        } else if (tuple.fst instanceof GCounter) {
            GCounter counter = (GCounter) tuple.fst;
            GCounterUtil.increment(counter, id);
            tuple.snd = ++maxVersion;
        }
        lock.unlock();
    }

    void merge(String key, Object value, long version) {
        lock.lock();
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
        lock.unlock();
    }

    /**
     * Generates the delta for this particular individual state. This is used in sending
     * updates to other nodes in the system
     * @param qMaxVersion the max version number that p knows that q has received
     * @param selfAddress the ID of r
     * @return the list of digests for this individual state
     */
    List<Digest> getDeltaScuttle(long qMaxVersion, String selfAddress) {
        lock.lock();
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
        lock.unlock();
        return digests;
    }

    /**
     * Gets the string message representing the local peers known state
     * @return string message
     */
    public String getAllResponse() {
        lock.lock();
        StringBuilder builder = new StringBuilder();
        builder.append("response: (").append(state.size()).append(" elements)\n");
        Set<Map.Entry<String, Tuple<Object, Long>>>  entries = state.entrySet();

        Map.Entry<String, Tuple<Object, Long>>[] arr = entries.toArray(new Map.Entry[entries.size()]);
        Arrays.sort(arr, new Comparator<Map.Entry<String, Tuple<Object, Long>>>() {
            @Override
            public int compare(Map.Entry<String, Tuple<Object, Long>> e0, Map.Entry<String, Tuple<Object, Long>> e1) {
                return e0.getKey().compareTo(e1.getKey());
            }
        });

        for (Map.Entry<String, Tuple<Object, Long>> entry : arr) {
            builder.append(entry.getKey())
                    .append(" : ")
                    .append(format.format(GCounterUtil.value((GCounter) entry.getValue().fst)))
                    .append("\n");
        }
        lock.unlock();
        return builder.toString();
    }
}
