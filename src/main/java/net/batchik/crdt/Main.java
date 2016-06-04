package net.batchik.crdt;

import net.batchik.crdt.gossip.GossipServer;
import net.batchik.crdt.web.StatusRequestHandler;
import net.batchik.crdt.web.UpdateRequestHandler;
import net.batchik.crdt.gossip.Peer;
import net.batchik.crdt.web.WebServer;
import org.apache.commons.cli.*;
import org.apache.http.ExceptionLogger;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.log4j.Logger;
import org.apache.thrift.server.TServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class Main {
    static final Logger log = Logger.getLogger(Main.class.getName());
    static final Options options = new Options();
    static final HelpFormatter formatter = new HelpFormatter();
    static final CommandLineParser parser = new DefaultParser();
    static final String VERSION = "0.0.1";

    static {
        options.addOption(Option.builder()
                .argName("self-id")
                .hasArg()
                .desc("the ID of the current peer")
                .longOpt("self-id")
                .build());
        options.addOption(Option.builder()
                .argName("self-address")
                .hasArg()
                .desc("the address this peer will listen on")
                .longOpt("self-address")
                .build());
        options.addOption(Option.builder()
                .argName("peers")
                .hasArgs()
                .desc("the list of peers in the system. Use format \"id-127.0.0.1:5001\", separated by commas")
                .longOpt("peers")
                .build());
        options.addOption(Option.builder()
                .argName("web-address")
                .hasArg()
                .desc("the address of the web port")
                .longOpt("web-address")
                .build());
        options.addOption("help", "print information about system and exit");
        options.addOption("version", "print the version information and exit");
    }

    public static InetSocketAddress convertAddress(String address) {
        String[] split = address.split(":");
        return new InetSocketAddress(split[0], Integer.parseInt(split[1]));
    }


    public static void main(String[] args) throws Exception {
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("help")) {
            formatter.printHelp("gossip-crdt", "Overview of argument flags", options,
                    "**just remember this is a pre-alpha personal project**", true);
            System.exit(0);
        }
        if (cmd.hasOption("version")) {
            System.out.println("version: " + VERSION);
            System.exit(0);
        }
        if (!cmd.hasOption("self-id")) {
            System.out.println("need to specify this peer's ID");
            System.exit(1);
        }
        if (!cmd.hasOption("self-address")) {
            System.out.println("need to specify the address this peer will listen on");
            System.exit(1);
        }
        if (!cmd.hasOption("web-address")) {
            System.out.print("need to specify the port of the web interface");
            System.exit(1);
        }
        if (!cmd.hasOption("peers")) {
            System.out.println("peers not specified, exiting...");
            System.exit(1);
        }
        int selfId = Integer.parseInt(cmd.getOptionValue("self-id"));
        InetSocketAddress selfAddress = convertAddress(cmd.getOptionValue("self-address"));
        int webPort = Integer.parseInt(cmd.getOptionValue("web-address"));
        Peer self = new Peer(selfId, selfAddress);
        String[] peerArray = cmd.getOptionValues("peers");

        List<Peer> peers = new ArrayList<>(peerArray.length);
        for (String peer : peerArray) {
            String[] split = peer.split("-");
            int id = Integer.parseInt(split[0]);
            InetSocketAddress address = convertAddress(split[1]);
            peers.add(new Peer(id, address));
        }

        log.info("starting up with " + peers.size() + " peers.\n" +
                 peers + "\n" +
                 "binding to address: " + selfAddress);


        HttpServer httpServer = WebServer.generateServer(webPort, self);
        httpServer.start();
        log.info("web started");

        TServer tServer = GossipServer.generateServer(self, peers);
        log.info("thrift backend starting...");
        tServer.serve();

    }

}
