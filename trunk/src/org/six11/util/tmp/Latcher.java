package org.six11.util.tmp;

import static java.lang.Math.min;
import static java.lang.Math.max;
import static java.lang.Math.toDegrees;
import static java.lang.Math.abs;
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
import org.six11.util.pen.Vec;
import org.six11.util.tmp.AntSegment.Terminal;

public class Latcher {

  AntCornerFinder acf;

  public Latcher(AntCornerFinder acf) {
    this.acf = acf;
  }

  public void latch() {
    SketchBook sketchBook = acf.getSketchBook();
    DrawingBufferLayers layers = acf.getLayers();
    List<Sequence> lastSequences = sketchBook.getLastBatch();
    List<AntSegment> last = SketchBook.extractSegments(lastSequences);
    if (last != null) {
      bug("Latch last batch of " + last.size() + " segments");
      // draw latch graphics
      DrawingBuffer db = layers.getLayer(AntCornerFinder.DB_LATCH);
      db.clear();
      Color zc = new Color(0.7f, 0.8f, 0.9f, 0.2f);
      double dotRadius = 3.0;
      Color dc = new Color(0.1f, 0.3f, 0.1f, 1f);
      for (AntSegment seg : last) {
        AntSegment.Terminal term = seg.getTerminal();
        bug("Looking at segment with hash: " + seg.hashCode() + ".");
        bug("It has a terminal: " + seg.getTerminal());
        bug("This segment (hash: " + seg.hashCode() + ") has " + term.size() + " terminals.");
        for (int i = 0; i < term.size(); i++) {
          double latchRadius = min(AntCornerFinder.maxLatchRadius, seg.length()
              / AntCornerFinder.latchNumerator);
          Pt lineA = new Pt(term.getPoint(i)).getTranslated(term.getDir(i), latchRadius);
          Pt lineB = new Pt(term.getPoint(i)).getTranslated(term.getDir(i).getFlip(), latchRadius);
          Line tangentLine = new Line(lineA, lineB);
          //          DrawingBufferRoutines.line(db, tangentLine, zc.darker(), 1.0);
          DrawingBufferRoutines.arrow(db, term.getPoint(i),
              term.getPoint(i).getTranslated(term.getDir(i), 20), 2.0, Color.MAGENTA.darker());
          DrawingBufferRoutines.dot(db, term.getPoint(i), latchRadius, 1.0, zc.darker(), zc);
          DrawingBufferRoutines.dot(db, term.getPoint(i), dotRadius, 1.0, dc, dc);
          Set<Pt> near = sketchBook.getCornerPoints().getNear(term.getPoint(i), latchRadius);
          List<AntSegment> neighbors = SketchBook.extractSegments(near);
          neighbors.remove(seg);
          for (AntSegment neighbor : neighbors) {
            acf.drawSegment(neighbor, db, Color.RED, 4.0);
            // which end of the neighbor should we try to latch to?
            Pt nA = neighbor.getEarlyPoint();
            Pt nB = neighbor.getLatePoint();
            double dA = nA.distance(term.getPoint(i));
            double dB = nB.distance(term.getPoint(i));
            int neighborIdx = -1;
            if (dA < dB && dA < latchRadius) {
              neighborIdx = neighbor.getEarlyPointIndex();
            } else if (dB <= dA && dB < latchRadius) {
              neighborIdx = neighbor.getLatePointIndex();
            }
            if (neighborIdx >= 0) {
              Vec neighborDir = neighbor.getDirection(neighborIdx);
              Pt neighborPt = neighbor.getRawInk().get(neighborIdx);
              Pt neighborPtA = new Pt(neighborPt.getTranslated(neighborDir, latchRadius));
              Pt neighborPtB = new Pt(neighborPt.getTranslated(neighborDir.getFlip(), latchRadius));
              Line neighborTangentLine = new Line(neighborPtA, neighborPtB);
              // DrawingBufferRoutines.line(db, neighborTangentLine, zc.darker(), 1.0);
              DrawingBufferRoutines.arrow(db, neighborPt,
                  neighborPt.getTranslated(neighborDir, 20), 2.0, Color.MAGENTA);
              Pt ix = Functions.getIntersectionPoint(tangentLine, neighborTangentLine);
              if (ix != null && ix.distance(term.getPoint(i)) < latchRadius) {
                DrawingBufferRoutines.cross(db, ix, 4.0, Color.GREEN);
                double neighborLatchRadius = min(AntCornerFinder.maxLatchRadius, neighbor
                    .getRawInk().length() / AntCornerFinder.latchNumerator);
                latch(seg, term.getPoint(i), term.getDir(i), latchRadius, neighbor, neighborPt,
                    neighborDir, neighborLatchRadius, ix);
              }
            }
          }
        }
      }
      layers.repaint();
      // end drawing section

    }
  }

  private void latch(AntSegment segA, Pt ptA, Vec dirA, double radA, AntSegment segB, Pt ptB,
      Vec dirB, double radB, Pt ix) {
    boolean condDist = (ptA.distance(ix) < radA) && (ptB.distance(ix) < radB);
    double ang = Functions.getSignedAngleBetween(dirA, dirB);
    boolean condDir = toDegrees(abs(ang)) > 15;
    if (condDist && condDir) {
      segA.rotateTo(ptA, ptB);
    }
  }

  private void bug(String what) {
    Debug.out("Latcher", what);
  }

}
