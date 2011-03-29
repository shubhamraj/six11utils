package org.six11.util.tmp;

import static java.lang.Math.min;
import static java.lang.Math.max;
import static org.six11.util.Debug.num;

import java.awt.Color;
import java.util.List;
import java.util.Set;

import org.six11.util.Debug;
import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.DrawingBufferRoutines;
import org.six11.util.pen.Functions;
import org.six11.util.pen.Line;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Sequence;

public class Latcher {

  AntCornerFinder acf;

  public Latcher(AntCornerFinder acf) {
    this.acf = acf;
  }

  public void latch() {
    SketchBook sketchBook = acf.getSketchBook();
    DrawingBufferLayers layers = acf.getLayers();
    List<Sequence> last = sketchBook.getLastBatch();
    if (last != null) {
      bug("Latch last batch of " + last.size() + " seqs.");
      // draw latch graphics
      DrawingBuffer db = layers.getLayer(AntCornerFinder.DB_LATCH);
      db.clear();
      Color zc = new Color(0.7f, 0.8f, 0.9f, 0.2f);
      double dotRadius = 3.0;
      Color dc = new Color(0.1f, 0.3f, 0.1f, 1f);
      for (Sequence seq : last) {
        double latchRadius = min(AntCornerFinder.maxLatchRadius, seq.length()
            / AntCornerFinder.latchNumerator);
        DrawingBufferRoutines.dot(db, seq.getFirst(), latchRadius, 1.0, zc.darker(), zc);
        DrawingBufferRoutines.dot(db, seq.getLast(), latchRadius, 1.0, zc.darker(), zc);
        DrawingBufferRoutines.dot(db, seq.getFirst(), dotRadius, 1.0, dc, dc);
        DrawingBufferRoutines.dot(db, seq.getLast(), dotRadius, 1.0, dc, dc);
      }
      layers.repaint();
      // end drawing section
      DrawingBuffer db2 = layers.getLayer(AntCornerFinder.DB_SEGMENT_DEBUG_LAYER);
      db2.clear();
      for (Sequence seq : last) {
        double latchRadius = min(AntCornerFinder.maxLatchRadius, seq.length() / 10);
        latch(seq.getFirst(), latchRadius);
        latch(seq.getLast(), latchRadius);
      }
    }
  }

  /**
   * Try to latch the given sequence to something. 'dot' is one of the endpoints of the sequence. It
   * looks for segments nearby and tries to latch it in one of the three ways: co-termination, tee,
   * or continuation.
   * 
   * @param seq
   *          A recently made sequence.
   * @param dot
   *          One of the endpoints of the sequence.
   * @param latchRadius
   *          The magnetized region around dot.
   */
  private void latch(Pt dot, double latchRadius) {
    SketchBook sketchBook = acf.getSketchBook();
    DrawingBufferLayers layers = acf.getLayers();
    Set<AntSegment> near = sketchBook.getSegmentsNear(dot, latchRadius);
    List<AntSegment> newSegs = (List<AntSegment>) dot.getAttribute(AntCornerFinder.POINT_SEGMENTS);
    near.removeAll(newSegs);
    DrawingBuffer db = layers.getLayer(AntCornerFinder.DB_SEGMENT_DEBUG_LAYER);
    DrawingBufferRoutines.dot(db, dot, 5.0, 0.5, Color.BLACK, Color.RED);
    for (AntSegment seg : near) {
      acf.drawSegment(seg, db, Color.RED, 5);
    }
    layers.repaint();
    for (AntSegment neighbor : near) {
      for (AntSegment mySeg : newSegs) {
        latchCoterminate(neighbor, mySeg, dot, latchRadius);
      }
    }
  }

  private static void bug(String what) {
    Debug.out("Latcher", what);
  }

