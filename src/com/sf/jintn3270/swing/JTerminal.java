package com.sf.jintn3270.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JPanel;
import javax.swing.KeyStroke;

import com.sf.jintn3270.event.*;
import com.sf.jintn3270.actions.TerminalAction;
import com.sf.jintn3270.TerminalModel;

public class JTerminal extends JPanel implements TerminalEventListener {
	Image offscreen;
	TerminalRenderer renderer;
	
	TerminalModel model;
	
	TerminalKeyMap keyMap;
	
	public JTerminal(TerminalModel tm) {
		this(tm, new DefaultTerminalKeyMap());
	}
	
	public JTerminal(TerminalModel tm, TerminalKeyMap km) {
		this(tm, new DefaultTerminalRenderer(), km);
	}
	
	public JTerminal(TerminalModel tm, TerminalRenderer rn) {
		this(tm, rn, new DefaultTerminalKeyMap());
	}
	
	public JTerminal(TerminalModel tm, TerminalRenderer rn, TerminalKeyMap km) {
		super();
		setFocusable(true);
		setTerminalRenderer(rn);
		setTerminalModel(tm);
		setKeyMap(km);
	}
	
	
	public void setTerminalModel(TerminalModel tm) {
		if (model != null) {
			model.removeTerminalEventListener(this);
		}
		model = tm;
		if (tm != null) {
			model.addTerminalEventListener(this);
		}
		setKeyMap(keyMap);
	}
	
	
	public void setTerminalRenderer(TerminalRenderer rend) {
		this.renderer = rend;
	}
	
	
	public void setKeyMap(TerminalKeyMap km) {
		this.keyMap = km;
		resetKeyboardActions();
		if (keyMap != null) {
			for (KeyStroke stroke : keyMap.getMappedStrokes()) {
				TerminalAction ta = keyMap.getAction(stroke);
				ta.setTerminalModel(model);
				getInputMap().put(stroke, ta.getName());
				getActionMap().put(ta.getName(), ta);
			}
		}
	}
	
	
	public TerminalRenderer getTerminalRenderer() {
		return this.renderer;
	}
	
	
	public TerminalModel getTerminalModel() {
		return model;
	}
	
	
	public void terminalChanged(TerminalEvent te) {
		if (isVisible()) {
			repaint();
		}
	}
	
	public void invalidate() {
		super.invalidate();
		offscreen = null;
	}
	
	
	public Dimension getPreferredSize() {
		Dimension size = renderer.getPreferredSize(model);
		size.setSize(size.getWidth() + (getInsets().left + getInsets().right),
		             size.getHeight() + (getInsets().top + getInsets().bottom));
		return size;
	}
	
	public Dimension getMinimumSize() {
		Dimension size = renderer.getMinimumSize(model);
		size.setSize(size.getWidth() + (getInsets().left + getInsets().right),
		             size.getHeight() + (getInsets().top + getInsets().bottom));
		return size;
	}
	
	protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
		boolean isMapped = false;
		// Do we know of anything to do for this specific KeyEvent (not KeyStroke!)
		TerminalAction ta = keyMap.getAction(e, model);
		if (ta != null) {
			isMapped = true;
			ta.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, KeyEvent.getKeyText(e.getKeyCode())));
		}
		
		// If we didn't handle the KeyEvent, send this along to the InputMap/ActionMap handling implemented by the parent class.
		if (!isMapped) {
			isMapped = super.processKeyBinding(ks, e, condition, pressed);
		}
		
		return isMapped;
	}
	
	
	protected void processKeyEvent(KeyEvent e) {
		super.processKeyEvent(e);
		keyMap.processKeyEvent(e);
	}
	
	
	public void paintComponent(Graphics g) {
		if (offscreen == null) {
			offscreen = createImage(getSize().width - (getInsets().right + getInsets().left), 
			                        getSize().height - (getInsets().bottom + getInsets().top));
		}
		// Set the clipping region to include the full size of the image to be printed.
		Graphics offG = offscreen.getGraphics();
		
		offG.setClip(0, 0, getSize().width - (getInsets().right + getInsets().left), 
		                   getSize().height - (getInsets().bottom + getInsets().top));
		
		// Render this component, then paint the subcomponents.
		renderer.paint(offG, model);
		
		// Then blit to screen.
		g.drawImage(offscreen, getInsets().left, getInsets().right, null);
		
		// Dispose the Graphics object to the off-screen image.
		offG.dispose();
	}
	
	public void update(Graphics g) {
		paint(g);
	}
	
	public void setFont(Font f) {
		super.setFont(f);
		if (renderer != null) {
			renderer.setFont(f);
		}
	}
}
