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
package fr.loria.score.jupiter.tree.operation;

import fr.loria.score.jupiter.tree.Tree;
import fr.loria.score.jupiter.tree.TreeUtils;

public class TreeUpdateElement extends TreeOperation
{
    public String tag;

    public String value;

    public TreeUpdateElement()
    {
    }

    public TreeUpdateElement(int siteId, int[] path, String tag, String value)
    {
        this.setPath(path);
        this.setSiteId(siteId);
        this.tag = tag;
        this.value = value;
    }

    public TreeUpdateElement(int siteId, int position, int[] path, String tag, String value)
    {
        this(siteId, path, tag, value);
        this.position = position;
    }

    public String getValue()
    {
        return value;
    }

    @Override
    public void execute(Tree root)
    {
        Tree tree = root.getChildFromPath(path);
        tree.setAttribute(tag, value);
    }

    @Override
    protected TreeOperation handleTreeInsertText(TreeInsertText op1)
    {
        return op1;
    }

    @Override
    protected TreeOperation handleTreeDeleteText(TreeDeleteText op1)
    {
        return op1;
    }

    @Override
    protected TreeOperation handleTreeNewParagraph(TreeNewParagraph op1)
    {
        return op1;
    }

    @Override
    protected TreeOperation handleTreeMergeParagraph(TreeMergeParagraph op1)
    {
        return op1;
    }

    @Override
    protected TreeOperation handleTreeInsertParagraph(TreeInsertParagraph op1)
    {
        return op1;
    }

    @Override
    protected TreeOperation handleTreeDeleteTree(TreeDeleteTree op1)
    {
        return op1;
    }

    @Override
    protected TreeOperation handleTreeStyle(TreeStyle op1)
    {
        return op1;
    }

    @Override
    protected TreeOperation handleTreeMoveParagraph(TreeMoveParagraph op1)
    {
        return op1;
    }

    @Override
    protected TreeOperation handleTreeCaretPosition(TreeCaretPosition op1)
    {
        return op1;
    }

    @Override
    protected TreeOperation handleTreeMergeItem(TreeMergeItem op1)
    {
        return op1;
    }

    @Override
    protected TreeOperation handleTreeMoveItem(TreeMoveItem op1)
    {
        return op1;
    }

    @Override
    protected TreeOperation handleTreeNewItem(TreeNewItem op1)
    {
        return op1;
    }

    @Override
    protected TreeOperation handleTreeNewList(TreeNewList op1)
    {
        return op1;
    }

    @Override
    protected TreeOperation handleTreeSplitItem(TreeSplitItem op1)
    {
        return op1;
    }

    @Override
    protected TreeOperation handleTreeUpdateElement(TreeUpdateElement op1)
    {
        if(TreeUtils.diff(op1.path,path)){
            return op1;
        }    
        if (!(op1.tag.equals(tag)) || op1.siteId < siteId) {//no conflict between s1 and s2, or s1 win
            return op1;
        }
        return new TreeIdOp();
        
    }

    @Override public String toString()
    {
        return "TreeUpdateElement(" + super.toString() + ")";
    }
}