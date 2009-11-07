package org.six11.util.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JTextArea;

/**
 * 
 * 
 * @author Gabe Johnson <johnsogg@cmu.edu>
 */
public class InfoTextBox extends ColoredTextPane {

  public InfoTextBox() {
    super();
    setEditable(false);
    // setEnabled(false);
    //setWrapStyleWord(true);
    //setLineWrap(true);
    // setForeground(Color.BLACK);
    // setDisabledTextColor(Color.BLACK); // necessary since the component is set to disabled above.
  }

  public void paint(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    super.paint(g);
  }

}
