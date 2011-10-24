package org.six11.util.solve;

import java.awt.Color;

import org.six11.util.gui.Strokes;
import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.DrawingBufferRoutines;
import org.six11.util.pen.Functions;
import org.six11.util.pen.Line;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;

public class PointOnLineConstraint extends Constraint {

  public static double TOLERANCE = 0.0001;

  Pt a, b, m;

  public PointOnLineConstraint(Pt a, Pt b, Pt m) {
    this.a = a;
    this.b = b;
    this.m = m;
  }

  public String getType() {
    return "Point On Line";
  }

  public void accumulateCorrection() {
    double e = measureError();
    if (e > TOLERANCE) {
      int pins = countPinned(a, b, m);
      maybeMove(pins, a, b, m); // possible move a towards the line formed by b and m
      maybeMove(pins, b, a, m); // b --> a-m
      maybeMove(pins, m, a, b); // m --> a-b
    }
  }

  private void maybeMove(int pins, Pt move, Pt pt1, Pt pt2) {
    if (!isPinned(move)) {
      Pt near = Functions.getNearestPointOnLine(move, new Line(pt1, pt2));
      double shift = near.distance(move) / (3 - pins);
      Vec delta = new Vec(move, near).getVectorOfMagnitude(shift);
      accumulate(move, delta);
    }
  }

  @Override
  public double measureError() {
    return Functions.getDistanceBetweenPointAndLine(m, new Line(a, b));
  }

  @Override
  public void draw(DrawingBuffer buf) {
    DrawingBufferRoutines.line(buf, new Line(a, b), Color.red, 1.0);
  }

}
