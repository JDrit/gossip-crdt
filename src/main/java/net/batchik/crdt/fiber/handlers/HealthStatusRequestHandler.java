package net.batchik.crdt.fiber.handlers;


import co.paralleluniverse.fibers.SuspendExecution;
import net.batchik.crdt.Main;
import net.batchik.crdt.fiber.Response;
import net.batchik.crdt.gossip.ParticipantStates;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;

/**
 * Used to display health metrics of the system
 */
public class HealthStatusRequestHandler implements RequestHandler {
    private static Logger log = LogManager.getLogger(HealthStatusRequestHandler.class);
    private final ParticipantStates states;
    private final StringReporter reporter;

    public HealthStatusRequestHandler(ParticipantStates states) {
        this.states = states;
        this.reporter = new StringReporter(Main.metrics, states);
    }

    @Override
    public HttpResponse handleGet(HttpRequest req, InetSocketAddress address) throws SuspendExecution {
        StringBuilder builder = new StringBuilder();
        reporter.report(builder);

        return Response.ok(builder.toString());
    }

}
