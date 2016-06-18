package net.batchik.crdt.fiber.handlers;

import net.batchik.crdt.fiber.Response;
import net.batchik.crdt.gossip.Peer;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import java.net.InetSocketAddress;

/**
 * Displays the counter for every value known on this local host
 */
public class StatusRequestHandler extends RequestHandler {
    private final Peer self;

    public StatusRequestHandler(Peer self) {
        this.self = self;
    }

    @Override
    public HttpResponse handleGet(HttpRequest req, InetSocketAddress address) {
        log.info("getting status");
        return Response.ok(self.getState().getAllResponse().getBytes());
    }
}
