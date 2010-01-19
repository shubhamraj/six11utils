// $Id$

package org.six11.util.pen;

import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.ArrayList;

import org.six11.util.Debug;

/**
 * This is a whole slew of static methods that operate on points, lines, vectors, and point
 * sequences.
 **/
public abstract class Functions {

  public static double HARD_TURN_THRESHOLD = 0.30;
  public static double EQ_TOL = 0.00001;

  public static final int PARTITION_LEFT = -1;
  public static final int PARTITION_ON_BORDER = 0;
  public static final int PARTITION_RIGHT = 1;

  public static double getDistanceBetween(Pt a, Pt b) {
    double dx = b.getX() - a.getX();
    double dy = b.getY() - a.getY();
    return Math.hypot(dx, dy);
  }

  public static Pt getPointBetween(Pt a, Pt b) {
    return new Pt((a.getX() + b.getX()) / 2.0, (a.getY() + b.getY()) / 2.0);
  }

  public static double getSlope(Pt a, Pt b) {
    double dy = b.getY() - a.getY();
    double dx = b.getX() - a.getX();
    if (dx == 0.0) {
      return Double.MAX_VALUE;
    } else {
      return dy / dx;
    }
  }

  public static boolean eq(Pt a, Pt b, double tolerance) {
    double xOk = Math.abs(a.getX() - b.getX());
    double yOk = Math.abs(a.getY() - b.getY());
    return (xOk < tolerance && yOk < tolerance);
  }

  public static boolean eq(double a, double b, double tolerance) {
    return (Math.abs(a - b) < tolerance);
  }

  public static Pt getMean(Pt... points) {
    List<Pt> list = new ArrayList<Pt>();
    for (Pt pt : points) {
      list.add(pt);
    }
    return getMean(list);
  }

  public static Pt getMean(List<Pt> points) {
    double sumX = 0.0;
    double sumY = 0.0;
    for (Pt pt : points) {
      sumX = sumX + pt.getX();
      sumY = sumY + pt.getY();
    }
    double n = (double) points.size();
    return new Pt(sumX / n, sumY / n);
  }

  public static Pt rotatePointAboutPivot(Pt point, Pt pivot, double radians) {
    Pt ret = new Pt();
    AffineTransform matrix = getRotationInstance(pivot, radians);
    matrix.transform(point, ret);
    return ret;
  }

  public static Sequence rotatePointsAboutPivot(Sequence points, Pt pivot, double radians) {
    Sequence ret = new Sequence();
    AffineTransform matrix = getRotationInstance(pivot, radians);
    Pt transformed;
    for (Pt pt : points) {
      transformed = new Pt();
      matrix.transform(pt, transformed);
      ret.add(transformed);
    }
    return ret;
  }

  private static AffineTransform getRotationInstance(Pt pivot, double radians) {
    // note that this is equivalent to the function
    // AffineTransform.getRotateInstance(angle, x, y). Seeing this in
    // action is instructive.

    // Say you have a pivot point and you want to transform other
    // points about it. Draw it on the whiteboard. Think about how you
    // may do it.

    // To set up a transformation matrix, do the following:
    // Step 1: translate everything so the pivot is at the origin.
    // Step 2: rotate everything by the proper angle.
    // Step 3: translate things back the opposite amount as step 1.

    // After you have a transformation matrix, you can apply that
    // matrix on every point and it will do the right thing. The
    // matrix is independent of the data it is transforming.

    // So when you actually write the code to do this, you have to
    // ==REVERSE== the steps you took on the whiteboard. The reason
    // for this is that the matrices are being multiplied together,
    // and due to the fact that matrix multiplication isn't
    // commutative (A*B isn't the same as B*A). It turns out that the
    // operation you want to happen first actually has to be pushed
    // onto the stack (meaning, multiplied onto the matrix) at the
    // end. This is confusing, but true.
    AffineTransform matrix = new AffineTransform();
    matrix.translate(pivot.getX(), pivot.getY()); // step 3
    matrix.rotate(radians); // step 2
    matrix.translate(-pivot.getX(), -pivot.getY()); // step 1
    return matrix;
  }

