package net.batchik.crdt.gossip.datatypes;

import net.batchik.crdt.gossip.PNCounter;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class PNCounterUtil {
    static Logger log = Logger.getLogger(PNCounterUtil.class.getName());

    public static PNCounter newPNCounter(int size, int id) {
        List<Integer> P = new ArrayList<>(size);
        List<Integer> N = new ArrayList<>(size);
        for (int i = 0 ; i < size ; i++) {
            P.add(0);
            N.add(0);
        }
        P.set(id, 1);
        return new PNCounter(P, N);
    }

    public static void increment(PNCounter counter, int id) {
        List<Integer> P = counter.getP();
        P.set(id, P.get(id) + 1);
    }

    public static void decrement(PNCounter counter, int id) {
        List<Integer> N = counter.getN();
        N.set(id, N.get(id) + 1);
    }

    public static int value(PNCounter counter) {
        int sum = 0;
        List<Integer> P = counter.getP();
        List<Integer> N = counter.getN();
        for (int i = 0 ; i < P.size() ; i++) {
            sum += P.get(i) - N.get(i);
        }
        return sum;
    }

    public static PNCounter merge(PNCounter c1, PNCounter c2) {
        int size = c1.getP().size();
        List<Integer> P1 = c1.getP();
        List<Integer> N1 = c1.getN();
        List<Integer> P2 = c2.getP();
        List<Integer> N2 = c2.getN();
        for (int i = 0 ; i < size ; i++) {
            P1.set(i, P1.get(i) > P2.get(i) ? P1.get(i) : P2.get(i));
            N1.set(i, N1.get(i) > N2.get(i) ? N1.get(i) : N2.get(i));
        }
        return c1;
    }
}
