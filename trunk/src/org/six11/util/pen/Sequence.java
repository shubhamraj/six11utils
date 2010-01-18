// $Id$

package org.six11.util.pen;

import java.util.NoSuchElementException;
import java.awt.geom.FlatteningPathIterator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.six11.util.Debug;
import org.six11.util.io.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.FileReader;

/**
 * Implementation of a bi-directional linked list for Pt objects, which are just Point2D.Double
 * objects with some added stuff. Many of the methods are deferred to Pt. This class handles the
 * issues that concern the lot of them -- e.g. moving all points, selecting/deseleting all points,
 * getting the size of the list, etc.
 **/
public class Sequence implements Shape, Iterable<Pt> {

  protected List<Pt> points;
  protected DrawFunction drawFunction;

  /**
   * True if this sequence represents the boundary of a 2D shape, false if it simply represents a
   * polyline.
   */
  protected boolean closedRegion;

  public Sequence() {
    points = new ArrayList<Pt>();
    closedRegion = false;

    // the default draw function uses the color of each point, or if
    // no "color" attribute is set, uses black.
    drawFunction = new DrawFunction() {
      public void draw(Sequence seq, Graphics2D g) {
        g.setStroke(new BasicStroke(2.0f));
        Line line = new Line();
        for (Pt pt : seq) {
          line.push(pt);
          if (line.isValid()) {
            if (pt.hasAttribute("color")) {
              Color color = (Color) pt.getAttribute("color");
              g.setColor(color);
            } else {
              g.setColor(Color.BLACK);
            }
            g.draw(line);
          }
        }
      }
    };
  }

  /**
   * Make a sequence with a default list of points. This is equivalent to making a blank sequence
   * and adding each point. If the input list of points represents a closed sequence, set the second
   * param (closed) to true.
   * 
   * This does NOT ensure that the first and last points are coincident so you need to do that
   * outside of this code.
   */
  public Sequence(List<Pt> points, boolean closed) {
    this();
    for (Pt pt : points) {
      add(pt);
    }
    setClosedRegion(closed);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer("[ ");
    for (Pt pt : this) {
      buf.append(Debug.num(pt) + " ");
    }
    buf.append("]");
    return buf.toString();
  }

  public boolean isClosedRegion() {
    return closedRegion;
  }

  public void setClosedRegion(boolean closedRegion) {
    this.closedRegion = closedRegion;
  }

  public void setDrawFunction(DrawFunction df) {
    this.drawFunction = df;
  }

  public Sequence copy() {
    Sequence ret = new Sequence();
    for (Pt pt : this) {
      ret.add(pt.copy());
    }
    return ret;
  }

  public Sequence copy(int beginInclusive, int endExclusive) {
    Sequence ret = new Sequence();
    for (int i = beginInclusive; i < endExclusive; i++) {
      ret.add(get(i));
    }
    return ret;
  }

  public int indexOf(Pt pt) {
    return points.indexOf(pt);
  }

  public Pt get(int idx) {
    return points.get(idx);
  }

  public Pt getLast() {
    return points.get(points.size() - 1);
  }

  public void add(Pt pt) {
    points.add(pt);
  }

  public void remove(int idx) {
    points.remove(idx);
  }

  public void removeLast() {
    remove(points.size() - 1);
  }

  public int size() {
    return points.size();
  }

  /**
   * Returns the arc length of the entire sequence, assuming straight lines between each point pair.
   */
  public double length() {
    return getPathLength(0, points.size() - 1);
  }

  /**
   * Returns the arc length of the portion of the sequence beginning and ending at the given
   * indices, assuming straight lines between each pair of successive points.
   */
  public double getPathLength(int idxStartInclusive, int idxEndInclusive) {
    double ret = 0.0;
    Pt prev = null;
    for (int i = idxStartInclusive; i <= idxEndInclusive; i++) {
      Pt pt = points.get(i);
      if (prev != null) {
        ret += prev.distance(pt);
      }
      prev = pt;
    }
    return ret;
  }

  /**
   * Returns the length of the line segment that directly connects the first and last points. If
   * there are not at least two points in the sequence this returns zero.
   */
  public double getEndpointDistance() {
    double ret = 0.0;
    if (points.size() > 1) {
      Pt a = points.get(0);
      Pt b = points.get(points.size() - 1);
      ret = a.distance(b);
    }
    return ret;
  }

