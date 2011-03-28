package org.six11.util.tmp;

import java.util.List;

import org.six11.util.Debug;
import org.six11.util.pen.Functions;
import org.six11.util.pen.Line;
import org.six11.util.pen.Pt;
import org.six11.util.pen.RotatedEllipse;
import org.six11.util.pen.Sequence;
import org.six11.util.pen.Vec;
import org.six11.util.tmp.Ant.SegType;

public class AntSegment implements Comparable<AntSegment> {

  private Ant.SegType type;
  private Line line;
  private RotatedEllipse ellipse;
  private Pt startPt;
  private Pt endPt;
  private Sequence rawInk;
  private List<Pt> spline; // set from outside
  private int earlyPointIndex;
  private int latePointIndex;
  private double cachedLength;
  private double fixedAngle;
  private Vec fixedVector;

  private AntSegment(Ant.SegType type, Pt startPoint, Pt endPoint, Sequence rawInk) {
    this.type = type;
    this.startPt = startPoint;
    this.endPt = endPoint;
    this.rawInk = rawInk;
    this.earlyPointIndex = -1;
    this.latePointIndex = -1;
    this.cachedLength = -1;
    this.fixedAngle = Double.NaN;
    this.fixedVector = null;
    
    if (rawInk.isForward() == false) {
      bug("Sorry, the rawInk argument is not forward. Committing ritualistic suicide (stacktrace first)");
      new RuntimeException("foo").printStackTrace();
      System.exit(0);
    }
  }

  public static AntSegment makeLineSegment(Pt a, Pt b, Sequence rawInk) {
    AntSegment ret = new AntSegment(Ant.SegType.Line, a, b, rawInk);
    ret.setLine(new Line(a, b));
    return ret;
  }

  public static AntSegment makeArcSegment(RotatedEllipse ellipse, Pt a, Pt b, Sequence rawInk) {
    AntSegment ret = new AntSegment(Ant.SegType.EllipticalArc, a, b, rawInk);
    ret.setEllipse(ellipse);
    return ret;
  }

  public double length() {
    double ret = cachedLength;
    if (ret < 0) {
      if (type == SegType.Line) {
        ret = startPt.distance(endPt);
      } else if (type == SegType.EllipticalArc) {
        if (hasSpline()) {
          ret = 0;
          for (int i = 1; i < spline.size(); i++) {
            ret = ret + spline.get(i-1).distance(spline.get(i));
          }
        } else {
          ret = rawInk.getPathLength(getEarlyPointIndex(), getLatePointIndex());
        }
      }
      cachedLength = ret;
    }
    return ret;
  }
  
  public Vec getFixedVector() {
    if (fixedVector == null) {
      Pt start = startPt;
      Pt end = endPt;
      Pt left = start;
      Pt right = end;
      if (Pt.sortByX.compare(start, end) == 1) {
        left = end;
        right = start;
      }
      double dx = right.x - left.x;
      double dy = right.y - left.y;
      fixedVector = new Vec(dx, dy);
    }
    return fixedVector;
  }

  public double getFixedAngle() {
    if (fixedAngle == Double.NaN) {
      Vec fv = getFixedVector();
      fixedAngle = Math.atan2(fv.getY(), fv.getX());
    }
    return fixedAngle;
  }


  public Sequence getRawInk() {
    return rawInk;
  }

  /**
   * Gives a sequence based on the raw ink and the start/end points. The returned sequence is in
   * increasing time-order.
   */
  public Sequence getRawInkSubsequence() {
    Sequence ret = null;
    int startIdx = Functions.seekByTime(getEarlyPoint(), rawInk, 0);
    int endIdx = Functions.seekByTime(getLatePoint(), rawInk, startIdx + 1);
    if (startIdx >= 0 && endIdx > startIdx) {
      ret = rawInk.getSubSequence(startIdx, endIdx + 1);
    }
    return ret;
  }

  private static void bug(String what) {
    Debug.out("AntSegment", what);
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

  public Pt getEarlyPoint() {
    return (startPt.getTime() < endPt.getTime() ? startPt : endPt);
  }

  public int getEarlyPointIndex() {
    if (earlyPointIndex < 0) {
      Pt early = getEarlyPoint();
      earlyPointIndex = Functions.seekByTime(early, rawInk, 0);
    }
    return earlyPointIndex;
  }

  public Pt getLatePoint() {
    return (startPt.getTime() < endPt.getTime() ? endPt : startPt);
  }

  public int getLatePointIndex() {
    if (latePointIndex < 0) {
      Pt late = getLatePoint();
      latePointIndex = Functions.seekByTime(late, rawInk, getEarlyPointIndex());
    }
    return latePointIndex;
  }

  public int compareTo(AntSegment other) {
    int ret = 0;
    long me = getEarlyPoint().getTime();
    long it = other.getEarlyPoint().getTime();
    if (me < it) {
      ret = -1;
    } else if (me > it) {
      ret = 1;
    }
    return ret;
  }

  public void setSpline(List<Pt> spline) {
    this.spline = spline;
  }

  public boolean hasSpline() {
    return (spline != null);
  }

  public List<Pt> getSpline() {
    return spline;
  }
}
