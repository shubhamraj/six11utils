// $Id$

package org.six11.util.pen;

import org.six11.util.Debug;

/**
 * 
 **/
public class TestFunction  {
  
  public static void main(String[] args) {
    testRotatePointAboutPivot();
  }

  public static void assertTolerance(double actualValue, double expectedValue, double tolerance) {
    double err = Math.abs(actualValue - expectedValue);
    if (err > tolerance) {
      Debug.stacktrace("TestFunction: tolerance failed (" + actualValue + 
		       " != " + expectedValue + " (tol: " + tolerance + ")", 3);
    }
  }

  public static void testRotatePointAboutPivot() {
    Pt point = new Pt(10d, 8d);
    Pt pivot = new Pt(8d, 8d);
    Pt rotated = Functions.rotatePointAboutPivot(point, pivot, Math.toRadians(90d));
    assertTolerance(rotated.getX(), 8.0, 0.001);
    assertTolerance(rotated.getY(), 10.0, 0.001);
  }
}
