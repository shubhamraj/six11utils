// $Id$

package org.six11.util.pen;

import java.util.List;
import java.util.ArrayList;

/**
 * 
 **/
public class ConvexHull {

  protected List<Pt> points;
  protected List<Pt> rotatedRect;
  protected double rotatedRectArea;
  protected Pt convexCentroid;
  protected double convexArea;

  public ConvexHull(List<Pt> input) {
    points = Functions.getConvexHull(input);
    rotatedRect = null;
    rotatedRectArea = -1.0;
    convexCentroid = null;
    convexArea = -1.0;
  }

  public List<Pt> getHull() {
    return points;
  }

  public List<Pt> getRotatedRect() {
    if (rotatedRect == null) {
      // Antipodal anti = new Antipodal(points);
      Antipodal2 anti = new Antipodal2(points);
      rotatedRect = anti.getMinimumBoundingRect();
    }
    return rotatedRect;
  }

  public double getRotatedRectArea() {
    if (rotatedRectArea < 0.0) {
      List<Pt> rect = getRotatedRect();
      double distA = Functions.getDistanceBetween(rect.get(0), rect.get(1));
      double distB = Functions.getDistanceBetween(rect.get(1), rect.get(2));
      rotatedRectArea = distA * distB;
    }
    return rotatedRectArea;
  }

  public Pt getConvexCentroid() {
    if (convexCentroid == null) {
      calcCentroidAndArea();
    }
    return convexCentroid;
  }

  public double getConvexArea() {
    if (convexArea < 0.0) {
      calcCentroidAndArea();
    }
    return convexArea;
  }

  private void calcCentroidAndArea() {
    List<Pt> rect = getRotatedRect();
    Pt m = Functions.getMean(rect); // the mean of the rect.
    List<Pt> c = new ArrayList<Pt>(); // triangle centroids
    List<Double> a = new ArrayList<Double>(); // triangle areas
    int n = points.size();
    Pt p1, p2;
    for (int i = 0; i < n; i++) {
      p1 = points.get(i);
      p2 = points.get(next(i, n));
      c.add(Functions.getMean(p1, p2, m));
      Vec v1 = new Vec(p1, m);
      Vec v2 = new Vec(p2, m);
      a.add(Math.abs(Functions.getDeterminant(v1, v2)));
    }
    convexArea = Functions.getSum(a);
    if (convexArea == 0.0) {
      convexArea = 1.0;
      convexCentroid = m;
    } else {
      double sumX = 0.0;
      double sumY = 0.0;
      for (int i = 0; i < n; i++) {
        Pt scaledC = (c.get(i).getScaled(a.get(i)));
        sumX = sumX + scaledC.getX();
        sumY = sumY + scaledC.getY();
      }
      double cX = sumX / convexArea;
      double cY = sumY / convexArea;
      convexCentroid = new Pt(cX, cY);
    }
  }

  private static int next(int cur, int upperBound) {
    return (cur + 1) % upperBound;
  }

}
