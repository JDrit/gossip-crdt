package net.batchik.crdt;

import co.paralleluniverse.strands.Strand;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import net.batchik.crdt.fiber.FiberServer;
import net.batchik.crdt.fiber.HttpRouter;
import net.batchik.crdt.fiber.handlers.*;
import net.batchik.crdt.gossip.GossipServer;
import net.batchik.crdt.gossip.ParticipantStates;
import net.batchik.crdt.gossip.Peer;
import net.batchik.crdt.zookeeper.ZKServiceDiscovery;
import org.apache.commons.cli.*;
import org.apache.curator.x.discovery.*;
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
    private static final String VERSION = "1.0.0";

    private static final int DEFAULT_GOSSIP_PORT = 5000;
    private static final int DEFAULT_FIBER_PORT = 8080;
    private static final int DEFAULT_GOSSIP_TIME = 5000;
    private static final String DEFAULT_SERVICE_NAME = "uvb-server";

    public static final MetricRegistry metrics = new MetricRegistry();

    private static final ConsoleReporter reporter = ConsoleReporter.forRegistry(Main.metrics)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();

    static {
        options.addOption(Option.builder()
                .argName("zookeeper address")
                .hasArg()
                .desc("the address of the zookeeper instance for configuration")
                .longOpt("zk")
                .build());
        options.addOption(Option.builder()
                .desc("should the gossip and web ports be random")
                .longOpt("random")
                .build());
        options.addOption(Option.builder()
                .desc("print information about the system configuration options, and exit")
                .longOpt("help")
                .build());
        options.addOption(Option.builder()
                .desc("print the version information and exit")
                .longOpt("version")
                .build());
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

        int webPort = DEFAULT_FIBER_PORT;
        int gossipPort = DEFAULT_GOSSIP_PORT;

        if (cmd.hasOption("random")) {
            int min = 4000;
            int max = 5000;
            gossipPort = new Random().nextInt(max - min + 1) + min;
            webPort = gossipPort + 1000;
        }

        String zk = cmd.getOptionValue("zk");
        InetAddress address = InetAddress.getLocalHost();
        InetSocketAddress gossipAddress = new InetSocketAddress(address, gossipPort);
        InetSocketAddress webAddress = new InetSocketAddress("0.0.0.0", webPort);
        Peer self = new Peer(gossipAddress);

        ZKServiceDiscovery zkService = new ZKServiceDiscovery(zk, DEFAULT_SERVICE_NAME);
        zkService.start();
        for (ServiceInstance<String> instance : zkService.getInstances()) {
            InetSocketAddress peerAddress = new InetSocketAddress(instance.getAddress(), instance.getPort());
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
                .registerEndpoint("/status", HttpMethod.GET, new StatusRequestHandler(self))
                .registerEndpoint("/update/.*", HttpMethod.GET, new UpdateRequestHandler(self))
                .registerEndpoint("/health", HttpMethod.GET, new HealthStatusRequestHandler(states))
                .registerEndpoint("/ping", HttpMethod.GET, new PingRequestHandler())
                .build();

        Service fiberServer = new FiberServer(webAddress, router);
        fiberServer.start();

        Service gossipServer = new GossipServer(states, DEFAULT_GOSSIP_TIME);
        gossipServer.start();

        Strand.sleep(Long.MAX_VALUE, TimeUnit.DAYS);
    }
}
