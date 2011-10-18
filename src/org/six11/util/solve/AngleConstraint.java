package org.six11.util.solve;

import org.six11.util.pen.Functions;
import org.six11.util.pen.Line;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;

import static org.six11.util.Debug.bug;
import static org.six11.util.Debug.num;
import static java.lang.Math.toDegrees;

public class AngleConstraint extends Constraint {

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
  }

  public double measureError() {
    double ret = 0;
    Vec fa = new Vec(a, f);
    Vec fb = new Vec(b, f);
    ret = Functions.getSignedAngleBetween(fa, fb);
    double alt = Functions.getSignedAngleBetween(fb, fa);
    bug("Angle (fa, fb): " + a.getString("name") + "-" + f.getString("name") + "-" + b.getString("name") + ": " + num(ret) + " (" + num(toDegrees(ret)) + " deg)");
    bug("Angle (fb, fa): " + a.getString("name") + "-" + f.getString("name") + "-" + b.getString("name") + ": " + num(alt) + " (" + num(toDegrees(alt)) + " deg)");
    return ret;
  }

  public Line getSegment1() {
    return new Line(f, a);
  }

  public Line getSegment2() {
    return new Line(f, b);
  }

}