  public static Pt getEndPoint(Pt pt, Vec vec) {
    return new Pt(pt.getX() + vec.getX(), pt.getY() + vec.getY());
  }

  @SuppressWarnings("unused")
  public static Line getLineFromPoint(Pt pt, Vec vec) {
    // TODO implement me
    return null;
  }

  /**
   * Given some sequence of points (or subsequence) return the average vector that represents the
   * change in position.
   */
  public static Vec getAverageChangeVector(Sequence seq, int start, int end) {
    Pt prev = null;
    Vec vec;
    double xComp = 0d;
    double yComp = 0d;
    for (Pt pt : seq.from(start, end)) {
      if (prev != null) {
        vec = new Vec(prev, pt);
        xComp += vec.getX();
        yComp += vec.getY();
      }
      prev = pt;
    }
    int n = end - start;
    double aveX = xComp / n;
    double aveY = yComp / n;
    return new Vec(aveX, aveY);
  }

  public static double getDistanceBetweenPointAndLine(Pt pt, Line line) {
    return line.ptLineDist(pt);
  }

  public static double getSignedDistanceBetweenPointAndLine(Pt pt, Line line) {
    double dist = getDistanceBetweenPointAndLine(pt, line);
    Vec lineVec = new Vec(line);
    Vec ptVec = new Vec(line.getStart(), pt);
    double det = getDeterminant(lineVec, ptVec);
    if (det < 0.0) {
      dist = -1.0 * dist;
    }
    return dist;
  }

  public static double getHausdorffDistance(Rectangle2D p, Rectangle2D q) {
    return Hausdorff.getHausdorffDistance(p, q);
  }

  public static Pt getRectangleCenter(Rectangle2D r) {
    double x = r.getX() + (r.getWidth() / 2.0);
    double y = r.getY() + (r.getHeight() / 2.0);
    return new Pt(x, y);
  }

  public static double getRectangleArea(Rectangle2D r) {
    return r.getWidth() * r.getHeight();
  }

  public static double getDistanceToCenter(Rectangle2D r, Pt p) {
    Pt mid = Functions.getRectangleCenter(r);
    return mid.distance(p);
  }

  public static Pt getNearestPointOnLine(Pt c, Line line) {
    // Assuming line is AB and the provided point is C, the nearest
    // point on the line P is found with a parameter r:
    //
    // AC dot AB
    // r = -----------
    // mag(AB)
    // 
    // if r == 0, then P == A
    // if r == 1, then P == B
    // for other r, P is somewhere along the line AB:
    // Px = Ax + r(Bx-Ax)
    // Py = Ay + r(By-Ay)
    Pt a = line.getStart();
    Pt b = line.getEnd();
    Vec ab = new Vec(a, b);
    Vec ac = new Vec(a, c);
    double mag = ab.mag();
    double r = Functions.getDotProduct(ac, ab) / (mag * mag);
    double x = a.getX() + r * (b.getX() - a.getX());
    double y = a.getY() + r * (b.getY() - a.getY());

    return new Pt(x, y);
  }

  // int See if p is left(-1)/on(0)/right(1) of a Line D int getPartition(Pt, Line)
  public static int getPartition(Pt pt, Line line) {
    return getPartition(pt, line.getStart(), line.getEnd());
  }

  public static int getPartition(Pt pt, Pt lineA, Pt lineB) {
    // translate both by 'lineA'
    Vec vecToPt = new Vec(pt, lineA);
    Vec vecForLine = new Vec(lineB, lineA);
    double det = getDeterminant(vecForLine, vecToPt);
    int ret = PARTITION_ON_BORDER;
    if (det < 0) {
      ret = PARTITION_LEFT;
    } else if (det > 0) {
      ret = PARTITION_RIGHT;
    }
    return ret;
  }

