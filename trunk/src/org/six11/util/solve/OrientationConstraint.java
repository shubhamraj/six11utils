package org.six11.util.solve;

import java.awt.Color;

import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.DrawingBufferRoutines;
import org.six11.util.pen.Functions;
import org.six11.util.pen.Line;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;

import static org.six11.util.Debug.num;
import static java.lang.Math.toDegrees;
import static java.lang.Math.abs;

public class OrientationConstraint extends Constraint {

  public static double TOLERANCE = 0.0001;

  Pt lineA1, lineA2, lineB1, lineB2;
  double angle;

  /**
   * This constrains two lines to some angle. 
   */
  public OrientationConstraint(Pt lineA1, Pt lineA2, Pt lineB1, Pt lineB2, double radians) {
    this.lineA1 = lineA1;
    this.lineA2 = lineA2;
    this.lineB1 = lineB1;
    this.lineB2 = lineB2;
    this.angle = radians;
  }
  
  public String getType() {
    return "Orientation";
  }

  public void accumulateCorrection() {
    double e = measureError();
    addMessage("Error is " + num(e) + " (" + num(toDegrees(e)) + " deg) Orientation: " + num(angle) + " (" + num(toDegrees(angle)) + ")");
    if (abs(e) > TOLERANCE) {
      Line lineA = new Line(lineA1, lineA2);
      Line lineB = new Line(lineB1, lineB2);
      Pt midA = lineA.getMidpoint();
      Pt midB = lineB.getMidpoint();
      Pt rotatedA1 = Functions.rotatePointAboutPivot(lineA1, midA, e / 2);
      Pt rotatedA2 = Functions.rotatePointAboutPivot(lineA2, midA, e / 2);
      Pt rotatedB1 = Functions.rotatePointAboutPivot(lineB1, midB, - e / 2);
      Pt rotatedB2 = Functions.rotatePointAboutPivot(lineB2, midB, - e / 2);
      Vec vecA1 = new Vec(rotatedA1.x - lineA1.x, rotatedA1.y - lineA1.y);
      Vec vecA2 = new Vec(rotatedA2.x - lineA2.x, rotatedA2.y - lineA2.y);
      Vec vecB1 = new Vec(rotatedB1.x - lineB1.x, rotatedB1.y - lineB1.y);
      Vec vecB2 = new Vec(rotatedB2.x - lineB2.x, rotatedB2.y - lineB2.y);
      accumulate(lineA1, vecA1);
      accumulate(lineA2, vecA2);
      accumulate(lineB1, vecB1);
      accumulate(lineB2, vecB2);
    }
  }

  public double measureError() {
    double ret = 0;
    Vec vA = new Vec(lineA1, lineA2);
    Vec vB = new Vec(lineB1, lineB2);
    double currentAngle = Functions.getSignedAngleBetween(vA, vB);
    ret = Math.signum(currentAngle) * (Math.abs(currentAngle) - angle);
    return ret;
  }

  @Override
  public void draw(DrawingBuffer buf) {
    Line lineA = new Line(lineA1, lineA2);
    Line lineB = new Line(lineB1, lineB2);
    DrawingBufferRoutines.cross(buf, lineA.getMidpoint(), 6, Color.LIGHT_GRAY);
    DrawingBufferRoutines.cross(buf, lineB.getMidpoint(), 6, Color.LIGHT_GRAY);
    DrawingBufferRoutines.line(buf, lineA, Color.CYAN, 2);
    DrawingBufferRoutines.line(buf, lineB, Color.CYAN.darker(), 2);
  }

}
