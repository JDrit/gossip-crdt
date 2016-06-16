package net.batchik.crdt.fiber;


import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.io.FiberServerSocketChannel;
import co.paralleluniverse.fibers.io.FiberSocketChannel;
import co.paralleluniverse.strands.SuspendableCallable;
import com.codahale.metrics.Timer;
import net.batchik.crdt.Main;
import net.batchik.crdt.Service;
import net.batchik.crdt.fiber.handlers.RequestHandler;
import org.apache.http.*;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.codahale.metrics.MetricRegistry.name;

public class FiberServer extends Service {
    private static Logger log = LogManager.getLogger(FiberServer.class);

    private static final int SESSION_BUFFER_SIZE = 100*1024; // 100KB
    private final ContentLengthStrategy contentLengthStrategy = StrictContentLengthStrategy.INSTANCE;
    private final HttpTransportMetricsImpl transMetricImpl = new HttpTransportMetricsImpl();
    private FiberServerSocketChannel serverChannel;
    private static final int parallelism = 10000;
    private final FiberScheduler fiberScheduler;
    private final InetSocketAddress address;
    private final HttpRouter router;

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    private final Timer responseTime = Main.metrics.timer(name(RequestHandler.class, "fiber-response-time"));


    public FiberServer(InetSocketAddress address, HttpRouter router) {
        fiberScheduler = new FiberForkJoinScheduler("web-scheduler", parallelism);
        this.address = address;
        this.router = router;

    }


    public void start() throws Exception {
        if (!initialized.compareAndSet(false, true)) {
            log.error("fiber server has already been initialized");
            throw new Exception("fiber server has already been initialized");
        }
        if (shutdown.get()) {
            log.error("server has already been shut down");
            throw new Exception("server has already been shut down");
        }

        Fiber<Void> bindFiber = new Fiber<>(fiberScheduler, new SuspendableCallable<Void>() {
            @Override
            public Void run() throws SuspendExecution, InterruptedException {
                try {
                    serverChannel = FiberServerSocketChannel.open(null).bind(address);

                    for (;;) {
                        if (shutdown.get()) {
                            log.info("Server was shutdown, exiting server routine.");
                            break;
                        }
                        final FiberSocketChannel ch = serverChannel.accept();
                        final InetSocketAddress remoteAddress = (InetSocketAddress) ch.getRemoteAddress();

                        new Fiber<>(fiberScheduler, new SuspendableCallable<Void>() {
                            @Override
                            public Void run() throws SuspendExecution, InterruptedException {
                                fiberServerRoutine(remoteAddress, ch);
                                return null;
                            }
                        }).start();
                    }
                    serverChannel.close();

                } catch (IOException ex) {
                    log.error("io exception while opening socket", ex);
                    shutdown.set(true);
                }
                return null;
            }
        });
        bindFiber.start();
    }

    /**
     * Handles all logic inside the fiber created for the given request.
     * @param address the remote address of the client
     * @param chTmp the channel to communicate on
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    private void fiberServerRoutine(InetSocketAddress address, FiberSocketChannel chTmp)
            throws SuspendExecution, InterruptedException {
        final Timer.Context context = responseTime.time();

        try (FiberSocketChannel ch = chTmp) {
            final SessionInputBufferImpl sessionInputBuffer = new SessionInputBufferImpl(transMetricImpl, SESSION_BUFFER_SIZE);
            final SessionOutputBufferImpl sessionOutputBuffer = new SessionOutputBufferImpl(transMetricImpl, SESSION_BUFFER_SIZE);
            OutputStream os = FiberChannels.newOutputStream(ch);
            InputStream is = FiberChannels.newInputStream(ch);
            sessionOutputBuffer.bind(os);
            sessionInputBuffer.bind(is);

            final DefaultHttpRequestParser parser = new DefaultHttpRequestParser(sessionInputBuffer);
            final HttpRequest rawRequest = parser.parse();

            // deals with PUT requests
            InputStream contentStream = null;
            /*if (rawRequest instanceof HttpEntityEnclosingRequest) {
                long len = contentLengthStrategy.determineLength(rawRequest);
                if (len > 0) {
                    if (len == ContentLengthStrategy.CHUNKED) {
                        contentStream = new ChunkedInputStream(sessionInputBuffer);
                    } else if (len == ContentLengthStrategy.IDENTITY) {
                        contentStream = new IdentityInputStream(sessionInputBuffer);
                    } else {
                        contentStream = new ContentLengthInputStream(sessionInputBuffer, len);
                    }
                }
            }*/
            /* We can wrap this in a fiber if we feel we can be more async */
            HttpResponse rawResponse = router.route(rawRequest, address, contentStream);
            //HttpResponse rawResponse = Response.OK;

            DefaultHttpResponseWriter msgWriter = new DefaultHttpResponseWriter(sessionOutputBuffer);
            msgWriter.write(rawResponse);
            sessionOutputBuffer.flush(); // flushes the header

            if (rawResponse.getEntity() != null) {
                rawResponse.getEntity().writeTo(os);
            }
            os.flush();
            sessionOutputBuffer.flush();
        } catch (HttpException | IOException e) {
            log.error("Error processing request: " + e.getMessage(), e);
        }
        context.stop();

    }

    public void close() {
        shutdown.set(true);
    }


}
