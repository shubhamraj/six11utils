package org.six11.util.tmp;

import java.util.List;

import org.six11.util.Debug;
import static java.lang.Math.min;
import static java.lang.Math.max;
import static org.six11.util.Debug.num;
import org.six11.util.pen.Functions;
import org.six11.util.pen.Line;
import org.six11.util.pen.Pt;
import org.six11.util.pen.RotatedEllipse;
import org.six11.util.pen.Sequence;
import org.six11.util.pen.Vec;
import org.six11.util.tmp.Ant.SegType;
import org.six11.util.tmp.AntSegment.Terminal;

public class AntSegment implements Comparable<AntSegment> {

  /**
   * Describes the ends of a segment when it is one (or both) ends of a stroke. If size is zero, it
   * means this segment is in the middle of a longer stroke phrase. If size is one, it means it is
   * one end of a longer stroke phrase. If size is two, it means the entire stroke consists of only
   * this segment.
   * 
   * You may query the terminal point and terminal directions using an index in the range from 0 to
   * (size() - 1).
   */
  public class Terminal {

    private Pt[] points;
    private Vec[] vecs;

    private Terminal() {
      int s = 0;
      if (getEarlyPointIndex() == 0) {
        s++;
      }
      if (getLatePointIndex() == rawInk.size() - 1) {
        s++;
      }
      points = new Pt[s];
      vecs = new Vec[s];
      int counter = 0;
      if (getEarlyPointIndex() == 0) {
        points[counter] = getEarlyPoint();
        vecs[counter] = getDirection(getEarlyPointIndex()).getFlip();
        counter++;
      }
      if (getLatePointIndex() == rawInk.size() - 1) {
        points[counter] = getLatePoint();
        vecs[counter] = getDirection(getLatePointIndex());
      }
      bug("Built a terminal structure for " + this);
    }

    /**
     * Tells you how many terminal points this segment has with respect to its raw stroke.
     */
    public int size() {
      return points.length;
    }

    /**
     * Gives you a terminal point.
     */
    public Pt getPoint(int i) {
      return points[i];
    }

    /**
     * Gives the direction the segment is travelling (away from the middle) at the terminal point.
     */
    public Vec getDir(int i) {
      return vecs[i];
    }

    public String toString() {
      return "Segment " + AntSegment.this.hashCode() + " has " + size() + " terminal points.";
    }
    
  }

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
  private Terminal terminal;
  private boolean baked = false;

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
    this.terminal = null;

