package org.six11.util.pen;

import java.util.EventObject;

import org.six11.util.Debug;

/**
 * A Pen Event. Possible types are taps and drags, as well as idle
 * (indicating the pen is no longer being used and the stroke, tap, or
 * selection is done), and flow selection. 
 *
 * The constructor for PenEvent is private. You must use one of the
 * factory methods to build your pen events.
 **/
public class PenEvent extends EventObject {

  public final static int TYPE_FLOW = 1;
  public final static int TYPE_TAP = 2;
  public final static int TYPE_DRAG = 3;
  public final static int TYPE_IDLE = 4;

  private int type; // type of event (on of the constants above)

  // various interesting points related to the pen event
  private Pt pt;         // most recent pen location
  private Pt ptPrevious; // previous pen location (e.g. for drawing)
  private Pt ptFlow;     // flow selection center point
  
  private long timestamp; // timestamp of when the event was created

  private int fsPhase; // number of times the flow selection has been
		       // entered since last idle.

  private PenEvent(Object source) {
    super(source);
    timestamp = System.currentTimeMillis();
  }

  /**
   * Returns one of the TYPE_ values.
   */
  public int getType() {
    return type;
  }

  /**
   * Returns the location the pen was at when the event was fired.
   */
  public Pt getPt() {
    return pt;
  }

  /**
   * In the case of a 'drag' event, this is the location that the pen
   * was in right before it's current location.
   */
  public Pt getPtPrevious() {
    return ptPrevious;
  }

  /**
   * In the case of a 'flow' or 'drag' event, this is the current
   * center of the flow operation. When you are dragging, this is the
   * center of the region that you must stay in for some period of
   * time in order to begin flow selection.
   */
  public Pt getPtFlow() {
    return ptFlow;
  }

  /**
   * The timestamp in milliseconds of when the event was generated.
   */
  public long getTimestamp() {
    return timestamp;
  }

  /**
   * Returns the number of times a unique flow selection phase has
   * been entered. For example if you push the pen down and start
   * dragging, the flow selection phase is zero.
   */
  public int getFlowSelectionPhase() {
    return fsPhase;
  }

  /**
   * Make a new flow selection event for TYPE_FLOW.
   */
  public static PenEvent buildFlowEvent(Object source, Pt pt, 
					int fsPhase, Pt flowPt) {
    PenEvent ret = new PenEvent(source);
    ret.type = TYPE_FLOW;
    ret.pt = pt;
    ret.ptFlow = flowPt;
    ret.fsPhase = fsPhase;
    return ret;
  }

  /**
   * Make a new flow selection event for TYPE_TAP.
   */
  public static PenEvent buildTapEvent(Object source, Pt pt) {
    PenEvent ret = new PenEvent(source);
    ret.type = TYPE_TAP;
    ret.pt = pt;
    return ret;
  }

  /**
   * Make a new flow selection event for TYPE_DRAG.
   */
  public static PenEvent buildDragEvent(Object source, Pt pt, Pt ptPrevious,
					int fsPhase, Pt flowPt) {
    PenEvent ret = new PenEvent(source);
    ret.type = TYPE_DRAG;
    ret.pt = pt;
    ret.ptPrevious = ptPrevious;
    ret.ptFlow = flowPt;
    ret.fsPhase = fsPhase;
    return ret; 
  }

  /**
   * Make a new flow selection event for TYPE_IDLE.
   */
  public static PenEvent buildIdleEvent(Object source) {
    PenEvent ret = new PenEvent(source);
    ret.type = TYPE_IDLE;
    ret.pt = null;
    ret.ptPrevious = null;
    ret.ptFlow = null;
    return ret; 
  }
  
  private static String getTypeString(int t) {
    String ret = null;
    switch (t) {
    case TYPE_FLOW: ret = "Flow"; break;
    case TYPE_TAP: ret = "Tap "; break;
    case TYPE_DRAG: ret = "Drag"; break;
    case TYPE_IDLE: ret = "Idle"; break;
    }
    return ret;
  }

  /**
   * Returns a nicely formatted debugging string.
   */
  public String toString() {
    return "[PenEvent " + PenEvent.getTypeString(type) + " pt: " + Debug.num(pt) +
      " prevPt: " + Debug.num(ptPrevious) + " flowPt: " + Debug.num(ptFlow) + " fsPhase: " + Debug.num(fsPhase) + "]";
  }

}
