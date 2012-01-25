package org.six11.util.solve;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;

import static java.lang.Math.toDegrees;
import static org.six11.util.Debug.bug;
import static org.six11.util.Debug.num;

public abstract class Constraint {
  
  StringBuffer messages;
  protected String secretName;
    
  public Constraint() {
    this.messages = new StringBuffer();
  }
  
  public String getSecretName() {
    return secretName;
  }
  
  public void setSecretName(String val) {
    this.secretName = val;
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
  
  public abstract JSONObject toJson() throws JSONException;
  
  public abstract void fromJson(JSONObject obj, VariableBank vars) throws JSONException;
  
  public static int countPinned(Pt ... somePoints) {
    int ret = 0;
    for (Pt pt : somePoints) {
      if (isPinned(pt)) {
        ret++;
      }
    }
    return ret;
  }
    
  /**
   * Use the values contained in the given manipulator
   * @param m
   */
  public abstract void assume(Manipulator m, VariableBank vars);
  
  public abstract Manipulator getManipulator(VariableBank vars);
  
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
    @SuppressWarnings("unchecked")
    List<Vec> corrections = (List<Vec>) pt.getAttribute(ConstraintSolver.ACCUM_CORRECTION);
    corrections.add(correction);
  }
  
  public abstract void draw(DrawingBuffer buf);

  public abstract String getHumanDescriptionString();
  
  /**
   * Returns p.getString("name"). This is a debugging method.
   */
  protected String name(Pt p) {
    return p.getString("name");
  }
  
  /**
   * Returns num(toDegrees(v)). This is a debugging method.
   */
  protected String deg(double v) {
    return num(toDegrees(v));
  }
  
  public abstract boolean involves(Pt who);
  
  public abstract Pt[] getRelatedPoints();

  public abstract void replace(Pt oldPt, Pt newPt);

}
