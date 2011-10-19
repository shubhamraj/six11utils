package org.six11.util.solve;

import java.awt.Color;

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

public class AngleConstraint extends Constraint {

  public static double TOLERANCE = 0.0001;

  Pt a, f, b;
  double angle;

  public AngleConstraint(Pt a, Pt fulcrum, Pt b, double radians) {
    this.a = a;
    this.f = fulcrum;
    this.b = b;
    this.angle = radians;
  }

  public String getType() {
    return "Angle";
  }

  public void accumulateCorrection() {
    double e = measureError();
    if (abs(e) > TOLERANCE) {
      // Rotate a and b about f by e/2 and -e/2 radians.
      Pt rotatedA = Functions.rotatePointAboutPivot(a, f, e / 2);
      Pt rotatedB = Functions.rotatePointAboutPivot(b, f, -e / 2);
      Vec vecA = new Vec(rotatedA.x - a.x, rotatedA.y - a.y);
      Vec vecB = new Vec(rotatedB.x - b.x, rotatedB.y - b.y);
      accumulate(a, vecA);
      accumulate(b, vecB);
    }
  }

  public double measureError() {
    Vec fa = new Vec(a, f);
    Vec fb = new Vec(b, f);
    double currentAngle = Functions.getSignedAngleBetween(fa, fb);
    double ret = Math.signum(currentAngle) * (Math.abs(currentAngle) - angle);
    return ret;
  }

  public Line getSegment1() {
    return new Line(f, a);
  }

  public Line getSegment2() {
    return new Line(f, b);
  }

  public void draw(DrawingBuffer buf) {
    DrawingBufferRoutines.line(buf, getSegment1(), Color.RED.darker(), 2);
    DrawingBufferRoutines.line(buf, getSegment2(), Color.RED.brighter(), 2);
    DrawingBufferRoutines.text(buf, f.getTranslated(0, 10), num(toDegrees(angle)) + " deg",
        Color.GRAY);
  }

}
