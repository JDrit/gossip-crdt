package net.batchik.crdt.gossip.datatypes;

import net.batchik.crdt.gossip.PNCounter;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class PNCounterUtil {
    static Logger log = Logger.getLogger(PNCounterUtil.class.getName());

    public static PNCounter newPNCounter(int size, int id) {
       return null;
    }

    public static void increment(PNCounter counter, int id) {

    }

    public static void decrement(PNCounter counter, int id) {

    }

    public static int value(PNCounter counter) {
        return -1;
    }

    public static PNCounter merge(PNCounter c1, PNCounter c2) {
       return null;
    }
}
