// $Id$

package org.six11.util.pen;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;

public interface DrawFunction {
  public void draw(Sequence seq, Graphics2D g);
}
