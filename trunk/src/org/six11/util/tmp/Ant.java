package org.six11.util.tmp;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.lang.Math.sqrt;

import org.six11.util.Debug;
import static org.six11.util.Debug.num;

import org.six11.util.math.EllipseFit;
import org.six11.util.pen.Functions;
import org.six11.util.pen.Line;
import org.six11.util.pen.Pt;
import org.six11.util.pen.RotatedEllipse;
import org.six11.util.pen.Sequence;

// import static org.six11.util.Debug.num;

public class Ant {

  public enum SegType {
    Line, EllipticalArc, None
  }

  // vars used by outside
  private SortedSet<AntSegment> segments;

  // vars used in various functions, not used by outside
  private Sequence patchSeq;
  private RotatedEllipse bestEllipse;

  public Ant(Sequence seq, int start, int end, double minPatchSize, double lineErrorThreshold,
      double ellipseErrorThreshold) {
    boolean done = false;
    int segmentCounter = 0;
    segments = new TreeSet<AntSegment>();
    while (!done) {
      double totalLength = seq.getPathLength(start, end);
      if (totalLength < 80) {
        // need more patches for short segments.
        double numerator = (-4.0 / 80.0 * totalLength) + 4f + 1; // y = -4/80x + 80 + 1
        minPatchSize = minPatchSize / numerator;
      }
      int numPatches = (int) Math.floor(totalLength / minPatchSize);
      double patchLength = totalLength / (double) numPatches;
      bug("Working patch: " + start + " to " + end + " in " + numPatches + " steps. patchLength "
          + num(patchLength));
      // make patch sequence
      patchSeq = Functions.getCurvilinearNormalizedSequence(seq, start, end, patchLength);
      int lineEnd = seekLine(lineErrorThreshold, 0);
      int ellipseEnd = seekEllipse(ellipseErrorThreshold, 0);
      int extent;
      if (ellipseEnd > lineEnd) {
        Pt pa = patchSeq.getFirst();
        Pt pb = patchSeq.get(ellipseEnd);
        segments.add(AntSegment.makeArcSegment(bestEllipse, pa, pb, segmentCounter));
        extent = ellipseEnd;
        bug("Segment: Arc!");
      } else if (lineEnd > 0) {
        Pt pa = patchSeq.getFirst();
        Pt pb = patchSeq.get(lineEnd);
        segments.add(AntSegment.makeLineSegment(pa, pb, segmentCounter));
        extent = lineEnd;
        bug("Segment: Line!");
      } else {
        extent = end;
        bug("Segment: None! **");
      }
      
      // Now determine if we should stop or not.
      if (extent == (patchSeq.size() - 1)) { // found last index.
        bug("Finished patch. Set done to true so we can bail.");
        done = true;
      } else {
        bug("Not done yet. Extent: " + extent + ", patch size: " + patchSeq.size());
        Sequence nextSeq = new Sequence();
        for (int i = extent; i < patchSeq.size(); i++) {
          nextSeq.add(patchSeq.get(i));
        }
        seq = nextSeq.getReverseSequence();
        start = 0;
        end = seq.size() - 1;
        bug("Reversed sequence (" + seq.size() + " points, " + num(seq.length())
            + " long). Updated start/end to " + start + ", " + end);
      }
      segmentCounter++;
    }
  }

