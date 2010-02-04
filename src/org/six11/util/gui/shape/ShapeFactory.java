package org.six11.util.gui.shape;

import java.awt.geom.Arc2D;

import org.six11.util.Debug;
import org.six11.util.pen.Functions;
import org.six11.util.pen.Pt;

/**
 * 
 * 
 * @author Gabe Johnson <johnsogg@cmu.edu>
 */
public abstract class ShapeFactory {

  /**
   * Returns a portion of a circle. The circle is based on three points: s, mid, and e. s and e are
   * the endpoints of the desired arc. mid is used only to define the circle, and must not be equal
   * to or colinear with the other two points.
   * 
   * See http://johnsogg.blogspot.com/2010/01/how-to-use-javas-javaawtgeomarc2d.html for a pretty
   * graphic.
   */
  public static Arc2D makeArc(Pt s, Pt mid, Pt e) {
    // Pt c = Functions.getCircleCenter(s, mid, e);
    // double radius = c.distance(s);
    //
    // double startAngle = Functions.makeAnglePositive(Math.toDegrees(-Math
    // .atan2(s.y - c.y, s.x - c.x)));
    // double midAngle = Functions.makeAnglePositive(Math.toDegrees(-Math.atan2(mid.y - c.y, mid.x
    // - c.x)));
    // double endAngle = Functions
    // .makeAnglePositive(Math.toDegrees(-Math.atan2(e.y - c.y, e.x - c.x)));
    //
    // // Now compute the phase-adjusted angles begining from startAngle, moving positive and
    // negative.
    // double midDecreasing = Functions.getNearestAnglePhase(startAngle, midAngle, -1);
    // double midIncreasing = Functions.getNearestAnglePhase(startAngle, midAngle, 1);
    // double endDecreasing = Functions.getNearestAnglePhase(midDecreasing, endAngle, -1);
    // double endIncreasing = Functions.getNearestAnglePhase(midIncreasing, endAngle, 1);
    //
    // // Each path from start -> mid -> end is technically, but one will wrap around the entire
    // // circle, which isn't what we want. Pick the one that with the smaller angular change.
    // double extent = 0;
    // if (Math.abs(endDecreasing - startAngle) < Math.abs(endIncreasing - startAngle)) {
    // extent = endDecreasing - startAngle;
    // } else {
    // extent = endIncreasing - startAngle;
    // }
    ArcData data = new ArcData(s, mid, e);
    return new Arc2D.Double(data.center.x - data.radius, data.center.y - data.radius,
        data.radius * 2, data.radius * 2, data.startAngle, data.extent, Arc2D.OPEN);
  }

  public static class ArcData {
    public Pt start, mid, end, center;
    public double radius, extent, startAngle, midAngle, endAngle;

    public ArcData(Pt s, Pt mid, Pt e) {
      this.start = s;
      this.mid = mid;
      this.end = e;

      this.center = Functions.getCircleCenter(s, mid, e);
      this.radius = center.distance(s);

      this.startAngle = Functions.makeAnglePositive(Math.toDegrees(-Math.atan2(s.y - center.y, s.x
          - center.x)));
      this.midAngle = Functions.makeAnglePositive(Math.toDegrees(-Math.atan2(mid.y - center.y,
          mid.x - center.x)));
      this.endAngle = Functions.makeAnglePositive(Math.toDegrees(-Math.atan2(e.y - center.y, e.x
          - center.x)));

      // Now compute the phase-adjusted angles begining from startAngle, moving positive and
      // negative.
      double midDecreasing = Functions.getNearestAnglePhase(startAngle, midAngle, -1);
      double midIncreasing = Functions.getNearestAnglePhase(startAngle, midAngle, 1);
      double endDecreasing = Functions.getNearestAnglePhase(midDecreasing, endAngle, -1);
      double endIncreasing = Functions.getNearestAnglePhase(midIncreasing, endAngle, 1);

      // Each path from start -> mid -> end is technically, but one will wrap around the entire
      // circle, which isn't what we want. Pick the one that with the smaller angular change.
      this.extent = 0;
      if (Math.abs(endDecreasing - startAngle) < Math.abs(endIncreasing - startAngle)) {
        this.extent = endDecreasing - startAngle;
      } else {
        this.extent = endIncreasing - startAngle;
      }
    }
  }

  private static void bug(String what) {
    Debug.out("ShapeFactory", what);
  }
}