  public static Pt getCircleCenter(Pt a, Pt b, Pt c) {
    double ax = a.getX();
    double ay = a.getY();
    double bx = b.getX();
    double by = b.getY();
    double cx = c.getX();
    double cy = c.getY();

    double A = bx - ax;
    double B = by - ay;
    double C = cx - ax;
    double D = cy - ay;

    double E = A * (ax + bx) + B * (ay + by);
    double F = C * (ax + cx) + D * (ay + cy);

    double G = 2 * (A * (cy - by) - B * (cx - bx));
    if (G == 0.0)
      return null; // a, b, c must be collinear

    double px = (D * E - B * F) / G;
    double py = (A * F - C * E) / G;
    return new Pt(px, py);
  }

  /**
   * Gives you the curvature at a point. It computes the midpoint of the circle that is defined by
   * the three points (if such a circle exists; if the three points are collinear, there is no
   * circle). The curvature returned is 0 for a straight line. For nonstraight samples, the
   * curvature is signed. If you were to walk in a straight line from point a to point c, b will
   * either be on your left (negative curvature) or on your right.
   */
  public static double getCurvature(Pt a, Pt b, Pt c) {
    Pt mid = getCircleCenter(a, b, c);
    double ret = 0.0;
    if (mid != null) {
      double dist = mid.distance(b);
      ret = 1 / dist;
    }
    // signededness depends on orientation of b compared to vector ac
    Vec ab = new Vec(a, b);
    Vec ac = new Vec(a, c);
    double angle = Functions.getAngleBetween(ab, ac);
    if (angle < 0) {
      ret = ret * -1.0;
    }
    return ret;
  }

  public static Vec getVectorNormal(Vec vec) {
    return vec.getNormal();
  }

  public static Vec getScaledVector(Vec vec, double scaleFactor) {
    double x = vec.getX() * scaleFactor;
    double y = vec.getY() * scaleFactor;
    return new Vec(x, y);
  }

  public static Vec getVectorOfMagnitude(Vec vec, double desiredMag) {
    double current = vec.mag();
    double scaleFactor = desiredMag / current;
    return Functions.getScaledVector(vec, scaleFactor);
  }

  @SuppressWarnings("unused")
  public static Vec getVectorFlip(Vec vec) {
    // TODO implement me
    return null;
  }

  public static double getAngleBetween(Line a, Line b) {
    Vec va = getLineVector(a);
    Vec vb = getLineVector(b);
    return getAngleBetween(va, vb);
  }

  public static double getAngleBetween(Vec a, Vec b) {    
    double a1 = Math.atan2(b.getY(), b.getX());
    double a2 = Math.atan2(a.getY(), a.getX());
    double ret = a1 - a2;
    return ret;
  }

//  public static double getAngleBetweenFullcircle(Vec a, Vec b) {
//    double ret = 0.0;
//    double numerator = Functions.getDotProduct(a, b);
//    double denom = a.mag() * b.mag();
//    if (denom != 0.0) {
//      double cos = numerator / denom;
//      double det = Functions.getDeterminant(a, b);
//      double ang = Math.acos(cos);
//      if (det > 0.0) {
//        ang = Math.PI + ang;
//      }
//      ret = ang;
//    }
//    return ret;
//  }

  public static double getDotProduct(Vec a, Vec b) {
    return a.getX() * b.getX() + a.getY() * b.getY();
  }

//  @SuppressWarnings("unused")
//  public static Vec getCrossProduct(Vec a, Vec b) {
//    // TODO implement me NOTE: It seems that the cross product of two
//    // 2D vectors yields a 'vector' with one component... a scalar.
//
//    // NOTE: if you are using this to get the signed angle between two
//    // vectors, look at getAngleBetween()
//    return null;
//  }

  public static double getDeterminant(Vec a, Vec b) {
    // det(ab, cd) = a*d - b*c;
    return a.getX() * b.getY() - a.getY() * b.getX();
  }

//  @SuppressWarnings("unused")
//  public static double getVectorMagnitude(Vec vec) {
//    // TODO implement me
//    return 0.0;
//  }
//
//  @SuppressWarnings("unused")
//  public static Pt getLineMidpoint(Line line) {
//    // TODO implement me
//    return null;
//  }

  // public static double getLineLength(Line line) {

