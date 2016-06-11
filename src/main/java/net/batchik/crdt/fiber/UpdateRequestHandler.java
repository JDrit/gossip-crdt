package net.batchik.crdt.fiber;

import net.batchik.crdt.gossip.Peer;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;

public class UpdateRequestHandler implements RequestHandler {
    private static final Logger log = LogManager.getLogger(UpdateRequestHandler.class);
    private final Peer self;

    public UpdateRequestHandler(Peer self) {
        this.self = self;
    }

    @Override
    public HttpResponse handleGet(HttpRequest req, InetSocketAddress address) {
        String uri = req.getRequestLine().getUri();
        if (uri.length() > 8) {
            String name = uri.substring(8);
            //self.getState().incrementCounter(name);
            return Response.OK;
        } else {
            return Response.BAD_REQUEST;
        }
    }
}
