package net.batchik.crdt.fiber;


import co.paralleluniverse.common.monitoring.MonitorType;
import co.paralleluniverse.fibers.*;
import co.paralleluniverse.fibers.io.FiberServerSocketChannel;
import co.paralleluniverse.fibers.io.FiberSocketChannel;
import co.paralleluniverse.strands.SuspendableCallable;
import net.batchik.crdt.Service;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main server code implemented using Fibers. Single Fiber that accepts connections and then
 * hands it off to a new fiber for processing.
 */
public class FiberServer extends Service {
    private static Logger log = LogManager.getLogger(FiberServer.class);

    private static final int parallelism = 10000;
    private final FiberScheduler fiberScheduler;
    private final InetSocketAddress address;
    private final HttpRouter router;

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public FiberServer(InetSocketAddress address, HttpRouter router) {
        fiberScheduler = new FiberForkJoinScheduler("web-scheduler", parallelism, MonitorType.METRICS, false);
        this.address = address;
        this.router = router;
    }

    @Override
    public void start() throws Exception {
        if (!initialized.compareAndSet(false, true)) {
            log.error("fiber server has already been initialized");
            throw new Exception("fiber server has already been initialized");
        }
        if (shutdown.get()) {
            log.error("server has already been shut down");
            throw new Exception("server has already been shut down");
        }

        fiberScheduler.newFiber(new SuspendableCallable<Void>() {
            @Override
            public Void run() throws SuspendExecution, InterruptedException {
                try(FiberServerSocketChannel channel = FiberServerSocketChannel.open().bind(address)) {
                    for (;;) {
                        if (shutdown.get()) {
                            log.info("Server was shutdown, exiting server routine.");
                            break;
                        }
                        FiberSocketChannel ch = channel.accept();
                        InetSocketAddress remoteAddress = (InetSocketAddress) ch.getRemoteAddress();
                        fiberScheduler.newFiber(new RequestCallable(router, remoteAddress, ch)).start();
                    }
                } catch (IOException ex) {
                    log.error("io exception while opening socket", ex);
                    shutdown.set(true);
                }
                return null;
            }
        }).start();
    }

    public void close() {
        shutdown.set(true);
    }


}
