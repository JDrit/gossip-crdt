package net.batchik.crdt.gossip;

import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParticipantStates {
    static Logger log = Logger.getLogger(ParticipantStates.class.getName());

    private Map<Integer, Peer> peerMap;
    private Peer self;

    public ParticipantStates(Peer self, List<Peer> peers) {
        this.self = self;
        peerMap = new HashMap<>(peers.size());
        peerMap.put(self.getId(), self);
        for (Peer peer : peers) {
            log.debug("add peer: " + peer.getId() + ", " + peer);
            peerMap.put(peer.getId(), peer);
        }
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
        log.debug("getInitialDigest: " + digest);
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
        log.debug("getDeltaScuttle: " + digests);
        return digests;
    }
}
