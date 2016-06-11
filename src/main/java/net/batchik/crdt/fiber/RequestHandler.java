package net.batchik.crdt.fiber;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import javax.xml.ws.Response;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;

/**
 * Interface to the different supported HTTP methods
 */
public interface RequestHandler {
    default HttpResponse handleGet(HttpRequest req, InetSocketAddress address) {
        return net.batchik.crdt.fiber.Response.BAD_REQUEST;
    }

    default HttpResponse handlePost(HttpRequest req, InetSocketAddress address, InputStream payload) {
        return net.batchik.crdt.fiber.Response.BAD_REQUEST;
    }

    default HttpResponse handleDelete(HttpRequest req, InetSocketAddress address) {
        return net.batchik.crdt.fiber.Response.BAD_REQUEST;
    }

    default HttpResponse handlePut(HttpRequest req, InetSocketAddress address) {
        return net.batchik.crdt.fiber.Response.BAD_REQUEST;
    }
}