  // }

  public static Vec getLineVector(Line line) {
    double dx = 0.0;
    double dy = 0.0;
    if (line.isValid()) {
      dx = line.getEnd().getX() - line.getStart().getX();
      dy = line.getEnd().getY() - line.getStart().getY();
    }
    return new Vec(dx, dy);
  }

  public static IntersectionData getIntersectionData(Line a, Line b) {
    return new IntersectionData(a, b);
  }

  /**
   * Returns the intersection point of the two given lines. If they are colinear or parallel this
   * will return null.
   */
  public static Pt getIntersectionPoint(Line a, Line b) {
    IntersectionData id = getIntersectionData(a, b);
    Pt ret = id.getIntersection();
    return ret;
  }

  @SuppressWarnings("unused")
  public static double getMinDistBetweenLines(Line a, Line b) {
    // TODO implement me
    return 0.0;
  }

  public static Sequence getNormalizedSequence(Sequence seq, double d) {
    Sequence normalizedSequence = new Sequence();
    normalizedSequence.add(seq.get(0).copy());
    List<Pt> normPoints = getNormalizedSequence(seq.getPoints(), d);
    for (Pt pt : normPoints) {
      normalizedSequence.add(pt);
    }
    normalizedSequence.add(seq.getLast().copy());
    return normalizedSequence;
  }

  public static List<Pt> getNormalizedSequence(List<Pt> points, double d) {

    List<Pt> ret = new ArrayList<Pt>();

    Vec bigVec;
    Vec smallVec;
    Pt prev = points.get(0);
    for (Pt n : points) {
      if (n.equals(points.get(0))) {
        continue;
      }
      bigVec = new Vec(prev, n);
      while (bigVec.mag() > d) {
        smallVec = bigVec.getVectorOfMagnitude(d);
        Pt addMe = smallVec.add(prev);
        // interpolate the timestamp. Points are prev < addMe < n
        long diff = n.getTime() - prev.getTime();
        double magFraction = smallVec.mag() / bigVec.mag();
        long timeGuess = prev.getTime() + (long) ((double) diff * magFraction);
        addMe.setTime(timeGuess);
        ret.add(addMe);
        prev = addMe;
        bigVec = new Vec(prev, n);
      }
    }

    return ret;
  }

  public static double getMinDistBetweenPointAndSequence(Pt pt, Sequence seq) {
    double ret = Double.MAX_VALUE;
    Pt nearest = getNearestPointOnSequence(pt, seq);
    if (nearest != null) {
      ret = nearest.distance(pt);
    }
    return ret;
  }

  /**
   * Returns the point (or the oldest point, in the event of a tie) on the given sequence that is
   * nearest the provided epicenter point.
   */
  public static Pt getNearestPointOnSequence(Pt epicenter, Sequence seq) {
    double minDist = Double.MAX_VALUE;
    Pt nearest = null;
    for (Pt pt : seq) {
      if (epicenter.distance(pt) < minDist) {
        nearest = pt;
        minDist = epicenter.distance(pt);
      }
    }
    return nearest;
  }

  public static double getMinDistBetweenPointsOnSequence(Sequence seq, Pt a, Pt b) {
    boolean foundA = false;
    boolean foundB = false;
    double dist = 0.0;
    Pt prev = null;
    for (Pt pt : seq) {
      if ((prev != null) && (foundA ^ foundB))
        dist += prev.distance(pt);
      if (pt.equals(a))
        foundA = true;
      if (pt.equals(b))
        foundB = true;
      if (foundA && foundB)
        break;
      prev = pt;
    }
    return dist;
  }

  @SuppressWarnings("unused")
  public static Sequence getTweenBetweenSequences(Sequence a, Sequence b) {
    // TODO implement me
    return null;
  }

