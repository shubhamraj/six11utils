// $Id$

package org.six11.util.pen;

import java.util.List;
import java.util.ArrayList;

/**
 * 
 **/
public class Antipodal2 {

  public static void main(String[] args) {
//    Sequence seq = Sequence.loadFromFile(args[0]);
//    if (seq.get(0).equals(seq.getLast())) {
//      seq.removeLast();
//    }
//    List<Pt> points = Graham.getConvexHull(seq.getPoints());
//
//    new Antipodal2(points);
    System.out.println("Broken until Sequence.loadFromFile() has a replacement");
  }

  List<Pt> mbr; // minimum bounding rectangle

  public Antipodal2(List<Pt> convexHull) {
    int bestIdx = -1;
    double bestArea = Double.MAX_VALUE;
    double d;
    List<Pt> r;
    for (int i = 0; i < convexHull.size(); i++) {
      r = findBox(i, convexHull, false);
      d = area(r);
      if (d < bestArea) {
        bestArea = d;
        bestIdx = i;
        mbr = r;
      }
    }

    // just debug the best one
    findBox(bestIdx, convexHull, true);
  }

  private static double area(List<Pt> rect) {
    double distA = Functions.getDistanceBetween(rect.get(0), rect.get(1));
    double distB = Functions.getDistanceBetween(rect.get(1), rect.get(2));
    return distA * distB;
  }

  private List<Pt> findBox(int i, List<Pt> convexHull, boolean debug) {
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
    if (debug) {
      Graham.outputFile("ap-input.data", convexHull);
      Graham.outputFile("ap-a.data", Graham.makeList(makeLongLine(a, 100)));
      Graham.outputFile("ap-b.data", Graham.makeList(makeLongLine(b, 100)));
      Graham.outputFile("ap-c.data", Graham.makeList(makeLongLine(c, 100)));
      Graham.outputFile("ap-d.data", Graham.makeList(makeLongLine(d, 100)));
      Graham.outputFile("ap-e.data", Graham.makeList(makeLongLine(e, 100)));
      Graham.outputFile("ap-bounds.data", ret);
    }
    return ret;
  }

  private Line makeLongLine(Line in, double len) {
    Vec dirA = new Vec(in).getVectorOfMagnitude(len / 2.0);
    Vec dirB = dirA.getFlip();
    Pt newA = Functions.getEndPoint(in.getStart(), dirA);
    Pt newB = Functions.getEndPoint(in.getStart(), dirB);
    return new Line(newA, newB);
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
