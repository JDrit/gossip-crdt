package net.batchik.crdt.web;

import net.batchik.crdt.gossip.Peer;
import org.apache.http.ExceptionLogger;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.impl.nio.reactor.IOReactorConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class WebServer {

    public static HttpServer generateServer(int port, Peer peer, int clusterSize) throws IOException, InterruptedException {
        IOReactorConfig config = IOReactorConfig.custom()
                .setIoThreadCount(Runtime.getRuntime().availableProcessors())
                .setConnectTimeout(30000)
                .setSoTimeout(30000)
                .setTcpNoDelay(true)
                .build();

        HttpServer httpServer = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setExceptionLogger(ExceptionLogger.STD_ERR)
                .setIOReactorConfig(config)
                .registerHandler("/", new StatusRequestHandler(peer))
                .registerHandler("/update/*", new UpdateRequestHandler(peer, clusterSize))
                .setServerInfo("uvb-server")
                .create();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> httpServer.shutdown(5, TimeUnit.SECONDS)));

        return httpServer;
    }
}
