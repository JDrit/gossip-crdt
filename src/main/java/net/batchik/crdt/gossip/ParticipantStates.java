package net.batchik.crdt.gossip;

import net.batchik.crdt.Main;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParticipantStates {
    static Logger log = Logger.getLogger(ParticipantStates.class.getName());

    private Map<String, Peer> peerMap;
    private Peer self;

    public ParticipantStates(Peer self, List<Peer> peers) {
        this.self = self;
        peerMap = new HashMap<>(peers.size());
        peerMap.put(self.getId(), self);
        for (Peer peer : peers) {
            log.debug("add peer: " + peer);
            peerMap.put(peer.getId(), peer);
        }
    }

    public Peer getSelf() { return self; }

    public synchronized Map<String, Peer> getPeers() { return peerMap; }

    public synchronized Peer getPeer(String id) {
        Peer peer = peerMap.get(id);
        if (peer == null) {
            log.info("id: " + id);
            InetSocketAddress address = Main.convertAddress(id);
            peer = new Peer(address);
            peerMap.put(id, peer);
            log.info("creating new peer: " + peer);
        }
        return peer;
    }

    public synchronized int getClusterSize() {
        return peerMap.size();
    }

    /**
     * Generates the initial digest of the states that this node knows about
     * @return HashMap representing the digest
     */
    public synchronized Map<String, Long> getInitialDigest() {
        HashMap<String, Long> digest = new HashMap<>(peerMap.size());
        for (Map.Entry<String, Peer> entry : peerMap.entrySet()) {
            digest.put(entry.getKey(), entry.getValue().getState().getMaxVersion());
        }
        log.debug("getInitialDigest: " + digest);
        return digest;
    }

    /**
     * Generates the list of deltas that should be send to the node with an ID of q.
     * @return the delta of changes
     */
    public synchronized List<Digest> getDeltaScuttle(Map<String, Long> initial) {
        List<Digest> digests = new ArrayList<>();
        log.info("initial: " + initial);

        // the max version that p knows about q
        for (Map.Entry<String, Peer> entry : peerMap.entrySet()) {
            String address = entry.getKey();
            Peer peer = entry.getValue();
            Long otherMaxVersion = initial.get(address);

            if (otherMaxVersion == null) {
                log.debug("other node does not know about: " + address);
                otherMaxVersion = -1L;
            }

            digests.addAll(peer.getState().getDeltaScuttle(otherMaxVersion, address));
        }
        log.debug("getDeltaScuttle: " + digests);
        return digests;
    }
}
