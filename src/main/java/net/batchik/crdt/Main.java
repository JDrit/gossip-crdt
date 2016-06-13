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
    private static final String DEFAULT_GOSSIP_ADDRESS = "0.0.0.0";
    private static final String DEFAULT_WEB_ADDRESS = "0.0.0.0:6000";
    private static final int DEFAULT_GOSSIP_TIME = 5000;

    public static final MetricRegistry metrics = new MetricRegistry();

    static ConsoleReporter reporter = ConsoleReporter.forRegistry(Main.metrics)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();

    static {
        options.addOption(Option.builder()
                .argName("config")
                .hasArg()
                .desc("configuration file")
                .longOpt("config")
                .build());
        options.addOption(Option.builder()
                .argName("peer")
                .hasArg()
                .desc("the address of another node")
                .longOpt("peer")
                .build());
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

        if (cmd.hasOption("help")) {
            formatter.printHelp("gossip-crdt", "Overview of argument flags", options,
                    "**just remember this is a pre-alpha personal project**", true);
            System.exit(0);
        }
        if (cmd.hasOption("version")) {
            System.out.println("version: " + VERSION);
            System.exit(0);
        }

        String gossipAddress = DEFAULT_GOSSIP_ADDRESS + ":" + DEFAULT_GOSSIP_PORT;
        String webAddress = DEFAULT_WEB_ADDRESS;
        int sleepTime = DEFAULT_GOSSIP_TIME;
        List<Peer> peers = new ArrayList<>();
        Peer self = null;
        ParticipantStates states = null;

        if (cmd.hasOption("config")) {

            CompositeConfiguration config = new CompositeConfiguration();
            config.addConfiguration(new SystemConfiguration());
            config.addConfiguration(new PropertiesConfiguration(cmd.getOptionValue("config")));
            config.setThrowExceptionOnMissing(false);

            String level = config.getString("log.level");
            if (level != null) {
                switch (level.toUpperCase()) {
                    case "TRACE":
                        Logger.getRootLogger().setLevel(Level.TRACE);
                        break;
                    case "DEBUG":
                        Logger.getRootLogger().setLevel(Level.DEBUG);
                        break;
                    case "INFO":
                        Logger.getRootLogger().setLevel(Level.INFO);
                        break;
                    case "WARN":
                        Logger.getRootLogger().setLevel(Level.WARN);
                        break;
                    case "ERROR":
                        Logger.getRootLogger().setLevel(Level.ERROR);
                        break;
                    case "FATAL":
                        Logger.getRootLogger().setLevel(Level.FATAL);
                        break;
                }
            }

            gossipAddress = config.getString("gossip.address");
            self = new Peer(convertAddress(gossipAddress));
            sleepTime = config.getInt("gossip.time");
            webAddress = config.getString("web.address");

            for (String peer : config.getStringArray("gossip.peers")) {
                InetSocketAddress address = convertAddress(peer);
                peers.add(new Peer(address));
            }
            states =  new ParticipantStates(self, peers);

        } else if (cmd.hasOption("peer")) {
            peers.add(new Peer(convertAddress(cmd.getOptionValue("peer"))));
            self = new Peer(convertAddress(gossipAddress));
            states =  new ParticipantStates(self, peers);

        } else if (cmd.hasOption("zk")) {

            int max = 5000;
            int min = 3000;
            int port = new Random().nextInt(max - min + 1) + min;
            webAddress = "0.0.0.0:" + (port + 1000);

            //int port = DEFAULT_GOSSIP_PORT;

            gossipAddress = InetAddress.getLocalHost().getHostAddress();
            self = new Peer(new InetSocketAddress(gossipAddress, port));
            String zk = cmd.getOptionValue("zk");
            final String serviceName = "uvb-server";
            ZKServiceDiscovery zkService = new ZKServiceDiscovery(zk, serviceName);
            zkService.start();

            for (ServiceInstance<String> instance : zkService.getInstances()) {
                String instanceAddress = instance.getAddress();
                int instancePort = instance.getPort();
                InetSocketAddress address = new InetSocketAddress(instanceAddress, instancePort);
                log.info("adding peer at address: " + instanceAddress + ":" + instancePort);
                peers.add(new Peer(address));
            }
            states =  new ParticipantStates(self, peers);

            zkService.register(gossipAddress, port);
            zkService.addListener(states);
            log.info("instance registered");

        } else {
            System.out.println("no config file specified or peer given, please specify one. exiting...");
            System.exit(1);
        }

        log.info("self peer: " + self);
        log.info("starting peer with " + peers.size() + " other peer(s)");
        log.info("gossipping on: " + states.getSelf().getAddress());
        log.info("web requests on: " + webAddress);

        //Service httpServer = new WebServer(convertAddress(webAddress), self);
        log.info("web starting...");
        //httpServer.start();

        reporter.start(3, TimeUnit.SECONDS);

        HttpRouter router = HttpRouter.builder()
                .registerEndpoint("/status", new StatusRequestHandler(self))
                .registerEndpoint("/update/.*", new UpdateRequestHandler(self))
                .setParallelism(1024 * 1024)
                .build();

        Service httpServer = new FiberServer("0.0.0.0", 6000, router);
        httpServer.start();

        Service gossipServer = new GossipServer(states, sleepTime);
        log.info("thrift backend starting...");
        gossipServer.start();

        Thread.sleep(Long.MAX_VALUE);
    }
}