  /**
   * Simply returns the total arc length devided by the endpoint distance. This checks for division
   * by zero, in which case Double.MAX_VALUE is returned.
   */
  public double getRoundaboutness() {
    double num = length();
    double den = getEndpointDistance();
    double ret = Double.MAX_VALUE;
    if (den != 0.0) {
      ret = num / den;
    }
    return ret;
  }

  /**
   * Returns the sum of each point's "curvature" attribute. If this attribute is not set on all of
   * the points on the line, you will get a bogus result.
   */
  public double getSignedCurvatureSum() {
    double ret = 0.0;
    for (Pt pt : points) {
      if (pt.hasAttribute("curvature")) {
        ret += pt.getDouble("curvature");
      }
    }
    return ret;
  }

  public double getCurvature(int idx, int windowSize) {
    double ret = 0.0;
    int lower = idx - windowSize;
    int upper = idx + windowSize;
    if (lower >= 0 && upper < points.size()) {
      double dx = points.get(upper).x - points.get(lower).x;
      double dy = points.get(upper).y - points.get(lower).y;
      double numer = Math.atan2(dy, dx);
      double denom = getPathLength(lower, upper);
      ret = numer / denom;
    } else if (windowSize > 1) {
      return getCurvature(idx, windowSize - 1);
    }
    return ret;
  }

  /**
   * Returns the absolue value of each point's "curvature" attribute. See getSignedCurvatureSum().
   */
  public double getAbsoluteCurvatureSum() {
    double ret = 0.0;
    for (Pt pt : points) {
      if (pt.hasAttribute("curvature")) {
        ret += Math.abs(pt.getDouble("curvature"));
      }
    }
    return ret;
  }

  /**
   * Generate a list of points that the sequence crosses itself within a certain localized area.
   * This is useful for detecting tight loops in the sequence without picking up self-intersections
   * that are far apart. If you are interested in finding all self intersection points, just supply
   * a negative number or Double.MAX_VALUE as your argument. A non-null (but possibly empty) list
   * will be returned.
   */
  public List<Pt> getSelfIntersectionPoints(double localDistance) {
    // find the places that this sequence crosses itself without
    // traversing more than 'localDistance' pixels.

    // a b c d e f g h
    // *----*----*----*-----*----*-----*----*-----*
    // 1 2 3 4 5 6 7 8 9
    //
    // Distance is calculated between the start points of the two line
    // segments under consideration. Adjacent line segments are never
    // compared. Say that points 1 and 5 are within localDistance, but
    // points 1 and 6 are not. This means that segment a will be
    // compared against c, d, and e.

    // Because it is likely that the segment distances involved will
    // be used frequently and will not change, I will cache it the
    // first time using the attribute name "dist_to_next".

    List<Pt> ret = new ArrayList<Pt>();

    double runDist;
    Line lineA = new Line();
    Line lineB = new Line();
    for (int i = 0; i < (points.size() - 3); i++) {
      lineA.push(points.get(i));
      if (lineA.isValid()) {
        runDist = cacheSegmentData(lineA);
        lineB.clear();
        for (int j = i + 1; j < (points.size() - 1); j++) {
          lineB.push(points.get(j));
          if (lineB.isValid()) {
            runDist += cacheSegmentData(lineB);
            if (!lineA.isAdjacentTo(lineB) && lineA.intersectsLine(lineB)) {
              IntersectionData ix = new IntersectionData(lineA, lineB);
              if (ix.intersectsInSegments() && (!ret.contains(ix.getIntersection()))) {
                ret.add(ix.getIntersection());
              }
            }
          }
          if (runDist > localDistance) {
            break;
          }
        }
      }
    }
    return ret;
  }

  private double cacheSegmentData(Line line) {
    if (!line.getStart().hasAttribute("dist_to_next")) {
      double dist = line.getLength();
      line.getStart().setAttribute("dist_to_next", dist);
    }
    return (Double) line.getStart().getAttribute("dist_to_next");
  }

  public List<Pt> getPoints() {
    return points;
  }

  public Iterator<Pt> iterator() {
    return points.iterator();
  }

  /**
   * Get an Iterable<Pt> that runs from idx up to and including the last Pt.
   */
  public Iterable<Pt> forward(final int idx) {
    return from(idx, size() - 1);
  }

  /**
   * Get an Iterable<Pt> that runs from idx down to and including the first (index 0) Pt.
   */
  public Iterable<Pt> backward(final int idx) {
    return from(idx, 0);
  }

  public Iterable<Pt> from(final int idxA, final int idxB) {
    return new Iterable<Pt>() {
      public Iterator<Pt> iterator() {
        return new SequenceCursor(Sequence.this, idxA, idxB);
      }
    };
  }

