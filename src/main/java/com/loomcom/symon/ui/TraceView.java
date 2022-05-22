package com.loomcom.symon.ui;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import java.awt.*;
import java.io.*;

public class TraceView extends JFrame {
    private JEditorPane editorPane;
    private JScrollPane scrollPane;

    private static final Dimension MIN_SIZE       = new Dimension(356, 240);
    private static final Dimension PREFERRED_SIZE = new Dimension(720, 940);
    private static final Color DEFAULT_COLOR = new Color(230, 230, 210);
    private File file;
    
    private int PC, line_start, line_end;
    private Highlighter.HighlightPainter painter;
    private Object highlight;
    
    private Action copyAction = new DefaultEditorKit.CopyAction();

    public TraceView() {
        editorPane = new JEditorPane();

        setMinimumSize(MIN_SIZE);
        setPreferredSize(PREFERRED_SIZE);
        setResizable(true);
        setTitle("Trace Log");

        editorPane = new JEditorPane();
        editorPane.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        editorPane.setEditable(false);
        editorPane.setText("No file loaded : firmware.reloc");

        JScrollPane scrollableView = new JScrollPane(editorPane);
        getContentPane().add(scrollableView);

        JMenu menu = new JMenu("Edit");
        menu.add(new JMenuItem(copyAction));

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(menu);
        this.setJMenuBar(menuBar);

		try {
			file = new File("firmware.reloc");
			editorPane.setPage(file.toURI().toURL());
		} catch (IOException e) {
			System.out.println("Error "+e);
		}
		
		painter = new DefaultHighlighter.DefaultHighlightPainter(new Color(255,255,0));

        this.PC = 0;
        try {
			highlight = editorPane.getHighlighter().addHighlight(0, 6, painter);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        this.syncEditorPosition();
        
        pack();
    }

    private int setCurrentLine()
    {
    	String h = "00" + Integer.toHexString(this.PC).toUpperCase();
    	this.line_start = editorPane.getText().indexOf(h);
    	this.line_end = editorPane.getText().indexOf('\n', this.line_start);
    	
    	try {
    		if (line_start > -1) {
    			if (line_end < 0) {
    				line_end = line_start + 6;
    			}
       			editorPane.getHighlighter().changeHighlight(highlight, line_start, line_end);
    		}
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return this.line_start;
    }
    
    private void syncEditorPosition() {
    	int caretPos = setCurrentLine();
    	editorPane.setCaretPosition(caretPos);
    }
    
    public void setPC(int pc)
    {
    	this.PC = pc;
    }

    public void refresh() {
    	syncEditorPosition();
    }
    
    public void reset() {
            editorPane.setText("");
            editorPane.setEnabled(true);
    }
    
    public boolean shouldUpdate() {
        return isVisible() && editorPane.isEnabled();
    }
    

}
