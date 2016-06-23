package net.batchik.crdt.fiber.handlers;


import co.paralleluniverse.common.monitoring.Metrics;
import co.paralleluniverse.fibers.SuspendExecution;
import com.codahale.metrics.MetricRegistry;
import net.batchik.crdt.Main;
import net.batchik.crdt.fiber.Response;
import net.batchik.crdt.fiber.StringReporter;
import net.batchik.crdt.gossip.ParticipantStates;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;

/**
 * Used to display health metrics of the system. Also displays the max version that this
 * host knows that every other host knows about
 */
public class HealthStatusRequestHandler extends RequestHandler {

    private final StringReporter reporter;

    public HealthStatusRequestHandler(ParticipantStates states) {
        this.reporter = new StringReporter(new MetricRegistry[]{Main.metrics, Metrics.registry()}, states);
    }

    @Override
    public HttpResponse handleGet(InetSocketAddress address, String uri) throws SuspendExecution {
        StringBuilder builder = new StringBuilder();
        reporter.report(builder);
        return Response.ok(builder.toString());
    }

}
