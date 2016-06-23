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
    private final ArrayList<EndPointEntry> endPointEntries;

    private static RequestHandler notFoundHandler = new RequestHandler() {};

    private HttpRouter(ArrayList<EndPointEntry> endPointEntries) {
        this.endPointEntries = endPointEntries;
    }

    /**
     * Takes the request and run the correct handler on it. This can suspend if needed to by the
     * fiber scheduler
     * @param request the request sent from the client
     * @param address the address of the client that sent the request
     * @param payload the payload that came with the request
     * @return the response from the given handler
     * @throws SuspendExecution
     */
    HttpResponse route(HttpRequest request, InetSocketAddress address, InputStream payload) throws SuspendExecution {
        try {
            final URI uri = new URI(request.getRequestLine().getUri());
            final String path = uri.getPath();
            final String query = uri.getQuery();
            final HttpMethod method = HttpMethod.valueOf(request.getRequestLine().getMethod());

            for (EndPointEntry entry : endPointEntries) {
                if (entry.getMethod().equals(method) && entry.getPattern().matcher(path).matches()) {
                    switch (method) {
                        case GET:
                            return entry.getHandler().handleGet(request, address);
                        case PUT:
                            return entry.getHandler().handlePut(request, address);
                        case DELETE:
                            return entry.getHandler().handleDelete(request, address);
                        case POST:
                            return entry.getHandler().handlePost(request, address, payload);
                    }
                }
            }
            log.debug("no handler found for path: " + path);
            return notFoundHandler.handleGet(request, address);
        } catch (URISyntaxException ex) {
            log.error("uri syntax error", ex);
            return Response.BAD_REQUEST;
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
