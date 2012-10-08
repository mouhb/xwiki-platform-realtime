/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package fr.loria.score.server;

import fr.loria.score.jupiter.JupiterAlg;
import fr.loria.score.jupiter.model.AbstractOperation;
import fr.loria.score.jupiter.model.Document;
import fr.loria.score.jupiter.model.Message;
import fr.loria.score.jupiter.transform.Transformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ServerJupiterAlg extends JupiterAlg {

    /* The logger to use for logging. */
    private Logger logger = LoggerFactory.getLogger(ServerJupiterAlg.class);
    
    private final List<Message> unsentMessages = new ArrayList<Message>();
    private final SortedSet<Message> causalOrderedMessages = new TreeSet<Message>(new Comparator<Message>() {
        @Override
        public int compare(Message m1, Message m2) { //+1  if o1 > o2, 0 if o1 equals o2, -1 if o1 < o2
            return m1.getState().getGeneratedMsgs() - m2.getState().getGeneratedMsgs();
        }
    });

    protected ServerJupiterAlg() {}

    public ServerJupiterAlg(Document document) {
        super(document);
    }

    public ServerJupiterAlg(Document document, Transformation transform) {
        super(document, transform);
    }

    @Override
    public void receive(Message receivedMsg) {
        doReceive(receivedMsg);
    }

    private void doReceive(Message receivedMsg) {
        // Ensure causality processing
        if (receivedMsg.getState().getGeneratedMsgs() > currentState.getReceivedMsgs()) {
            logger.debug(this + "\tEnsuring causal receival. Add " + receivedMsg + " to: " + causalOrderedMessages);
            causalOrderedMessages.add(receivedMsg);
        } else {
            super.receive(receivedMsg);
            for (Iterator<Message> it = causalOrderedMessages.iterator(); it.hasNext();) {
                Message m = it.next();
                if (m.getState().getGeneratedMsgs() == currentState.getReceivedMsgs()) {
                    super.receive(m);
                    it.remove();
                }
            }
        }
    }

    @Override
    protected void execute(Message m) {
        //Broadcasting to peers in the same editing session
        AbstractOperation op = m.getOperation();
        List<Integer> peersIdList = ClientServerCorrespondents.getInstance().getEditingSessions().get(m.getEditingSessionId());
        for (Integer peerId : peersIdList) {
            ServerJupiterAlg peerServer = ClientServerCorrespondents.getInstance().getCorrespondents().get(peerId);
            if (!peerServer.equals(this)) {
                logger.debug(this + "\tSend message " + m + " to server = " + peerServer);
                peerServer.generate(op);
            }
        }
    }

    @Override
    protected void send(Message m) {
        logger.debug(this + " Adding " + m + "to unsent buffer" + unsentMessages);
        synchronized (unsentMessages) {
            unsentMessages.add(new Message(m));
        }
    }

    public Message[] getMessages() {
        Message[] msgs;
        synchronized (unsentMessages) {
            msgs = unsentMessages.toArray(new Message[0]);
            unsentMessages.clear();
        }
        return msgs;
    }

    public Set<Message> getCausalOrderedMessages() {
        return new TreeSet<Message>(causalOrderedMessages);
    }
}

