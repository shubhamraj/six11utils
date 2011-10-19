package org.six11.util.solve;

import java.util.List;

import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;

public abstract class Constraint {
  
  StringBuffer messages;
  
  public Constraint() {
    this.messages = new StringBuffer();
  }
  
  public void addMessage(String msg) {
    messages.append(msg + "\n");
  }
  
  public void clearMessages() {
    messages.setLength(0);
  }

  public String getMessages() {
    return messages.toString();
  }
  
  public abstract String getType();
  
  public abstract void accumulateCorrection();
  
  public abstract double measureError();

  public void accumulate(Pt pt, Vec correction) {
    List<Vec> corrections = (List<Vec>) pt.getAttribute(Main.ACCUM_CORRECTION);
    corrections.add(correction);
  }
  
  public abstract void draw(DrawingBuffer buf);
}
