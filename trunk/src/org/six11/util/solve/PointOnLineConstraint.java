package org.six11.util.solve;

import java.awt.Color;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.six11.util.Debug;
import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.DrawingBufferRoutines;
import org.six11.util.pen.Functions;
import org.six11.util.pen.Line;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;
import static java.lang.Math.abs;
import static org.six11.util.Debug.bug;
import static org.six11.util.Debug.num;

public class PointOnLineConstraint extends Constraint {

  public static double TOLERANCE = 0.0001;
  public static final String NAME = "Point On Line";

  Pt a, b, m;

  public PointOnLineConstraint(Pt a, Pt b, Pt m) {
    this.a = a;
    this.b = b;
    this.m = m;
  }

  public PointOnLineConstraint(JSONObject obj, VariableBank vars) throws JSONException {
    super(obj);
    fromJson(obj, vars);
  }

  public String getType() {
    return NAME;
  }

  public void accumulateCorrection(double heat) {
    double e = measureError();
    if (e > TOLERANCE) {
      int pins = countPinned(a, b, m);
      maybeMove(pins, a, b, m, heat); // possible move a towards the line formed by b and m
      maybeMove(pins, b, a, m, heat); // b --> a-m
      maybeMove(pins, m, a, b, heat); // m --> a-b
    }
  }

  private void maybeMove(int pins, Pt move, Pt pt1, Pt pt2, double heat) {
    if (!isPinned(move)) {
      Pt near = Functions.getNearestPointOnLine(move, new Line(pt1, pt2));
      double shift = near.distance(move) / (3 - pins);
      Vec delta = new Vec(move, near).getVectorOfMagnitude(shift);
      accumulate(move, delta, heat);
    }
  }

  @Override
  public double measureError() {
    return Functions.getDistanceBetweenPointAndLine(m, new Line(a, b));
  }

  @Override
  public void draw(DrawingBuffer buf) {
    Color col = (abs(measureError()) > TOLERANCE) ? Color.RED : Color.GREEN;
    DrawingBufferRoutines.line(buf, new Line(a, b), col, 1.0);
  }

  public static Manipulator getManipulator() {
    Manipulator man = new Manipulator(PointOnLineConstraint.class, "Point on Line", //
        new Manipulator.Param("p1", "Point 1 (Line)", true), new Manipulator.Param("p2",
            "Point 2 (Line)", true), new Manipulator.Param("p3", "Point 3 (Target)", true));
    return man;
  }

  @Override
  public void assume(Manipulator man, VariableBank vars) {
    if (man.ptOrConstraint != getClass()) {
      bug("Can't build " + getClass().getName() + " based on manipulator for " + man.label
          + "(its ptOrConstraint is " + man.ptOrConstraint.getName() + ")");
    } else {
      bug("Yay I can build a point-on-line thing from this manipulator");
    }
    Map<String, String> paramVals = man.getParamsAsMap();
    bug(num(paramVals.values(), " "));
    a = vars.getPointWithName(paramVals.get("p1"));
    b = vars.getPointWithName(paramVals.get("p2"));
    m = vars.getPointWithName(paramVals.get("p3"));
  }

  /**
   * Create a manipulator that holds the values of this constraint.
   */
  public Manipulator getManipulator(VariableBank vars) {
    Manipulator man = PointOnLineConstraint.getManipulator();
    man.setParamValue("p1", a.getString("name"));
    man.setParamValue("p2", b.getString("name"));
    man.setParamValue("p3", m.getString("name"));
    man.newThing = false;
    man.constraint = this;
    return man;
  }

  public String getHumanDescriptionString() {
    return "PointAsLineParam " + name(a) + ", " + name(b) + ", " + name(m);
  }

  public JSONObject toJson() throws JSONException {
    JSONObject ret = new JSONObject();
    ret.put("p1", a.getString("name"));
    ret.put("p2", b.getString("name"));
    ret.put("p3", m.getString("name"));
    return ret;
  }

  public void fromJson(JSONObject obj, VariableBank vars) throws JSONException {
    a = vars.getPointWithName(obj.getString("p1"));
    b = vars.getPointWithName(obj.getString("p2"));
    m = vars.getPointWithName(obj.getString("p3"));
    Debug.errorOnNull(a, "a");
    Debug.errorOnNull(b, "b");
    Debug.errorOnNull(m, "m");
  }

  @Override
  public boolean involves(Pt who) {
    return (a == who || b == who || m == who);
  }

  @Override
  public void replace(Pt oldPt, Pt newPt) {
    if (a == oldPt) {
      a = newPt;
    }
    if (b == oldPt) {
      b = newPt;
    }
    if (m == oldPt) {
      m = newPt;
    }
  }

  @Override
  public Pt[] getRelatedPoints() {
    return new Pt[] { a, b, m };
  }

}
