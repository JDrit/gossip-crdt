package net.batchik.crdt.fiber.handlers;

import co.paralleluniverse.fibers.SuspendExecution;
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
    public HttpResponse handleGet(InetSocketAddress address, String uri) throws SuspendExecution {
        return Response.ok(self.getState().getAllResponse().getBytes());
    }
}
