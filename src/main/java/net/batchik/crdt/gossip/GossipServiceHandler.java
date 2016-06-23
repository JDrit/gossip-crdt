package net.batchik.crdt.gossip;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import java.util.List;
import java.util.Map;

public class GossipServiceHandler implements GossipService.Iface {
    static Logger log = Logger.getLogger(GossipServiceHandler.class.getName());
    private ParticipantStates state;

    public GossipServiceHandler(ParticipantStates state) {
        this.state = state;
    }

    @Override
    @Suspendable
    public GossipResponse gossip(GossipRequest request) throws TException {
        log.debug("received gossip message");
        List<Digest> digests = state.getDeltaScuttle(request.getMax());
        return new GossipResponse(digests);
    }
}
