package org.six11.util.tmp;

import org.six11.util.pen.Line;
import org.six11.util.pen.Pt;
import org.six11.util.pen.RotatedEllipse;

public class AntSegment implements Comparable<AntSegment> {

  private Ant.SegType type;
  private Line line;
  private RotatedEllipse ellipse;
  private int id;
  private Pt startPt;
  private Pt endPt;

  private AntSegment(Ant.SegType type, int segId, Pt startPoint, Pt endPoint) {
    this.type = type;
    this.id = segId;
    this.startPt = startPoint;
    this.endPt = endPoint;
  }

  public static AntSegment makeLineSegment(Pt a, Pt b, int segId) {
    AntSegment ret = new AntSegment(Ant.SegType.Line, segId, a, b);
    ret.setLine(new Line(a, b));
    return ret;
  }

  public static AntSegment makeArcSegment(RotatedEllipse ellipse, Pt a, Pt b, int segId) {
    AntSegment ret = new AntSegment(Ant.SegType.EllipticalArc, segId, a, b);
    ret.setEllipse(ellipse);
    return ret;
  }

  private void setEllipse(RotatedEllipse ellipse) {
    this.ellipse = ellipse;
  }

  public RotatedEllipse getEllipse() {
    return ellipse;
  }

  public Line getLine() {
    return line;
  }

  private void setLine(Line line) {
    this.line = line;
  }

  public Ant.SegType getType() {
    return type;
  }

  public Pt getSegmentStartPoint() {
    return startPt;
  }
  
  public Pt getSegmentEndPoint() {
    return endPt;
  }
  
  public int compareTo(AntSegment other) {
    int ret = 0;
    if (id < other.id) {
      ret = -1;
    } else if (id > other.id) {
      ret = 1;
    }
    return ret;
  }
}
