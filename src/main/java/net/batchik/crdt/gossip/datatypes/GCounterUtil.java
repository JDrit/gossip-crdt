package net.batchik.crdt.gossip.datatypes;

import net.batchik.crdt.gossip.*;

import java.util.ArrayList;
import java.util.List;

public class GCounterUtil {

    public static GCounter newGCounter(int size, int id) {
        List<Integer> P = new ArrayList<>(size);
        for (int i = 0 ; i < size ; i++) {
            P.add(0);
        }
        return new GCounter(id, P);
    }

    public static void increment(GCounter counter) {
        int id = counter.getId();
        List<Integer> P = counter.getP();
        P.set(id, P.get(id) + 1);
    }

    public static int value(GCounter counter) {
        int sum = 0;
        List<Integer> P = counter.getP();
        for (int i = 0 ; i < P.size() ; i++) {
            sum += P.get(i);
        }
        return sum;
    }

    public static GCounter merge(GCounter c1, GCounter c2) {
        int size = c1.getP().size();
        List<Integer> P1 = c1.getP();
        List<Integer> P2 = c2.getP();
        for (int i = 0 ; i < size ; i++) {
            P1.set(i, P1.get(i) > P2.get(i) ? P1.get(i) : P2.get(i));
        }
        return c1;
    }


}
