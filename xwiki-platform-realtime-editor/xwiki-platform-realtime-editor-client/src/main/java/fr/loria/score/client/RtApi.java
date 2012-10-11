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
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.TextArea;
import fr.loria.score.jupiter.plain.PlainDocument;
import fr.loria.score.jupiter.plain.operation.DeleteOperation;
import fr.loria.score.jupiter.plain.operation.InsertOperation;
import fr.loria.score.jupiter.plain.operation.Operation;
import org.xwiki.gwt.dom.client.JavaScriptObject;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.internal.DefaultConfig;

import java.util.logging.Logger;

public class RtApi {
    public static final String DOCUMENT_ID = "documentId";

    private Editor editor;
    private JsBundle bundle = GWT.create(JsBundle.class);

    private CommunicationServiceAsync comService = CommunicationService.ServiceHelper.getCommunicationService();
    private ClientJupiterAlg clientJupiter = new ClientJupiterAlg(new PlainDocument(""));

    private static final Logger logger = Logger.getLogger(RtApi.class.getName());

    /**
     * Publishes the RT editor API.
     */
    public static native void  publish()/*-{
          $wnd.RtApi = function(cfg) {
            if(typeof cfg == 'object') {
                this.instance = @fr.loria.score.client.RtApi::new(Lorg/xwiki/gwt/dom/client/JavaScriptObject;)(cfg);
            }
          }
    }-*/;

    public RtApi(JavaScriptObject jsConfig) {
        // and set the caret at pos 0
        Config config = new DefaultConfig(jsConfig);

        // Get the text area element
        Element htmlTextAreaElement = DOM.getElementById(config.getParameter("hookId"));
        if (htmlTextAreaElement == null) {
            return;
        }

        if (htmlTextAreaElement.getTagName().equalsIgnoreCase("textarea")) {
            int width = 500;
            int height = 210;

            TextArea tArea = TextArea.wrap(htmlTextAreaElement);
            height = tArea.getOffsetHeight();
            width = tArea.getOffsetWidth();

            Element canvasEl = DOM.createElement("canvas");
            canvasEl.setId("editor");
            canvasEl.setPropertyInt("width", width);
            canvasEl.setPropertyInt("height", height);

            com.google.gwt.dom.client.Element parentElem = htmlTextAreaElement.getParentElement();
            parentElem.insertFirst(canvasEl);
            parentElem.removeChild(htmlTextAreaElement);

            injectJSFilesForRTEditor(parentElem);

            editor = Editor.getEditor();
            editor.addHooksToEventListeners(new EditorApi());

            clientJupiter.setCommunicationService(comService);
            clientJupiter.setDocument(new PlainDocument(tArea.getText()));
            clientJupiter.setEditingSessionId(Long.valueOf(config.getParameter(DOCUMENT_ID)));
            clientJupiter.setCallback(new ClientCallback.PlainClientCallback(editor));
            clientJupiter.connect();
        }
    }

    private void injectJSFilesForRTEditor(com.google.gwt.dom.client.Element parentElem) {
        ScriptElement u1 = createScriptElement();
        u1.setText(bundle.jquery().getText());
        parentElem.appendChild(u1);

        ScriptElement u2 = createScriptElement();
        u2.setText(bundle.theme().getText());
        parentElem.appendChild(u2);

        ScriptElement u3 = createScriptElement();
        u3.setText(bundle.utils().getText());
        parentElem.appendChild(u3);

        ScriptElement u4 = createScriptElement();
        u4.setText(bundle.keys().getText());
        parentElem.appendChild(u4);

        ScriptElement u5 = createScriptElement();
        u5.setText(bundle.clipboard().getText());
        parentElem.appendChild(u5);

        ScriptElement u6 = createScriptElement();
        u6.setText(bundle.history().getText());
        parentElem.appendChild(u6);

        ScriptElement u7 = createScriptElement();
        u7.setText(bundle.cursor().getText());
        parentElem.appendChild(u7);

        ScriptElement u8 = createScriptElement();
        u8.setText(bundle.editor().getText());
        parentElem.appendChild(u8);

        ScriptElement u9 = createScriptElement();
        u9.setText(bundle.model().getText());
        parentElem.appendChild(u9);

        ScriptElement u10 = createScriptElement();
        u10.setText(bundle.model().getText());
        parentElem.appendChild(u10);

        ScriptElement u11 = createScriptElement();
        u11.setText(bundle.parser().getText());
        parentElem.appendChild(u11);

        ScriptElement u12 = createScriptElement();
        u12.setText(bundle.initEditor().getText());
        parentElem.appendChild(u12);
    }

    private static ScriptElement createScriptElement() {
        ScriptElement script = Document.get().createScriptElement();
        script.setAttribute("language", "javascript");
        return script;
      }

    //EDITOR API
    class EditorApi {
     /**
     * On insertion/deletion, the JavaScript editor generates an insert/delete operation which is then sent to server
     * @param s the inserted string(split in chars sequence)/character
     * @param position the insertion position
     */
        public void clientInsert(String s, int position) {
            if (s.length() > 1) {
                char [] charSeq = s.toCharArray();
                for (int i = 0; i < charSeq.length; i++) {
                    clientInsert(charSeq[i], position + i);
                }
            } else if (s.length() == 1){
              clientInsert(s.charAt(0), position);
            }
        }

        public void clientInsert(char c, int position) {
            Operation op = new InsertOperation(clientJupiter.getSiteId(), position, c);
            clientJupiter.generate(op);
        }

        public void clientDelete(int pos) {
            Operation op = new DeleteOperation(clientJupiter.getSiteId(), pos);
            clientJupiter.generate(op);
        }

        public void clientDelete(int from, int to) {
            for (int i = to - 1; i >= from; i--) { // from index is inclusive, to is exclusive, as the end selection idx is positioned at the next position
                clientDelete(i);
            }
        }

        public void clientQuitsEditingSession() {
            clientJupiter.disconnect();
        }
    }

    interface JsBundle extends ClientBundle {
        @Source("jquery-1.4.3.min.js")
        TextResource jquery();

        @Source("theme.js")
        TextResource theme();

        @Source("utils.js")
        TextResource utils();

        @Source("keys.js")
        TextResource keys();

        @Source("clipboard.js")
        TextResource clipboard();

        @Source("history.js")
        TextResource history();

        @Source("cursor.js")
        TextResource cursor();

        @Source("editor.js")
        TextResource editor();

        @Source("model.js")
        TextResource model();

        @Source("parser.js")
        TextResource parser();

        @Source("init-editor.js")
        TextResource initEditor();
    }
}
