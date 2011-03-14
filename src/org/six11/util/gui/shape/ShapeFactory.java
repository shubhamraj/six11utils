package org.six11.util.gui.shape;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import static java.lang.Math.toRadians;
import static java.lang.Math.abs;

import org.six11.util.Debug;
import static org.six11.util.Debug.num;
import org.six11.util.gui.BoundingBox;
import org.six11.util.pen.Functions;
import org.six11.util.pen.IntersectionData;
import org.six11.util.pen.Line;
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

    public boolean contains(Point2D pt) {
      boolean ret;
      if (ellie.isRestrictedArc()) {
        ret = false;
      } else {
        Pt ix = ellie.getCentroidIntersect(new Pt(pt));
        double distToInput = ellie.getCentroid().distance(pt);
        double distToEdge = ellie.getCentroid().distance(ix);
        ret = distToInput < distToEdge;
      }
      return ret;
    }

    /**
     * A rectangle is entirely inside an ellipse (or circle) if each corner is inside.
     */
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

    public boolean contains(double x, double y) {
      return contains(new Pt(x, y));
    }

    public boolean contains(double x, double y, double w, double h) {
      return contains(new Rectangle2D.Double(x, y, w, h));
    }

    public Rectangle getBounds() {
      if (bounds == null) {
        getPathIterator(null);
      }
      return bounds;
    }

    public Rectangle2D getBounds2D() {
      return getBounds();
    }

    public PathIterator getPathIterator(AffineTransform at) {
      if (path == null) {

        BoundingBox bb = new BoundingBox();
        path = new GeneralPath();
        List<Pt> surface = getSegmentedSurface();
        if (ellie.isRestrictedArc()) {
          List<Pt> region = ellie.getRegionPoints();
          int segA = findIntersectionIndex(surface, region.get(0));
          int segB = findIntersectionIndex(surface, region.get(1));
          int segC = findIntersectionIndex(surface, region.get(2));
          while (segA == segB || segA == segC || segB == segC) {
            bug("Dang I have to ratchet up number of segments to " + (2 * numSegments)
                + " and try again (" + segA + " " + segB + " " + segC + ")");
            numSegments = numSegments * 2;
            surface = getSegmentedSurface();
            region = ellie.getRegionPoints();
            segA = findIntersectionIndex(surface, region.get(0));
            segB = findIntersectionIndex(surface, region.get(1));
            segC = findIntersectionIndex(surface, region.get(2));
          }
          List<Pt> arcPoints = new ArrayList<Pt>();
          if (segA > segB && segB > segC) { //        segA -> segC
            addRange(arcPoints, surface, segA, segC);
          } else if (segA > segB && segB < segC) {
            if (segA > segC) {
              addRange(arcPoints, surface, segA, surface.size() - 1);
              addRange(arcPoints, surface, 0, segC);
            } else {
              addRange(arcPoints, surface, segA, 0);
              addRange(arcPoints, surface, surface.size() - 1, segC);
            }
          } else if (segA < segB && segB > segC) { // segA -> n; 0 -> segC
            if (segA > segC) {
              addRange(arcPoints, surface, segA, surface.size() - 1);
              addRange(arcPoints, surface, 0, segC);
            } else {
              addRange(arcPoints, surface, segA, 0);
              addRange(arcPoints, surface, surface.size() - 1, segC);
            }
          } else if (segA < segB && segB < segC) { // segA -> segC
            addRange(arcPoints, surface, segA, segC);
          } else {
            bug("Warning: couldn't figure out the order of intersections. segA, segB, segC: "
                + segA + ", " + segB + ", " + segC);
          }
          path = makeLinePath(arcPoints, false);
          bb = new BoundingBox(arcPoints);
        } else {
          path = makeLinePath(surface, true);
          bb = new BoundingBox(surface);
        }
        if (bb.isValid()) {
          bounds = bb.getRectangle().getBounds();
        } else {
          Debug.stacktrace("Bounding box is invalid.", 8);
          bug("Is this a restricted ellipse? " + ellie.isRestrictedArc());
          if (ellie.isRestrictedArc()) {
            bug("Region points: " + Debug.num(ellie.getRegionPoints(), " "));

          }
        }
      }

      return path.getPathIterator(at);
    }

    private void addRange(List<Pt> dst, List<Pt> src, int startIdx, int endIdx) {
      if (startIdx < endIdx) { // ascending
        for (int i = startIdx; i <= endIdx; i++) {
          dst.add(src.get(i));
        }
      } else { // descending
        for (int i = startIdx; i >= endIdx; i--) {
          dst.add(src.get(i));
        }
      }
    }

    private List<Pt> getSegmentedSurface() {
      double step = (Math.PI * 2.0) / (double) numSegments;
      List<Pt> ret = new ArrayList<Pt>();
      for (double t = 0; t < Math.PI * 2.0; t += step) {
        Pt pt = ellie.getEllipticalPoint(t);
        ret.add(pt);
      }
      return ret;
    }

    private int findIntersectionIndex(List<Pt> surface, Pt pt) {
      int ret = -1;
      for (int i = 0; i < surface.size(); i++) {
        Pt a = surface.get(i);
        Pt b = (i + 1 >= surface.size()) ? surface.get(0) : surface.get(i + 1);
        Line toPt = new Line(ellie.getCentroid(), pt);
        Line seg = new Line(a, b);
        IntersectionData ix = Functions.getIntersectionData(toPt, seg);
        if (ix.intersectsOnLineTwo() && ix.getLineOneParam() > 0) {
          ret = i;
          break;
        }
      }
      return ret;
    }

    public PathIterator getPathIterator(AffineTransform at, double flatnessIgnored) {
      return getPathIterator(at);
    }

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

    public boolean intersects(double x, double y, double w, double h) {
      return intersects(new Rectangle2D.Double(x, y, w, h));
    }

  }

  public static GeneralPath makeLinePath(List<Pt> points, boolean closed) {
    boolean first = true;
    GeneralPath ret = new GeneralPath();
    for (Pt pt : points) {
      if (first) {
        ret.moveTo(pt.getX(), pt.getY());
        first = false;
      } else {
        ret.lineTo(pt.getX(), pt.getY());
      }
    }
    if (closed) {
      ret.lineTo(points.get(0).getX(), points.get(0).getY());
    }
    return ret;
  }

  @SuppressWarnings("unused")
  private static void bug(String what) {
    Debug.out("ShapeFactory", what);
  }

}
