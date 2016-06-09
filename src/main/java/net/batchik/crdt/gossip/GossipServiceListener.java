package net.batchik.crdt.gossip;

import net.batchik.crdt.Main;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.ServiceCacheListener;
import org.apache.log4j.Logger;


public class GossipServiceListener<T> implements ServiceCacheListener {
    static final Logger log = Logger.getLogger(GossipServiceListener.class.getName());
    private ParticipantStates states;
    private ServiceDiscovery<T> serviceDiscovery;
    private String serviceName;

    public GossipServiceListener(ParticipantStates states, ServiceDiscovery<T> serviceDiscovery,
                                 String serviceName) {
        this.states = states;
        this.serviceDiscovery = serviceDiscovery;
        this.serviceName = serviceName;
    }

    @Override
    public void cacheChanged() {
        log.info("cache change");
        try {
            for (ServiceInstance<T> instance : serviceDiscovery.queryForInstances(serviceName)) {
                String address = instance.getAddress() + ":" + instance.getPort();
                states.addPeer(address);
            }
        } catch (Exception e) { }
    }

    @Override
    public void stateChanged(CuratorFramework client, ConnectionState newState) {
        log.info("state change");
    }
}
