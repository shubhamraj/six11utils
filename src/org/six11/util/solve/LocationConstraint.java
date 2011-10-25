package org.six11.util.solve;

import java.awt.Color;

import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.DrawingBufferRoutines;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;
import static org.six11.util.Debug.num;

public class LocationConstraint extends Constraint {

  public static double TOLERANCE = 0.0001;

  Pt p, target;

  public LocationConstraint(Pt p, Pt target) {
    this.p = p;
    this.target = target;
  }

  public String getType() {
    return "Pin";
  }

  public void accumulateCorrection() {
    double error = measureError();
    addMessage(p.getString("name") + ": " + num(error) + ". correction vector: "
        + num(target.x - p.x) + ", " + num(target.y - p.y));
    if (!isPinned(p) && error > TOLERANCE) {
      Vec toTarget = new Vec(target.x - p.x, target.y - p.y);
      accumulate(p, toTarget);
    }
  }

  @Override
  public double measureError() {
    return p.distance(target);
  }

  @Override
  public void draw(DrawingBuffer buf) {
    DrawingBufferRoutines.dot(buf, target, 3, 0.1, Color.BLACK, Color.red.brighter());
    if (!p.isSameLocation(target)) {
      DrawingBufferRoutines.arrow(buf, p, target, 2, Color.LIGHT_GRAY);
    }
  }

  @Override
  public void assume(Manipulator m, VariableBank vars) {
    // TODO Auto-generated method stub
    
  }

}
