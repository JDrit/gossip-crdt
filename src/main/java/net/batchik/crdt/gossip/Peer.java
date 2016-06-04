package net.batchik.crdt.gossip;

import java.net.InetSocketAddress;

public class Peer {
    private int id;
    private InetSocketAddress address;
    private IndividualState state;

    public Peer(int id, InetSocketAddress address) {
        this.id = id;
        this.address = address;
        this.state = new IndividualState();
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public IndividualState getState() {
        return state;
    }

    public int getId() { return id; }

    @Override
    public String toString() {
        return "peer: " + id + ", " + address.toString();
    }
}
