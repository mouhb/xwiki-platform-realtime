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

import com.google.gwt.core.client.JavaScriptObject;

import java.util.logging.Logger;

public final class Editor extends JavaScriptObject {
    private static final Logger logger = Logger.getLogger(Editor.class.getName());
    private static int oldCaretPos;

    protected Editor() {}

    public static native Editor getEditor() /*-{
        return $wnd.editor;
    }-*/;

    public native void addHooksToEventListeners(RtApi.EditorApi api)/*-{

        $wnd.insertHook = function(str, position) {
            api.@fr.loria.score.client.RtApi.EditorApi::clientInsert(Ljava/lang/String;I)(str, position);
        };

        $wnd.enterHook = function(position, cartPos) {
            api.@fr.loria.score.client.RtApi.EditorApi::clientInsert(Ljava/lang/String;I)( "\n", position);
        };

        $wnd.deleteHook = function(position) {
            api.@fr.loria.score.client.RtApi.EditorApi::clientDelete(I)(position);
        };

        $wnd.deleteStringHook = function(from, end) {
            if (from > end) { // user could select starting from line end backwards
                api.@fr.loria.score.client.RtApi.EditorApi::clientDelete(II)(end, from);
            } else {
                api.@fr.loria.score.client.RtApi.EditorApi::clientDelete(II)(from, end);
            }
        };

        $wnd.tabHook = function(position) {
            api.@fr.loria.score.client.RtApi.EditorApi::clientInsert(Ljava/lang/String;I)("    ", position);
        };

        //Cancel
        $wnd.onCancelHook = function() {
            api.@fr.loria.score.client.RtApi.EditorApi::clientQuitsEditingSession()();
        }

        //Save&View
        $wnd.onSaveAndViewHook = function() {
            //save is handled into actionButtonsRT.js
            $wnd.onCancelHook();
        }

        $wnd.onSaveAndContinueHook = function() {
            //nothing to be done
        }
    }-*/;

    public native void setContent(String content) /*-{
        $wnd.editor.setContent(content);
    }-*/;

    public native void paint() /*-{
        $wnd.editor.paint();
    }-*/;

    /**
     * Local caret consistency with respect to remote operations
     */
    public native int getCaretPosition()/*-{
        return $wnd.editor.cursor.getPosition();
    }-*/;

    public int getOldCaretPos() {
        return this.oldCaretPos;
    }

    public void setOldCaretPos(int oldCaretPos) {
        this.oldCaretPos = oldCaretPos;
    }

    public native void setSiteId(int siteId)/*-{
        $wnd.editor.siteId = siteId;
    }-*/;

    public native int getSiteId()/*-{
        return $wnd.editor.siteId;
    }-*/;

    /**
     * Shifts left/right the UI caret.
     * N.B. the new content was setup before calling this method
     * @param position the position <strong> in the linear model</strong> at which the insert/remove op occurred
     */
    public native void shiftCaret(int position) /*-{
        var caret = $wnd.editor.cursor;
        caret.toPosition(position);
    }-*/;

    public native void prepareUI(int pos, int siteId, boolean remove) /*-{
       //$wnd.editor.updateHighlighter(pos, siteId, remove);//todo-bf: uncomment when js code is synched
    }-*/;

    public native void toggleHighlighting(boolean checked) /*-{
        $wnd.editor.showHighlighting = checked;
    }-*/;
}
