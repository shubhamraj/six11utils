package org.six11.util.tmp2;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import static java.lang.Math.min;
import static java.lang.Math.abs;
import static java.lang.Math.toDegrees;
import static java.lang.Math.max;
import static org.six11.util.Debug.num;
import org.six11.util.Debug;
import org.six11.util.pen.Functions;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;
import org.six11.util.tmp2.Segment.Terminal;

public class Latcher {

  private static final double maxLatchDist = 40;
  private static final double minLatchDist = 9;
  private static final double latchNumerator = 9;

  JunctionFinder jf;
  private static final double continuationNearnessThreshold = 20;

  public Latcher(JunctionFinder jf) {
    this.jf = jf;
  }

  public void latch() {
    List<Segment> last = jf.getSketchBook().getLastSegmentBatch();
    for (Segment seg : last) {
      double latchDist = getLatchRadius(seg);
      List<Segment.Terminal> terminals = seg.getTerminals();
      for (Segment.Terminal term : terminals) {
        jf.getDebugThing().drawTerm(term, getLatchRadius(term.getSegment()));
        if (!term.isFixed()) {
          List<Segment.Terminal> nearbySegs = jf.getSketchBook().getTerminalsNear(term, latchDist);
          for (Segment.Terminal near : nearbySegs) {
            latch(term, near);
          }
        }
      }
    }
  }

  private double getLatchRadius(Segment seg) {
    double latchDist = seg.length() / latchNumerator;
    latchDist = min(maxLatchDist, latchDist);
    latchDist = max(minLatchDist, latchDist);
    return latchDist;
  }

  /**
   * Latch the segments at the given terminals if any latch condition passes.
   * 
   * @param recent
   *          The terminal that will be transformed. Consider this the 'new' stroke (even though
   *          they could both come from the same batch).
   * @param near
   *          The terminal that serves as the reference. The 'recent' terminal will latch onto the
   *          'near' terminal.
   */
  private void latch(Terminal recent, Terminal near) {
    if (!recent.getPoint().isSameLocation(near.getPoint())) {
      double latchRadiusTerm = getLatchRadius(recent.getSegment());
      double latchRadiusNear = getLatchRadius(near.getSegment());
      //      jf.getDebugThing().drawTerm(term, latchRadiusTerm);
      //      jf.getDebugThing().drawTerm(near, latchRadiusNear);
      double distBetwixt = near.getPoint().distance(recent.getPoint());
      boolean latched = false;

      // Three latching types: co-terminate, continuation, and tee. 

      // 1. Co-terminate
      if ((distBetwixt < latchRadiusNear) && (distBetwixt < latchRadiusTerm)) {
        Pt ix = Functions.getIntersectionPoint(recent.getLine(), near.getLine());
        if (ix != null && ix.distance(recent.getPoint()) < latchRadiusTerm
            && ix.distance(near.getPoint()) < latchRadiusNear) {
          bug("Coterminate latch: " + recent + ", " + near);
          performCoterminateLatch(recent, near);
          latched = true;
          bug("set latched = true (co-terminate)");
        }
      }

      // 2. Continuation
      if (!latched) {
        //        bug("trying to find continuation...");
        if (recent == near || recent.getSegment() == near.getSegment()) {
          //          bug("Avoiding self-continuation. Silly.");
        } else {
          List<Pt> recentPoints = recent.getSurfacePolyline();
          List<Pt> nearPoints = near.getSurfacePolyline();
          // see if the terminal points are near the other segment.
          Pt np = Functions.getNearestPointOnPolyline(recent.getPoint(), nearPoints);
          Pt rp = Functions.getNearestPointOnPolyline(near.getPoint(), recentPoints);
          double npDist = np.distance(recent.getPoint());
          double rpDist = rp.distance(near.getPoint());
          if (npDist < continuationNearnessThreshold && rpDist < continuationNearnessThreshold) {
            //          bug("recentPoints: " + recentPoints.size());
            //          bug("nearPoints: " + nearPoints.size());
            List<Pt> recentPointsOverlap = new ArrayList<Pt>();
            List<Pt> nearPointsOverlap = new ArrayList<Pt>();
            Functions.createOverlap(recentPoints, nearPoints, recentPointsOverlap,
                nearPointsOverlap, continuationNearnessThreshold, true);
            //          bug("overlap a: " + recentPointsOverlap.size());
            //          bug("overlap b: " + nearPointsOverlap.size());
            if (recentPointsOverlap.size() > 1 && nearPointsOverlap.size() > 1) {
              Vec recentVec = new Vec(recentPointsOverlap.get(0),
                  recentPointsOverlap.get(recentPointsOverlap.size() - 1));
              Vec nearVec = new Vec(nearPointsOverlap.get(0),
                  nearPointsOverlap.get(nearPointsOverlap.size() - 1));
              bug("Vector magnitudes: " + num(recentVec.mag()) + ", " + nearVec.mag());
              double angle = min(abs(Functions.getSignedAngleBetween(recentVec, nearVec)),
                  abs(Functions.getSignedAngleBetween(recentVec.getFlip(), nearVec)));
              bug("Angle at continuation: " + num(toDegrees(angle)) + " degrees");
              jf.getDebugThing().drawPoints(JunctionFinder.DB_DOT_LAYER, recentPointsOverlap,
                  Color.BLUE, Color.BLUE);
              jf.getDebugThing().drawPolyline(JunctionFinder.DB_DOT_LAYER, recentPointsOverlap,
                  Color.BLUE, 6);
              jf.getDebugThing().drawPoints(JunctionFinder.DB_DOT_LAYER, nearPointsOverlap,
                  Color.GREEN, Color.GREEN);
              jf.getDebugThing().drawPolyline(JunctionFinder.DB_DOT_LAYER, nearPointsOverlap,
                  Color.GREEN, 6);
              bug("Continution latch: " + recent + ", " + near);
              performContinuationLatch(recent, near);
              latched = true;
            }
          }
        }
      }
    }
  }

