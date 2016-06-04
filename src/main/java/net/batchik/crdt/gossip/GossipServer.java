package net.batchik.crdt.gossip;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;

import java.util.List;

public class GossipServer {

    public static TServer generateServer(Peer self, List<Peer> peers) throws TTransportException {

        ParticipantStates states =  new ParticipantStates(self, peers);
        GossipThread thread = new GossipThread(states);
        thread.start();

        GossipServiceHandler handler = new GossipServiceHandler(states);
        GossipService.Processor processor = new GossipService.Processor<GossipService.Iface>(handler);
        TNonblockingServerTransport transport = new TNonblockingServerSocket(self.getAddress());
        TThreadedSelectorServer.Args serverArgs = new TThreadedSelectorServer.Args(transport)
            .transportFactory(new TFramedTransport.Factory())
            .protocolFactory(new TBinaryProtocol.Factory())
            .processor(processor)
            .selectorThreads(4)
            .workerThreads(32);
        return new TThreadedSelectorServer(serverArgs);

    }
}