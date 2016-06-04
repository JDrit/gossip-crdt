package net.batchik.crdt.gossip;

import net.batchik.crdt.*;
import net.batchik.crdt.gossip.datatypes.Type;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;

import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.util.Map;

public class GossipThread extends Thread {
    static Logger log = Logger.getLogger(GossipThread.class.getName());
    ParticipantStates states;
    private final long SECOND = 1000;

    public GossipThread(ParticipantStates states) {
        this.states = states;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(5 * SECOND);
                for (Map.Entry<Integer, Peer> entry : states.getPeers().entrySet()) {
                    if (entry.getValue().getId() != states.getSelf().getId()) {
                        String hostname = entry.getValue().getAddress().getHostName();
                        log.info("connecting to : " + entry.getValue().getId());
                        int port = entry.getValue().getAddress().getPort();
                        TTransport transport = new TFramedTransport(new TSocket(hostname, port));
                        transport.open();
                        TProtocol protocol = new TBinaryProtocol(transport);
                        GossipService.Client client = new GossipService.Client(protocol);
                        GossipResponse response = client.gossip(new GossipRequest(states.getInitialDigest()));

                        for (Digest digest : response.getDigests()) {
                            log.debug("processing digest: " + digest);
                            Peer peer = states.getPeers().get(digest.getR());
                            log.debug("processing digest for peer: " + peer);
                            Type type = Type.deser(digest.bufferForV());
                            peer.getState().merge(response.getId(), digest.getK(), type, digest.getN());
                            states.getSelf().getState().merge(states.getSelf().getId(), digest.getK(), type, digest.getN());
                        }

                        transport.close();
                    }
                }
            } catch (InterruptedException ex) {
                log.warn("interrupted exception in thread", ex);
            } catch (TTransportException ex) {
                log.error("thrift transport exception", ex);
            } catch (TException ex) {
                log.error("thrift general exception while sending intial", ex);
            }
        }
    }
}
