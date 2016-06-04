namespace java net.batchik.crdt.gossip

/**
 * Captures a state of a participant for transport
 */
struct Digest {
    1: i64 r;     // the id of the participant
    2: string k;  // key in the map
    3: binary v;  // the value in the map
    4: i64 n;     // the version number of the value
}

service GossipService {
    // When p and q start gossiping, they first exchange digests
    // {(r, max(μ_p (r))) | r ∈ P} and {(r, max(μ_q (r))) | r ∈ P}
    // map<i32, i64> initial(1: map<i32, i64> request);

    // On receipt, p sends to q
    // {(r, k,v, n) | μ_p(r)(k) = (v, n) ∧ n > max(μ_q (r))}
    // list<Digest> digests(1: i32 qId, 2: list<Digest> digests);

    list<Digest> gossip(1: map<i32, i64> max);
}