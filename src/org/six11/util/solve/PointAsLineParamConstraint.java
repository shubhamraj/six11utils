package org.six11.util.solve;

import java.awt.Color;

import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.DrawingBufferRoutines;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;
import static java.lang.Math.abs;
import static java.lang.Math.toDegrees;
import static org.six11.util.Debug.num;

/**
 * Constrains a point to be on a line segment and some percentage of the distance between the end
 * points of that line segment. Useful for forcing a point to be exactly between two others, for
 * example.
 */
public class PointAsLineParamConstraint extends Constraint {

  public static double TOLERANCE = 0.0001;

  Pt lineA, lineB, target;
  NumericValue dist;

  public PointAsLineParamConstraint(Pt lineA, Pt lineB, NumericValue proportionFromAToB, Pt target) {
    this.lineA = lineA;
    this.lineB = lineB;
    this.target = target;
    this.dist = proportionFromAToB;
    setPinned(target, true);
  }

  public String getType() {
    return "Point As Line Param";
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
    double authDist = len * dist.getValue();
    Vec v = new Vec(lineA, lineB).getUnitVector().getScaled(authDist);
    return new Pt(lineA.x + v.getX(), lineA.y + v.getY());
  }

  public void draw(DrawingBuffer buf) {
    // only need to draw a line. the points should be taken care of elsewhere.
    Color col = (abs(measureError()) > TOLERANCE) ? Color.RED : Color.GREEN;
    DrawingBufferRoutines.line(buf, lineA, lineB, col, 1.0);
  }

  public Pt getTarget() {
    return target;
  }

  @Override
  public void assume(Manipulator m, VariableBank vars) {
    // TODO Auto-generated method stub

  }

  @Override
  public Manipulator getManipulator(VariableBank vars) {
    // TODO Auto-generated method stub
    return null;
  }

  public String getHumanDescriptionString() {
    return "PointAsLineParam " + name(lineA) + ", " + name(lineB) + ", " + name(lineB) + ", "
        + name(target) + " =  " + num(dist.getValue());
  }

}
