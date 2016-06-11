package net.batchik.crdt;

import java.io.Closeable;

/**
 * Base representation of services for the cluster
 */
public abstract class Service implements Closeable {

    public abstract void start() throws Exception;

}
