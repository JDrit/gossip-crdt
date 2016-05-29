package net.batchik.crdt;

import java.util.HashMap;

public class ParticipantStates {
    private HashMap<Long, IndividualState> states;
    private int maxVersion = 0;

    public ParticipantStates(long[] participants) {
        states = new HashMap<>(participants.length);
        for (long p : participants) {
            states.put(p, new IndividualState());
        }
    }

    public int getMaxVersion() {
        return maxVersion;
    }
}