  /**
   * The provided epicenter point may or may not be on the sequence. Wherever it is, there is a
   * point on the sequence that is closest to it. This method runs through all points in the
   * sequence and writes the distance from that closest point to each point, and puts it in the
   * attribute slot with the provided name.
   */
  public static void setDistancesOnSequence(String attribName, Sequence seq, Pt epicenter) {
    Pt zero = getNearestPointOnSequence(epicenter, seq);
    if (zero == null) {
      return;
    }
    zero.setDouble(attribName, 0.0);
    int zeroIdx = seq.indexOf(zero);
    double runDist = 0.0;
    Pt prev = null;
    for (Pt pt : seq.backward(zeroIdx)) {
      if (prev != null && !pt.equals(zero)) {
        runDist += prev.distance(pt);
        pt.setDouble(attribName, runDist);
      }
      prev = pt;
    }
    prev = null;
    runDist = 0.0;
    for (Pt pt : seq.forward(zeroIdx)) {
      if (prev != null && !pt.equals(zero)) {
        runDist += prev.distance(pt);
        pt.setDouble(attribName, runDist);
      }
      prev = pt;
    }
  }

  public static void calculateCurvature(Sequence seq) {
    calculateCurvature(seq.getPoints());
  }

  public static void calculateCurvature(List<Pt> points) {
    Pt a = null;
    Pt b = null;
    Pt c = null;
    for (int i = 1; i < points.size() - 1; i++) {
      a = points.get(i - 1);
      b = points.get(i);
      c = points.get(i + 1);
      double curvature = Functions.getCurvature(a, b, c);
      b.setDouble("curvature", curvature);
    }
    // the first and last points have zero curvature
    points.get(0).setDouble("curvature", 0.0);
    points.get(points.size() - 1).setDouble("curvature", 0.0);
  }

  public static Rectangle2D getSequenceBoundingBox(Sequence seq) {

    double maxX = Double.MIN_VALUE;
    double minX = Double.MAX_VALUE;
    double maxY = Double.MIN_VALUE;
    double minY = Double.MAX_VALUE;

    for (Pt pt : seq) {
      maxX = Math.max(maxX, pt.getX());
      minX = Math.min(minX, pt.getX());
      maxY = Math.max(maxY, pt.getY());
      minY = Math.min(minY, pt.getY());
    }

    return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
  }

  public static List<Pt> getConvexHull(Sequence seq) {
    return getConvexHull(seq.getPoints());
  }

  public static List<Pt> getConvexHull(List<Pt> unsortedPoints) {
    return Graham.getConvexHull(unsortedPoints);
  }

  // public static Sequence getDerivative(Sequence seq) {
  // Sequence ret = new Sequence();
  // Pt addMe;
  // List<Pt> source = seq.getPoints();
  // double dy, dx, slope;
  // if (source.size() > 2) {
  // int last = source.size() - 1;
  // // initial slope is between 0 and 1.
  // addMe = new Pt(source.get(0));
  // addMe.setLocation(addMe.getX(), Functions.getSlope(source.get(0), source.get(1)));
  // ret.add(addMe);
  // for (int i = 1; i < last; i++) {
  // addMe = new Pt(source.get(i));
  // addMe.setLocation(addMe.getX(), Functions.getSlope(source.get(i-1), source.get(i+1)));
  // ret.add(addMe);
  // }
  // // final slope is between last and it's predecessor
  // addMe = new Pt(source.get(last));
  // addMe.setLocation(addMe.getX(), Functions.getSlope(source.get(last-1), source.get(last)));
  // ret.add(addMe);
  // }
  // return ret;
  // }

  @SuppressWarnings("unused")
  public static double getMinDistBetweenLineAndSequence(Line line, Sequence seq) {
    // TODO implement me
    return 0.0;
  }

  @SuppressWarnings("unused")
  public static Pt[] getIntersectionsOfLineAndSequence(Line line, Sequence seq) {
    // TODO implement me
    return null;
  }

