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
import fr.loria.score.client.CommunicationService;
import fr.loria.score.server.ClientServerCorrespondents;
import fr.loria.score.server.CommunicationServiceImpl;
import fr.loria.score.server.ServerJupiterAlg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * User: Bogdan Flueras (email: Bogdan.Flueras@inria.fr)
 */
public class MultipleEditingSessionsTest {
    private static final Logger LOG = LoggerFactory.getLogger(MultipleEditingSessionsTest.class);
    
    private static final int NR_CLIENTS = 14;
    private static final int NR_SESSIONS = 7;
    private static final int NR_MESSAGES = 10;

    private CommunicationService commService;

    @Before
    public void setUp() throws Exception {
        commService = new CommunicationServiceImpl();

        TestUtils.createServerPairs(NR_CLIENTS, NR_SESSIONS, commService, 0);
        TestUtils.sendMessagesToServer(NR_CLIENTS, NR_MESSAGES, NR_SESSIONS, commService);
    }

    @After
    public void tearDown() throws Exception {
        ClientServerCorrespondents.getInstance().getEditingSessions().clear();
        ClientServerCorrespondents.getInstance().getCorrespondents().clear();
    }

    /**
     * Test that:
     * 1) servers in same editing session have same data
     * 2) servers in different editing sessions have different data
     */
    @Test
    public void testEditingSessions() throws Exception {
        /* Run the test only if explicitly requested on the command line by passing a system property */
        if (System.getProperty(TestUtils.RUN_STRESS_TESTS_FLAG) == null) {
            TestUtils.warnSkipped(LOG, TestUtils.RUN_STRESS_TESTS_FLAG);
            return;
        }
        
        List<ServerJupiterAlg> serversInDifferentSessions = new ArrayList<ServerJupiterAlg>();
        ServerJupiterAlg previous = null;

        // now check if servers in same editing session have the same data & state at quiescence
        for (Map.Entry<Long, List<Integer>> e : ClientServerCorrespondents.getInstance().getEditingSessions().entrySet()) {
            List<Integer> serverIds = e.getValue();
            List<ServerJupiterAlg> servers = new ArrayList<ServerJupiterAlg>();
            for (Integer serverId : serverIds) {
                servers.add(ClientServerCorrespondents.getInstance().getCorrespondents().get(serverId));
            }

            serversInDifferentSessions.add(servers.get(0));

            previous = null;
            for (Iterator<ServerJupiterAlg> it = servers.iterator(); it.hasNext(); ) {
                ServerJupiterAlg s = it.next();
                if (previous != null) {
                    assertEquals("Inconsistent document among servers in same editing session. Server1:" + previous + ", Server2:" + s, previous.getDocument().getContent(), s.getDocument().getContent());
                    assertEquals("Inconsistent state among servers in same editing sesson", previous.getCurrentState(), s.getCurrentState());
                }
                previous = s;
            }
        }

        //check if servers in different editing sessions have different data
        previous = null;
        for (Iterator<ServerJupiterAlg> it = serversInDifferentSessions.iterator(); it.hasNext(); ) {
            ServerJupiterAlg s = it.next();
            if (previous != null) {
                assertTrue("Servers in different editing session have same document!. Server1:" + previous + ", Server2:" + s, !previous.getDocument().equals(s.getDocument()));
            }
            previous = s;
        }
    }

    /**
     * Scenario: There is a current editing session. A new client joins the same session and he receives the existing content
     */
    @Test
    public void testNewClientJoinsSameEditingSession() throws Exception {
        /* Run the test only if explicitly requested on the command line by passing a system property */
        if (System.getProperty(TestUtils.RUN_STRESS_TESTS_FLAG) == null) {
            TestUtils.warnSkipped(LOG, TestUtils.RUN_STRESS_TESTS_FLAG);
            return;
        }
        
        ClientDTO client = new ClientDTO("", NR_CLIENTS + 1, NR_CLIENTS +1 % NR_SESSIONS);
        int esid = client.getSiteId() % NR_SESSIONS;
        client.setEditingSessionId(esid);
        String content = commService.createServerPairForClient(client).getContent();

        String expected = ClientServerCorrespondents.getInstance().getCorrespondents().get(
                        ClientServerCorrespondents.getInstance().getEditingSessions().get(esid).get(0)
        ).getDocument().getContent();

        assertNotNull("Received content should not be null for client joining existing editing session", content);
        assertEquals("Client received wrong content when joining existing editing session", content, expected);
    }

    /**
     * Scenario: There is a current editing session. A new client joins new session and he receives empty string
     */
    @Test
    public void testNewClientJoinsInDifferentEditingSession() throws Exception {
        /* Run the test only if explicitly requested on the command line by passing a system property */
        if (System.getProperty(TestUtils.RUN_STRESS_TESTS_FLAG) == null) {
            TestUtils.warnSkipped(LOG, TestUtils.RUN_STRESS_TESTS_FLAG);
            return;
        }
        
        ClientDTO client = new ClientDTO("", NR_CLIENTS + 1, Integer.MAX_VALUE);
        String expected = client.getDocument().getContent();
//        ((MockCommunicationService)commService).getEditingSessions().put(Integer.MAX_VALUE, new ArrayList<Integer>());
//
//        String actual = commService.createServerPairForClient(client);
//        assertNotNull("Received content should not be null for client joining new editing session", actual);
//        assertEquals("Client received wrong content when joining new editing session", expected, actual);
    }
}