  /**
   * Determine if the two input segments are probably intended to co-terminate, and if so, transform
   * one or both of them to the new co-termination point.
   * 
   * @param neighbor
   *          A nearby segment
   * @param mySeg
   *          The segment currently under consideration
   * @param dot
   *          The endpoint of the segment currently under consideration
   * @param latchRadius
   *          The radius around 'dot'.
   */
  private void latchCoterminate(AntSegment neighbor, AntSegment mySeg, Pt dot, double latchRadius) {
    Pt candStart = neighbor.getEarlyPoint();
    Pt candEnd = neighbor.getLatePoint();
    Pt close, far;
    if (candStart.distance(dot) < candEnd.distance(dot)) {
      close = candStart;
      far = candEnd;
    } else {
      close = candEnd;
      far = candStart;
    }
    double otherLatchRadius = min(AntCornerFinder.maxLatchRadius, neighbor.length()
        / AntCornerFinder.latchNumerator);
    if (close.distance(dot) < otherLatchRadius) {
      Line neighborLine = neighbor.createTangentLine(close);
      Line myLine = mySeg.createTangentLine(dot);
      Pt ix = Functions.getIntersectionPoint(neighborLine, myLine);
      if (ix != null) {
        DrawingBufferLayers layers = acf.getLayers();
        DrawingBuffer db = layers.getLayer(AntCornerFinder.DB_SEGMENT_DEBUG_LAYER);
        DrawingBufferRoutines.cross(db, ix, 5.0, Color.PINK);
        layers.repaint();
        double smallRadius = min(latchRadius, otherLatchRadius);
        if (ix.distance(close) <= smallRadius && ix.distance(dot) < smallRadius) {
          bug("OK, I can perform the co-terminate latch!");
          performLatchCoterminate(mySeg, dot, neighbor, close);
        }
      }
    }
  }

  private void performLatchCoterminate(AntSegment mySeg, Pt dot, AntSegment neighbor, Pt close) {
    bug("performLatchCoterminate");
    // get the other end of mySeg to use as the fulcrum
    Sequence subseq = mySeg.getRawInkSubsequence();
    Pt fulcrum = mySeg.getEarlyPoint();
    if (mySeg.getEarlyPoint().isSameLocation(dot)) {
      fulcrum = mySeg.getLatePoint();
    }
    int fulcrumIdx = subseq.indexOf(fulcrum);
    int dotIdx = subseq.indexOf(dot);
    bug("dot: " + num(dot));
    bug("dot index: " + dotIdx + " of " + subseq.size());
    bug("fulcrum: " + num(fulcrum));
    bug("fulcrum index: " + fulcrumIdx + " of " + subseq.size());
    
    double dx = close.x - dot.x;
    double dy = close.y - dot.y;
    bug("dx/dy: " + num(dx) + "/" + num(dy));

    double segLen = subseq.length();
    bug("segLen: " + num(segLen));
    bug("path length from dot to fulcrum: "
        + num(subseq.getPathLength(min(fulcrumIdx, dotIdx), max(fulcrumIdx, dotIdx))));
    int dir = dotIdx > fulcrumIdx ? 1 : -1;
    Pt prev = null;
    bug("start/stop/dir: " + fulcrumIdx + "/" + dotIdx + "/" + dir);
    double dist = 0;
    if (dir > 0) {
      for (int i = fulcrumIdx; i <= dotIdx; i++) {
        Pt pt = subseq.get(i);
        if (prev != null) {
          dist = dist + pt.distance(prev);
          double frac = dist / segLen;
          double newX = pt.x + frac * dx;
          double newY = pt.y + frac * dy;
          bug("(up) Moving " + num(frac * 100) + "%: " + num(pt) + " to " + num(newX) + ", "
              + num(newY));
          pt.setLocation(newX, newY);
        }
        prev = pt;
      }
    } else {
      for (int i = fulcrumIdx; i >= dotIdx; i--) {
        Pt pt = subseq.get(i);
        if (prev != null) {
          dist = dist + pt.distance(prev);
          double frac = dist / segLen;
          double newX = pt.x + frac * dx;
          double newY = pt.y + frac * dy;
          bug("(down) Moving " + num(frac * 100) + "%: " + num(pt) + " to " + num(newX) + ", "
              + num(newY));
          pt.setLocation(newX, newY);
        }
        prev = pt;
      }
    }
  }
}
