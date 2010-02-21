package org.six11.util.pen;

import static java.lang.Math.*;

import java.util.ArrayList;
import java.util.List;

import org.six11.util.Debug;

/**
 * 
 * 
 * @author Gabe Johnson <johnsogg@cmu.edu>
 */
public class CardinalSpline {

  /**
   * Given a list of control points, produce a list of nicely interpolated points. It will calculate
   * the slope for each control point using the supplied tightness parameter. The resuling
   * interpolated points will be no greater than maxPointDist units from each other.
   */
  public static List<Pt> interpolateCardinal(List<Pt> controlPoints, double tightness,
      double maxPointDist) {
    List<Pt> interpolatedPoints = new ArrayList<Pt>();
    calculateCardinalSlopeVectors(controlPoints, tightness);
    for (int i = 0; i < controlPoints.size() - 1; i++) {
      interpolatedPoints.addAll(interpolateCardinalPatch(controlPoints.get(i), controlPoints
          .get(i + 1), maxPointDist));
    }
    return interpolatedPoints;
  }

  /**
   * Returns a vector in the direction of a to b, with the magnitude (tightness * ( |b-a| / 2 )).
   */
  private static Vec calculateCardinalSlopeVector(Pt a, Pt b, double tightness) {
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
  private static void calculateCardinalSlopeVectors(List<Pt> points, double tightness) {
    // calculate the tangent vector passing through each point. The magnitude will be somewhere
    // between zero and half the distance between the surrounding points.
    for (int i = 1; i < points.size() - 1; i++) {
      Pt a = points.get(i - 1);
      Pt b = findNextDistinctPoint(points, i, a, 1);
      if (b != null) {
        Vec slope = calculateCardinalSlopeVector(a, b, tightness);
        points.get(i).setVec("slope", slope);
      } else {
        // last ditch. not guaranteed to work since point i-1 might be the first, but then again,
        // you're asking me to get the slope for a set of points that are all the same.
        points.get(i).setVec("slope", a.getVec("slope"));
      }
    }
    if (points.size() > 1) { // don't forget the start/end points.
      Pt first = points.get(0);
      Pt firstNeighbor = findNextDistinctPoint(points, 0, first, 1);
      Pt last = points.get(points.size() - 1);
      Pt lastNeighbor = findNextDistinctPoint(points, points.size() - 1, last, -1);
      first.setVec("slope", calculateCardinalSlopeVector(first, firstNeighbor, tightness));
      last.setVec("slope", calculateCardinalSlopeVector(lastNeighbor, last, tightness));
    }
  }

  private static Pt findNextDistinctPoint(List<Pt> points, int i, Pt compareToMe, int dir) {
    Pt ret = null;
    int incr = 1;
    if (dir > 0) {
      incr = 1;
    } else {
      incr = -1;
    }
    for (int j = i + incr; j < points.size() && j >= 0; j = j + incr) {
      Pt pt = points.get(j);
      if (compareToMe.distance(pt) > 0) {
        ret = pt;
        break;
      }
    }
    if (ret == null) {
      bug("Could not find next distinct point. i=" + i + ", compareToMe=" + Debug.num(compareToMe)
          + ", dir=" + dir);
      for (int j = 0; j < points.size(); j++) {
        bug("  " + j + ": " + Debug.num(points.get(j)));
      }
    }
    return ret;
  }

  /**
   * Using each point's location and 'slope' vector (see other methods in this class), returns a
   * list of interpolated points.
   */
  private static List<Pt> interpolateCardinalPatch(Pt a, Pt b, double maxPointDist) {
    List<Pt> ret = new ArrayList<Pt>();
    Vec m0 = a.getVec("slope");
    Vec m1 = b.getVec("slope");
    int numSteps = Math.max(1, (int) (a.distance(b) / maxPointDist));
    Pt prev = null;
    Pt pt = null;
    if (m0 != null && m1 != null) {
      for (int i = 0; i <= numSteps; i++) {
        double t = (double) i / numSteps;
        double x = a.x * h1(t) + m0.getX() * h2(t) + b.x * h3(t) + m1.getX() * h4(t);
        double y = a.y * h1(t) + m0.getY() * h2(t) + b.y * h3(t) + m1.getY() * h4(t);
        pt = new Pt(x, y);
        ret.add(pt);
        if (prev != null && prev.distance(pt) > maxPointDist) {
          i = 0;
          ret.clear();
          numSteps = Math.max(1, 1 + (int) Math.ceil((double) numSteps * 1.2));
          prev = null;
        } else {
          prev = pt;
        }
      }
    }
    return ret;
  }

  private static void bug(String what) {
    Debug.out("CardinalSpline", what);
  }

  /**
   * Basis function 1.
   */
  private static double h1(double t) {
    return (2 * pow(t, 3) - 3 * pow(t, 2) + 1);
  }

  /**
   * Basis function 2.
   */
  private static double h2(double t) {
    return (pow(t, 3) - 2 * pow(t, 2) + t);
  }

  /**
   * Basis function 3.
   */
  private static double h3(double t) {
    return (-2 * pow(t, 3) + 3 * pow(t, 2));
  }

  /**
   * Basis function 4.
   */
  private static double h4(double t) {
    return (pow(t, 3) - pow(t, 2));
  }

}
