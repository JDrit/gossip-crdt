package net.batchik.crdt.zookeeper;

import net.batchik.crdt.Service;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.*;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.curator.x.discovery.details.ServiceCacheListener;
import org.apache.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.concurrent.Executors;

/**
 * Manages all the zookeeper logic required to do service discovery. This uses Apache
 * Curator behind the scenes. Used for service discovery / service registration / cache listeners
 */
public class ZKServiceDiscovery extends Service {
    private static final Logger log = Logger.getLogger(ZKServiceDiscovery.class.getName());
    private static final String basePath = "/services";

    private final CuratorFramework client;
    private final ServiceDiscovery<String> serviceDiscovery;
    private final ServiceCache<String> cache;
    private final String serviceName;

    public ZKServiceDiscovery(String address, String sn) throws Exception {
        this.serviceName = sn;
        RetryPolicy retry = new ExponentialBackoffRetry(1000, 3);
        client =  CuratorFrameworkFactory.newClient(address, retry);


         serviceDiscovery = ServiceDiscoveryBuilder.builder(String.class)
                 .client(client)
                 .serializer(new JsonInstanceSerializer<>(String.class))
                 .basePath(basePath)
                 .build();


        // sets up a listener to the cache of instances to update the peers
        cache = serviceDiscovery.serviceCacheBuilder()
                .name(serviceName)
                .build();

    }

    public void start() throws Exception {
        client.start();
        serviceDiscovery.start();
        cache.start();
    }

    /**
     * Registers a new node for the given service
     * @param address the address for the new instance
     * @param port the port of the new instance
     * @throws Exception
     */
    public void register(InetAddress address, int port) throws Exception {
        final ServiceInstance<String> thisInstance = ServiceInstance.<String>builder()
                .name(serviceName)
                .port(port)
                .address(address.getHostAddress())
                .uriSpec(new UriSpec("{scheme}://{address}:{port}"))
                .build();

        serviceDiscovery.registerService(thisInstance);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    serviceDiscovery.unregisterService(thisInstance);
                } catch (Exception e) { }
            }
        });

    }

    /**
     * Gets all instances running for this service
     * @return the collection of all instances for the given service
     * @throws Exception
     */
    public Collection<ServiceInstance<String>> getInstances() throws Exception {
        return serviceDiscovery.queryForInstances(serviceName);
    }

    /**
     * Adds a listener to the given service to notice for changes to the cluster membership.
     * @param serviceListener the listener to call with the new nodes
     */
    public void addListener(final ZKServiceListener serviceListener) {
        cache.addListener(new ServiceCacheListener() {

            /**
             * Called when there is a state change in the connection
             *
             * @param client   the client
             * @param newState the new state
             */
            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {

            }

            /**
             * Called when the cache has changed (instances added/deleted, etc.)
             */
            @Override
            public void cacheChanged() {
                try {
                    for (ServiceInstance<String> instance : serviceDiscovery.queryForInstances(serviceName)) {
                        String address = instance.getAddress() + ":" + instance.getPort();
                        serviceListener.newPeer(address);
                    }
                } catch (Exception e) { }
            }
        }, Executors.newSingleThreadExecutor());
    }

    public void close() {
        try {
            cache.close();
            serviceDiscovery.close();
            client.close();
        } catch (IOException ex) {
            log.warn("IO exception while closing zookeeper connections", ex);
        }
    }
}
