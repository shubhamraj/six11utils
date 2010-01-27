package org.six11.util.pen;

import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.six11.util.Debug;

public class OliveSoup {

  private Sequence seq;
  private List<Sequence> pastSequences;
  private List<DrawingBuffer> drawingBuffers;
  private MouseThing mouseThing;
  private Map<Sequence, DrawingBuffer> seqToDrawBuf;

  // The currentSeq and last index are for managing the currently-in-progress ink stroke
  private GeneralPath gp;
  private int lastCurrentSequenceIdx;

  // change listeners are interested in visual changes
  private List<ChangeListener> changeListeners;

  // sequence listeners are interested in pen activity
  private Set<SequenceListener> sequenceListeners;

  public OliveSoup() {
    drawingBuffers = new ArrayList<DrawingBuffer>();
    pastSequences = new ArrayList<Sequence>();
    sequenceListeners = new HashSet<SequenceListener>();
    seqToDrawBuf = new HashMap<Sequence, DrawingBuffer>();
  }

  public void addSequenceListener(SequenceListener lis) {
    sequenceListeners.add(lis);
  }

  public void removeSequenceListener(SequenceListener lis) {
    sequenceListeners.remove(lis);
  }

  private void fireSequenceEvent(SequenceEvent ev) {
    for (SequenceListener lis : sequenceListeners) {
      lis.handleSequenceEvent(ev);
    }
  }

  /**
   * Registers a change listener, which is whacked every time some (potentially) visual aspect of
   * the soup has changed.
   */
  public void addChangeListener(ChangeListener lis) {
    if (changeListeners == null) {
      changeListeners = new ArrayList<ChangeListener>();
    }
    if (!changeListeners.contains(lis)) {
      changeListeners.add(lis);
    }
  }

  /**
   * Removes a change listener previously added with addChangeListener.
   */
  public void removeChangeListener(ChangeListener lis) {
    if (changeListeners != null) {
      changeListeners.remove(lis);
    }
  }

  /**
   * Fires a simple event indicating some (potentially) visual aspect of the soup has changed.
   */
  private void fireChange() {
    if (changeListeners != null) {
      ChangeEvent ev = new ChangeEvent(this);
      for (ChangeListener cl : changeListeners) {
        cl.stateChanged(ev);
      }
    }
  }

  public List<Sequence> getSequences() {
    return pastSequences;
  }

  /**
   * Draws the portion of the current sequence that has not yet been drawn. The input parameter is
   * expected to contain variables that refer to 'Pt' instances. Each Pt instance has an x and y
   * member that can be resolved to an integer. This is an efficient implementation minimizes the
   * amount of time spent in the drawing routine. It does this by caching the index of the last
   * drawn Pt object. To reset this cache use the forgetCurrentSequence() method.
   */
  protected void drawSequence() {
    if (seq != null) {
      for (int i = lastCurrentSequenceIdx; i < seq.size(); i++) {
        Pt pt = seq.get(i);
        if (i == 0) {
          gp = new GeneralPath();
          gp.moveTo(pt.x, pt.y);
        } else {
          gp.lineTo(pt.x, pt.y);
        }
        lastCurrentSequenceIdx = i;
      }
    }
    fireChange();
  }

  /**
   * Adds a complete visual element: perhaps a raw Sequence, but it could be something else such as
   * a filled region or a rectified shape.
   */
  public void addBuffer(DrawingBuffer buf) {
    if (!drawingBuffers.contains(buf)) {
      drawingBuffers.add(buf);
    }
    fireChange();
  }

  /**
   * Resets the display list by removing all elements.
   */
  public void clearBuffers() {
    drawingBuffers.clear();
    fireChange();
  }

  public void addRawInputBegin(int x, int y, long t) {
    seq = new Sequence();
    addRawInputProgress(x, y, t);
    SequenceEvent sev = new SequenceEvent(this, seq, SequenceEvent.Type.BEGIN);
    fireSequenceEvent(sev);
  }

  public void addRawInputProgress(int x, int y, long t) {
    // Avoid adding duplicate points to the end of the sequence.
    Pt pt = new Pt(x, y, t);
    if (seq.size() == 0 || !seq.getLast().isSameLocation(pt)) {
      seq.add(pt);
      SequenceEvent sev = new SequenceEvent(this, seq, SequenceEvent.Type.PROGRESS);
      fireSequenceEvent(sev);
      drawSequence();
    }
  }

  public void addRawInputEnd() {
    if (seq == null) {
      return;
    } else {
      addFinishedSequence(seq);
      seq = null;
      lastCurrentSequenceIdx = 0;
      gp = null;
      fireChange();
    }
  }

  public void addFinishedSequence(Sequence s) {
    if (s != null && s.size() > 1) {
      DrawingBuffer buf = new DrawingBuffer();
      seqToDrawBuf.put(s, buf);
      buf.setColor(DrawingBuffer.getBasicPen().color);
      buf.setThickness(DrawingBuffer.getBasicPen().thickness);
      buf.up();
      buf.moveTo(s.get(0).x, s.get(0).y);
      buf.down();
      for (Pt pt : s) {
        buf.moveTo(pt.x, pt.y);
      }
      buf.up();
      drawingBuffers.add(buf);
      pastSequences.add(s);
      SequenceEvent sev = new SequenceEvent(this, s, SequenceEvent.Type.END);
      fireSequenceEvent(sev);
    }
  }

  public DrawingBuffer getDrawingBufferForSequence(Sequence s) {
    return seqToDrawBuf.get(s);
  }

  public void clearDrawing() {
    seq = null;
    drawingBuffers = new ArrayList<DrawingBuffer>();
    fireChange();
  }

  /**
   * Returns a reference to the currently in-progress scribble, suitable for efficient drawing.
   */
  public Shape getCurrentSequenceShape() {
    return gp;
  }

  /**
   * Returns a list of all cached drawing buffers.
   */
  public List<DrawingBuffer> getDrawingBuffers() {
    return drawingBuffers;
  }

  public MouseThing getMouseThing() {
    if (mouseThing == null) {
      mouseThing = new OliveMouseThing(this);
    }
    return mouseThing;
  }

  /**
   * A mouse motion and click adapter that sends events to an OliveSoup instance.
   * 
   * @author Gabe Johnson <johnsogg@cmu.edu>
   */
  private static class OliveMouseThing extends MouseThing {

    private OliveSoup soup;

    public OliveMouseThing(OliveSoup soup) {
      this.soup = soup;
    }

    public void mousePressed(MouseEvent ev) {
      soup.addRawInputBegin(ev.getX(), ev.getY(), ev.getWhen());
    }

    public void mouseDragged(MouseEvent ev) {
      soup.addRawInputProgress(ev.getX(), ev.getY(), ev.getWhen());
    }

    public void mouseReleased(MouseEvent ev) {
      soup.addRawInputEnd();
    }

    @SuppressWarnings("unused")
    private void bug(final String what) {
      Debug.out("OliveMouseThing", what);
    }
  }

  @SuppressWarnings("unused")
  private void bug(String what) {
    Debug.out("OliveSoup", what);
  }
}
