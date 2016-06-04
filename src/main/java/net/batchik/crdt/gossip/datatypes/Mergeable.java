package net.batchik.crdt.gossip.datatypes;


interface Mergeable<S> {
    public void merge(S other);
}
