package org.six11.util.tmp2;

import java.util.ArrayList;
import java.util.List;

import org.six11.util.tmp2.Segment.Terminal;

public class SketchBook {

  List<Integer> segmentBatches; // holds the indexes into 'segments' where batches begin/end
  List<Segment> segments;

  public SketchBook() {
    segmentBatches = new ArrayList<Integer>();
    segments = new ArrayList<Segment>();
  }

  public void record(List<Segment> segs) {
    segmentBatches.add(segments.size());
    segments.addAll(segs);
  }

  public List<Segment> getLastSegmentBatch() {
    List<Segment> ret = new ArrayList<Segment>();
    if (segmentBatches.size() > 0) {
      int lastBatchBegin = segmentBatches.get(segmentBatches.size() - 1);
      for (int i=lastBatchBegin; i < segments.size(); i++) {
        ret.add(segments.get(i));
      }
    }
    return ret;
  }

  public List<Terminal> getTerminalsNear(Terminal term, double dist) {
    List<Terminal> nearby = new ArrayList<Terminal>();
    for (Segment seg : segments) {
      for (Terminal t : seg.getTerminals()) {
        if (term != t && term.getPoint().distance(t.getPoint()) < dist) {
          nearby.add(t);
        }
      }
    }
    return nearby;
  }

  public List<Segment> getAllSegments() {
    return segments;
  }

}
