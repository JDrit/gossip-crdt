package net.batchik.crdt;

import org.apache.thrift.TException;

import java.util.List;
import java.util.Map;

public class GossipServiceHandler implements GossipService.Iface {
    private ParticipantStates state;

    public GossipServiceHandler(ParticipantStates state) {
        this.state = state;
    }

    public Map<Long,Long> initial(Map<Long,Long> request) throws TException {
        return null;
    }

    public List<Digest> digests(List<Digest> digests) throws TException {
        return null;
    }
}
