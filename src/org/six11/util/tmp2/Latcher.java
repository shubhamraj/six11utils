package org.six11.util.tmp2;

import java.util.List;
import static java.lang.Math.min;
import static java.lang.Math.max;
import static org.six11.util.Debug.num;
import org.six11.util.Debug;
import org.six11.util.pen.Functions;
import org.six11.util.pen.Pt;
import org.six11.util.tmp2.Segment.Terminal;

public class Latcher {

  private static final double maxLatchDist = 40;
  private static final double minLatchDist = 9;
  private static final double latchNumerator = 9;

  JunctionFinder jf;

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

  private void latch(Terminal term, Terminal near) {
    if (!term.getPoint().isSameLocation(near.getPoint())) {
      double latchRadiusTerm = getLatchRadius(term.getSegment());
      double latchRadiusNear = getLatchRadius(near.getSegment());
      //      jf.getDebugThing().drawTerm(term, latchRadiusTerm);
      //      jf.getDebugThing().drawTerm(near, latchRadiusNear);
      double distBetwixt = near.getPoint().distance(term.getPoint());
      if ((distBetwixt < latchRadiusNear) && (distBetwixt < latchRadiusTerm)) {
        Pt ix = Functions.getIntersectionPoint(term.getLine(), near.getLine());
        if (ix != null && ix.distance(term.getPoint()) < latchRadiusTerm
            && ix.distance(near.getPoint()) < latchRadiusNear) {
          performLatch(term, near);
        }
      }
    }
  }

  private void performLatch(Terminal term, Terminal other) {
    Segment seg = term.getSegment();
    if (seg.getType() == Segment.Type.Curve || seg.getType() == Segment.Type.Line
        || seg.getType() == Segment.Type.EllipticalArc) {
      int idxHinge = seg.getP1() == term.getOpposingTermPoint() ? 0 : seg.points.size() - 1;
      int idxReference = seg.getP1() == term.getOpposingTermPoint() ? seg.points.size() - 1 : 0;
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
