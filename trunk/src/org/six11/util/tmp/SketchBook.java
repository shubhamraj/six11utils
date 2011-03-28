package org.six11.util.tmp;

import java.util.ArrayList;
import java.util.List;

import org.six11.util.Debug;
import org.six11.util.pen.AngleGraph;
import org.six11.util.pen.LengthGraph;
import org.six11.util.pen.PointGraph;
import org.six11.util.pen.Sequence;
import org.six11.util.pen.TimeGraph;

public class SketchBook {

  PointGraph allPoints;
  TimeGraph timeGraph;
  LengthGraph lengthGraph;
  AngleGraph angleGraph;
  List<List<Sequence>> batches;
  
  public SketchBook() {
    allPoints = new PointGraph();
    timeGraph = new TimeGraph();
    lengthGraph = new LengthGraph();
    angleGraph = new AngleGraph();
    batches = new ArrayList<List<Sequence>>();
  }
  
  public void addBatch(List<Sequence> sequences) {
    for (Sequence seq: sequences) {
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

  private void add(Sequence seq) {
    allPoints.addAll(seq);
    timeGraph.add(seq);
    List<AntSegment> allSegments = (List<AntSegment>) seq.getAttribute("segments");
    for (AntSegment segment : allSegments) {
      lengthGraph.add(segment);
      angleGraph.add(segment);
    }
    bug("Added sequence " + seq.getId() + " to the sketchbook.");
  }

  private void bug(String what) {
    Debug.out("SketchBook", what);
  }

}
