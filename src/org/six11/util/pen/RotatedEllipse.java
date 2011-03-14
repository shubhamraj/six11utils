package org.six11.util.pen;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.min;
import static java.lang.Math.max;
import static java.lang.Math.atan2;
import static java.lang.Math.toDegrees;

import static org.six11.util.Debug.num;
import org.six11.util.Debug;
import org.six11.util.gui.shape.ShapeFactory;

import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * 
 * @author Gabe Johnson <johnsogg@cmu.edu>
 */
public class RotatedEllipse {

  Pt center; // the centroid of this ellipse.
  double a; // the 'horizontal' radius when the ellipse isn't rotated.
  double b; // the 'vertical' radius when the ellipse isn't rotated.
  double ellipseRotation; // the radian angle of rotation

  private boolean restrictedArc;
  private double extent;
  private double startAngle;
  private double midAngle;
  private double endAngle;

  public List<Pt> regionPoints;
  private List<Pt> restrictedArcPath;

  public RotatedEllipse(Pt center, double a, double b, double ellipseRotation) {
    this.center = center;
    this.a = a;
    this.b = b;
    this.ellipseRotation = ellipseRotation;
  }

  public boolean isRestrictedArc() {
    return restrictedArc;
  }

  public List<Pt> getRestrictedArcPath() {
    if (restrictedArcPath == null) {
      getRestrictedArcPath(60);
    }
    return restrictedArcPath;
  }

  public List<Pt> getRestrictedArcPath(int numPoints) {
    if (restrictedArcPath == null) {
      PathIterator path = new ShapeFactory.RotatedEllipseShape(this, numPoints)
          .getPathIterator(null);
      restrictedArcPath = new ArrayList<Pt>();
      double[] coords = new double[6];
      while (!path.isDone()) {
        path.currentSegment(coords);
        restrictedArcPath.add(new Pt(coords[0], coords[1]));
        path.next();
      }
    }
    return restrictedArcPath;
  }

  public double getStartAngle() {
    return startAngle;
  }

  public double getExtent() {
    return extent;
  }

  public double getMajorRadius() {
    return max(a, b);
  }

  public double getMinorRadius() {
    return min(a, b);
  }

  /**
   * Specify three points that are *near* the ellipse surface. Points a and b are endpoints, and m
   * is any point on the inside.
   */
  public void setArcRegion(Pt a, Pt m, Pt b) {
    restrictedArc = true;
    Pt s = getCentroidIntersect(a);
    Pt mid = getCentroidIntersect(m);
    Pt e = getCentroidIntersect(b);
    regionPoints = new ArrayList<Pt>();
    regionPoints.add(s);
    regionPoints.add(mid);
    regionPoints.add(e);
    startAngle = Functions.makeAnglePositive(toDegrees(-atan2(s.y - center.y, s.x - center.x)));
    midAngle = Functions.makeAnglePositive(toDegrees(-atan2(mid.y - center.y, mid.x - center.x)));
    endAngle = Functions.makeAnglePositive(toDegrees(-atan2(e.y - center.y, e.x - center.x)));

    double midDecreasing = Functions.getNearestAnglePhase(startAngle, midAngle, -1);
    double midIncreasing = Functions.getNearestAnglePhase(startAngle, midAngle, 1);
    double endDecreasing = Functions.getNearestAnglePhase(midDecreasing, endAngle, -1);
    double endIncreasing = Functions.getNearestAnglePhase(midIncreasing, endAngle, 1);

    extent = 0;
    if (Math.abs(endDecreasing - startAngle) < Math.abs(endIncreasing - startAngle)) {
      extent = endDecreasing - startAngle;
    } else {
      extent = endIncreasing - startAngle;
    }
  }

  public static void bug(String what) {
    Debug.out("RotatedEllipse", what);
  }

  public List<Pt> getRegionPoints() {
    return regionPoints;
  }

  public void translate(double dx, double dy) {
    center.setLocation(center.getX() + dx, center.getY() + dy);
  }

  public double getRotation() {
    return ellipseRotation;
  }

  public void setRotation(double newRotation) {
    this.ellipseRotation = newRotation;
  }

  public RotatedEllipse copy() {
    return new RotatedEllipse(new Pt(center.getX(), center.getY()), a, b, ellipseRotation);
  }

  /**
   * Returns the point on the ellipse boundary that is between the centroid and the target point.
   * This is NOT the nearest point on the ellipse to the target, but that is more complicated to
   * calculate.
   */
  public Pt getCentroidIntersect(Pt target) {
    double x0 = target.x - center.x;
    double y0 = target.y - center.y;
    double xRot = x0 * cos(-ellipseRotation) + y0 * sin(-ellipseRotation);
    double yRot = -x0 * sin(-ellipseRotation) + y0 * cos(-ellipseRotation);
    double denom = Math.sqrt((a * a * yRot * yRot) + (b * b * xRot * xRot));
    double xTermRot = (a * b * xRot) / denom;
    double yTermRot = (a * b * yRot) / denom;
    double xTerm = xTermRot * cos(ellipseRotation) + yTermRot * sin(ellipseRotation);
    double yTerm = -xTermRot * sin(ellipseRotation) + yTermRot * cos(ellipseRotation);
    Pt xNeg = new Pt(-xTerm + center.x, -yTerm + center.y);
    Pt xPos = new Pt(xTerm + center.x, yTerm + center.y);
    double distNeg = xNeg.distance(target);
    double distPos = xPos.distance(target);
    Pt ret = distNeg < distPos ? xNeg : xPos;
    return ret;
  }

  /**
   * Returns a point on the ellipse boundary, parameterized by the given radial angle. If you call
   * this a bunch of times for t=0..2pi you sample the entire ellipse.
   */
  public Pt getEllipticalPoint(double t) {
    double x = (a * cos(t));
    double y = (b * sin(t));
    double xRot = x * cos(ellipseRotation) + y * sin(ellipseRotation);
    double yRot = -x * sin(ellipseRotation) + y * cos(ellipseRotation);
    return new Pt(xRot + center.x, yRot + center.y);
  }

  public double getArea() {
    return Math.PI * a * b;
  }

  public Pt getCentroid() {
    return center;
  }

  public String getDebugString() {
    return "RotatedEllipse[maj:" + num(getMajorRadius()) + ", min: " + num(getMinorRadius())
        + ", rot: " + num(getRotation()) + ", center: " + num(center) + "]";
  }
}
