package net.batchik.crdt.fiber;

import co.paralleluniverse.fibers.SuspendExecution;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class HttpRouter {
    private static final Logger log = LogManager.getLogger(HttpRouter.class);
    private final ListeningExecutorService executor;
    private final ArrayList<EndPointEntry> endPointEntries;

    private static RequestHandler notFoundHandler = new RequestHandler() {};

    private HttpRouter(ArrayList<EndPointEntry> endPointEntries, int parallelism) {
        this.endPointEntries = endPointEntries;
        //executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(parallelism));
        executor = MoreExecutors.listeningDecorator(Executors.newWorkStealingPool());
    }

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

    public static class HttpRouterBuilder {
        ArrayList<EndPointEntry> endpoints;
        int parallelism = 10000;

        public HttpRouterBuilder() {
            this.endpoints = new ArrayList<>();
        }

        public HttpRouterBuilder registerEndpoint(String regex, RequestHandler handler) {
            endpoints.add(new EndPointEntry(regex, handler));
            return this;
        }

        public HttpRouterBuilder setParallelism(int parallelism) {
            this.parallelism = parallelism;
            return this;
        }

        public HttpRouter build() {
            return new HttpRouter(endpoints, parallelism);
        }


    }
}
