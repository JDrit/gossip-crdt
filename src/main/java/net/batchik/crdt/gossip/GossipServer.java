package net.batchik.crdt.gossip;

import net.batchik.crdt.Service;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;

import java.util.List;

public class GossipServer extends Service {
    private final TServer tServer;

    public GossipServer(ParticipantStates states, int sleepTime) throws TTransportException {
        //GossipThread thread = new GossipThread(states, sleepTime);
        //thread.start();

        GossipServiceHandler handler = new GossipServiceHandler(states);
        GossipService.Processor processor = new GossipService.Processor<GossipService.Iface>(handler);
        TNonblockingServerTransport transport = new TNonblockingServerSocket(states.getSelf().getAddress());
        TThreadedSelectorServer.Args serverArgs = new TThreadedSelectorServer.Args(transport)
            .transportFactory(new TFramedTransport.Factory())
            .protocolFactory(new TBinaryProtocol.Factory())
            .processor(processor)
            .selectorThreads(4)
            .workerThreads(32);
        tServer = new TThreadedSelectorServer(serverArgs);
    }

    public void start() {
        tServer.serve();
    }

    public void close() {
        tServer.stop();
    }
}
