package net.batchik.crdt;

import org.apache.log4j.Logger;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.*;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;


public class Main {
    static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        Map<Integer, Peer> peers = new HashMap<>();
        int index = 0;
        int port = Integer.parseInt(args[0]);
        for (int i = 1 ; i < args.length ; i++) {
            String[] split = args[i].split(":");
            peers.put(index++, new Peer(new InetSocketAddress(split[0], Integer.parseInt(split[1]))));
        }
        log.info("starting up with " + peers.size() + " peers.");
        log.info("binding to port: " + port);

        ParticipantStates states =  new ParticipantStates(peers);
        GossipThread thread = new GossipThread(states);
        thread.start();

        GossipServiceHandler handler = new GossipServiceHandler(states);
        GossipService.Processor processor = new GossipService.Processor<GossipService.Iface>(handler);
        TNonblockingServerTransport transport = new TNonblockingServerSocket(port);
        TThreadedSelectorServer.Args serverArgs = new TThreadedSelectorServer.Args(transport);
        serverArgs.transportFactory(new TFramedTransport.Factory());
        serverArgs.protocolFactory(new TBinaryProtocol.Factory());
        serverArgs.processor(processor);
        serverArgs.selectorThreads(4);
        serverArgs.workerThreads(32);
        TServer server = new TThreadedSelectorServer(serverArgs);
        server.serve();
    }

}
