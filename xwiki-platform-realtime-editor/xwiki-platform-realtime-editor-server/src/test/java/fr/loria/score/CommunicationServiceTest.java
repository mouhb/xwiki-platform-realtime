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
package fr.loria.score;

import fr.loria.score.client.ClientDTO;
import fr.loria.score.client.ClientJupiterAlg;
import fr.loria.score.client.CommunicationService;
import fr.loria.score.jupiter.model.Message;
import fr.loria.score.jupiter.plain.PlainDocument;
import fr.loria.score.server.ClientServerCorrespondents;
import fr.loria.score.server.CommunicationServiceImpl;
import fr.loria.score.server.ServerJupiterAlg;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * @author Bogdan Flueras (email: Bogdan.Flueras@inria.fr)
 */
public class CommunicationServiceTest {
    public static final int NR_CLIENTS = 6;
    public static final int NR_MESSAGES = 25;
    public static final int NR_SESSIONS = 1;

    private static final Logger LOG = LoggerFactory.getLogger(CommunicationServiceTest.class);
    
    private CommunicationService communicationService = new CommunicationServiceImpl();

    @Before
    public void setUp() {
        Map<Integer, List<Integer>> editingSessions = ClientServerCorrespondents.getInstance().getEditingSessions();
        assertEquals("Invalid initial size for editing sessions", 0, editingSessions.size());
        Map<Integer, ServerJupiterAlg> correspondents = ClientServerCorrespondents.getInstance().getCorrespondents();
        assertEquals("Invalid initial size for client-server correspondents sessions", 0, correspondents.size());
    }

    @After
    public void tearDown() {
        ClientServerCorrespondents.getInstance().getEditingSessions().clear();
        ClientServerCorrespondents.getInstance().getCorrespondents().clear();
    }

    @Test
    public void generateClientId() {
        int expected = 0;

        int id = communicationService.generateClientId();
        assertEquals("Invalid id generated", expected, id);

        id=communicationService.generateClientId();
        assertEquals("Invalid id generated", ++expected, id);
    }

