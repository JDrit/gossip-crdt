package net.batchik.crdt.gossip;

import java.net.InetSocketAddress;

public class Peer {
    private InetSocketAddress address;
    private IndividualState state;
    private String id;

    public Peer(InetSocketAddress address) {
        this.address = address;
        this.id = address.toString();
        this.state = new IndividualState(id);
    }

    public String getId() { return id; }

    public InetSocketAddress getAddress() {
        return address;
    }

    public IndividualState getState() {
        return state;
    }

    @Override
    public String toString() { return "Peer(address: " + address.toString() + ")"; }
}
