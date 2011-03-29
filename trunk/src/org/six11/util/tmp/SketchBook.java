package org.six11.util.tmp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.six11.util.Debug;
import org.six11.util.pen.AngleGraph;
import org.six11.util.pen.LengthGraph;
import org.six11.util.pen.PointGraph;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Sequence;
import org.six11.util.pen.TimeGraph;

public class SketchBook {

  public static final String SEQUENCE = "sequence";
  PointGraph allPoints;
  PointGraph cornerPoints;
  TimeGraph timeGraph;
  LengthGraph lengthGraph;
  AngleGraph angleGraph;
  List<List<Sequence>> batches;
  List<AntSegment> segments;

  public SketchBook() {
    allPoints = new PointGraph();
    cornerPoints = new PointGraph();
    timeGraph = new TimeGraph();
    lengthGraph = new LengthGraph();
    angleGraph = new AngleGraph();
    segments = new ArrayList<AntSegment>();
    batches = new ArrayList<List<Sequence>>();
  }

  public void addBatch(List<Sequence> sequences) {
    for (Sequence seq : sequences) {
      add(seq);
    }
    batches.add(new ArrayList<Sequence>(sequences));
  }

  public List<Sequence> getLastBatch() {
    return getBatch(batches.size() - 1);
  }

  public List<Sequence> getBatch(int which) {
    return batches.get(which);
  }

  public int getBatchCount() {
    return batches.size();
  }

  @SuppressWarnings("unchecked")
  private void add(Sequence seq) {
    for (Pt pt : seq) {
      pt.setAttribute(SEQUENCE, seq);
    }
    allPoints.addAll(seq);
    timeGraph.add(seq);
    List<AntSegment> allSegments = (List<AntSegment>) seq.getAttribute("segments");
    for (AntSegment segment : allSegments) {
      for (int i = segment.getEarlyPointIndex(); i <= segment.getLatePointIndex(); i++) {
        Pt pt = segment.getRawInk().get(i);
        if (!pt.hasAttribute(AntCornerFinder.POINT_SEGMENTS)) {
          pt.setAttribute(AntCornerFinder.POINT_SEGMENTS, new ArrayList<AntSegment>());
        }
        List<AntSegment> ptSegs = (List<AntSegment>) pt
            .getAttribute(AntCornerFinder.POINT_SEGMENTS);
        ptSegs.add(segment); // points can be part of more than one segment, e.g. on corners.
      }
      lengthGraph.add(segment);
      angleGraph.add(segment);
      segments.add(segment);
    }
    List<Integer> cornerIndices = (List<Integer>) seq
        .getAttribute(AntCornerFinder.SEGMENT_JUNCTIONS);
    for (int idx : cornerIndices) {
      cornerPoints.add(seq.get(idx));
    }
    bug("Added sequence " + seq.getId() + " to the sketchbook.");
  }

  /**
   * Gives you a set of all segments that have geometry within 'dist' units of 'pt'.
   */
  public Set<AntSegment> getSegmentsNear(Pt pt, double dist) {
    Set<AntSegment> ret = new HashSet<AntSegment>();
    for (AntSegment segment : segments) {
      double distToSeg = segment.getMinDistance(pt);
      if (distToSeg <= dist) {
        ret.add(segment);
      }
    }
    return ret;
  }

  public PointGraph getAllPoints() {
    return allPoints;
  }

  public PointGraph getCornerPoints() {
    return cornerPoints;
  }

  public TimeGraph getTimeGraph() {
    return timeGraph;
  }

  public LengthGraph getLengthGraph() {
    return lengthGraph;
  }

  public AngleGraph getAngleGraph() {
    return angleGraph;
  }

  private void bug(String what) {
    Debug.out("SketchBook", what);
  }

}
