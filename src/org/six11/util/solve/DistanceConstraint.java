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
  NumericValue d;

  public DistanceConstraint(Pt a, Pt b, NumericValue d) {
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
    ret = (a.distance(b) - d.getValue());
    return ret;
  }

  public void draw(DrawingBuffer buf) {
    double e = measureError();
    Color col = (e > TOLERANCE) ? Color.RED : Color.GREEN;
    DrawingBufferRoutines.line(buf, getCurrentSegment(), col, 2);
    Pt mid = getCurrentSegment().getMidpoint();
    DrawingBufferRoutines.text(buf, mid.getTranslated(0, 10), "length: " + d, col.darker());
  }

}
