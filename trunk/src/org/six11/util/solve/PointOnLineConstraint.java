package org.six11.util.solve;

import java.awt.Color;

import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.DrawingBufferRoutines;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;

public class PointOnLineConstraint extends Constraint {

  public static double TOLERANCE = 0.0001;
  
  Pt lineA, lineB, target;
  double dist;

  public PointOnLineConstraint(Pt lineA, Pt lineB, double proportionFromAToB, Pt target) {
    this.lineA = lineA;
    this.lineB = lineB;
    this.target = target;
    this.dist = proportionFromAToB;
    setPinned(target, true);
  }

  public String getType() {
    return "Point-On-Line";
  }

  public void accumulateCorrection() {
    double e = measureError();
    if (e > TOLERANCE) {
      Pt auth = getAuthority();
      Vec v = new Vec(target, auth);
      target.move(v);
    }
  }

  public double measureError() {
    Pt auth = getAuthority();
    return auth.distance(target);
  }

  private Pt getAuthority() {
    double len = lineA.distance(lineB);
    double authDist = len * dist;
    Vec v = new Vec(lineA, lineB).getUnitVector().getScaled(authDist);
    return new Pt(lineA.x + v.getX(), lineA.y + v.getY());
  }

  public void draw(DrawingBuffer buf) {
    // only need to draw a line. the points should be taken care of elsewhere.
    DrawingBufferRoutines.line(buf, lineA, lineB, Color.BLACK, 1.0);
  }

  public Pt getTarget() {
    return target;
  }

}
