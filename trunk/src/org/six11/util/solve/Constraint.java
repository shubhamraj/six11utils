package org.six11.util.solve;

import java.util.List;

import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;
import static org.six11.util.Debug.bug;

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
  
  public static int countPinned(Pt ... somePoints) {
    int ret = 0;
    for (Pt pt : somePoints) {
      if (isPinned(pt)) {
        ret++;
      }
    }
    return ret;
  }
    
  public abstract String getType();
  
  public abstract void accumulateCorrection();
  
  public abstract double measureError();

  public static boolean isPinned(Pt pt) {
    return pt.getBoolean("pinned", false);
  }
  
  public static void setPinned(Pt pt, boolean val) {
    pt.setBoolean("pinned", val);
  }
  
  public void accumulate(Pt pt, Vec correction) {
    if (isPinned(pt)) {
      bug("Warning: you are adding a correction vector to point " + pt.getString("name") + ", but it is pinned. Constraint type: " + getType());
    }
    List<Vec> corrections = (List<Vec>) pt.getAttribute(Main.ACCUM_CORRECTION);
    corrections.add(correction);
  }
  
  public abstract void draw(DrawingBuffer buf);
}
