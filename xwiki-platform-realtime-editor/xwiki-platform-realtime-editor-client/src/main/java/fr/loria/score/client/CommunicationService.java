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
package fr.loria.score.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import fr.loria.score.jupiter.model.Document;
import fr.loria.score.jupiter.model.Message;
import org.xwiki.component.annotation.ComponentRole;

@ComponentRole
@RemoteServiceRelativePath("CommunicationService.gwtrpc")
public interface CommunicationService extends RemoteService {
    /**
     * Every new generated client gets an ID
     *
     * @deprecated Use initClient() instead
     */
    Integer generateClientId();

    /**
     * Each time a new client joins the editing session, it's corresponding server pair is created
     *
     * @param clientDTO@return the available content on which other clients are performing real time operations <b>if any</b>
     * @deprecated Use initClient() instead
     */
    Document createServerPairForClient(ClientDTO clientDTO);

    /**
     * Initializes on server side the client and then piggybacks it:<br/>
     * 1) generates the site id<br/>
     * 2) creates the corresponding server pair
     *
     * @param client the client DTO to be filled with document
     * @return the initialized client
     */
    ClientDTO initClient(ClientDTO client);

    /**
     * This method is regularly called by the client in an attempt to simulate 'server-push. Consider using WebSockets etc.
     * It fetches from the server the messages to be applied on client side.
     *
     * @param siteId   the id of the site
     */
    Message[] clientReceive(int siteId);

    /**
     * Client pushes the document to server, simulating a 'receive' from the server
     *
     * @param msg      the message to be 'received' by server, which is actually sent by the client
     */
    void serverReceive(Message msg);

    /**
     * As the client quits the real-time editing session, it's corresponding server pair is removed
     *
     * @param clientDTO the client DTO for whom to remove it's corresponding server pair
     */
    void removeServerPairForClient(ClientDTO clientDTO);

    static class ServiceHelper {
        private static CommunicationServiceAsync communicationService = GWT.create(CommunicationService.class);

        public static synchronized CommunicationServiceAsync getCommunicationService() {
            return communicationService;
        }
    }
}


