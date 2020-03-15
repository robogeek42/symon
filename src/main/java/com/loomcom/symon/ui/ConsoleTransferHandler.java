package com.loomcom.symon.ui;

import java.io.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import javax.swing.*;
//import com.loomcom.symon.ui.*;
import com.loomcom.symon.ui.Console;

public class ConsoleTransferHandler extends TransferHandler {

    /**
     * Perform the actual data import.
     */
    public boolean importData(TransferHandler.TransferSupport info) {
        String data = null;

        //If we can't handle the import, bail now.
        if (!canImport(info)) {
            return false;
        }

        Console console = (Console)info.getComponent();
        //DefaultConsoleModel model = (DefaultListModel)list.getModel();
        //Fetch the data -- bail if this fails
        try {
            data = (String)info.getTransferable().getTransferData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException ufe) {
            System.out.println("importData: unsupported data flavor");
            return false;
        } catch (IOException ioe) {
            System.out.println("importData: I/O exception");
            return false;
        }

        if (!info.isDrop()) { //This is a paste
			//console.writeString(data);
            for (int i=0; i<data.length(); i++)
            {
                console.writeChar(data.charAt(i));
                //console.print(data.charAt(i));
                //console.repaint();
                /*
                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    System.out.println("Interrupted "+e);
                }
                */
            }

            return true;
        }
        else {
            return false;
        }
    }


    /**
     * We only support importing strings.
     */
    public boolean canImport(TransferHandler.TransferSupport support) {
        // we only import Strings
        if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return false;
        }
        return true;
    }
}

