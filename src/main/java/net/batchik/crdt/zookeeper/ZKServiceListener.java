package net.batchik.crdt.zookeeper;


/**
 * This is used to enable listening to nodes being added to the zookeeper service.
 */
public interface ZKServiceListener {

    /**
     * Called when a new peer is added to the cluster.
     * WARNING: the address given could be already in the cluster so the implementer needs
     * to check for this
     * @param address the address of the new location
     */
    void newPeer(String address);
}
