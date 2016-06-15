package net.batchik.crdt;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import net.batchik.crdt.fiber.FiberServer;
import net.batchik.crdt.fiber.HttpRouter;
import net.batchik.crdt.fiber.StatusRequestHandler;
import net.batchik.crdt.fiber.UpdateRequestHandler;
import net.batchik.crdt.gossip.GossipServer;
import net.batchik.crdt.gossip.ParticipantStates;
import net.batchik.crdt.gossip.Peer;
import net.batchik.crdt.zookeeper.ZKServiceDiscovery;
import org.apache.commons.cli.*;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.curator.x.discovery.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());

    private static final Options options = new Options();
    private static final HelpFormatter formatter = new HelpFormatter();
    private static final CommandLineParser parser = new DefaultParser();
    private static final String VERSION = "0.0.1";

    private static final int DEFAULT_GOSSIP_PORT = 5000;
    private static final int DEFAULT_FIBER_PORT = 6000;
    private static final String DEFAULT_WEB_ADDRESS = "0.0.0.0:6000";
    private static final int DEFAULT_GOSSIP_TIME = 5000;
    private static final String DEFAULT_SERVICE_NAME = "uvb-server";

    public static final MetricRegistry metrics = new MetricRegistry();

    private static final ConsoleReporter reporter = ConsoleReporter.forRegistry(Main.metrics)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();

    static {
        options.addOption(Option.builder()
                .argName("zookeeper")
                .hasArg()
                .desc("the address of the zookeeper instance for configuration")
                .longOpt("zk")
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
        ArrayList<Peer> peers = new ArrayList<>();

        if (cmd.hasOption("help")) {
            formatter.printHelp("gossip-crdt", "Overview of argument flags", options,
                    "**just remember this is a pre-alpha personal project**", true);
            System.exit(0);
        }
        if (cmd.hasOption("version")) {
            System.out.println("version: " + VERSION);
            System.exit(0);
        }
        if (!cmd.hasOption("zk")) {
            log.error("no zookeeper instance given, please specify with '--zk'");
            System.exit(1);
        }

        int min = 4000;
        int max = 5000;
        int gossipPort = new Random().nextInt(max - min + 1) + min;
        int webPort = gossipPort + 1000;

        String zk = cmd.getOptionValue("zk");
        InetAddress address = InetAddress.getLocalHost();
        InetSocketAddress gossipAddress = new InetSocketAddress(address, gossipPort);
        InetSocketAddress webAddress = new InetSocketAddress(address, webPort);
        Peer self = new Peer(gossipAddress);

        ZKServiceDiscovery zkService = new ZKServiceDiscovery(zk, DEFAULT_SERVICE_NAME);
        zkService.start();
        for (ServiceInstance<String> instance : zkService.getInstances()) {
            String instanceAddress = instance.getAddress();
            int instancePort = instance.getPort();
            InetSocketAddress peerAddress = new InetSocketAddress(instanceAddress, instancePort);
            log.info("adding peer at address: " + peerAddress);
            peers.add(new Peer(peerAddress));
        }
        ParticipantStates states =  new ParticipantStates(self, peers);

        zkService.register(address, gossipPort);
        zkService.addListener(states);
        log.info("instance registered");

        log.info("self peer: " + self);
        log.info("starting peer with " + peers.size() + " other peer(s)");
        log.info("gossipping on: " + gossipAddress);
        log.info("web requests on: " + webAddress);

        //reporter.start(3, TimeUnit.SECONDS);

        HttpRouter router = HttpRouter.builder()
                .registerEndpoint("/status", new StatusRequestHandler(self))
                .registerEndpoint("/update/.*", new UpdateRequestHandler(self))
                .build();

        Service fiberServer = new FiberServer(webAddress, router);
        fiberServer.start();

        Service gossipServer = new GossipServer(states, DEFAULT_GOSSIP_TIME);
        gossipServer.start();
    }
}
