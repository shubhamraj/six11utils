package org.six11.util.tmp2;

import java.awt.Color;
import java.util.List;

import org.six11.util.Debug;
import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.DrawingBufferRoutines;
import org.six11.util.pen.Line;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Sequence;
import org.six11.util.tmp2.Segment.Terminal;

@SuppressWarnings("unchecked")
public class JunctionFinderDebugThing {

  JunctionFinder jf;

  public JunctionFinderDebugThing(JunctionFinder jf) {
    this.jf = jf;
  }

  public void drawJunctions(Sequence seq) {
    DrawingBuffer db = jf.getLayers().getLayer(JunctionFinder.DB_JUNCTION_LAYER);
    List<Integer> juncts = (List<Integer>) seq.getAttribute(JunctionFinder.SEGMENT_JUNCTIONS);
    for (int idx : juncts) {
      Pt where = seq.get(idx);
      DrawingBufferRoutines.dot(db, where, 3.0, 0.5, Color.BLACK, Color.RED);
    }
    jf.getLayers().repaint();
  }

  public void drawPoints(List<Pt> patch) {
    DrawingBuffer db = jf.getLayers().getLayer(JunctionFinder.DB_DOT_LAYER);
    DrawingBufferRoutines.dots(db, patch, 2.5, 0.3, Color.LIGHT_GRAY, Color.white.darker());
    jf.getLayers().repaint();
  }

  private void bug(String what) {
    Debug.out("JunctionFinderDebugThing", what);
  }

  public void drawSegments(List<Segment> segments) {
    DrawingBuffer db = jf.getLayers().getLayer(JunctionFinder.DB_SEGMENT_LAYER);
    drawSegments(db, segments, null);
  }

  public void drawTerm(Terminal term, double latchRadius) {
    Color dotColor = new Color(0.8f, 0.3f, 0.3f);
    Color zoneColor = new Color(0.8f, 0.6f, 0.6f, 0.2f);
    DrawingBuffer db = jf.getLayers().getLayer(JunctionFinder.DB_LATCH_LAYER);
    DrawingBufferRoutines.dot(db, term.getPoint(), 3.0, 0.5, dotColor, dotColor);
    DrawingBufferRoutines.dot(db, term.getPoint(), latchRadius, 0.5, zoneColor.darker(), zoneColor);
    Pt antenna = term.getPoint().getTranslated(term.getDir(), latchRadius);
    //    DrawingBufferRoutines.line(db, new Line(term.getPoint(), antenna), zoneColor.darker(), 1.0);
    DrawingBufferRoutines.arrow(db, term.getPoint(), antenna, 2.0, zoneColor.darker());
    jf.getLayers().repaint();
  }

  /**
   * Draw some segments with an optional color. If the color is null, default colors are chosen
   * based on segment type.
   * 
   * @param allSegments
   * @param preferredColor
   */
  public void drawSegments(DrawingBuffer db, List<Segment> segments, Color preferredColor) {

    Color darkGreen = Color.green.darker();
    Color darkBlue = Color.blue.darker();
    Color pc = preferredColor;
    for (Segment seg : segments) {
      if (seg.getType() == Segment.Type.Line) {
        DrawingBufferRoutines.line(db, seg.asLine(), pc == null ? darkGreen : pc, 2.0);
      } else if (seg.getType() == Segment.Type.Curve) {
        DrawingBufferRoutines.drawShape(db, seg.asSpline(), pc == null ? darkBlue : pc, 2.0);
      } else if (seg.getType() == Segment.Type.EllipticalArc) {
        DrawingBufferRoutines.drawShape(db, seg.asPolyline(), pc == null ? darkBlue : pc, 2.0);
      } else {
        Debug.warn(this, "Unknown segment type in drawSegments: " + seg.getType());
      }
    }
  }
}