  public static void getSplinePatch(double x0, double x1, double x2, double x3, double y0,
      double y1, double y2, double y3, Sequence seq, double numSteps) {

    // patch is drawn between points 1 and 2; if they are the same
    // point, don't draw the patch!
    // if ((x1 == x2) && (y1 == y2)) return;

    double step = 1.0 / numSteps;
    double x = 0, y = 0;

    double t0 = 0, t1 = 0, t2 = 0, t3 = 0;
    for (double u = 0.0; u <= 1.0; u += step) {

      t0 = x0 * (Math.pow((1.0 - u), 3.0)) / (6.0);
      t1 = x1 * ((3.0 * Math.pow(u, 3.0)) - (6.0 * Math.pow(u, 2.0)) + (4.0)) / (6.0);
      t2 = x2 * ((-3.0 * Math.pow(u, 3.0)) + (3.0 * Math.pow(u, 2.0)) + (3.0 * u) + (1.0)) / (6.0);
      t3 = x3 * (Math.pow(u, 3.0)) / (6.0);

      x = t0 + t1 + t2 + t3;

      t0 = y0 * (Math.pow((1.0 - u), 3.0)) / (6.0);
      t1 = y1 * ((3.0 * Math.pow(u, 3.0)) - (6.0 * Math.pow(u, 2.0)) + (4.0)) / (6.0);
      t2 = y2 * ((-3.0 * Math.pow(u, 3.0)) + (3.0 * Math.pow(u, 2.0)) + (3.0 * u) + (1.0)) / (6.0);
      t3 = y3 * (Math.pow(u, 3.0)) / (6.0);

      y = t0 + t1 + t2 + t3;

      seq.add(new Pt(x, y));
    }

  }

  public static int getSplinePatch(Pt a, Pt b, Pt c, Pt d, Sequence seq, int numSteps) {
    int before = seq.size();
    getSplinePatch(a.getX(), b.getX(), c.getX(), d.getX(), a.getY(), b.getY(), c.getY(), d.getY(),
        seq, numSteps);
    int after = seq.size();
    return after - before;
  }

  /**
   * Gets a spline with some number of control points. To get a straight line, use numCtrlPoints of
   * two.
   */
  public static Sequence getSpline(Sequence input, int numCtrlPoints, double density) {
    int numPatches = numCtrlPoints - 1;
    double inputLength = input.length();
    double lengthPerPatch = inputLength / (double) numCtrlPoints;
    Sequence ctrlPoints = Functions.getNormalizedSequence(input, lengthPerPatch);
    numPatches = ctrlPoints.size() - 1;
    int inSize = input.size();
    double pointsPerPatch = ((double) inSize / (double) numPatches);
    int numSteps = (int) Math.ceil(pointsPerPatch * density);
    Sequence ret = new Sequence();
    Pt a, b, c, d;

    int idxA, idxB, idxC, idxD;
    for (int i = 0; i <= ctrlPoints.size(); i++) {
      idxA = numInRange(i - 2, 0, numPatches);
      idxB = numInRange(i - 1, 0, numPatches);
      idxC = numInRange(i, 0, numPatches);
      idxD = numInRange(i + 1, 0, numPatches);

      a = ctrlPoints.get(idxA);
      b = ctrlPoints.get(idxB);
      c = ctrlPoints.get(idxC);
      d = ctrlPoints.get(idxD);

      getSplinePatch(a, b, c, d, ret, numSteps);
    }

    return ret;

  }

  private static int numInRange(int i, int low, int high) {
    if (i < low)
      i = low;
    if (i > high)
      i = high;
    return i;
  }

