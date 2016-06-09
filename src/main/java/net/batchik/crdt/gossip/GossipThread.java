package net.batchik.crdt.gossip;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;

import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.util.Map;
import java.util.Set;

public class GossipThread extends Thread {
    static Logger log = Logger.getLogger(GossipThread.class.getName());
    ParticipantStates states;
    private int sleepTime;

    public GossipThread(ParticipantStates states, int sleepTime) {
        this.states = states;
        this.sleepTime = sleepTime;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ex) {
                log.warn("interrupted exception while sleeping...");
            }

            // loop through and talk to all other peers
            Set<Map.Entry<String, Peer>> peerEntries = states.getPeers().entrySet();
            Map.Entry<String, Peer>[] peerArray = peerEntries.toArray(new Map.Entry[peerEntries.size()]);

            for (Map.Entry<String, Peer> entry : peerArray) {
                String address = entry.getKey();
                Peer peer = entry.getValue();

                // do not try and talk to yourself
                if (!peer.getId().equals(states.getSelf().getId())) {
                    log.info("talking to " + peer);
                    String[] split = address.split("/")[1].split(":");
                    String hostname = split[0];
                    int port = Integer.parseInt(split[1]);

                    try (TTransport tran = new TFramedTransport(new TSocket(hostname, port))) {
                        tran.open();
                        TProtocol protocol = new TBinaryProtocol(tran);
                        GossipService.Client client = new GossipService.Client(protocol);
                        GossipResponse response = client.gossip(new GossipRequest(states.getInitialDigest()));

                        for (Digest digest : response.getDigests()) {
                            Peer otherPeer = states.getPeer(digest.getR());

                            Object value = null;
                            switch (digest.getType()) {
                                case GCCOUNTER:
                                    value = digest.getGCounter();
                                    break;
                                case PNCOUNTER:
                                    value = digest.getPNCounter();
                                    break;
                            }

                            otherPeer.getState().merge(digest.getK(), value, digest.getN());
                            states.getSelf().getState().merge(digest.getK(), value, digest.getN());
                        }
                    } catch (TTransportException ex) {
                        log.warn("thrift transportation exception while opening socket to: " + address);
                    } catch (TException ex) {
                        log.warn("thrift regular exception while gossiping with: " + address);
                    }
                }
            }
        }
    }
}
