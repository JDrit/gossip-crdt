package net.batchik.crdt.fiber.handlers;

import co.paralleluniverse.fibers.SuspendExecution;
import net.batchik.crdt.fiber.Response;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpResponse;

import java.net.InetSocketAddress;

/**
 * Basic Ping test used for the load balancer. Just to check if the server is up.
 */
public class PingRequestHandler extends RequestHandler {
    private static final HttpResponse response = new BasicHttpResponse(Response.VERSION, HttpStatus.SC_OK, "OK");

    static {
        response.setEntity(new ByteArrayEntity("<html><body><h1>OK</h1></body></html>".getBytes()));
    }

    @Override
    public HttpResponse handleGet(HttpRequest req, InetSocketAddress address) throws SuspendExecution {
        return response;
    }
}
