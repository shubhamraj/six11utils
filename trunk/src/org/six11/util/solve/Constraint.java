package org.six11.util.solve;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.Entropy;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;

import static java.lang.Math.toDegrees;
import static org.six11.util.Debug.bug;
import static org.six11.util.Debug.num;

public abstract class Constraint {

  private static int ID_COUNTER = 0;
  static Entropy entropy;
  static {
    entropy = Entropy.getEntropy();
    entropy.setSeed(System.currentTimeMillis());
  }

  StringBuffer messages;
  protected String secretName;
  protected double lastKnownError;
  protected final int id;

  public Constraint() {
    this.messages = new StringBuffer();
    id = ID_COUNTER++;
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

  public static int countPinned(Pt... somePoints) {
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
   * 
   * @param m
   */
  public abstract void assume(Manipulator m, VariableBank vars);

  public abstract Manipulator getManipulator(VariableBank vars);

  public abstract String getType();

  public abstract void accumulateCorrection(double heat);

  public abstract double measureError();

  public void resetLastError() {
    this.lastKnownError = Double.MAX_VALUE;
  }

  public void pushLastError() {
    this.lastKnownError = measureError();
  }

  public boolean detectOscillation() {
    // a oscillation is when the constraint's signed error bounces between two 
    // roughly equal values of opposite sign. E.g. -0.43 and +0.45. Return true 
    // if such an oscillation is found.
    double currentError = measureError();
    boolean ret = false;
    if (Math.signum(currentError) != Math.signum(lastKnownError)) {
      double a = Math.abs(currentError);
      double b = Math.abs(lastKnownError);
      double ratio = (Math.min(a, b) / Math.max(a, b));
      ret = (ratio > 0.9);
    }
    return ret;
  }

  public static boolean isPinned(Pt pt) {
    return pt.getBoolean("pinned", false);
  }

  //  public static double makeRandom(double src, double heat) {
  //    double r = entropy.getNearbyDouble(src, src * heat / 4.0);
  //    //    bug("makeRandom(" + src + ", " + heat + ") ==> " + r);
  //    return r;
  //  }

  public static void setPinned(Pt pt, boolean val) {
    pt.setBoolean("pinned", val);
  }

  public void accumulate(Pt pt, Vec correction, double heat) {
    if (isPinned(pt)) {
      bug("Warning: you are adding a correction vector to point " + pt.getString("name")
          + ", but it is pinned. Constraint type: " + getType());
    }
    boolean add = true;
    if (detectOscillation()) {
      if (entropy.getBoolean()) {
        Vec correction2 = new Vec(correction.getX() * entropy.getDouble(2 * heat),
            correction.getY() * entropy.getDouble(2 * heat));
//        bug(getClass().getName() + " " + id + ": Oscillation found: " + lastKnownError + " and "
//            + measureError() + ": " + num(correction) + " --> " + num(correction2));
        correction = correction2;
      } else {
        add = false;
//        bug("Not adding " + this + " this time.");
      }
    }
    if (add) {
      @SuppressWarnings("unchecked")
      List<Vec> corrections = (List<Vec>) pt.getAttribute(ConstraintSolver.ACCUM_CORRECTION);
      corrections.add(correction);
    }
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

  public String toString() {
    return getClass() + "." + id;
  }

}
