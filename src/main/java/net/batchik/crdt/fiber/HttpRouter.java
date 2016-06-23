package net.batchik.crdt.fiber;

import co.paralleluniverse.fibers.SuspendExecution;
import com.codahale.metrics.Meter;
import net.batchik.crdt.Main;
import net.batchik.crdt.fiber.handlers.HttpMethod;
import net.batchik.crdt.fiber.handlers.RequestHandler;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * The router used to redirect HTTP request to the correct handler. Used regular expressions
 * to match the URI.
 */
public class HttpRouter {
    private static final Logger log = LogManager.getLogger(HttpRouter.class);
    private static final RequestHandler notFoundHandler = new RequestHandler() {};
    private final ArrayList<EndPointEntry> endPointEntries;

    private HttpRouter(ArrayList<EndPointEntry> endPointEntries) {
        this.endPointEntries = endPointEntries;
    }

    HttpResponse route(InetSocketAddress address, HttpMethod method, String uri) throws SuspendExecution {
        try {

            for (EndPointEntry entry : endPointEntries) {
                if (entry.getMethod().equals(method) && entry.getPattern().matcher(uri).matches()) {
                    switch (method) {
                        case GET:
                            return entry.getHandler().handleGet(address, uri);
                        case PUT:
                            return entry.getHandler().handlePut(address, uri);
                        case DELETE:
                            return entry.getHandler().handleDelete(address, uri);
                        case POST:
                            return entry.getHandler().handlePost(address, uri);
                    }
                }
            }
            log.debug("no handler found for path: " + uri);
            return notFoundHandler.handleGet(address, uri);
        } catch (NullPointerException | IllegalArgumentException ex) {
            log.error("could not determine method type", ex);
            return Response.BAD_REQUEST;
        }
    }

    public static HttpRouterBuilder builder() {
        return new HttpRouterBuilder();
    }

    private static class EndPointEntry {
        private final Pattern pattern;
        private final RequestHandler handler;
        private final HttpMethod method;
        private final Meter meter;

        EndPointEntry(String regex, HttpMethod method, RequestHandler handler) {
            this.meter = Main.metrics.meter(regex + " requests");
            this.pattern = Pattern.compile(regex);
            this.method = method;
            this.handler = handler;
        }

        HttpMethod getMethod() { return method; }

        Pattern getPattern() { return pattern; }

        RequestHandler getHandler() {
            meter.mark();
            return handler;
        }
    }

    /**
     * Used to build the router for all HTTP Requests. Implements the builder pattern.
     */
    public static class HttpRouterBuilder {
        ArrayList<EndPointEntry> endpoints;

        HttpRouterBuilder() {
            this.endpoints = new ArrayList<>();
        }

        public HttpRouterBuilder registerEndpoint(String regex, HttpMethod method, RequestHandler handler) {
            endpoints.add(new EndPointEntry(regex, method, handler));
            return this;
        }

        public HttpRouter build() {
            return new HttpRouter(endpoints);
        }


    }
}
