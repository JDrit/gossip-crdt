package net.batchik.crdt;

import net.batchik.crdt.gossip.GossipServer;
import net.batchik.crdt.gossip.Peer;
import net.batchik.crdt.web.WebServer;
import org.apache.commons.cli.*;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.net.SyslogAppender;
import org.apache.thrift.server.TServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


public class Main {
    static final Logger log = Logger.getLogger(Main.class.getName());
    static final Options options = new Options();
    static final HelpFormatter formatter = new HelpFormatter();
    static final CommandLineParser parser = new DefaultParser();
    static final String VERSION = "0.0.1";

    static final String DEFAULT_GOSSIP_ADDRESS = "0.0.0.0:5000";
    static final String DEFAULT_WEB_ADDRESS = "0.0.0.0:6000";
    static final int DEFAULT_GOSSIP_TIME = 2000;

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

        String gossipAddress = DEFAULT_GOSSIP_ADDRESS;
        String webAddress = DEFAULT_WEB_ADDRESS;
        int sleepTime = DEFAULT_GOSSIP_TIME;
        List<Peer> peers = new ArrayList<>();

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
            sleepTime = config.getInt("gossip.time");
            webAddress = config.getString("web.address");

            for (String peer : config.getStringArray("gossip.peers")) {
                InetSocketAddress address = convertAddress(peer);
                peers.add(new Peer(address));
            }

        } else if (cmd.hasOption("peer")) {
            peers.add(new Peer(convertAddress(cmd.getOptionValue("peer"))));
        } else {
            System.out.println("no config file specified or peer given, please specify one. exiting...");
            System.exit(1);
        }


        Peer self = new Peer(convertAddress(gossipAddress));
        log.info("starting peer with " + peers.size() + " other peer(s)");

        HttpServer httpServer = WebServer.generateServer(convertAddress(webAddress), self);
        httpServer.start();
        log.info("web started");

        TServer tServer = GossipServer.generateServer(self, peers, sleepTime);
        log.info("thrift backend starting...");
        tServer.serve();

    }

}
