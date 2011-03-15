package org.six11.util.tmp;

import java.util.ArrayList;
import java.util.List;
import static java.lang.Math.sqrt;

import org.six11.util.Debug;
import static org.six11.util.Debug.num;

import org.six11.util.math.EllipseFit;
import org.six11.util.pen.Functions;
import org.six11.util.pen.Line;
import org.six11.util.pen.Pt;
import org.six11.util.pen.RotatedEllipse;
import org.six11.util.pen.Sequence;
//import static org.six11.util.Debug.num;

public class Ant {

  Sequence patchSeq;
  int lineEnd;
  int ellipseEnd;
  RotatedEllipse bestEllipse;
  RotatedEllipse[] ellipses;

  public Ant(Sequence seq, int start, int end, double minPatchSize, double lineErrorThreshold,
      double ellipseErrorThreshold) {
    double totalLength = seq.getPathLength(start, end);
    if (totalLength < 80) {
      // need more patches for short segments.
      double numerator = (-4.0/80.0 * totalLength) + 4 + 1; // y = -4/80x + 80 + 1
      minPatchSize = minPatchSize / numerator;
    }
    int numPatches = (int) Math.floor(totalLength / minPatchSize);
    double patchLength = totalLength / (double) numPatches;
    bug(start + " to " + end + " in " + numPatches + " steps. patchLength " + num(patchLength));
    // make patch sequence
    patchSeq = Functions.getCurvilinearNormalizedSequence(seq, start, end, patchLength);
    lineEnd = seekLine(lineErrorThreshold, 0);
    ellipseEnd = seekEllipse(ellipseErrorThreshold, 0);
  }

  private int seekEllipse(double ellipseErrorThreshold, int startIdx) {
    bug("--------------------------(startIdx=" + startIdx + ")");
    int ret = -1;
    double bigT = ellipseErrorThreshold * 2;
    double[] ellipseError = new double[patchSeq.size()];
    ellipses = new RotatedEllipse[patchSeq.size()];
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
            //            System.out.println(i + "\t" + num(ellipseError[i]));
            ellipses[i] = ellie;
            ret = i;
            biggestIndexExamined = i;
            bestEllipse = ellie;
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
      //      bug("Error array follows (biggestIndexExamined=" + biggestIndexExamined + "):");
      //      for (int errorIdx = 0; errorIdx < ellipseError.length; errorIdx++) {
      //        System.out.println(errorIdx + "\t" + num(ellipseError[errorIdx]));
      //      }
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
          //          ret = j + 1;
          bestEllipse = ellipses[ret];
          bug("woo! best ellipse is at index " + ret);
          break;
        } else {
          ret = j;
        }
      }
    }
    if (ret > 0 && ellipses[ret] == null) {
      ret = -1;
    }
    if (ret < 0) {
      bug("Couldn't find a reasonable ellipse for this segment. Returning -1.");
    } else {
      bug("Longest ellipse: " + startIdx + " to " + ret);
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
      // if we have too much error, we know the solution is somewhere behind us. Look backwards
      // and pick the first local minimum you come to that is within the lineErrorThreshold.
      biggestIndexExamined = i; // set this in case the entire patchSeq is a straight line
      //      if (lineError[i] > bigT) {
      //        break;
      //      }
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

    for (int i = 0; i < lineError.length; i++) {
      System.out.println(i + "\t" + num(lineError[i]) + (i == ret ? " *" : ""));
    }

    bug("Longest line: " + startIdx + " to " + ret);
    return ret;
  }

  public Sequence getPatchSeq() {
    return patchSeq;
  }

  private static void bug(String what) {
    Debug.out("Ant", what);
  }
}
