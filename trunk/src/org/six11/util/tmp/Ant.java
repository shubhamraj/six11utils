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
    Sequence originalSequence = seq;
    while (!done) {
      double totalLength = seq.getPathLength(start, end);
      if (totalLength < 80) {
        // need more patches for short segments.
        double numerator = (-4.0 / 80.0 * totalLength) + 4f + 1; // y = -4/80x + 80 + 1
        minPatchSize = minPatchSize / numerator;
      }
      int numPatches = (int) Math.floor(totalLength / minPatchSize);
      double patchLength = totalLength / (double) numPatches;
      // make patch sequence
      patchSeq = Functions.getCurvilinearNormalizedSequence(seq, start, end, patchLength);
      int lineEnd = seekLine(lineErrorThreshold, 0);
      int ellipseEnd = seekEllipse(ellipseErrorThreshold, 0);
      int extent;
      if (ellipseEnd > lineEnd) {
        Pt pa = patchSeq.getFirst();
        Pt pb = patchSeq.get(ellipseEnd);
        segments.add(AntSegment.makeArcSegment(bestEllipse, pa, pb, originalSequence));
        extent = ellipseEnd;
      } else if (lineEnd > 0) {
        Pt pa = patchSeq.getFirst();
        Pt pb = patchSeq.get(lineEnd);
        segments.add(AntSegment.makeLineSegment(pa, pb, originalSequence));
        extent = lineEnd;
      } else {
        extent = end;
      }

      // Now determine if we should stop or not.
      if (extent == (patchSeq.size() - 1)) { // found last index.
        done = true;
      } else {
        Sequence nextSeq = new Sequence();
        for (int i = extent; i < patchSeq.size(); i++) {
          nextSeq.add(patchSeq.get(i));
        }
        seq = nextSeq.getReverseSequence();
        start = 0;
        end = seq.size() - 1;
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
      // Make a bunch of ellipses and measure their errors.
      for (int i = startIdx + 4; i < patchSeq.size(); i++) {
        List<Pt> somePoints = patchSeq.getSubSequence(startIdx, i + 1).getPoints();
        RotatedEllipse ellie = Functions.createEllipse(somePoints);
        if (ellie != null) {
          ellipseError[i] = Functions.getEllipseError(ellie, patchSeq.getSubSequence(startIdx, i));
          ellipses[i] = ellie;
          ret = i;
          biggestIndexExamined = i;
          if (ellipseError[i] > bigT) {
            break; // if error becomes too great, stop the process.
          }
        }
      }
      // loop through those ellipses and find the best one. Go backwards from longest ellipse found.
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
      lineError[i] = Functions.getLineError(new Line(startPt, patchSeq.get(i)), patchSeq
          .getSubSequence(startIdx + 1, i));
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
