package net.batchik.crdt.gossip;

import com.pinterest.quasar.thrift.TFiberServer;
import com.pinterest.quasar.thrift.TFiberServerSocket;
import net.batchik.crdt.Service;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.*;

public class GossipServer extends Service {
    private final TServer tServer;

    public GossipServer(ParticipantStates states, int sleepTime) throws TTransportException {
        GossipFiber thread = new GossipFiber(states, sleepTime);
        thread.start();

        GossipServiceHandler handler = new GossipServiceHandler(states);
        GossipService.Processor processor = new GossipService.Processor<GossipService.Iface>(handler);

        TFiberServerSocket trans = new TFiberServerSocket(states.getSelf().getAddress());
        TFiberServer.Args targs = new TFiberServer.Args(trans, processor)
                .protocolFactory(new TBinaryProtocol.Factory())
                .transportFactory(new TFramedTransport.Factory());
        tServer = new TFiberServer(targs);
    }

    public void start() {
        tServer.serve();
    }

    public void close() {
        tServer.stop();
    }
}
