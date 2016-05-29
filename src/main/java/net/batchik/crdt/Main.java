package net.batchik.crdt;

import org.apache.log4j.Logger;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TNonblockingServerSocket;


public class Main {
    static Logger log = Logger.getLogger(Main.class.getName());
    static int port = 9090;

    public static void main(String[] args) throws Exception {
        log.info("main starting up");
        GossipServiceHandler handler = new GossipServiceHandler();
        GossipService.Processor processor = new GossipService.Processor<GossipService.Iface>(handler);
        TNonblockingServerSocket serverTransport = new TNonblockingServerSocket(port);
        TServer server = new TThreadedSelectorServer(new TThreadedSelectorServer.Args(serverTransport).processor(processor));
        server.serve();

    }

}
