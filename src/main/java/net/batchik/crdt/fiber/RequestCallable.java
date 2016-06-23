package net.batchik.crdt.fiber;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.io.FiberSocketChannel;
import co.paralleluniverse.strands.SuspendableCallable;
import com.codahale.metrics.Timer;
import net.batchik.crdt.Main;
import net.batchik.crdt.fiber.handlers.HttpMethod;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * Callable used to handle each individual HTTP request.
 */
class RequestCallable implements SuspendableCallable<Void> {
    private static Logger log = LogManager.getLogger(RequestCallable.class);
    private static final int SESSION_BUFFER_SIZE = 100*1024; // 100KB
    private final HttpTransportMetricsImpl transMetricImpl = new HttpTransportMetricsImpl();
    private final Timer responseTime = Main.metrics.timer("fiber response time");

    private final HttpRouter router;
    private final InetSocketAddress address;
    private final FiberSocketChannel chTmp;

    RequestCallable(HttpRouter router, InetSocketAddress address, FiberSocketChannel chTmp) {
        this.router = router;
        this.address = address;
        this.chTmp = chTmp;
    }

    public Void run() throws SuspendExecution, InterruptedException {
        final Timer.Context context = responseTime.time();
        String line = null;

        try (FiberSocketChannel ch = chTmp) {
            final SessionInputBufferImpl sessionInputBuffer = new SessionInputBufferImpl(transMetricImpl, SESSION_BUFFER_SIZE);
            final SessionOutputBufferImpl sessionOutputBuffer = new SessionOutputBufferImpl(transMetricImpl, SESSION_BUFFER_SIZE);
            OutputStream os = FiberChannels.newOutputStream(ch);
            InputStream is = FiberChannels.newInputStream(ch);
            sessionOutputBuffer.bind(os);
            sessionInputBuffer.bind(is);

            line = sessionInputBuffer.readLine().trim();
            int methodOffset = line.indexOf(" ");
            HttpMethod method = HttpMethod.valueOf(line.substring(0, methodOffset));
            String uri = line.substring(methodOffset, line.lastIndexOf(" ")).trim();
            log.debug("method: " + method + ", uri: " + uri);

            /* We can wrap this in a fiber if we feel we can be more async */
            HttpResponse rawResponse = router.route(address, method, uri);

            DefaultHttpResponseWriter msgWriter = new DefaultHttpResponseWriter(sessionOutputBuffer);
            msgWriter.write(rawResponse);

            sessionOutputBuffer.flush(); // flushes the header

            if (rawResponse.getEntity() != null) {
                rawResponse.getEntity().writeTo(os);
            }
            os.flush();
            sessionOutputBuffer.flush();
        } catch (HttpException | IOException e) {
            log.error("Error processing request");
        } catch (IllegalArgumentException e) {
            log.error("invalid HTTP Method: " + line);
        } finally {
            context.stop();
        }
        return null;
    }
}

