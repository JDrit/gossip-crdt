package net.batchik.crdt.gossip;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.concurrent.ReentrantReadWriteLock;
import net.batchik.crdt.Main;
import net.batchik.crdt.zookeeper.ZKServiceListener;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * This is the internal mapping of the cluster, which is used to keep both the state for this node
 * and for all other nodes
 */
public class ParticipantStates implements ZKServiceListener {
    private final static Logger log = Logger.getLogger(ParticipantStates.class.getName());
    private final Map<String, Peer> peerMap;
    private final ReentrantReadWriteLock lock;
    private final Peer self;

    public ParticipantStates(Peer self, List<Peer> peers) {
        this.self = self;
        peerMap = new HashMap<>(peers.size());
        lock = new ReentrantReadWriteLock(true);
        peerMap.put(self.getId(), self);
        for (Peer peer : peers) {
            peerMap.put(peer.getId(), peer);
        }
    }

    Peer getSelf() { return self; }

    public Collection<Peer> getPeers() throws SuspendExecution {
        lock.readLock().lock();
        try {
            return peerMap.values();
        } finally {
            lock.readLock().unlock();
        }
    }

    Peer getPeer(String key) throws SuspendExecution {
        lock.readLock().lock();
        try {
            return peerMap.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Called when a new peer is added to the cluster.
     * WARNING: the address given could be already in the cluster so the implementer needs
     * to check for this
     * @param address the address of the new location
     */
    @Suspendable
    public void newPeer(String address) {
        lock.writeLock().lock();
        try {
            if (!peerMap.containsKey(address)) {
                log.info("Adding peer at address: " + address);
                peerMap.put(address, new Peer(Main.convertAddress(address)));
            } else {
                log.debug("Address " + address + " is already in the cluster");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Generates the initial digest of the states that this node knows about
     * @return HashMap representing the digest
     */
    Map<String, Long> getInitialDigest() throws SuspendExecution {
        lock.readLock().lock();
        try {
            HashMap<String, Long> digest = new HashMap<>(peerMap.size());
            for (Map.Entry<String, Peer> entry : peerMap.entrySet()) {
                digest.put(entry.getKey(), entry.getValue().getState().getMaxVersion());
            }
            return digest;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Generates the list of deltas that should be send to the node with an ID of q.
     * @return the delta of changes
     */
    List<Digest> getDeltaScuttle(Map<String, Long> initial) {
        lock.readLock().lock();
        try {
            List<Digest> digests = new ArrayList<>();
            for (Map.Entry<String, Peer> entry : peerMap.entrySet()) {
                Long qMaxVersion = initial.get(entry.getKey());
                if (qMaxVersion != null) {
                    digests.addAll(entry.getValue().getState().getDeltaScuttle(qMaxVersion, entry.getKey()));
                }
            }
            return digests;
        } finally {
            lock.readLock().unlock();
        }
    }
}
