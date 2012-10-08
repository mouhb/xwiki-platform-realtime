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
package fr.loria.score.jupiter.transform;

import fr.loria.score.jupiter.model.AbstractOperation;
import fr.loria.score.jupiter.plain.operation.DeleteOperation;
import fr.loria.score.jupiter.plain.operation.InsertOperation;
import fr.loria.score.jupiter.plain.operation.Operation;
import fr.loria.score.jupiter.plain.operation.NoOperation;

/**
 * Ressel transformation implementation
 */
public class ResselTransformation extends Transformation {

     public AbstractOperation transform(AbstractOperation op1, AbstractOperation op2) {
        if (op1 instanceof InsertOperation) {
            if (op2 instanceof InsertOperation) {
                return handleInsertInsert((InsertOperation)op1, (InsertOperation)op2);
            } else if (op2 instanceof DeleteOperation) {
                return handleInsertDelete((InsertOperation)op1, (DeleteOperation)op2);
            } else { // op2 is NoOp
                return op1;
            }
        } else if (op1 instanceof DeleteOperation) {
            if (op2 instanceof InsertOperation) {
                return handleDeleteInsert((DeleteOperation)op1, (InsertOperation)op2);
            } else if (op2 instanceof DeleteOperation) {
                return handleDeleteDelete((DeleteOperation)op1, (DeleteOperation)op2);
            } else { // op2 is NoOp
                return op1;
            }
        } else { 
            // op1 is NoOp
            return op1;
        }
    }

    protected Operation handleInsertInsert(Operation op1, Operation op2) {
        int p1 = op1.getPosition();
        int p2 = op2.getPosition();

        int siteId1 = op1.getSiteId();
        int siteId2 = op2.getSiteId();

        InsertOperation i1 = (InsertOperation) op1;

        if ((p1 < p2) || ((p1 == p2) && (siteId1 < siteId2))) {
            return new InsertOperation(siteId1, p1, i1.getChr());
        } else {
            return new InsertOperation(siteId1, p1 + 1, i1.getChr());
        }
    }

    protected Operation handleInsertDelete(Operation op1, Operation op2) {
        InsertOperation i1 = (InsertOperation) op1;

        int p1 = op1.getPosition();
        int p2 = op2.getPosition();
        int siteId1 = op1.getSiteId();

        if (p1 <= p2) {
            return new InsertOperation(siteId1, p1, i1.getChr());
        } else {
            return new InsertOperation(siteId1, p1 - 1, i1.getChr());
        }
    }

    protected Operation handleDeleteInsert(Operation op1, Operation op2) {
        int p1 = op1.getPosition();
        int p2 = op2.getPosition();
        int siteId1 = op1.getSiteId();
        if (p1 < p2) {
            return new DeleteOperation(siteId1, p1);
        } else {
            return new DeleteOperation(siteId1, p1 + 1);
        }
    }

    protected Operation handleDeleteDelete(Operation op1, Operation op2) {
        int p1 = op1.getPosition();
        int p2 = op2.getPosition();
        int siteId1 = op1.getSiteId();

        if (p1 < p2) {
            return new DeleteOperation(siteId1, p1);
        } else if (p1 > p2) {
            return new DeleteOperation(siteId1, p1 - 1);
        } else {
            return new NoOperation();
        }
    }
}
