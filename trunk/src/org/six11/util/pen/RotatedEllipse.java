package org.six11.util.pen;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

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

  public RotatedEllipse(Pt center, double a, double b, double ellipseRotation) {
    this.center = center;
    this.a = a;
    this.b = b;
    this.ellipseRotation = ellipseRotation;
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
}
