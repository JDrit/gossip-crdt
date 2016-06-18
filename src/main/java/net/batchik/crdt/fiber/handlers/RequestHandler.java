package net.batchik.crdt.fiber.handlers;

import co.paralleluniverse.fibers.SuspendExecution;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.xml.ws.Response;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;

/**
 * base class to handle all type of requests for the given handler
 */
public abstract class RequestHandler {
    protected Logger log = LogManager.getLogger(this.getClass().getName());

    public HttpResponse handleGet(HttpRequest req, InetSocketAddress address) throws SuspendExecution {
        return net.batchik.crdt.fiber.Response.BAD_REQUEST;
    }

    public HttpResponse handlePost(HttpRequest req, InetSocketAddress address, InputStream payload) {
        return net.batchik.crdt.fiber.Response.BAD_REQUEST;
    }

    public HttpResponse handleDelete(HttpRequest req, InetSocketAddress address) {
        return net.batchik.crdt.fiber.Response.BAD_REQUEST;
    }

    public HttpResponse handlePut(HttpRequest req, InetSocketAddress address) {
        return net.batchik.crdt.fiber.Response.BAD_REQUEST;
    }
}