  public Rectangle getBounds() {
    Rectangle2D twodee = getBounds2D();
    return new Rectangle((int) twodee.getX(), (int) twodee.getY(), (int) twodee.getWidth(),
        (int) twodee.getHeight());
  }

  public Rectangle2D getBounds2D() {
    return Functions.getSequenceBoundingBox(this);
  }

  public boolean contains(double x, double y) {
    int count = 0;
    if (closedRegion) {
      count = Functions.getCrossingNumber(new Pt(x, y), this);
    }
    return (count % 2) == 1; // true if count is odd
  }

  public Sequence transform(SequenceFunction sf) {
    Sequence ret = new Sequence();
    for (Pt pt : this) {
      ret.add(sf.transform(pt));
    }
    return ret;
  }

  public boolean containsVertex(Pt target) {
    boolean ret = false;
    for (Pt pt : this) {
      if (Functions.eq(pt, target, Functions.EQ_TOL)) {
        ret = true;
        break;
      }
    }
    return ret;
  }

  public boolean contains(Point2D p) {
    return contains(p.getX(), p.getY());
  }

  public boolean intersects(double x, double y, double w, double h) {
    return false;
  }

  public boolean intersects(Rectangle2D rec) {
    return false;
  }

  public boolean contains(double x, double y, double w, double h) {
    return false;
  }

  public boolean contains(Rectangle2D rec) {
    return false;
  }

  public PathIterator getPathIterator(AffineTransform affine) {
    return new PtPathIterator(affine);
  }

  public PathIterator getPathIterator(AffineTransform affine, double flatness) {
    return new FlatteningPathIterator(getPathIterator(affine), flatness);
  }

  public void draw(Graphics2D g) {
    drawFunction.draw(this, g);
  }

  /**
   * A PathIterator for going through a Sequence as though it were a Shape. (in fact, this Sequence
   * IS a Shape, and this is what Sequence uses).
   */
  private class PtPathIterator implements PathIterator {
    SequenceCursor cursor;
    AffineTransform affine;

    PtPathIterator(AffineTransform affine) {
      this.cursor = new SequenceCursor(Sequence.this);
      this.affine = affine;
    }

    public int getWindingRule() {
      return WIND_NON_ZERO;
    }

    public boolean isDone() {
      return !cursor.hasNext();
    }

    public void next() {
      cursor.next();
    }

    public int currentSegment(float[] coords) {
      if (isDone()) {
        throw new NoSuchElementException("Sequence path iterator out of bounds");
      }
      int type = cursor.getCurrentIdx() == 0 ? SEG_MOVETO : SEG_LINETO;
      coords[0] = (float) get(cursor.getCurrentIdx()).getX();
      coords[1] = (float) get(cursor.getCurrentIdx()).getY();
      if (affine != null) {
        affine.transform(coords, 0, coords, 0, cursor.getCurrentIdx() == 0 ? 1 : 3);
      }
      return type;
    }

    public int currentSegment(double[] coords) {
      if (isDone()) {
        throw new NoSuchElementException("Sequence path iterator out of bounds");
      }
      int type = cursor.getCurrentIdx() == 0 ? SEG_MOVETO : SEG_LINETO;
      coords[0] = get(cursor.getCurrentIdx()).getX();
      coords[1] = get(cursor.getCurrentIdx()).getY();
      if (affine != null) {
        affine.transform(coords, 0, coords, 0, cursor.getCurrentIdx() == 0 ? 1 : 3);
      }
      return type;
    }
  }

  public void writeToFile(String fileName) {
    File outFile = new File(fileName);
    StringBuilder buf = new StringBuilder();
    buf.append("# Sequence.java: " + points.size() + " points; " + new Date().toString() + "\n");
    for (Pt pt : points) {
      buf.append(pt.x + "\t" + pt.y + "\n");
    }
    FileUtil.writeStringToFile(outFile, buf.toString(), false);
  }

  public static Sequence loadFromFile(String file) {
    Sequence ret = new Sequence();
    try {
      BufferedReader in = new BufferedReader(new FileReader(file));
      StringTokenizer tok;
      String line;
      while (in.ready()) {
        line = in.readLine();
        if (!line.trim().startsWith("#")) { // avoid comment lines.
          tok = new StringTokenizer(line, "\t");
          String x = tok.nextToken();
          String y = tok.nextToken();
          ret.add(new Pt(Double.valueOf(x), Double.valueOf(y)));
        }
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return ret;
  }
}
