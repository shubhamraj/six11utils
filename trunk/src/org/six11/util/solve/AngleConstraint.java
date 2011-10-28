package org.six11.util.solve;

import java.awt.Color;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.DrawingBufferRoutines;
import org.six11.util.pen.Functions;
import org.six11.util.pen.Line;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;

import static org.six11.util.Debug.bug;
import static org.six11.util.Debug.num;
import static java.lang.Math.abs;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

public class AngleConstraint extends Constraint {

  public static double TOLERANCE = 0.0001;

  Pt a, f, b;
  NumericValue angle;

  public AngleConstraint(Pt a, Pt fulcrum, Pt b, NumericValue radians) {
    this.a = a;
    this.f = fulcrum;
    this.b = b;
    this.angle = radians;
  }

  public AngleConstraint() {
    
  }
  
  public String getType() {
    return "Angle";
  }

  public void accumulateCorrection() {
    int free = 2 - countPinned(a, b);
    if (free > 0) {
      double e = measureError();
      if (abs(e) > TOLERANCE) {
        // Rotate a and b about f by e/2 and -e/2 radians. (assuming free = 2)
        double shift = e / free;
        if (!isPinned(a)) {
          Pt rotatedA = Functions.rotatePointAboutPivot(a, f, shift);
          Vec vecA = new Vec(rotatedA.x - a.x, rotatedA.y - a.y);
          accumulate(a, vecA);
        }
        if (!isPinned(b)) {
          Pt rotatedB = Functions.rotatePointAboutPivot(b, f, -shift);
          Vec vecB = new Vec(rotatedB.x - b.x, rotatedB.y - b.y);
          accumulate(b, vecB);
        }
      }
    }
  }

  public double measureError() {
    Vec fa = new Vec(a, f);
    Vec fb = new Vec(b, f);
    double currentAngle = Functions.getSignedAngleBetween(fa, fb);
    double ret = Math.signum(currentAngle) * (Math.abs(currentAngle) - angle.getValue());
    return ret;
  }

  public Line getSegment1() {
    return new Line(f, a);
  }

  public Line getSegment2() {
    return new Line(f, b);
  }

  public void draw(DrawingBuffer buf) {
    double e = measureError();
    Color col = (abs(e) > TOLERANCE) ? Color.RED : Color.GREEN;
    DrawingBufferRoutines.line(buf, getSegment1(), col, 2);
    DrawingBufferRoutines.line(buf, getSegment2(), col, 2);
    DrawingBufferRoutines.text(buf, f.getTranslated(0, 10), num(toDegrees(angle.getValue())) + " deg",
        col.darker());
  }

  public static Manipulator getManipulator() {
    Manipulator man = new Manipulator(AngleConstraint.class, "Angle", //
        new Manipulator.Param("a", "Point 1", true),
        new Manipulator.Param("b", "Point 2", true),
        new Manipulator.Param("f", "Fulcrum", true),
        new Manipulator.Param("angle", "Angle (degrees)", true));
    return man;
  }
  
  @Override
  public void assume(Manipulator m, VariableBank vars) {
    if (m.ptOrConstraint != getClass()) {
      bug("Can't build " + getClass().getName() + " based on manipulator for " + m.label
          + "(its ptOrConstraint is " + m.ptOrConstraint.getName() + ")");
    } else {
      bug("Yay I can build a angle thing from this manipulator");
    }
    Map<String, String> paramVals = m.getParamsAsMap();
    a = vars.getPointWithName(paramVals.get("a"));
    b = vars.getPointWithName(paramVals.get("b"));
    f = vars.getPointWithName(paramVals.get("f"));
    angle = new NumericValue(toRadians(Double.parseDouble(paramVals.get("angle"))));
  }
  
  /**
   * Create a manipulator that holds the values of this constraint.
   */
  public Manipulator getManipulator(VariableBank vars) {
    Manipulator man = AngleConstraint.getManipulator();
    man.setParamValue("a", a.getString("name"));
    man.setParamValue("b", b.getString("name"));
    man.setParamValue("f", f.getString("name"));
    man.setParamValue("angle", "" + toDegrees(angle.getValue()));
    man.newThing = false;
    man.constraint = this;
    return man;
  }

  @Override
  public String getHumanDescriptionString() {
    return "Angle " + name(a) + ", " + name(b) + ", " + name(f) + num(toDegrees(angle.getValue())); 
  }

  public JSONObject toJson() throws JSONException {
    JSONObject ret = new JSONObject();
    ret.put("a", a.getString("name"));
    ret.put("b", b.getString("name"));
    ret.put("f", f.getString("name"));
    ret.put("angle", angle.getValue());
    return ret;
  }

  public void fromJson(JSONObject obj, VariableBank vars) throws JSONException {
    a = vars.getPointWithName(obj.getString("a"));
    b = vars.getPointWithName(obj.getString("b"));
    f = vars.getPointWithName(obj.getString("f"));
    angle = new NumericValue(obj.getDouble("angle"));
  }

}