  private int seekEllipse(double ellipseErrorThreshold, int startIdx) {
    int ret = -1;
    double bigT = ellipseErrorThreshold * 2;
    double[] ellipseError = new double[patchSeq.size()];
    RotatedEllipse[] ellipses = new RotatedEllipse[patchSeq.size()];
    int biggestIndexExamined = -1;
    if (patchSeq.size() - (startIdx + 4) >= 0) {
      for (int i = startIdx + 4; i < patchSeq.size(); i++) { // between startIdx (incl.) and i (excl.)
        List<Pt> somePoints = patchSeq.getSubSequence(startIdx, i + 1).getPoints();
        Sequence somePointsSeq = new Sequence(somePoints);
        if (!Functions.arePointsColinear(somePoints)) {
          try {
            Pt midPt = somePointsSeq.get(somePointsSeq.size() / 2);
            RotatedEllipse ellie = EllipseFit.ellipseFit(somePoints);
            if (ellie.getMinorRadius() < 2 || ellie.getMajorRadius() < 2) {
              continue;
            }
            ellie.setArcRegion(somePointsSeq.getFirst(), midPt, somePointsSeq.getLast());
            List<Pt> ellipseSurface = ellie.getRestrictedArcPath(patchSeq.size() * 8);
            double errorSum = 0;
            for (int j = startIdx; j < i; j++) {
              Pt nearest = Functions.getNearestPointOnSequence(patchSeq.get(j), ellipseSurface);
              double error = patchSeq.get(j).distance(nearest);
              errorSum = errorSum + (error * error);
            }
            ellipseError[i] = sqrt(errorSum) / (i - 2);
            ellipses[i] = ellie;
            ret = i;
            biggestIndexExamined = i;
            if (ellipseError[i] > bigT) {
              break;
            }
          } catch (Exception ex) {
            bug("Can't make ellipse out of indices " + startIdx + " to " + i
                + ". Straight line, maybe.");
            ex.printStackTrace();
          }
        } else {
          bug("Avoiding ellipse fit on colinear sequence from " + startIdx + " to " + i);
        }
      }
      boolean found = false;
      for (int j = biggestIndexExamined; j > startIdx; j--) {
        if (j == biggestIndexExamined && ellipseError[j] < ellipseErrorThreshold) {
          ret = j;
          found = true;
        }
        if (!found && ellipseError[j] < (ellipseErrorThreshold / 2)) {
          ret = j;
          found = true;
        }
        if (!found && j < biggestIndexExamined
            && (ellipseError[j] < ellipseErrorThreshold && ellipseError[j] > ellipseError[j + 1])) {
          ret = j + 1;
          found = true;
        }
        if (found) {
          break;
        } else {
          ret = j;
        }
      }
    }
    if (ret >= 0 && ellipses[ret] == null) {
      ret = -1;
      bestEllipse = null;
    } else if (ret < 0) {
      bestEllipse = null;
    } else {
      bestEllipse = ellipses[ret];
    }
    return ret;
  }

  private int seekLine(double lineErrorThreshold, int startIdx) {
    int ret = -1;
    int biggestIndexExamined = -1;
    Pt startPt = patchSeq.get(startIdx);
    double[] lineError = new double[patchSeq.size()];
    for (int i = startIdx + 2; i < patchSeq.size(); i++) { // patch between startIdx and i
      Line line = new Line(startPt, patchSeq.get(i));
      double errorSum = 0;
      for (int j = startIdx + 1; j < i; j++) { // measure points between startIdx and i
        Pt pt = patchSeq.get(j);
        double error = Functions.getDistanceBetweenPointAndLine(pt, line);
        errorSum = errorSum + (error * error);
      }
      lineError[i] = sqrt(errorSum) / (i - 1);
      biggestIndexExamined = i; // set this in case the entire patchSeq is a straight line
    }

    boolean found = false;
    for (int j = biggestIndexExamined; j > startIdx; j--) {
      if (j == biggestIndexExamined && lineError[j] < lineErrorThreshold) {
        ret = j;
        found = true;
      }
      if (!found && lineError[j] < (lineErrorThreshold / 2)) {
        ret = j;
        found = true;
      }
      if (!found && j < biggestIndexExamined
          && (lineError[j] < lineErrorThreshold && lineError[j] > lineError[j + 1])) {
        ret = j + 1;
        found = true;
      }
    }
    return ret;
  }

  public Sequence getPatchSeq() {
    return patchSeq;
  }

  public SortedSet<AntSegment> getSegments() {
    return segments;
  }

  private static void bug(String what) {
    Debug.out("Ant", what);
  }
}
