package net.batchik.crdt.fiber.handlers;

import net.batchik.crdt.fiber.Response;
import net.batchik.crdt.gossip.Peer;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import java.net.InetSocketAddress;

public class StatusRequestHandler implements RequestHandler {
    private final Peer self;

    public StatusRequestHandler(Peer self) {
        this.self = self;
    }

    @Override
    public HttpResponse handleGet(HttpRequest req, InetSocketAddress address) {
        return Response.ok(self.getState().getAllResponse().getBytes());
    }
}
