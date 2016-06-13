package net.batchik.crdt.fiber;

import co.paralleluniverse.fibers.SuspendExecution;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import net.batchik.crdt.Main;
import net.batchik.crdt.gossip.Peer;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class UpdateRequestHandler implements RequestHandler {
    private static final Logger log = LogManager.getLogger(UpdateRequestHandler.class);
    private final Peer self;

    private final Meter requests = Main.metrics.meter("update requests/sec");

    public UpdateRequestHandler(Peer self) {
        this.self = self;
    }

    @Override
    public HttpResponse handleGet(HttpRequest req, InetSocketAddress address) throws SuspendExecution {
        String uri = req.getRequestLine().getUri();
        if (uri.length() > 8) {
            String name = uri.substring(8);
            requests.mark();
            self.getState().incrementCounter(name);
            return Response.OK;
        } else {
            return Response.BAD_REQUEST;
        }
    }
}
