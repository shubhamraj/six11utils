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

  /**
   * combinedBuffers is ONLY to be used in getDrawingBuffers(). It is nulled out whenever
   * drawingBuffers or namedBuffers changes.
   **/
  private List<DrawingBuffer> combinedBuffers;
  private List<DrawingBuffer> drawingBuffers;
  private Map<String, DrawingBuffer> namedBuffers;
  private MouseThing mouseThing;
  private Map<Sequence, DrawingBuffer> seqToDrawBuf;

  // The currentSeq and last index are for managing the currently-in-progress ink stroke
  private GeneralPath gp;
  private boolean gpVisible;
  private int lastCurrentSequenceIdx;

  // change listeners are interested in visual changes
  private List<ChangeListener> changeListeners;

  // sequence listeners are interested in pen activity
  private Set<SequenceListener> sequenceListeners;

  // olive soup listeners are interested in hearing about recognition or any other analysis. There
  // are many types of listeners, so in order to make routing faster, each listener must say which
  // specific event(s) they are interested in.
  private Map<String, List<OliveSoupListener>> allSoupListeners;

  private Map<String, List<Object>> soupData;

  public OliveSoup() {
    drawingBuffers = new ArrayList<DrawingBuffer>();
    namedBuffers = new HashMap<String, DrawingBuffer>();
    pastSequences = new ArrayList<Sequence>();
    sequenceListeners = new HashSet<SequenceListener>();
    allSoupListeners = new HashMap<String, List<OliveSoupListener>>();
    soupData = new HashMap<String, List<Object>>();
    seqToDrawBuf = new HashMap<Sequence, DrawingBuffer>();
  }

  public void addSoupListener(String type, OliveSoupListener lis) {
    if (!getSoupListeners(type).contains(lis)) {
      getSoupListeners(type).add(lis);
    }
  }

  public List<OliveSoupListener> getSoupListeners(String type) {
    if (!allSoupListeners.containsKey(type)) {
      allSoupListeners.put(type, new ArrayList<OliveSoupListener>());
    }
    return allSoupListeners.get(type);
  }

  public void removeSoupListener(String type, OliveSoupListener lis) {
    getSoupListeners(type).remove(lis);
  }

  private void fireOliveSoupEvent(OliveSoupEvent ev) {
    for (OliveSoupListener lis : getSoupListeners(ev.type)) {
      lis.handleSoupEvent(ev);
    }
  }

  public void addSoupData(String key, Object data) {
    getData(key).add(data);
    OliveSoupEvent ev = new OliveSoupEvent(this, key, data);
    fireOliveSoupEvent(ev);
  }

  public List<Object> getData(String key) {
    if (!soupData.containsKey(key)) {
      soupData.put(key, new ArrayList<Object>());
    }
    return soupData.get(key);
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
   * the soup has changed and the GUI should be repainted.
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
   * Draws the portion of the current sequence that has not yet been drawn.
   */
  protected void drawSequence() {
    if (seq != null) {
      for (int i = lastCurrentSequenceIdx; i < seq.size(); i++) {
        Pt pt = seq.get(i);
        if (i == 0) {
          gp = new GeneralPath();
          gp.moveTo((float) pt.x, (float) pt.y);
        } else {
          gp.lineTo((float) pt.x, (float) pt.y);
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
      combinedBuffers = null;
    }
    fireChange();
  }

  public void addBuffer(String name, DrawingBuffer buf) {
    namedBuffers.put(name, buf);
    combinedBuffers = null;
    fireChange();
  }

  public void removeBuffer(String name) {
    if (namedBuffers.containsKey(name)) {
      namedBuffers.remove(name);
      combinedBuffers = null;
      fireChange();
    }
  }

  public DrawingBuffer getBuffer(String name) {
    return namedBuffers.get(name);
  }

  /**
   * Resets the display list by removing all elements.
   */
  public void clearBuffers() {
    drawingBuffers.clear();
    namedBuffers.clear();
    combinedBuffers = null;
    fireChange();
  }

  public void addRawInputBegin(int x, int y, long t) {
    seq = new Sequence();
    Pt pt = new Pt(x, y, t);
    seq.add(pt);
    gpVisible = true;
    // addRawInputProgress(x, y, t);
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
      gpVisible = false;
      fireChange();
    }
  }

  public void addFinishedSequence(Sequence s) {
    if (s != null && s.size() > 1 && gpVisible) {
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
      combinedBuffers = null;
      pastSequences.add(s);
    }

    if (s != null) {
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
    combinedBuffers = null;
    fireChange();
  }

  /**
   * Returns a reference to the currently in-progress scribble, suitable for efficient drawing.
   */
  public Shape getCurrentSequenceShape() {
    return gp;
  }
  
  public boolean isCurrentSequenceShapeVisible() {
    return gpVisible;
  }
  
  public void setCurrentSequenceShapeVisible(boolean vis) {
    gpVisible = vis;
  }

  /**
   * Returns a list of all cached drawing buffers.
   */
  public List<DrawingBuffer> getDrawingBuffers() {
    if (combinedBuffers == null) {
      combinedBuffers = new ArrayList<DrawingBuffer>();
      combinedBuffers.addAll(drawingBuffers);
      combinedBuffers.addAll(namedBuffers.values());
    }
    return combinedBuffers;
  }

  public List<DrawingBuffer> getAnonymousDrawingBuffers() {
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