    if (rawInk.isForward() == false) {
      bug("Sorry, the rawInk argument is not forward. Committing ritualistic suicide (stacktrace first)");
      new RuntimeException("foo").printStackTrace();
      System.exit(0);
    }
  }

  public String toString() {
    return type.toString();
  }

  /**
   * Removes unnecessary information. Trust me kid, this is for your own good. The longer you have
   * access to things like the raw ink and the integers that index into it, the longer you can screw
   * things up. Bake the segment to cause it to only be used as a line, or arc, or whatever segment
   * type it is.
   */
  public void bake() {
    terminal = new Terminal();
    bug("Just set terminal: it is: " + terminal);
    startPt = null;
    endPt = null;
    rawInk = null;
    earlyPointIndex = -1;
    latePointIndex = -1;
    cachedLength = -1;
    fixedAngle = Double.NaN;
    fixedVector = null;

    if (type == SegType.Line) {
      ellipse = null;
      spline = null;
    } else if (type == SegType.EllipticalArc) {
      line = null;
    }
    bug("Baked a segment of type: " + type + ", hash: " + AntSegment.this.hashCode()
        + ". Terminal: " + terminal);
    baked = true;
  }

  public boolean isBaked() {
    return baked;
  }

  public Vec getDirection(int idx) {
    Vec ret = null;
    if (type == SegType.Line) {
      ret = new Vec(startPt, endPt).getUnitVector();
    } else {
      int splineIdx = Functions.seekByTime(rawInk.get(idx), spline, 0);
      if (splineIdx == 0) {
        ret = new Vec(spline.get(0), spline.get(1)).getUnitVector();
      } else if (splineIdx == spline.size() - 1) {
        ret = new Vec(spline.get(splineIdx - 1), spline.get(splineIdx)).getUnitVector();
      }
    }
    return ret;
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

  /**
   * Returns the minimum distance from this segment's points and connecting curves to the target.
   */
  public double getMinDistance(Pt target) {
    double ret = 0;
    if (type == SegType.Line) {
      ret = Functions.getDistanceBetweenPointAndSegment(target, getLine());
    } else if (type == SegType.EllipticalArc) {
      if (hasSpline()) {
        Pt near = Functions.getNearestPointOnSequence(target, getSpline());
        ret = near.distance(target);
      } else {
        Pt near = Functions.getNearestPointOnSequence(target, rawInk);
        ret = near.distance(target);
      }
    }
    return ret;
  }

  /**
   * Returns the length of the idealized segment.
   * 
   * @return
   */
  public double length() {
    double ret = cachedLength;
    if (ret < 0) {
      if (type == SegType.Line) {
        ret = line.getLength();
      } else if (type == SegType.EllipticalArc) {
        if (hasSpline()) {
          ret = 0;
          for (int i = 1; i < spline.size(); i++) {
            ret = ret + spline.get(i - 1).distance(spline.get(i));
          }
        } else {
          bug("Improper type for segment: " + type);
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
    int startIdx = Functions.seekByTime(getEarlyPoint(), rawInk.getPoints(), 0);
    int endIdx = Functions.seekByTime(getLatePoint(), rawInk.getPoints(), startIdx + 1);
    if (startIdx >= 0 && endIdx > startIdx) {
      ret = rawInk.getSubSequence(startIdx, endIdx + 1);
    }
    return ret;
  }

  private static void bug(String what) {
    Debug.out("AntSegment", what);
  }

  private static void warn(String what) {
    Debug.out("AntSegment", "** warning ** " + what);
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
      earlyPointIndex = Functions.seekByTime(early, rawInk.getPoints(), 0);
    }
    return earlyPointIndex;
  }

  public Pt getLatePoint() {
    return (startPt.getTime() < endPt.getTime() ? endPt : startPt);
  }

  public int getLatePointIndex() {
    if (latePointIndex < 0) {
      Pt late = getLatePoint();
      latePointIndex = Functions.seekByTime(late, rawInk.getPoints(), getEarlyPointIndex());
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

  /**
   * Creates a line that is tangent to the segment at the given point.
   */
  public Line createTangentLine(Pt where) {
    Sequence subseq = getRawInkSubsequence();
    int idx = subseq.indexOf(where);
    double windowSize = max(10, subseq.length() / 10);
    double toEnd = subseq.getPathLength(idx, subseq.size() - 1);
    double fromStart = subseq.getPathLength(0, idx);
    if (toEnd > 0 && fromStart > 0) {
      if (toEnd < windowSize) {
        idx = subseq.size() - 1;
      } else if (fromStart < windowSize) {
        idx = 0;
      }
      bug("Changed idx to " + idx
          + " because we weren't at the start/end and we were too close to an endpoint.");
    }
    Line ret = null;
    if (idx < 0) {
      warn("Point " + num(where) + " not found in this sequence.");
    } else {
      int dir = 0;
      if (idx == 0) {
        dir = 1;
      } else if (idx >= (subseq.size() - 1)) {
        dir = -1;
      }

      if (dir != 0) {
        Pt other = Functions.getCurvilinearNeighbor(subseq, idx, windowSize, dir);
        if (other == null) {
          warn("'other' is null. index: " + idx + ". Window size: " + num(windowSize) + ". dir: "
              + dir + ".");
        }
        ret = new Line(where, other);
      } else {
        bug("Direction is zero. idx: " + idx + ", subseq.size(): " + subseq.size());
        Pt[] window = Functions.getCurvilinearWindow(subseq, idx, min(10, subseq.length() / 10));
        ret = new Line(window[0], window[1]);
      }
    }
    return ret;
  }

  public Terminal getTerminal() {
    return terminal;
  }

  /**
   * Rotates this segment so that ptA (which should be part of this segment) is at the same location
   * as ptB. The other points on this segment should rotate about the end point that is farthest
   * from ptA.
   * 
   * @param ptA
   *          a point on this segment
   * @param ptB
   *          a destination point that ptA should rotate to.
   */
  public void rotateTo(Pt ptA, Pt ptB) {
    //    int idxA = rawInk.
    bug("rotateTo not implemented");
  }
}
