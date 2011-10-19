package org.six11.util.solve;

import java.awt.Color;

import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.DrawingBufferRoutines;
import org.six11.util.pen.Line;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;

import static org.six11.util.Debug.num;
import static org.six11.util.Debug.bug;
import static java.lang.Math.abs;

public class DistanceConstraint extends Constraint {

  public static double TOLERANCE = 0.0001;

  Pt a, b;
  double d;

  public DistanceConstraint(Pt a, Pt b, double d) {
    this.a = a;
    this.b = b;
    this.d = d;
  }

  public Line getCurrentSegment() {
    return new Line(a, b);
  }

  public String getType() {
    return "Distance";
  }

  public void accumulateCorrection() {
    double e = measureError();
    if (abs(e) > TOLERANCE) {
      Vec aToB = new Vec(a, b).getUnitVector().getScaled(e / 2);
      Vec bToA = aToB.getFlip();
      accumulate(a, aToB);
      accumulate(b, bToA);
    }
  }

  public double measureError() {
    double ret = 0;
    ret = (a.distance(b) - d);
    return ret;
  }

  @Override
  public void draw(DrawingBuffer buf) {
    DrawingBufferRoutines.line(buf, getCurrentSegment(), Color.RED, 2);
    Pt mid = getCurrentSegment().getMidpoint();
    DrawingBufferRoutines.text(buf, mid.getTranslated(0, 10), num(d) + " length", Color.GRAY);
  }

}
