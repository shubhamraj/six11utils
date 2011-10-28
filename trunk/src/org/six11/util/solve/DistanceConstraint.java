package org.six11.util.solve;

import java.awt.Color;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.DrawingBufferRoutines;
import org.six11.util.pen.Line;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;

import static org.six11.util.Debug.num;
import static org.six11.util.Debug.bug;
import static java.lang.Math.abs;
import static java.lang.Math.toRadians;
import static java.lang.Math.toDegrees;

public class DistanceConstraint extends Constraint {

  public static double TOLERANCE = 0.0001;

  Pt a, b;
  NumericValue d;

  public DistanceConstraint(Pt a, Pt b, NumericValue d) {
    this.a = a;
    this.b = b;
    this.d = d;
  }
  
  public DistanceConstraint() {
    
  }

  public Line getCurrentSegment() {
    return new Line(a, b);
  }

  public String getType() {
    return "Distance";
  }

  public void accumulateCorrection() {
    int free = 2 - countPinned(a, b);
    if (free > 0) {
      double e = measureError();
      if (abs(e) > TOLERANCE) {
        double shift = e / free; // move each free point its fair share of the way to the goal
        if (!isPinned(a)) {
          Vec aToB = new Vec(a, b).getUnitVector().getScaled(shift);
          accumulate(a, aToB);
        }
        if (!isPinned(b)) {
          Vec bToA = new Vec(b, a).getUnitVector().getScaled(shift);
          accumulate(b, bToA);
        }
      }
    }
  }

  public double measureError() {
    double ret = 0;
    ret = a.distance(b) - d.getValue();
    return ret;
  }

  public void draw(DrawingBuffer buf) {
    double e = measureError();
    Color col = (abs(e) > TOLERANCE) ? Color.RED : Color.GREEN;
    DrawingBufferRoutines.line(buf, getCurrentSegment(), col, 2);
    Pt mid = getCurrentSegment().getMidpoint();
    DrawingBufferRoutines.text(buf, mid.getTranslated(0, 10), "length: " + d, col.darker());
  }

  public static Manipulator getManipulator() {
    Manipulator man = new Manipulator(DistanceConstraint.class, "Distance", //
        new Manipulator.Param("p1", "Point 1", true),
        new Manipulator.Param("p2", "Point 2", true),
        new Manipulator.Param("dist", "Distance", true));
    return man;
  }

  public void assume(Manipulator m, VariableBank vars) {
    if (m.ptOrConstraint != getClass()) {
      bug("Can't build " + getClass().getName() + " based on manipulator for " + m.label
          + "(its ptOrConstraint is " + m.ptOrConstraint.getName() + ")");
    } else {
      bug("Yay I can build a distance thing from this manipulator");
    }
    Map<String, String> paramVals = m.getParamsAsMap();
    bug(num(paramVals.values(), " "));
    a = vars.getPointWithName(paramVals.get("p1"));
    b = vars.getPointWithName(paramVals.get("p2"));
    d = new NumericValue(Double.parseDouble(paramVals.get("dist")));
  }

  /**
   * Create a manipulator that holds the values of this constraint.
   */
  public Manipulator getManipulator(VariableBank vars) {
    Manipulator man = DistanceConstraint.getManipulator();
    man.setParamValue("p1", a.getString("name"));
    man.setParamValue("p2", b.getString("name"));
    man.setParamValue("dist", "" + d.getValue());
    man.newThing = false;
    man.constraint = this;
    return man;
  }

  public String getHumanDescriptionString() {
    return "Distance " + name(a) + ", " + name(b) + " =  " + num(d.getValue());
  }
  
  public JSONObject toJson() throws JSONException {
    JSONObject ret = new JSONObject();
    ret.put("p1", a.getString("name"));
    ret.put("p2", b.getString("name"));
    ret.put("dist", d.getValue());
    return ret;
  }

  public void fromJson(JSONObject obj, VariableBank vars) throws JSONException {
    a = vars.getPointWithName(obj.getString("p1"));
    b = vars.getPointWithName(obj.getString("p2"));
    d = new NumericValue(obj.getDouble("dist"));
  }

}
