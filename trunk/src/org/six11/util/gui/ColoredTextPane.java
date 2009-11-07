package org.six11.util.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JTextPane;
import javax.swing.text.*;

/**
 * 
 * 
 * @author Gabe Johnson <johnsogg@cmu.edu>
 */
public class ColoredTextPane extends JTextPane {

  protected boolean autoscroll;
  protected boolean lineWrap;
  protected Component viewport;

  /**
   * 
   */
  public ColoredTextPane() {
    super();
  }

  public ColoredTextPane(Component scrollpaneViewport) {
    viewport = scrollpaneViewport;
  }

  public Dimension getPreferredSize() {
    Dimension ret = super.getPreferredSize();
    if (viewport != null) {
      Dimension min = viewport.getSize();
      ret = new Dimension(Math.max(min.width, ret.width), Math.max(min.height, ret.height));
    }
    return ret;
  }

  /**
   * @param doc
   */
  public ColoredTextPane(StyledDocument doc) {
    super(doc);
    this.lineWrap = true;
  }

  public void setLineWrap(boolean v) {
    this.lineWrap = v;
  }

  public boolean getScrollableTracksViewportWidth() {
    return lineWrap;
  }

  /**
   * Appends the given string to the server message area in the provided color. This does NOT add an
   * extra newline, so you have to add that if you want it.
   * 
   * @param c
   *          the Color
   * @param s
   *          the string to append
   */
  public void append(Color c, String s) {
    StyleContext sc = StyleContext.getDefaultStyleContext();
    AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);
    int len = getDocument().getLength();
    try {
      getStyledDocument().insertString(len, s, aset);
      if (autoscroll) {
        setCaretPosition(getDocument().getLength());
      }
    } catch (BadLocationException ex) {
      ex.printStackTrace();
    }

  }

  public boolean isAutoscrollEnabled() {
    return autoscroll;
  }

  public void setAutoscroll(boolean autoscroll) {
    this.autoscroll = autoscroll;
  }

}
