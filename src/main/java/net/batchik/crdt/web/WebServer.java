package net.batchik.crdt.web;

import net.batchik.crdt.Service;
import net.batchik.crdt.gossip.Peer;
import org.apache.http.ExceptionLogger;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.impl.nio.reactor.IOReactorConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class WebServer extends Service {
    private final HttpServer httpServer;

    public WebServer(InetSocketAddress address, Peer peer) throws IOException, InterruptedException {
        IOReactorConfig config = IOReactorConfig.custom().setTcpNoDelay(true).build();

        httpServer = ServerBootstrap.bootstrap()
                .setLocalAddress(address.getAddress())
                .setListenerPort(address.getPort())
                .setExceptionLogger(ExceptionLogger.STD_ERR)
                .setIOReactorConfig(config)
                .registerHandler("/", new StatusRequestHandler(peer))
                .registerHandler("/update/*", new UpdateRequestHandler(peer))
                .setServerInfo("uvb-server")
                .create();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                httpServer.shutdown(5, TimeUnit.SECONDS);
            }
        });
    }

    public void start() throws Exception {
        httpServer.start();
    }

    public void close() {
        httpServer.shutdown(5, TimeUnit.SECONDS);
    }
}
