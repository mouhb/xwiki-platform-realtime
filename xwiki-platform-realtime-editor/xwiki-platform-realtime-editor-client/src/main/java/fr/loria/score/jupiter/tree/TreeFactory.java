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
package fr.loria.score.jupiter.tree;

/**
 * Factory method for creating various types of Tree's: paragraphs, texts aso.
 * This is the recommended way to create instances of Tree and not the constructor,even in this package
 *
 * @author Bogdan.Flueras@inria.fr
 */
public class TreeFactory {

    /**
     * @return an empty tree
     */
    public static Tree createEmptyTree() {
        Tree t = new Tree();
        t.setNodeName("");
        t.setAttribute(Tree.NODE_TYPE, String.valueOf(Tree.ELEMENT_NODE));
        return t;
    }

    /**
     * @return a paragraph tree
     */
    public static Tree createParagraphTree() {
        Tree t = createElementTree("p");
        t.setValue(null);
        return t;
    }

    /**
     * @return a heading tree
     */
    public static Tree createHeadingTree(int level) {
        Tree t = createElementTree("h" + level);
        t.setValue(null);
        return t;
    }

    /**
     * @return a horizontal rule tree
     */
    public static Tree createHorizontalRuleTree() {
        Tree t = createElementTree("hr");
        t.setValue(null);
        return t;
    }
    
    /**
     * @param text the value of this text tree
     * @return a text tree
     */
    public static Tree createTextTree(String text) {
        Tree t = createEmptyTree();
        t.setAttribute(Tree.NODE_TYPE, String.valueOf(Tree.TEXT_NODE));
        t.setNodeName("#text");
        t.setValue(text);
        return t;
    }

    /**
     * @param url the URL for the link
     * @param labelText the text for the URL
     * @return a link tree with a text tree child
     */
    public static Tree createLink(String url, String labelText) {  // todo: where it is used?
        Tree link = createElementTree("a");
        link.setAttribute("href", url);
        link.addChild(createTextTree(labelText));
        return link;
    }

    public static Tree createElementTree(String tagName) {
        Tree t = createEmptyTree();
        t.setNodeName(tagName);
        return t;
    }
}
