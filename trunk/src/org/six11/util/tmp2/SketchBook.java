package org.six11.util.tmp2;

import java.util.ArrayList;
import java.util.List;

import org.six11.util.Debug;
import org.six11.util.spud.ConstraintModel;
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
