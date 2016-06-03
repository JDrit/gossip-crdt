package net.batchik.crdt;

import java.net.InetSocketAddress;

public class Peer {
    private InetSocketAddress address;
    private IndividualState state;

    public Peer(InetSocketAddress address) {
        this.address = address;
        this.state = new IndividualState();
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public IndividualState getState() {
        return state;
    }

    @Override
    public String toString() {
        return "peer: " + address.toString();
    }
}
