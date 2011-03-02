package org.six11.util.gui.shape;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.six11.util.Debug;
import org.six11.util.gui.BoundingBox;
import org.six11.util.pen.Functions;
import org.six11.util.pen.Pt;
import org.six11.util.pen.RotatedEllipse;

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
      if (center != null) {
        this.radius = center.distance(s);

        this.startAngle = Functions.makeAnglePositive(Math.toDegrees(-Math.atan2(s.y - center.y,
            s.x - center.x)));
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

    public boolean isValid() {
      return center != null;
    }
  }

  public static class RotatedEllipseShape implements Shape {

    RotatedEllipse ellie;
    int numSegments;
    GeneralPath path;
    Rectangle bounds;

    public RotatedEllipseShape(RotatedEllipse ellie, int numSegments) {
      this.ellie = ellie;
      this.numSegments = numSegments;
    }

    @Override
    public boolean contains(Point2D pt) {
      Pt ix = ellie.getCentroidIntersect(new Pt(pt));
      double distToInput = ellie.getCentroid().distance(pt);
      double distToEdge = ellie.getCentroid().distance(ix);
      return distToInput < distToEdge;
    }

    /**
     * A rectangle is entirely inside an ellipse (or circle) if each corner is inside.
     */
    @Override
    public boolean contains(Rectangle2D r) {
      List<Pt> corners = Functions.getRectangleCorners(r.getBounds());
      boolean ok = true;
      for (int i = 0; i < 4; i++) {
        ok = contains(corners.get(i));
        if (!ok)
          break;
      }
      return ok;
    }

    @Override
    public boolean contains(double x, double y) {
      return contains(new Pt(x, y));
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
      return contains(new Rectangle2D.Double(x, y, w, h));
    }

    @Override
    public Rectangle getBounds() {
      if (bounds == null) {
        getPathIterator(null);
      }
      return bounds;
    }

    @Override
    public Rectangle2D getBounds2D() {
      return getBounds();
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
      if (path == null) {
        path = new GeneralPath();
        BoundingBox bb = new BoundingBox();
        boolean first = true;
        Pt firstPt = null;
        double step = (Math.PI * 2.0) / (double) numSegments;
        for (double t = 0; t < Math.PI * 2.0; t += step) {
          Pt pt = ellie.getEllipticalPoint(t);
          bb.add(pt);
          if (first) {
            firstPt = pt;
            path.moveTo(pt.getX(), pt.getY());
            first = false;
          } else {
            path.lineTo(pt.getX(), pt.getY());
          }
        }
        path.lineTo(firstPt.getX(), firstPt.getY());
        bounds = bb.getRectangle().getBounds();
      }
      return path.getPathIterator(at);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatnessIgnored) {
      return getPathIterator(at);
    }

    @Override
    public boolean intersects(Rectangle2D r) {
      PathIterator pi = getPathIterator(null);
      boolean ix = false;
      double[] coords = new double[6];
      while (!pi.isDone()) {
        pi.currentSegment(coords);
        ix = r.contains(coords[0], coords[1]);
        if (ix) {
          break;
        }
        pi.next();
      }
      return ix;
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
      return intersects(new Rectangle2D.Double(x, y, w, h));
    }

  }

  @SuppressWarnings("unused")
  private static void bug(String what) {
    Debug.out("ShapeFactory", what);
  }
}
