package net.batchik.crdt.gossip;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import com.pinterest.quasar.thrift.TFiberSocket;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;

import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.*;

import java.io.IOException;
import java.util.Map;

class GossipThread extends Fiber<Void> {
    private static Logger log = Logger.getLogger(GossipThread.class.getName());
    private ParticipantStates states;
    private int sleepTime;

    GossipThread(ParticipantStates states, int sleepTime) {
        this.states = states;
        this.sleepTime = sleepTime;
    }

    @Override
    public Void run() throws SuspendExecution, InterruptedException {
        for (;;) {
            try {
                Fiber.sleep(sleepTime);
            } catch (InterruptedException ex) {
                log.warn("interrupted exception while sleeping...");
            }

            for (Peer peer : states.getPeers()) {

                // do not try and talk to yourself
                if (!peer.equals(states.getSelf())) {
                    log.debug("gossiping to " + peer);

                    try (TTransport tran = new TFramedTransport(TFiberSocket.open(peer.getAddress()))) {
                        tran.open();
                        TProtocol protocol = new TBinaryProtocol(tran);
                        GossipService.Client client = new GossipService.Client(protocol);
                        GossipResponse response = client.gossip(new GossipRequest(states.getInitialDigest()));

                        for (Digest digest : response.getDigests()) {
                            Peer otherPeer = states.getPeer(digest.getR());
                            log.info("received digest about peer: " + digest.getR());

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
                    } catch (IOException | TTransportException ex) {
                        log.warn("thrift transportation exception while opening socket to: " + peer.getAddress());
                    } catch (TException ex) {
                        log.warn("thrift regular exception while gossiping with: " + peer.getAddress());
                    }
                }
            }
        }
    }
}
