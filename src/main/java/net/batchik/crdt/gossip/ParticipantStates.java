package net.batchik.crdt.gossip;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParticipantStates {
    private long maxVersion = 0;
    private Map<Integer, Peer> peerMap;
    private Peer self;

    public ParticipantStates(Peer self, List<Peer> peers) {
        this.self = self;
        peerMap = new HashMap<>(peers.size());
        for (Peer peer : peers) {
            peerMap.put(peer.getId(), peer);
        }
    }

    public synchronized long getMaxVersion() {
        return maxVersion;
    }

    public Peer getSelf() { return self; }

    public synchronized Map<Integer, Peer> getPeers() { return peerMap; }

    public synchronized int getClusterSize() {
        return peerMap.size();
    }

    /**
     * Generates the initial digest of the states that this node knows about
     * @return HashMap representing the digest
     */
    public synchronized HashMap<Integer, Long> getInitialDigest() {
        HashMap<Integer, Long> digest = new HashMap<>(peerMap.size());
        for (Map.Entry<Integer, Peer> entry : peerMap.entrySet()) {
            digest.put(entry.getKey(), entry.getValue().getState().getMaxVersion());
        }
        return digest;
    }

    /**
     * Generates the list of deltas that should be send to the node with an ID of q.
     * @return the delta of changes
     */
    public synchronized List<Digest> getDeltaScuttle(Map<Integer, Long> initial) {
        List<Digest> digests = new ArrayList<>();
        // the max version that p knows about q

        for (Map.Entry<Integer, Peer> entry : peerMap.entrySet()) {
            long qMaxVersion = initial.get(entry.getKey());
            digests.addAll(entry.getValue().getState().getDeltaScuttle(qMaxVersion, entry.getKey()));
        }
        return digests;
    }
}
