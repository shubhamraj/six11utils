package org.six11.util.tmp2;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.six11.util.Debug;

import org.six11.util.spud.CLine;
import org.six11.util.spud.CPoint;
import org.six11.util.spud.ConstraintModel;
import org.six11.util.spud.Geom;
import org.six11.util.tmp2.Segment.Terminal;

public class SketchBook {

  private List<Segment> lastBatch;
  private List<Segment> segments;
  private ConstraintModel constraints;

  public SketchBook() {
    lastBatch = null;
    segments = new ArrayList<Segment>();
    constraints = new ConstraintModel();
  }

  public void record(List<Segment> segs) {
    lastBatch = segs;
    segments.addAll(segs);

    for (Segment seg : segs) {
      switch (seg.getType()) {
        case Line:
          CPoint cpoint1 = new CPoint(seg.getP1());
          CPoint cpoint2 = new CPoint(seg.getP2());
          CLine cline = CLine.makeLine(constraints, cpoint1, cpoint2);
          break;
        case EllipticalArc:
          break;
        case Curve:
          break;
        case Unknown:
          break;
        default:
          Debug.warn(this, "Unknown segment type in record()");
      }
    }
    constraints.solve();
    Set<Geom> geometry = constraints.getAllGeometry();
    for (Geom g : geometry) {
      switch (g.getType()) {
        case Line:
          bug(g.getDebugString());
          break;
      }
    }
  }

  private static void bug(String what) {
    Debug.out("SketchBook", what);
  }

  public List<Segment> getLastSegmentBatch() {
    return lastBatch;
  }

  public List<Terminal> getTerminalsNear(Terminal term, double dist) {
    List<Terminal> nearby = new ArrayList<Terminal>();
    for (Segment seg : segments) {
      if (seg.isNear(term.getPoint(), dist)) {
        for (Terminal t : seg.getTerminals()) {
          nearby.add(t);
        }
      }
      //      for (Terminal t : seg.getTerminals()) {
      //        if (term != t && term.getPoint().distance(t.getPoint()) < dist) {
      //          nearby.add(t);
      //        }
      //      }
    }
    return nearby;
  }

  public List<Segment> getAllSegments() {
    return segments;
  }

}
