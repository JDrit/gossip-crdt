package net.batchik.crdt.fiber;

import net.batchik.crdt.gossip.Peer;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;

public class StatusRequestHandler implements RequestHandler {
    private final Peer self;

    public StatusRequestHandler(Peer self) {
        this.self = self;
    }

    @Override
    public HttpResponse handleGet(HttpRequest req, InetSocketAddress address) {
        try {
            return Response.ok(self.getState().getAllResponse());
        } catch (UnsupportedEncodingException ex) {
            return Response.BAD_REQUEST;
        }
    }
}