  /**
   * From a given index that is a known distance, find the point in some direction (+1 for
   * increasing indexes, -1 for decreasing) that is the specified distance along the sequence. The
   * return value is an interpolated Point based on the surrounding two points that are actually
   * found on the sequence.
   * 
   * For example: given the sequence:
   * 
   * 0 1 2 3 4 5 6 7 distance between points=10.0 * o * * * p * * +---^ With knownIndex = 5,
   * knownDistance = 40.0, direction = 1, and desiredDistance = 53, the returned Point is an
   * interpolated point between points 6 and 7 (closer to 6).
   * 
   * @param seq
   *          the Sequence to search
   * @param knownIndex
   *          the index of a known point p
   * @param knownDistance
   *          the distance that known point p is from some other interesting point o
   * @param direction
   *          the direction to search through the sequence--this parameter must be 1 for increasing
   *          indexes or -1 for decreasing indexes.
   * @param desiredDistance
   *          the target point t should be from the other interesting point o
   * 
   * @return an interpolated point (or, extrapolated if the given distance is outside the first or
   *         last point)
   */
  public static Pt getPointAtDistance(Sequence seq, int knownIndex, double knownDistance,
      int direction, double desiredDistance) {
    if (desiredDistance < knownDistance)
      throw new RuntimeException(
          "desiredDistance must be greater than or equal to known distance: "
              + Debug.num(desiredDistance) + " !< " + Debug.num(knownDistance));
    if (direction != 1 && direction != -1)
      throw new RuntimeException("Direction must be -1 or 1. Provided: " + direction);

    // first thing to do is find the indexes of points that surround
    // the known point, or if the desired point equals one of the
    // points on the sequence. It is possible that the desired point
    // is outside the confines of the sequence, in which case we'll
    // have to extrapolate.

    // at any rate, I'll enter a loop and not come out until I have
    // two indexes, a and b. a is the closer one to the known index,
    // and b is the farther one. In the event of the desired point
    // being on the sequence, a == b. In the event of extrapolation, b
    // < 0.

    int a = knownIndex;
    int b = a + direction;
    double d = knownDistance;
    Pt ret = null;
    while (true) {
      if (b < 0 || b >= seq.size()) {
        b = -1;
        break;
      }
      Pt ptA = seq.get(a);
      Pt ptB = seq.get(b);
      double segDist = ptA.distance(ptB);
      if (segDist + d > desiredDistance) {
        break; // found it!
      }
      d += segDist;
      a = b;
      b += direction;
    }

    if (b >= 0) {
      // if b is non-negative, then a and b are indexes of points that
      // surround the desired point. Interpolate.
      // a is at 'd' distance, b is beyond 'desiredDistance'.
      Pt ptA = seq.get(a);
      Pt ptB = seq.get(b);
      double bdist = ptA.distance(ptB);
      double s = (desiredDistance - d) / bdist;
      double newX = ptA.getX() + ((ptB.getX() - ptA.getX()) * s);
      double newY = ptA.getY() + ((ptB.getY() - ptA.getY()) * s);
      ret = new Pt(newX, newY);
    } else {
      // if b is negative, then the desired point is outside the
      // boundary of the sequence. Using a and the point before it,
      // extrapolate.
      Pt ptA = seq.get(a - direction);
      Pt ptB = seq.get(a);
      double bdist = ptA.distance(ptB);
      double s = (desiredDistance - d) / bdist;
      double newX = ptB.getX() + ((ptB.getX() - ptA.getX()) * s);
      double newY = ptB.getY() + ((ptB.getY() - ptA.getY()) * s);
      ret = new Pt(newX, newY);
    }
    return ret;
  }

  /**
   * Returns an array of values to be used in a matrix equation for performing an affine
   * transformation. The returned values, in order, are: scaleX, shearY, shearX, scaleY, translateX,
   * translateY. The returned array can be used as the sole argument to initialize a
   * java.awt.geom.AffineTransform. The shear values are always zero and are only returned so you
   * can directly use this method in making an AffineTransform.
   */
  public static double[] getAffineTransform(Rectangle2D source, Rectangle2D dest) {
    double scaleX = dest.getWidth() / source.getWidth();
    double scaleY = dest.getHeight() / source.getHeight();
    double transX = dest.getX() - (source.getX() * scaleX);
    double transY = dest.getY() - (source.getY() * scaleY);
    return new double[] {
        scaleX, 0.0, 0.0, scaleY, transX, transY
    };
  }

  /**
   * Creates a rectangle that includes the two points, as well as padW/2 and padH/2 surrounding it.
   */
  public static Rectangle2D makeRectangle(Pt a, Pt b, double padW, double padH) {
    double x = Math.min(a.getX(), b.getX()) - (padW / 2.0);
    double y = Math.min(a.getY(), b.getY()) - (padH / 2.0);
    double w = Math.abs(a.getX() - b.getX()) + padW;
    double h = Math.abs(a.getY() - b.getY()) + padH;
    return new Rectangle2D.Double(x, y, w, h);
  }