    @Test
    public void generateClientIdFromMultipleThreads() {
        /* Run the test only if explicitly requested on the command line by passing a system property */
        if (System.getProperty(TestUtils.RUN_STRESS_TESTS_FLAG) == null) {
            TestUtils.warnSkipped(LOG, TestUtils.RUN_STRESS_TESTS_FLAG);
            return;
        }
        
        final CountDownLatch startSignal = new CountDownLatch(1);
        final CountDownLatch endSignal = new CountDownLatch(NR_CLIENTS);
        Executor executor = Executors.newFixedThreadPool(NR_CLIENTS);

        final Set<Integer> clientIds = Collections.synchronizedSet(new TreeSet<Integer>());
        for (int i = 0; i < NR_CLIENTS; i++) {
            executor.execute(new Runnable() {
                public void run() {
                    try {
                        startSignal.await();
                        clientIds.add(communicationService.generateClientId());
                    } catch (InterruptedException e) {
                        fail(e.getMessage());
                    } finally {
                        endSignal.countDown();
                    }
                }
            });
        }

        startSignal.countDown();
        try {
            endSignal.await();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertEquals("Invalid nr of clients", NR_CLIENTS, clientIds.size());

        for (int i = 0; i < NR_CLIENTS; i++) {
            assertTrue("Invalid id generated", clientIds.contains(i));
        }
    }

    @Test
    public void createServerPairForClient() {
        int siteId = 1;
        int sessionId = 47;

        ClientJupiterAlg client = new ClientJupiterAlg(new PlainDocument("foo"));
        client.setSiteId(siteId);
        client.setEditingSessionId(sessionId);

        String actualData = communicationService.createServerPairForClient(new ClientDTO(client)).getContent();
        assertEquals("Wrong data received when creating server pair for client", "foo", actualData);

        Map<Integer, ServerJupiterAlg> correspondents = ClientServerCorrespondents.getInstance().getCorrespondents();
        assertNotNull(correspondents);
        assertEquals("Invalid correspondents size", 1, correspondents.size());
        assertEquals("Invalid data for server correspondent", "foo", correspondents.get(siteId).getDocument().getContent());
        assertEquals("Invalid siteId for server correspondent", siteId, correspondents.get(siteId).getSiteId());

        Map<Integer, List<Integer>> sessions = ClientServerCorrespondents.getInstance().getEditingSessions();
        assertNotNull(sessions);
        assertEquals("Invalid editing session size", 1, sessions.size());
        assertEquals("Invalid siteId for sessionId:" + sessionId, (Object) siteId, sessions.get(sessionId).get(0));
    }

    @Test
    public void createServerPairForClientMultipleThreads() {
        /* Run the test only if explicitly requested on the command line by passing a system property */
        if (System.getProperty(TestUtils.RUN_STRESS_TESTS_FLAG) == null) {
            TestUtils.warnSkipped(LOG, TestUtils.RUN_STRESS_TESTS_FLAG);
            return;
        }
        
        //for each client it's corresponding server is created and is correctly mapped to editing session
        TestUtils.createServerPairs(NR_CLIENTS, 1, communicationService, 0);
        Map<Integer, ServerJupiterAlg> correspondents = ClientServerCorrespondents.getInstance().getCorrespondents();
        assertEquals("Invalid nr of server pairs", NR_CLIENTS, correspondents.size());

        for (Iterator<ServerJupiterAlg> it = correspondents.values().iterator(); it.hasNext();) {
            ServerJupiterAlg server = it.next();
            assertNotNull("Server cannot be null", server);
            assertEquals("Invalid server data", "", server.getDocument().getContent());
        }

        Map<Integer, List<Integer>> sessions = ClientServerCorrespondents.getInstance().getEditingSessions();
        assertEquals(NR_CLIENTS, sessions.get(0).size());
    }

    @Test
    public void removeServerPairForClient() {
        Map<Integer, List<Integer>> editingSessions;
        Map<Integer, ServerJupiterAlg> correspondents;

        //add it
        int siteId = 10;
        int sessionId = 47;
        ClientDTO client = new ClientDTO("", siteId, sessionId);
        communicationService.createServerPairForClient(client);

		editingSessions = ClientServerCorrespondents.getInstance().getEditingSessions();
        assertEquals("Invalid editing session size", 1, editingSessions.size());
        assertTrue("Invalid editing session id", editingSessions.containsKey(sessionId));
        assertEquals("Site id mapped for editing session:", 1, editingSessions.get(sessionId).size());
        assertTrue("Site id exists for editing session:",  editingSessions.get(sessionId).contains(siteId));

        correspondents = ClientServerCorrespondents.getInstance().getCorrespondents();
        assertEquals("Invalid correspondents size", 1, correspondents.size());
        assertTrue("Invalid siteId", correspondents.containsKey(siteId));		

        //remove it
        communicationService.removeServerPairForClient(client);
        editingSessions = ClientServerCorrespondents.getInstance().getEditingSessions();
        assertEquals("Invalid nr of editing sessions", 0, editingSessions.size());

        correspondents = ClientServerCorrespondents.getInstance().getCorrespondents();
        assertEquals("Invalid correspondents size", 0, correspondents.size());
    }

    @Test
    public void removeServerPairsForClientsInDifferentSessions() {
        int esid1 = 500;
        ClientDTO client511 = new ClientDTO("", 11, esid1);
        ClientDTO client513 = new ClientDTO("", 13, esid1);

        communicationService.createServerPairForClient(client511);
        communicationService.createServerPairForClient(client513);


        int esid2 = 700;
        ClientDTO client709 = new ClientDTO("", 9, esid2);
        ClientDTO client725 = new ClientDTO("", 25, esid2);
        ClientDTO client759 = new ClientDTO("", 59, esid2);

        communicationService.createServerPairForClient(client709);
        communicationService.createServerPairForClient(client725);
        communicationService.createServerPairForClient(client759);


        // we have 2 sessions
        assertEquals("Invalid nr of editing sessions", 2, ClientServerCorrespondents.getInstance().getEditingSessions().size());

        // esid1 has 2 clients, esid2 has 3
        assertEquals("Invalid nr of clients for session 1", 2, ClientServerCorrespondents.getInstance().getEditingSessions().get(esid1).size());
        assertEquals("Invalid nr of clients for session 2", 3, ClientServerCorrespondents.getInstance().getEditingSessions().get(esid2).size());
        assertEquals("Invalid nr of server correspondents", 5, ClientServerCorrespondents.getInstance().getCorrespondents().size());

        // remove clients from esid2
        communicationService.removeServerPairForClient(client709);
        // we still have 2 sessions
        assertEquals("Invalid nr of editing sessions", 2, ClientServerCorrespondents.getInstance().getEditingSessions().size());
        assertEquals("Invalid nr of clients for session 1", 2, ClientServerCorrespondents.getInstance().getEditingSessions().get(esid1).size());
        assertEquals("Invalid nr of clients for session 2", 2, ClientServerCorrespondents.getInstance().getEditingSessions().get(esid2).size());
        assertEquals("Invalid nr of server correspondents", 4, ClientServerCorrespondents.getInstance().getCorrespondents().size());

        communicationService.removeServerPairForClient(client759);
        assertEquals("Invalid nr of editing sessions", 2, ClientServerCorrespondents.getInstance().getEditingSessions().size());
        assertEquals("Invalid nr of clients for session 1", 2, ClientServerCorrespondents.getInstance().getEditingSessions().get(esid1).size());
        assertEquals("Invalid nr of clients for session 2", 1, ClientServerCorrespondents.getInstance().getEditingSessions().get(esid2).size());
        assertEquals("Invalid nr of server correspondents", 3, ClientServerCorrespondents.getInstance().getCorrespondents().size());

        communicationService.removeServerPairForClient(client511);
        assertEquals("Invalid nr of editing sessions", 2, ClientServerCorrespondents.getInstance().getEditingSessions().size());
        assertEquals("Invalid nr of clients for session 1", 1, ClientServerCorrespondents.getInstance().getEditingSessions().get(esid1).size());
        assertEquals("Invalid nr of clients for session 2", 1, ClientServerCorrespondents.getInstance().getEditingSessions().get(esid2).size());
        assertEquals("Invalid nr of server correspondents", 2, ClientServerCorrespondents.getInstance().getCorrespondents().size());

        communicationService.removeServerPairForClient(client513);
        assertEquals("Invalid nr of editing sessions", 1, ClientServerCorrespondents.getInstance().getEditingSessions().size());
        assertEquals("Invalid nr of clients for session 2", 1, ClientServerCorrespondents.getInstance().getEditingSessions().get(esid2).size());
        assertEquals("Invalid nr of server correspondents", 1, ClientServerCorrespondents.getInstance().getCorrespondents().size());
    }

    /**
     * Ensures that server correctly receives and handles all client messages
     */
    @Test
    public void serverReceive() {
        /* Run the test only if explicitly requested on the command line by passing a system property */
        if (System.getProperty(TestUtils.RUN_STRESS_TESTS_FLAG) == null) {
            TestUtils.warnSkipped(LOG, TestUtils.RUN_STRESS_TESTS_FLAG);
            return;
        }
        
        //create servers
        TestUtils.createServerPairs(NR_CLIENTS, 1, communicationService, 0);
        TestUtils.sendMessagesToServer(NR_CLIENTS, NR_MESSAGES, NR_SESSIONS, communicationService);

        // now check if servers have the same document & state at quiescence
        Collection<ServerJupiterAlg> servers = ClientServerCorrespondents.getInstance().getCorrespondents().values();
        ServerJupiterAlg previous = null;

        for (Iterator<ServerJupiterAlg> it = servers.iterator(); it.hasNext(); ) {
            ServerJupiterAlg s = it.next();

            if (previous != null) {
                assertEquals("Inconsistent final document among servers. Server1:" + previous + ", Server2:" + s, previous.getDocument().getContent(), s.getDocument().getContent());
                assertEquals("Inconsistent final state among servers", previous.getCurrentState(), s.getCurrentState());
            }
            previous = s;
        }

        //a new client joins and should receive the existing content
        Integer id = communicationService.generateClientId();
        String clientContent = communicationService.createServerPairForClient(new ClientDTO("", id, 0)).getContent();
        String serverContent = ClientServerCorrespondents.getInstance().getCorrespondents().get(id).getDocument().getContent();
        assertEquals("Invalid client content joining an editing session", previous.getDocument().getContent(), clientContent);
        assertEquals("Invalid server content joining an editing session", previous.getDocument().getContent(), serverContent);
    }

    /**
     * Ensures that clients will eventually read all pending messages (sent by other server pair participants) under concurrency
     */
    @Test
    public void clientReceive() {
        TestUtils.createServerPairs(NR_CLIENTS, 1, communicationService, 0);
        Map<Integer, List<Message>> clientMessages = TestUtils.sendMessagesToServer(NR_CLIENTS, NR_MESSAGES, 1, communicationService);

        //for each client get the expected messages that it should receive
        Map<Integer, List<Message>> tmp = new HashMap<Integer, List<Message>>(clientMessages);
        final Map<Integer, Message[]> expectedMessages = new HashMap<Integer, Message[]>();
        for (int i = 0; i < NR_CLIENTS; i++) {
            tmp.remove(i);
            Collection<List<Message>> coll = tmp.values();
            List<Message> msgs = new ArrayList<Message>();
            for (List<Message> messages : coll) {
                for (Message m: messages) {
                    msgs.add(m);
                }
            }
            expectedMessages.put(i, msgs.toArray(new Message[]{}));
            tmp = new HashMap<Integer, List<Message>>(clientMessages);
        }

        //now each client needs to have all the messages sent by the other
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch end = new CountDownLatch(NR_CLIENTS);
        final Executor executor = Executors.newFixedThreadPool(NR_CLIENTS);

        final AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < NR_CLIENTS; i++) {
            executor.execute(new Runnable() {
                public void run() {
                    try {
                        start.await();
                        int increment = count.getAndIncrement();
                        //messages == all messages received by a client (in the queue waiting to be fetched)
                        Message[] actualMsgs = communicationService.clientReceive(increment);
                        Message[] expectedMsgs = expectedMessages.get(increment);
                        assertEquals("Unexpected messages in queue", expectedMsgs.length, actualMsgs.length);
                        //don't test the actualMsgs vs expectedMsgs as the operation position will be randomly shifted
                        //upon receival
                    } catch (InterruptedException e) {
                        fail(e.getMessage());
                    } finally {
                        end.countDown();
                    }
                }
            });
        }
        start.countDown();
        try {
            end.await();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void initClients() {
        Map<Integer, List<Integer>> editingSessions;
        Map<Integer, ServerJupiterAlg> correspondents;

        //add it
        int siteId = 0;
        int sessionId = -1874759369;
        ClientDTO client = new ClientDTO("foo", siteId, sessionId);
        ClientDTO client1 = new ClientDTO("", siteId + 1, sessionId);

        client = communicationService.initClient(client);
        client1 = communicationService.initClient(client1);
        assertEquals("foo",client1.getDocument().getContent());

		editingSessions = ClientServerCorrespondents.getInstance().getEditingSessions();
        assertEquals("Invalid editing session size", 1, editingSessions.size());
        assertTrue("Invalid editing session id", editingSessions.containsKey(sessionId));
        assertEquals("Site id mapped for editing session:", 2, editingSessions.get(sessionId).size());
        assertTrue("Site id exists for editing session:",  editingSessions.get(sessionId).contains(siteId));
        assertTrue("Site id exists for editing session:",  editingSessions.get(sessionId).contains(siteId + 1));

        correspondents = ClientServerCorrespondents.getInstance().getCorrespondents();
        assertEquals("Invalid correspondents size", 2, correspondents.size());
        assertTrue("Invalid siteId", correspondents.containsKey(siteId));
        assertTrue("Invalid siteId", correspondents.containsKey(siteId+1));

        //remove it
        communicationService.removeServerPairForClient(client);
        editingSessions = ClientServerCorrespondents.getInstance().getEditingSessions();
        assertEquals("Invalid nr of editing sessions", 1, editingSessions.size());

        correspondents = ClientServerCorrespondents.getInstance().getCorrespondents();
        assertEquals("Invalid correspondents size", 1, correspondents.size());

        communicationService.removeServerPairForClient(client1);
        editingSessions = ClientServerCorrespondents.getInstance().getEditingSessions();
        assertEquals("Invalid nr of editing sessions", 0, editingSessions.size());

        correspondents = ClientServerCorrespondents.getInstance().getCorrespondents();
        assertEquals("Invalid correspondents size", 0, correspondents.size());

        ClientDTO client2 = new ClientDTO("", 5, sessionId);
        client2 = communicationService.initClient(client2);
        assertEquals("", client2.getDocument().getContent());
    }
}