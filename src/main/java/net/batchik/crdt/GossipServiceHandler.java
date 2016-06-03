package net.batchik.crdt;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GossipServiceHandler implements GossipService.Iface {
    static Logger log = Logger.getLogger(GossipServiceHandler.class.getName());
    private ParticipantStates state;

    public GossipServiceHandler(ParticipantStates state) {
        this.state = state;
    }

    public Map<Integer,Long> initial(Map<Integer,Long> request) throws TException {
        log.debug("received an initial request");
        return state.getInitialDigest();
    }

    public List<Digest> digests(int qId, List<Digest> digests) throws TException {
        log.debug("received a digest request");
        return state.getDeltaScuttle(qId);
    }
}
