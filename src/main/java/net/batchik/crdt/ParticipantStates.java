package net.batchik.crdt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParticipantStates {
    private long maxVersion = 0;
    private Map<Integer, Peer> peers;

    public ParticipantStates(Map<Integer, Peer> peers) {
        this.peers = peers;
    }

    public synchronized long getMaxVersion() {
        return maxVersion;
    }

    public synchronized Map<Integer, Peer> getPeers() { return peers; }

    public synchronized int getClusterSize() {
        return peers.size();
    }

    /**
     * Generates the initial digest of the states that this node knows about
     * @return HashMap representing the digest
     */
    public synchronized HashMap<Integer, Long> getInitialDigest() {
        HashMap<Integer, Long> digest = new HashMap<>(peers.size());
        for (Map.Entry<Integer, Peer> entry : peers.entrySet()) {
            digest.put(entry.getKey(), entry.getValue().getState().getMaxVersion());
        }
        return digest;
    }

    /**
     * Generates the list of deltas that should be send to the node with an ID of q.
     * @param qId the ID of the node being sent to
     * @return the delta of changes!@
     */
    public synchronized List<Digest> getDeltaScuttle(long qId) {
        List<Digest> digests = new ArrayList<>();
        // the max version that p knows about q
        long qMaxVersion = peers.get(qId).getState().getMaxVersion();

        for (Map.Entry<Integer, Peer> entry : peers.entrySet()) {
            digests.addAll(entry.getValue().getState().getDeltaScuttle(qMaxVersion, entry.getKey()));
        }
        return digests;
    }
}
