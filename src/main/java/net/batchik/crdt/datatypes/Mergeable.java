package net.batchik.crdt.datatypes;


interface Mergeable<S> {
    public void merge(S other);
}