  //  /**
  //   * Creates a subset of 'points', including only those that are within 't' units to the polyline.
  //   * The results are placed in the 'ret' list in the same order they appear in 'points'. The
  //   * algorithm stops when it finds a point that is beyond 't' units away.
  //   * 
  //   * @param points
  //   *          source list of points. a subset ends up in 'ret'
  //   * @param polyline
  //   *          taken to be a polyline of discrete line segments
  //   * @param ret
  //   *          results are put in this list.
  //   * @param t
  //   *          nearness threshold
  //   */
  //  private void createOverlap(List<Pt> points, List<Pt> polyline, List<Pt> ret, double t) {
  //    for (Pt pt : points) {
  //      Pt where = Functions.getNearestPointOnPolyline(pt, polyline);
  //      if (where != null && where.getDouble("nearest-polyline") <= t) {
  //        bug("  Points are " + num(where.getDouble("nearest-polyline")) + " apart. Overlap yay.");
  //        ret.add(pt);
  //      } else {
  //        bug("  Points are too far apart or there isn't a nearest point. Stopping!");
  //        break;
  //      }
  //    }
  //  }

  private void performContinuationLatch(Terminal recent, Terminal near) {
    // for now just see if the coterminate code does the trick.
    performCoterminateLatch(recent, near);
  }

  private void performCoterminateLatch(Terminal newTerm, Terminal other) {
    Segment seg = newTerm.getSegment();
    if (seg.getType() == Segment.Type.Curve || seg.getType() == Segment.Type.Line
        || seg.getType() == Segment.Type.EllipticalArc) {
      int idxHinge = seg.getP1() == newTerm.getOpposingTermPoint() ? 0 : seg.points.size() - 1;
      int idxReference = seg.getP1() == newTerm.getOpposingTermPoint() ? seg.points.size() - 1 : 0;
      Pt target = other.getPoint();
      List<Pt> transformed = Functions.hinge(idxHinge, idxReference, target, seg.points);
      if (transformed.size() == seg.points.size()) {
        for (int i = 0; i < transformed.size(); i++) {
          seg.points.get(i).setLocation(transformed.get(i));
        }
      } else {
        Debug.warn(this, "Can't latch: transformed list wrong size!" + transformed.size() + " != "
            + seg.points.size());
      }
      seg.setModified();
    } else {
      Debug.warn(this, "Can't latch unknown segment type: " + seg.getType());
    }
  }

  private void bug(String what) {
    Debug.out("Latcher", what);
  }

}
