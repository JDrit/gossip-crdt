package net.batchik.crdt.web;

import net.batchik.crdt.gossip.Peer;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.protocol.*;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

public class UpdateRequestHandler implements HttpAsyncRequestHandler<HttpRequest> {
    private Peer peer;

    public UpdateRequestHandler(Peer peer) {
        this.peer = peer;
    }

    /**
     * Triggered when an incoming request is received. This method should
     * return a {@link HttpAsyncRequestConsumer} that will be used to process
     * the request and consume message content if enclosed. The consumer
     * can optionally parse or transform the message content into a structured
     * object which is then passed onto
     * the {@link #handle(Object, HttpAsyncExchange, HttpContext)}
     * method for further processing.
     *
     * @param request the entity enclosing request.
     * @param context the execution context.
     * @return request consumer.
     * @throws IOException   in case of an I/O error.
     * @throws HttpException in case of HTTP protocol violation or a processing
     *                       problem.
     */
    @Override
    public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context) throws HttpException, IOException {
        // Buffer request content in memory for simplicity
        return new BasicAsyncRequestConsumer();
    }

    /**
     * Triggered to complete request processing and to initiate the process of
     * generating a response. This method does not have to submit a response
     * immediately. It can defer transmission of an HTTP response back to
     * the client without blocking the I/O thread by delegating the process
     * of request handling to another service or a worker thread. HTTP response
     * can be submitted as a later a later point of time using
     * {@link HttpAsyncExchange} once response content becomes available.
     *
     * @param data         request data returned by the request consumer.
     * @param httpExchange HTTP exchange.
     * @param context      HTTP execution context.
     * @throws IOException   in case of an I/O error.
     * @throws HttpException in case of HTTP protocol violation or a processing
     *                       problem.
     */
    @Override
    public void handle(HttpRequest data, HttpAsyncExchange httpExchange, HttpContext context) throws HttpException, IOException {
        try {
            String name = data.getRequestLine().getUri().substring(8);
            peer.getState().incrementCounter(name);
        } catch (Exception ex) {
            System.out.println("exception while updating: " + ex);
            ex.printStackTrace();
        }
        HttpResponse response = httpExchange.getResponse();
        httpExchange.submitResponse(new BasicAsyncResponseProducer(response));
    }
}
