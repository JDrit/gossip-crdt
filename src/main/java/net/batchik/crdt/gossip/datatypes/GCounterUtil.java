package net.batchik.crdt.gossip.datatypes;

import net.batchik.crdt.gossip.*;
import org.apache.log4j.Logger;

import java.util.*;

public class GCounterUtil {
    static Logger log = Logger.getLogger(GCounterUtil.class.getName());

    public static GCounter newGCounter(String id) {
        Map<String, Integer> P = new HashMap<>();
        P.put(id, 1);
        return new GCounter(P);
    }

    public static void increment(GCounter counter, String id) {
        log.debug("incrementing for " + id);
        Map<String, Integer> P = counter.getP();
        Integer currentValue = P.get(id);
        if (currentValue == null) {
            P.put(id, 1);
        } else {
            P.put(id, currentValue + 1);
        }
    }

    public static int value(GCounter counter) {
        int sum = 0;
        for (Integer  i : counter.getP().values()) {
            sum += i;
        }
        return sum;
    }

    public static GCounter merge(GCounter c1, GCounter c2) {
        Map<String, Integer> p1 = c1.getP();

        p1.putAll(c2.getP());

        for (Map.Entry<String, Integer> entry : c2.getP().entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();

            int currentValue = p1.get(key);
            if (value > currentValue) {
                p1.put(key, value);
            }
        }
        /*
        for (Map.Entry<String, Integer> entry : c2.getP().entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();
            Integer i = p1.get(key);
            if (i == null || i < value) {
                p1.put(key, value);
            }
        }
        */
        return c1;
    }


}