  public static Rectangle2D growRectangle(Rectangle2D r, double dx, double dy) {
    return new Rectangle2D.Double(r.getX() - (dx / 2.0), r.getY() - (dy / 2.0), r.getWidth() + dx,
        r.getHeight() + dy);
  }

  /**
   * It is common to need to get the fraction of some value within a range. Assuming your input
   * value is a and the range's lower and upper bounds are b and c respectively, you can use this
   * funtion to get the fraction of a in that range.
   * 
   * @param a
   *          the value that is to be tested inside some range.
   * @param b
   *          the lower (b less than c) value of the range
   * @param c
   *          the upper (c greater than b) value of the range
   * 
   * @return (a-b) / (c-b)
   */
  public static double getFraction(double a, double b, double c) {
    return (a - b) / (c - b);
  }

  /**
   * Returns the same as getFraction except the return value is guaranteed to be clamped to the
   * range 0..1.
   */
  public static double getClampedFraction(double a, double b, double c) {
    double d = getFraction(a, b, c);
    return Math.max(0.0, Math.min(1.0, d));
  }

  public static int getCrossingNumber(Pt inputPt, Sequence seq) {
    int ret = 0;
    if (seq.isClosedRegion()) {
      Line line = new Line();
      List<Pt> points = seq.getPoints();
      for (Pt pt : points) {
        line.push(pt);
        if (line.isValid()) {
          if (Functions.getWindingIntersect(inputPt, line)) {
            ret++;
          }
        }
      }
      if (points.size() > 2) {
        line.push(points.get(points.size() - 1));
        line.push(points.get(0));
        if (Functions.getWindingIntersect(inputPt, line)) {
          ret++;
        }
      }
    }
    return ret;
  }

  public static int getCrossingNumber(Pt pt, List<Sequence> sequences) {
    int count = 0;
    for (Sequence seq : sequences) {
      count = count + getCrossingNumber(pt, seq);
    }
    return count;
  }

  public static double getSum(double... numbers) {
    List<Double> list = new ArrayList<Double>();
    for (double d : numbers) {
      list.add(d);
    }
    return getSum(list);
  }

  public static double getSum(List<Double> numbers) {
    double sum = 0.0;
    for (double d : numbers) {
      sum = sum + d;
    }
    return sum;
  }

  /**
   * Tells you if a ray projected from the given point to the right (increasing x) intersects with
   * the given line. This uses some simple inequality checks in order to avoid doing math when
   * possible, so it does not calculate the exact intersection point. If you need the exact
   * intersection point, use getIntersectionData or getIntersectionPoint.
   **/
  public static boolean getWindingIntersect(Pt pt, Line line) {
    boolean ret = false;

    // first determine if y is in range
    boolean yInRange = inRange(pt.getY(), line.getStart().getY(), line.getEnd().getY());
    if (yInRange) {
      double p_x = pt.getX();
      double a_x = line.getStart().getX();
      double b_x = line.getEnd().getX();
      // most likely p will be entirely to the left or to the right of
      // the line. We are only interested if the point is to the LEFT
      // of the line, or if it is in the RANGE of the line. If it is
      // to the RIGHT of the line we can give up now.
      boolean isRight = (p_x > a_x) && (p_x > b_x);
      if (!isRight) {
        boolean isLeft = (p_x < a_x) && (p_x < b_x);
        if (isLeft) {
          ret = true;
        } else {
          Line horizon = new Line(pt, new Pt(pt.getX() + 1.0, pt.getY()));
          Pt xsec = Functions.getIntersectionPoint(horizon, line);
          if (xsec != null && xsec.getX() > pt.getX()) {
            ret = true;
          }
        }
      }
    }
    return ret;
  }

  private static boolean inRange(double val, double rangeA, double rangeB) {
    return ((val <= rangeA && val > rangeB) || (val >= rangeA && val < rangeB));
  }

  public static void bug(String what) {
    Debug.out("Functions", what);
  }
}
