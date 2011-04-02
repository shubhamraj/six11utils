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
      bug("Using latchDist: " + num(latchDist));
      List<Segment.Terminal> terminals = seg.getTerminals();
      for (Segment.Terminal term : terminals) {
        bug("Thinking about term: " + term);
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
      bug("maybe latch " + term + " to " + near);
      double latchRadiusTerm = getLatchRadius(term.getSegment());
      double latchRadiusNear = getLatchRadius(near.getSegment());
      //      jf.getDebugThing().drawTerm(term, latchRadiusTerm);
      //      jf.getDebugThing().drawTerm(near, latchRadiusNear);
      double distBetwixt = near.getPoint().distance(term.getPoint());
      if ((distBetwixt < latchRadiusNear) && (distBetwixt < latchRadiusTerm)) {
        bug(" ... they are close enough...");
        Pt ix = Functions.getIntersectionPoint(term.getLine(), near.getLine());
        if (ix != null && ix.distance(term.getPoint()) < latchRadiusTerm
            && ix.distance(near.getPoint()) < latchRadiusNear) {
          bug(" ... the intersection is close. Latch!");
          performLatch(term, near);
        } else {
          bug(" ... intersection not close enough.");
        }
      } else {
        bug(" ... not close enough.");
      }
    }
  }

  private void performLatch(Terminal term, Terminal other) {
    Segment seg = term.getSegment();
    if (seg.getType() == Segment.Type.Line) {
      term.getPoint().setLocation(other.getPoint().getX(), other.getPoint().getY());
    } else if (seg.getType() == Segment.Type.Curve) {
      Pt hinge = term.getOpposingTermPoint();
      double dx = other.getPoint().getX() - term.getPoint().getX();
      double dy = other.getPoint().getY() - term.getPoint().getY();
      int start = seg.getP1() == hinge ? 0 : seg.points.size() - 1;
      int dir = seg.getP1() == hinge ? 1 : -1;
      double segLength = seg.ctrlPointLength();
      double runningDist = 0;
      Pt prev = null;
      for (int i = start; i >= 0 && i < seg.points.size(); i += dir) {
        Pt pt = seg.points.get(i);
        Pt ptOrig = pt.copyXYT();
        if (prev != null) {
          runningDist = runningDist + prev.distance(pt);
          double frac = runningDist / segLength;
          double localDx = frac * dx;
          double localDy = frac * dy;
          pt.setLocation(pt.getX() + localDx, pt.getY() + localDy);
        }
        prev = ptOrig;
      }
      seg.setModified();
    }
  }

  private void bug(String what) {
    Debug.out("Latcher", what);
  }

}
