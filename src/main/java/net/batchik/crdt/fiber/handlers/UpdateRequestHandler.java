package net.batchik.crdt.fiber.handlers;

import co.paralleluniverse.fibers.SuspendExecution;
import com.codahale.metrics.Meter;
import net.batchik.crdt.Main;
import net.batchik.crdt.fiber.Response;
import net.batchik.crdt.gossip.Peer;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;

public class UpdateRequestHandler extends RequestHandler {
    private final Peer self;

    public UpdateRequestHandler(Peer self) {
        this.self = self;
    }

    @Override
    public HttpResponse handleGet(HttpRequest req, InetSocketAddress address) throws SuspendExecution {
        String uri = req.getRequestLine().getUri();
        if (uri.length() > 8) {
            String name = uri.substring(8);
            self.getState().incrementCounter(name);
            return Response.OK;
        } else {
            return Response.BAD_REQUEST;
        }
    }
}
