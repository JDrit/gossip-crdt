package net.batchik.crdt.fiber;

import co.paralleluniverse.fibers.SuspendExecution;
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
            final String method = request.getRequestLine().getMethod();

            for (EndPointEntry entry : endPointEntries) {
                if (entry.getPattern().matcher(path).matches()) {
                    return entry.getHandler().handleGet(request, address);
                }
            }
            log.debug("no handler found for path: " + path);
        } catch (URISyntaxException ex) {
            log.error("uri syntax error", ex);
        }
        return notFoundHandler.handleGet(request, address);

    }

    public static HttpRouterBuilder builder() {
        return new HttpRouterBuilder();
    }

    private static class EndPointEntry {
        private final Pattern pattern;
        private final RequestHandler handler;

        public EndPointEntry(String regex, RequestHandler handler) {
            this.pattern = Pattern.compile(regex);
            this.handler = handler;
        }

        public Pattern getPattern() { return pattern; }

        public RequestHandler getHandler() { return handler; }
    }

    /**
     * Used to build the router for all HTTP Requests. Implements the builder pattern.
     */
    public static class HttpRouterBuilder {
        ArrayList<EndPointEntry> endpoints;

        public HttpRouterBuilder() {
            this.endpoints = new ArrayList<>();
        }

        public HttpRouterBuilder registerEndpoint(String regex, RequestHandler handler) {
            endpoints.add(new EndPointEntry(regex, handler));
            return this;
        }

        public HttpRouter build() {
            return new HttpRouter(endpoints);
        }


    }
}
