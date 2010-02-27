// $Id: Antipodal2.java 55 2010-02-21 00:31:17Z gabe.johnson@gmail.com $

package org.six11.util.pen;

import java.util.List;
import java.util.ArrayList;

/**
 * 
 **/
public class Antipodal {

  List<Pt> mbr; // minimum bounding rectangle

  public Antipodal(List<Pt> convexHull) {
    double bestArea = Double.MAX_VALUE;
    double d;
    List<Pt> r;
    for (int i = 0; i < convexHull.size(); i++) {
      r = findBox(i, convexHull);
      d = area(r);
      if (d < bestArea) {
        bestArea = d;
        mbr = r;
      }
    }

    // just debug the best one
    // findBox(bestIdx, convexHull); // necessary?
  }

  private static double area(List<Pt> rect) {
    double distA = Functions.getDistanceBetween(rect.get(0), rect.get(1));
    double distB = Functions.getDistanceBetween(rect.get(1), rect.get(2));
    return distA * distB;
  }

  private List<Pt> findBox(int i, List<Pt> convexHull) {
    // algorithm is on page 161
    convexHull.size();
    Line a = lineAt(i, convexHull);
    Vec aVec = new Vec(a);
    Vec aVecNormal = aVec.getNormal();
    Extreme indices = findExtremePoints(a, convexHull);
    int j = indices.getAbsoluteMaxIdx();
    Line b = new Line(convexHull.get(j), aVec);
    Line perpA = new Line(convexHull.get(i), aVecNormal);
    Pt k = Functions.getIntersectionPoint(perpA, b);
    Line c = new Line(convexHull.get(i), k);
    indices = findExtremePoints(c, convexHull);
    Line d = new Line(convexHull.get(indices.lowestIdx), aVecNormal);
    Line e = new Line(convexHull.get(indices.highestIdx), aVecNormal);
    List<Pt> ix = new ArrayList<Pt>();
    ix.add(Functions.getIntersectionPoint(a, d));
    ix.add(Functions.getIntersectionPoint(a, e));
    ix.add(Functions.getIntersectionPoint(b, d));
    ix.add(Functions.getIntersectionPoint(b, e));
    List<Pt> ret = Graham.getConvexHull(ix);
    return ret;
  }

  private Extreme findExtremePoints(Line line, List<Pt> convexHull) {
    double smallestVal = Double.MAX_VALUE;
    double highestVal = -Double.MAX_VALUE;
    int smallestIdx = -1;
    int highestIdx = -1;
    double d;
    for (int i = 0; i < convexHull.size(); i++) {
      d = Functions.getSignedDistanceBetweenPointAndLine(convexHull.get(i), line);
      if (d < smallestVal) {
        smallestIdx = i;
        smallestVal = d;
      }
      if (d > highestVal) {
        highestIdx = i;
        highestVal = d;
      }
    }
    return new Extreme(smallestIdx, smallestVal, highestIdx, highestVal);
  }

  private static class Extreme {
    int lowestIdx;
    double lowestVal;
    int highestIdx;
    double highestVal;

    Extreme(int lowestIdx, double lowestVal, int highestIdx, double highestVal) {
      this.lowestIdx = lowestIdx;
      this.lowestVal = lowestVal;
      this.highestIdx = highestIdx;
      this.highestVal = highestVal;
    }

    int getAbsoluteMaxIdx() {
      return ((Math.abs(lowestVal) > Math.abs(highestVal)) ? lowestIdx : highestIdx);
    }
  }

  public List<Pt> getMinimumBoundingRect() {
    return mbr;
  }

  private static Line lineAt(int i, List<Pt> points) {
    int j = inc(points, i);
    return new Line(points.get(i), points.get(j));
  }

  private static int inc(List<Pt> list, int current) {
    return inc(list.size(), current);
  }

  private static int inc(int n, int current) {
    return (current + 1) % n;
  }
}
