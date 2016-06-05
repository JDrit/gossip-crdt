namespace java net.batchik.crdt.gossip

struct GCounter {
    2: list<i32> P;
}

struct PNCounter {
    2: list<i32> P;
    3: list<i32> N;
}

enum Type {
    GCCOUNTER,
    PNCOUNTER
}

/**
 * Captures a state of a participant for transport
 */
struct Digest {
    1: i32 r;     // the id of the participant
    2: string k;  // key in the map
    3: i64 n;     // the version number of the value
    4: Type type;
    5: optional GCounter gCounter;
    6: optional PNCounter pNCounter;
}



struct GossipRequest {
    1: map<i32, i64> max;
}

struct GossipResponse {
    1: i32 id;
    2: list<Digest> digests;
}

service GossipService {
    // When p and q start gossiping, they first exchange digests
    // {(r, max(μ_p (r))) | r ∈ P} and {(r, max(μ_q (r))) | r ∈ P}
    // map<i32, i64> initial(1: map<i32, i64> request);

    // On receipt, p sends to q
    // {(r, k,v, n) | μ_p(r)(k) = (v, n) ∧ n > max(μ_q (r))}
    // list<Digest> digests(1: i32 qId, 2: list<Digest> digests);

    GossipResponse gossip(1: GossipRequest request);
}