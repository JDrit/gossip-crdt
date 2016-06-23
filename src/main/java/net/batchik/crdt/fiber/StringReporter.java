package net.batchik.crdt.fiber;

import co.paralleluniverse.fibers.SuspendExecution;
import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import net.batchik.crdt.gossip.ParticipantStates;
import net.batchik.crdt.gossip.Peer;

import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Generates reports of all metrics in the system along with every peers' status.
 * This report is generated using a StringBuilder
 */
public class StringReporter implements Reporter {
    private final MetricRegistry[] registries;
    private final ParticipantStates states;

    private final Locale locale = Locale.getDefault();
    private final Clock  clock = Clock.defaultClock();
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT,
            DateFormat.MEDIUM, locale);

    private final TimeUnit rateUnit = TimeUnit.SECONDS;
    private final double rateFactor = rateUnit.toSeconds(1);
    private final TimeUnit durationUnit = TimeUnit.MILLISECONDS;
    private final double durationFactor = 1.0 / durationUnit.toNanos(1);

    public StringReporter(MetricRegistry[] registries, ParticipantStates states) {
        this.registries = registries;
        this.states = states;

    }

    public void report(StringBuilder output) throws SuspendExecution {
        SortedMap<String, Gauge> gauges = new TreeMap<>();
        SortedMap<String, Counter> counters = new TreeMap<>();
        SortedMap<String, Histogram> histograms = new TreeMap<>();
        SortedMap<String, Meter> meters = new TreeMap<>();
        SortedMap<String, Timer> timers = new TreeMap<>();

        for (MetricRegistry registry : registries) {
            gauges.putAll(registry.getGauges());
            counters.putAll(registry.getCounters());
            histograms.putAll(registry.getHistograms());
            meters.putAll(registry.getMeters());
            timers.putAll(registry.getTimers());
        }

        final String dateTime = dateFormat.format(new Date(clock.getTime()));
        Formatter fm = new Formatter(output);
        printWithBanner(dateTime, '=', output);
        output.append("\n");

        if (!gauges.isEmpty()) {
            printWithBanner("-- Gauges", '-', output);
            for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
                if (!entry.getKey().equals("runawayFibers")) {
                    output.append(entry.getKey()).append("\n");
                    printGauge(entry, fm);
                    output.append("\n");
                }
            }
        }

        if (!counters.isEmpty()) {
            printWithBanner("-- Counters", '-', output);
            for (Map.Entry<String, Counter> entry : counters.entrySet()) {
                output.append(entry.getKey()).append("\n");
                printCounter(entry, fm);
                output.append("\n");
            }
        }

        if (!histograms.isEmpty()) {
            printWithBanner("-- Histograms", '-', output);
            for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
                output.append(entry.getKey()).append("\n");
                printHistogram(entry.getValue(), fm);
                output.append("\n");
            }
        }

        if (!meters.isEmpty()) {
            printWithBanner("-- Meters", '-', output);
            for (Map.Entry<String, Meter> entry : meters.entrySet()) {
                output.append(entry.getKey()).append("\n");
                printMeter(entry.getValue(), fm);
                output.append("\n");
            }
        }

        if (!timers.isEmpty()) {
            printWithBanner("-- Timers", '-', output);
            for (Map.Entry<String, Timer> entry : timers.entrySet()) {
                output.append(entry.getKey()).append("\n");
                printTimer(entry.getValue(), fm);
                output.append("\n");
            }
        }

        printWithBanner("-- Peers", '-', output);
        for (Peer peer : states.getPeers()) {
            printPeer(peer, fm);
        }
        output.append("\n");
    }

    private void printMeter(Meter meter, Formatter output) {
        output.format(locale, "             count = %d%n", meter.getCount());
        output.format(locale, "         mean rate = %2.2f events/%s%n", convertRate(meter.getMeanRate()), rateUnit);
        output.format(locale, "     1-minute rate = %2.2f events/%s%n", convertRate(meter.getOneMinuteRate()), rateUnit);
        output.format(locale, "     5-minute rate = %2.2f events/%s%n", convertRate(meter.getFiveMinuteRate()), rateUnit);
        output.format(locale, "    15-minute rate = %2.2f events/%s%n", convertRate(meter.getFifteenMinuteRate()), rateUnit);
    }

    private void printCounter(Map.Entry<String, Counter> entry, Formatter output) {
        output.format(locale, "             count = %d%n", entry.getValue().getCount());
    }

    private void printGauge(Map.Entry<String, Gauge> entry, Formatter output) {
        output.format(locale, "             value = %s%n", entry.getValue().getValue());
    }

    private void printHistogram(Histogram histogram, Formatter output) {
        Snapshot snapshot = histogram.getSnapshot();
        output.format(locale, "             count = %d%n", histogram.getCount());
        output.format(locale, "               min = %d%n", snapshot.getMin());
        output.format(locale, "               max = %d%n", snapshot.getMax());
        output.format(locale, "              mean = %2.2f%n", snapshot.getMean());
        output.format(locale, "            stddev = %2.2f%n", snapshot.getStdDev());
        output.format(locale, "            median = %2.2f%n", snapshot.getMedian());
        output.format(locale, "              75%% <= %2.2f%n", snapshot.get75thPercentile());
        output.format(locale, "              95%% <= %2.2f%n", snapshot.get95thPercentile());
        output.format(locale, "              98%% <= %2.2f%n", snapshot.get98thPercentile());
        output.format(locale, "              99%% <= %2.2f%n", snapshot.get99thPercentile());
        output.format(locale, "            99.9%% <= %2.2f%n", snapshot.get999thPercentile());
    }

    private void printTimer(Timer timer, Formatter output) {
        final Snapshot snapshot = timer.getSnapshot();
        output.format(locale, "             count = %d%n", timer.getCount());
        output.format(locale, "         mean rate = %2.2f calls/%s%n", convertRate(timer.getMeanRate()), rateUnit);
        output.format(locale, "     1-minute rate = %2.2f calls/%s%n", convertRate(timer.getOneMinuteRate()), rateUnit);
        output.format(locale, "     5-minute rate = %2.2f calls/%s%n", convertRate(timer.getFiveMinuteRate()), rateUnit);
        output.format(locale, "    15-minute rate = %2.2f calls/%s%n", convertRate(timer.getFifteenMinuteRate()), rateUnit);

        output.format(locale, "               min = %2.2f %s%n", convertDuration(snapshot.getMin()), durationUnit);
        output.format(locale, "               max = %2.2f %s%n", convertDuration(snapshot.getMax()), durationUnit);
        output.format(locale, "              mean = %2.2f %s%n", convertDuration(snapshot.getMean()), durationUnit);
        output.format(locale, "            stddev = %2.2f %s%n", convertDuration(snapshot.getStdDev()), durationUnit);
        output.format(locale, "            median = %2.2f %s%n", convertDuration(snapshot.getMedian()), durationUnit);
        output.format(locale, "              75%% <= %2.2f %s%n", convertDuration(snapshot.get75thPercentile()), durationUnit);
        output.format(locale, "              95%% <= %2.2f %s%n", convertDuration(snapshot.get95thPercentile()), durationUnit);
        output.format(locale, "              98%% <= %2.2f %s%n", convertDuration(snapshot.get98thPercentile()), durationUnit);
        output.format(locale, "              99%% <= %2.2f %s%n", convertDuration(snapshot.get99thPercentile()), durationUnit);
        output.format(locale, "            99.9%% <= %2.2f %s%n", convertDuration(snapshot.get999thPercentile()), durationUnit);
    }

    private void printPeer(Peer peer, Formatter output) {
        output.format(locale, "%20s: max version = %d%n", peer.getAddress(), peer.getState().getMaxVersion());
    }

    private void printWithBanner(String s, char c, StringBuilder output) {
        output.append(s);
        output.append(' ');
        for (int i = 0; i < (80 - s.length() - 1); i++) {
            output.append(c);
        }
        output.append("\n");
    }

    private double convertRate(double rate) {
        return rate * rateFactor;
    }

    private double convertDuration(double duration) {
        return duration * durationFactor;
    }
}
