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

import static org.six11.util.Debug.num;
import static org.six11.util.Debug.bug;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static java.lang.Math.abs;

public class OrientationConstraint extends Constraint {

  public static double TOLERANCE = 0.0001;

  Pt lineA1, lineA2, lineB1, lineB2;
  NumericValue angle;

  /**
   * This constrains two lines to some angle.
   */
  public OrientationConstraint(Pt lineA1, Pt lineA2, Pt lineB1, Pt lineB2, NumericValue radians) {
    this.lineA1 = lineA1;
    this.lineA2 = lineA2;
    this.lineB1 = lineB1;
    this.lineB2 = lineB2;
    this.angle = radians;
  }

  public OrientationConstraint() {

  }

  public String getType() {
    return "Orientation";
  }

  public void accumulateCorrection() {
    double e = measureError();
    addMessage("Error is " + num(e) + " (" + num(toDegrees(e)) + " deg) Orientation: "
        + num(angle.getValue()) + " (" + num(toDegrees(angle.getValue())) + ")");
    if (abs(e) > TOLERANCE) {
      rotate(lineA1, lineA2, e);
      rotate(lineB1, lineB2, -e);
      //      Line lineB = new Line(lineB1, lineB2);
      //      Pt midB = lineB.getMidpoint();
      //      Pt rotatedB1 = Functions.rotatePointAboutPivot(lineB1, midB, -e / 2);
      //      Pt rotatedB2 = Functions.rotatePointAboutPivot(lineB2, midB, -e / 2);
      //      Vec vecB1 = new Vec(rotatedB1.x - lineB1.x, rotatedB1.y - lineB1.y);
      //      Vec vecB2 = new Vec(rotatedB2.x - lineB2.x, rotatedB2.y - lineB2.y);
      //      accumulate(lineB1, vecB1);
      //      accumulate(lineB2, vecB2);
    }
  }

  private void rotate(Pt pt1, Pt pt2, double amt) {
    // three cases: 
    // both points are free = rotate about mid by (amt / 2)
    // one point free = rotate free point about pinned point by amt
    // both points are pinned = do nothing
    Line line = new Line(pt1, pt2);
    int free = 2 - countPinned(pt1, pt2);
    if (free == 2) {
      Pt pivot = line.getMidpoint();
      Pt rotated1 = Functions.rotatePointAboutPivot(pt1, pivot, amt / 2);
      Vec vec1 = new Vec(rotated1.x - pt1.x, rotated1.y - pt1.y);
      accumulate(pt1, vec1);
      Pt rotated2 = Functions.rotatePointAboutPivot(pt2, pivot, amt / 2);
      Vec vec2 = new Vec(rotated2.x - pt2.x, rotated2.y - pt2.y);
      accumulate(pt2, vec2);
    } else if (free == 1) {
      Pt pivot = isPinned(pt1) ? pt1 : pt2;
      Pt moveMe = isPinned(pt1) ? pt2 : pt1;
      //      double signedAmt = isPinned(pt1) ? amt : -amt;
      Pt rotated = Functions.rotatePointAboutPivot(moveMe, pivot, amt / 2);
      Vec vec = new Vec(rotated.x - moveMe.x, rotated.y - moveMe.y);
      accumulate(moveMe, vec);
    }

  }

  public double measureError() {
    double ret = 0;
    Vec vA = new Vec(lineA1, lineA2);
    Vec vB = new Vec(lineB1, lineB2);
    double currentAngle = Functions.getSignedAngleBetween(vA, vB);
    ret = Math.signum(currentAngle) * (Math.abs(currentAngle) - angle.getValue());
    return ret;
  }

  @Override
  public void draw(DrawingBuffer buf) {
    Color col = (abs(measureError()) > TOLERANCE) ? Color.RED : Color.GREEN;
    Line lineA = new Line(lineA1, lineA2);
    Line lineB = new Line(lineB1, lineB2);
    DrawingBufferRoutines.cross(buf, lineA.getMidpoint(), 6, Color.LIGHT_GRAY);
    DrawingBufferRoutines.cross(buf, lineB.getMidpoint(), 6, Color.LIGHT_GRAY);
    DrawingBufferRoutines.line(buf, lineA, col, 1);
    DrawingBufferRoutines.line(buf, lineB, col, 1);
  }

  public static Manipulator getManipulator() {
    Manipulator man = new Manipulator(OrientationConstraint.class, "Orientation", //
        new Manipulator.Param("pA1", "Line 1 start", true), //
        new Manipulator.Param("pA2", "Line 1 end", true), //
        new Manipulator.Param("pB1", "Line 2 start", true), //
        new Manipulator.Param("pB2", "Line 2 end", true), //
        new Manipulator.Param("angle", "Angle (degrees)", true));
    return man;
  }

  public void assume(Manipulator m, VariableBank vars) {
    if (m.ptOrConstraint != getClass()) {
      bug("Can't build " + getClass().getName() + " based on manipulator for " + m.label
          + "(its ptOrConstraint is " + m.ptOrConstraint.getName() + ")");
    } else {
      bug("Yay I can build an orientation thing from this manipulator");
    }
    Map<String, String> paramVals = m.getParamsAsMap();
    bug(num(paramVals.values(), " "));
    lineA1 = vars.getPointWithName(paramVals.get("pA1"));
    lineA2 = vars.getPointWithName(paramVals.get("pA2"));
    lineB1 = vars.getPointWithName(paramVals.get("pB1"));
    lineB2 = vars.getPointWithName(paramVals.get("pB2"));
    angle = new NumericValue(toRadians(Double.parseDouble(paramVals.get("angle"))));
  }
  
  /**
   * Create a manipulator that holds the values of this constraint.
   */
  public Manipulator getManipulator(VariableBank vars) {
    Manipulator man = OrientationConstraint.getManipulator();
    man.setParamValue("pA1", lineA1.getString("name"));
    man.setParamValue("pA2", lineA2.getString("name"));
    man.setParamValue("pB1", lineB1.getString("name"));
    man.setParamValue("pB2", lineB2.getString("name"));
    man.setParamValue("angle", "" + toDegrees(angle.getValue()));
    man.newThing = false;
    man.constraint = this;
    return man;
  }
  
  public String getHumanDescriptionString() {
    return "Orientation " + name(lineA1) + "--" + name(lineA2) + ", " + name(lineB1) + "--" + name(lineB2) + " =  " + num(toDegrees(angle.getValue()));
  }
  
  public JSONObject toJson() throws JSONException {
    JSONObject ret = new JSONObject();
    ret.put("pA1", lineA1.getString("name"));
    ret.put("pA2", lineA2.getString("name"));
    ret.put("pB1", lineB1.getString("name"));
    ret.put("pB2", lineB2.getString("name"));
    ret.put("angle", angle.getValue());
    return ret;
  }

  public void fromJson(JSONObject obj, VariableBank vars) throws JSONException {
    lineA1 = vars.getPointWithName(obj.getString("pA1"));
    lineA2 = vars.getPointWithName(obj.getString("pA2"));
    lineB1 = vars.getPointWithName(obj.getString("pB1"));
    lineB2 = vars.getPointWithName(obj.getString("pB2"));
    angle = new NumericValue(obj.getDouble("angle"));
  }
}
