/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package us.wardware.firstfruits.ui;

/*
 * ListTransferHandler.java is used by the DropDemo example.
 */
import javax.swing.*;

import java.awt.datatransfer.*;
import java.util.List;

public class ListTransferHandler extends TransferHandler
{
    private int[] indices = null;

    public boolean canImport(TransferHandler.TransferSupport info)
    {
        // Check for String flavor
        if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return false;
        }
        return true;
    }

    protected Transferable createTransferable(JComponent c)
    {
        return new StringSelection(exportString(c));
    }

    public int getSourceActions(JComponent c)
    {
        return TransferHandler.COPY_OR_MOVE;
    }

    public boolean importData(TransferHandler.TransferSupport info)
    {
        if (!info.isDrop()) {
            return false;
        }

        @SuppressWarnings("unchecked")
        JList<String> list = (JList<String>) info.getComponent();
        DefaultListModel<String> listModel = (DefaultListModel<String>) list.getModel();
        JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
        int index = dl.getIndex();
        boolean insert = dl.isInsert();

        // Get the string that is being dropped.
        Transferable t = info.getTransferable();
        String data;
        try {
            data = (String) t.getTransferData(DataFlavor.stringFlavor);
        } catch (Exception e) {
            return false;
        }

        // Perform the actual import.
        if (insert) {
            listModel.add(index, data);
        } else {
            listModel.set(index, data);
        }
        return true;
    }

    protected void exportDone(JComponent c, Transferable data, int action)
    {
        cleanup(c, action == TransferHandler.MOVE);
    }

    // Bundle up the selected items in the list
    // as a single string, for export.
    protected String exportString(JComponent c)
    {
        JList<?> list = (JList<?>) c;
        indices = list.getSelectedIndices();

        List<?> values = list.getSelectedValuesList();

        StringBuffer buff = new StringBuffer();

        for (int i = 0; i < values.size(); i++) {
            Object val = values.get(i);
            buff.append(val == null ? "" : val.toString());
            if (i != values.size() - 1) {
                buff.append("\n");
            }
        }

        return buff.toString();
    }

    // Take the incoming string and wherever there is a
    // newline, break it into a separate item in the list.
    protected void importString(JComponent c, String str)
    {
        @SuppressWarnings("unchecked")
        JList<String> target = (JList<String>) c;
        DefaultListModel<String> listModel = (DefaultListModel<String>) target.getModel();
        int index = target.getSelectedIndex();

        // Prevent the user from dropping data back on itself.
        // For example, if the user is moving items #4,#5,#6 and #7 and
        // attempts to insert the items after item #5, this would
        // be problematic when removing the original items.
        // So this is not allowed.
        if (indices != null && index >= indices[0] - 1 && index <= indices[indices.length - 1]) {
            indices = null;
            return;
        }

        int max = listModel.getSize();
        if (index < 0) {
            index = max;
        } else {
            index++;
            if (index > max) {
                index = max;
            }
        }
        String[] values = str.split("\n");
        for (int i = 0; i < values.length; i++) {
            listModel.add(index++, values[i]);
        }
    }

    // If the remove argument is true, the drop has been
    // successful and it's time to remove the selected items
    // from the list. If the remove argument is false, it
    // was a Copy operation and the original list is left
    // intact.
    protected void cleanup(JComponent c, boolean remove)
    {
        if (remove && indices != null) {
            JList<?> source = (JList<?>) c;
            DefaultListModel<?> model = (DefaultListModel<?>) source.getModel();
            model.remove(source.getSelectedIndex());
        }
        indices = null;
    }
}
