package org.six11.util.pen;

import static java.lang.Math.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * 
 * @author Gabe Johnson <johnsogg@cmu.edu>
 */
public class CardinalSpline {
  public static double h1(double t) {
    return (2 * pow(t, 3) - 3 * pow(t, 2) + 1);
  }

  public static double h2(double t) {
    return (pow(t, 3) - 2 * pow(t, 2) + t);
  }

  public static double h3(double t) {
    return (-2 * pow(t, 3) + 3 * pow(t, 2));
  }

  public static double h4(double t) {
    return (pow(t, 3) - pow(t, 2));
  }

  /**
   * Does the same math as calculateCardinalSlopeVectors, but only for the last two points. This is
   * useful if new points are added to a list, but the slope vectors have already been computed for
   * the previously added points. In that case, only the slope values for the last two points will
   * be different.
   * 
   * @param points
   *          the list of control points of a cardinal spline
   * @param tightness
   *          roughly describes how rounded the corners will be. To generate a Catmull-Rom spline,
   *          supply tightness = 1. For completely sharp corners, use tightness = 0.
   */
  public static void updateCardinalSlopeVectors(List<Pt> points, double tightness) {
    if (points.size() > 2) {
      Pt a = points.get(points.size() - 3);
      Pt b = points.get(points.size() - 2);
      Pt c = points.get(points.size() - 1);
      b.setVec("slope", calculateCardinalSlopeVector(a, c, tightness));
      c.setVec("slope", calculateCardinalSlopeVector(b, c, tightness));
    }
  }

  /**
   * Returns a vector in the direction of a to b, with the magnitude (tightness * ( |b-a| / 2 )).
   */
  public static Vec calculateCardinalSlopeVector(Pt a, Pt b, double tightness) {
    Vec v = new Vec(a, b);
    double mag = (tightness) * (v.mag() / 2);
    return v.getVectorOfMagnitude(mag);
  }

  /**
   * Calculate the cardinal slope vector for all points. On return, each point in the supplied list
   * has a "slope" attribute (which is a Vec).
   * 
   * @param points
   *          the list of control points of a cardinal spline.
   * @param tightness
   *          roughly describes how rounded the corners will be. To generate a Catmull-Rom spline,
   *          supply tightness = 1. For completely sharp corners, use tightness = 0.
   */
  public static void calculateCardinalSlopeVectors(List<Pt> points, double tightness) {
    // calculate the tangent vector passing through each point. The magnitude will be somewhere
    // between zero and half the distance between the surrounding points.
    for (int i = 1; i < points.size() - 1; i++) {
      Vec slope = calculateCardinalSlopeVector(points.get(i - 1), points.get(i + 1), tightness);
      points.get(i).setVec("slope", slope);
    }
    if (points.size() > 2) { // don't forget the start/end points.
      Pt first = points.get(0);
      Pt last = points.get(points.size() - 1);
      first.setVec("slope", calculateCardinalSlopeVector(first, points.get(1), tightness));
      last.setVec("slope", calculateCardinalSlopeVector(points.get(points.size() - 2), last,
          tightness));
    }
  }

  public static List<Pt> interpolateCardinal(List<Pt> controlPoints) {
    List<Pt> interpolatedPoints = new ArrayList<Pt>();
    for (int i = 0; i < controlPoints.size() - 1; i++) {
      interpolatedPoints.addAll(interpolateCardinalPatch(controlPoints.get(i), controlPoints
          .get(i + 1)));
    }
    return interpolatedPoints;
  }

  /**
   * Using each point's location and 'slope' vector (see other methods in this class), returns a
   * list of interpolated points.
   */
  public static List<Pt> interpolateCardinalPatch(Pt a, Pt b) {
    List<Pt> ret = new ArrayList<Pt>();
    Vec m0 = a.getVec("slope");
    Vec m1 = b.getVec("slope");
    if (m0 != null && m1 != null) {
      for (int i = 0; i < 20; i++) {
        double t = (double) i / 20.0;
        double x = a.x * h1(t) + m0.getX() * h2(t) + b.x * h3(t) + m1.getX() * h4(t);
        double y = a.y * h1(t) + m0.getY() * h2(t) + b.y * h3(t) + m1.getY() * h4(t);
        ret.add(new Pt(x, y));
      }
    }
    return ret;
  }

}
