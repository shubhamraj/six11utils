package org.six11.util.tmp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import org.six11.util.Debug;
import static org.six11.util.Debug.num;
import org.six11.util.args.Arguments;
import org.six11.util.gui.ApplicationFrame;
import org.six11.util.gui.BoundingBox;
import org.six11.util.lev.NamedAction;
import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.DrawingBufferRoutines;
import org.six11.util.pen.Functions;
import org.six11.util.pen.Line;
import org.six11.util.pen.PenEvent;
import org.six11.util.pen.PenListener;
import org.six11.util.pen.Pt;
import org.six11.util.pen.RotatedEllipse;
import org.six11.util.pen.Sequence;
import org.six11.util.pen.Vec;

import static java.lang.Math.acos;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static java.lang.Math.abs;

public class AntCornerFinder implements PenListener {

  //  ------------------------------------------------------------------------ - - - -
  // param block - keep these together - don't mix with non-params
  //
  private double highCurvatureThresholdDegrees = 45;
  // these are subject to zooming. e.g. if you zoom in by 2x, use windowSize/2, errorThresh/2, etc
  private double windowSize = 10;
  private double clusterDistanceThreshold = 6;
  private double minSegmentPatchLength = 12; // will be adjusted downward for short (<80px) segs.
  private double lineErrorThreshold = 1.5;
  private double ellipseErrorThreshold = 0.7;
  //
  // end param block
  //  ------------------------------------------------------------------------ - - - -

  private Map<String, Action> actions;
  private static final String ACTION_PRINT = "print";
  private static final String DEBUG_LAYER_PREFIX = "Debug layer ";
  public static final String SEGMENT_JUNCTIONS = "junctions";
  private static final String DB_PATCH_DOT_LAYER = "2";
  private static final String DB_LINE_LAYER = "3";
  private static final String DB_ELLIPSE_LAYER = "4";
  private static final String DB_BEST_SEGMENT_LAYER = "5";

  public static void main(String[] in) {
    Arguments args = new Arguments(in);
    Debug.enabled = args.hasFlag("debug");
    Debug.useColor = args.hasFlag("debug-color");
    Debug.useTime = !args.hasFlag("debug-no-time");
    AntCornerFinder acf = new AntCornerFinder();
  }

  Sequence currentSeq;
  DrawingBufferLayers layers;

  public AntCornerFinder() {
    ApplicationFrame af = new ApplicationFrame("Ant Corner Finder");
    layers = new DrawingBufferLayers();
    layers.addPenListener(this);
    for (int i = 1; i < 10; i++) {
      layers.createLayer("" + i, DEBUG_LAYER_PREFIX + i, i + 10, false);
    }
    currentSeq = new Sequence();
    createActions(af.getRootPane());
    af.setLayout(new BorderLayout());
    af.add(layers, BorderLayout.CENTER);
    af.setSize(800, 600);
    af.center();
    af.setVisible(true);
  }

  private void createActions(JRootPane rp) {
    // 1. Make action map. 
    actions = new HashMap<String, Action>();

    // 2. Fill action map with named actions.
    //
    // 2a. Start with keys for toggling layers 1-9
    for (int num = 1; num < 10; num++) {
      final String numStr = "" + num;
      KeyStroke numKey = KeyStroke.getKeyStroke(numStr.charAt(0));
      actions.put("DEBUG " + num, new NamedAction("Toggle Debug Layer " + num, numKey) {
        public void activate() {
          whackLayerVisibility(numStr);
        }
      });
    }
    //
    // 2b. Now give actions for other commands like printing, saving, launching ICBMs, etc
    actions.put(ACTION_PRINT, new NamedAction("Print", KeyStroke.getKeyStroke(KeyEvent.VK_P, 0)) {
      public void activate() {
        print();
      }
    });

    // 3. For those actions with keyboard accellerators, register them to the root pane.
    for (Action action : actions.values()) {
      KeyStroke s = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
      if (s != null) {
        rp.registerKeyboardAction(action, s, JComponent.WHEN_IN_FOCUSED_WINDOW);
      }
    }
  }

  private void print() {
    layers.print();
  }

  private void whackLayerVisibility(String name) {
    DrawingBuffer db = layers.getLayer(name);
    db.setVisible(!db.isVisible());
    bug((db.isVisible() ? "Showing" : "Hiding") + " layer " + db.getHumanReadableName());
    layers.repaint();
  }

  public void handlePenEvent(PenEvent ev) {
    switch (ev.getType()) {
      case PenEvent.TYPE_DRAG:
        if (currentSeq.size() == 0) {
          currentSeq.add(ev.getPtPrevious());
        }
        // avoid dupes
        Pt newPt = ev.getPt();
        if (!currentSeq.getLast().isSameLocation(newPt)) {
          currentSeq.add(newPt);
        }
        break;
      case PenEvent.TYPE_IDLE:
        findCorners();
        currentSeq = new Sequence();
        break;
    }
  }

  private void findCorners() {
    drawCurrentSequence(); // first draw currentSeq to the raw ink layer
    assignCurvature(); // put a 'curvature' double attribute at every point
    isolateCorners();
    drawJunctions();
    makeAnts();
    // other stuff goes here.

    //    drawCorners();
  }

  private void makeAnts() {
    @SuppressWarnings("unchecked")
    List<Integer> junctions = (List<Integer>) currentSeq.getAttribute(SEGMENT_JUNCTIONS);
    for (int i = 0; i < junctions.size() - 1; i++) {
      makeAnt(junctions, i);
    }
  }

  private void makeAnt(List<Integer> junctions, int i) {
    int a = junctions.get(i);
    int b = junctions.get(i + 1);
    Ant ant = new Ant(currentSeq, a, b, minSegmentPatchLength, lineErrorThreshold,
        ellipseErrorThreshold);
    drawAnt(ant);
  }

  private void drawAnt(Ant ant) {
    DrawingBuffer db = layers.getLayer(DB_PATCH_DOT_LAYER);
    DrawingBufferRoutines.dots(db, ant.getPatchSeq().getPoints(), 2.5, 0.5, Color.BLACK,
        Color.WHITE);
    SortedSet<AntSegment> segments = ant.getSegments();
    db = layers.getLayer(DB_BEST_SEGMENT_LAYER);
    Color m = Color.magenta.darker().darker();
    for (AntSegment seg : segments) {
      if (seg.getType() == Ant.SegType.Line) {
        Line line = seg.getLine();
        DrawingBufferRoutines.line(db, line, Color.GREEN.darker(), 3.0);
      } else if (seg.getType() == Ant.SegType.EllipticalArc) {
        RotatedEllipse ellie = seg.getEllipse();
        DrawingBufferRoutines.lines(db, ellie.getRestrictedArcPath(), Color.BLUE, 4.0);
      }
      
      if (seg.getType() != Ant.SegType.None) {
        DrawingBufferRoutines.cross(db, seg.getSegmentStartPoint(), 3, m);
        DrawingBufferRoutines.cross(db, seg.getSegmentEndPoint(), 3, m);
      }
    }
  }

  private void drawJunctions() {
    DrawingBuffer db = layers.getLayer("1");
    List<Integer> junctions = (List<Integer>) currentSeq.getAttribute(SEGMENT_JUNCTIONS);
    for (int idx : junctions) {
      Pt pt = currentSeq.get(idx);
      Color c = new Color(1f, 0, 0);
      DrawingBufferRoutines.dot(db, pt, 4.0, 1.0, c, c);
    }
    layers.repaint();
  }

  //  private void drawCurvature(List<Integer> highCurvature) {
  //    DrawingBuffer db = layers.getLayer("1");
  //    for (int idx : highCurvature) {
  //      Pt pt = currentSeq.get(idx);
  //      double howRed = 1 - abs(currentSeq.get(idx).getDouble("curvature")) / (2.0 * Math.PI);
  //      Color c = new Color((float) howRed, 0, 0);
  //      DrawingBufferRoutines.dot(db, pt, 4.0, 1.0, c, c);
  //    }
  //    layers.repaint();
  //  }

  /**
   * Sets the currentSeq's SEGMENT_JUNCTIONS attribute, which is a List<Integer> indicating where
   * segment boundaries are. It includes the endpoints of the stroke.
   */
  private void isolateCorners() {
    // there will be clusters of high curvature. Pick the one in the curvilinear-wise middle.
    int n = currentSeq.size();
    double highCurvatureThreshold = toRadians(highCurvatureThresholdDegrees);
    List<Integer> highCurvature = new ArrayList<Integer>();
    for (int i = 0; i < n; i++) {
      if (abs(currentSeq.get(i).getDouble("curvature")) > highCurvatureThreshold) {
        highCurvature.add(i);
      }
    }
    //    drawCurvature(highCurvature);
    List<int[]> clusterBoundaries = new ArrayList<int[]>();
    int lastIdx = 0;
    double clusterSize = 0;
    for (int idx : highCurvature) {
      if (clusterBoundaries.size() > 0) {
        double dist = currentSeq.getPathLength(lastIdx, idx);
        if (dist > clusterDistanceThreshold) {
          // we left the last cluster. finish the last one off and begin a new one.
          int[] bounds = clusterBoundaries.get(clusterBoundaries.size() - 1);
          bounds[2] = lastIdx;
          bounds = new int[] {
              idx, idx, idx
          };
          clusterBoundaries.add(bounds);
          clusterSize = 0;
        } else {
          clusterSize += dist;
          int[] bounds = clusterBoundaries.get(clusterBoundaries.size() - 1);
          bounds[2] = idx;
        }
      } else {
        int[] bounds = new int[] {
            idx, idx, idx
        };
        clusterBoundaries.add(bounds);
        clusterSize = 0;
      }
      lastIdx = idx;
    }
    // get the index of the point that is closest to the middle.
    for (int[] bounds : clusterBoundaries) {
      clusterSize = currentSeq.getPathLength(bounds[0], bounds[2]);
      double dist = 0;
      double target = clusterSize / 2;
      Pt prev = null;
      for (int idx = bounds[0]; idx <= bounds[2]; idx++) {
        Pt here = currentSeq.get(idx);
        if (prev != null) {
          double thisDist = here.distance(prev);
          if (dist + thisDist > target) {
            // which is closer?
            if (abs(target - dist) < abs(target - (thisDist + dist))) {
              bounds[1] = idx - 1;
            } else {
              bounds[1] = idx;
            }
            break;
          } else {
            dist = dist + thisDist;
          }
        }
        prev = here;
      }
    }

    List<Integer> junctions = new ArrayList<Integer>();
    junctions.add(0);
    for (int i = 0; i < clusterBoundaries.size(); i++) {
      int[] bounds = clusterBoundaries.get(i);
      junctions.add(bounds[1]);
    }
    junctions.add(currentSeq.size() - 1);
    currentSeq.setAttribute(SEGMENT_JUNCTIONS, junctions);
  }

  private void assignCurvature() {
    int n = currentSeq.size();
    Pt[][] windows = new Pt[n][2];
    for (int i = 0; i < n; i++) {
      windows[i] = Functions.getCurvilinearWindow(currentSeq, i, windowSize);
    }
    for (int i = 0; i < n; i++) {
      Pt me = currentSeq.get(i);
      if (windows[i][0] != null && windows[i][1] != null) {
        Vec a = new Vec(windows[i][0], me);
        Vec b = new Vec(me, windows[i][1]);
        double curvature = Functions.getSignedAngleBetween(a, b);
        me.setDouble("curvature", curvature);
      } else {
        me.setDouble("curvature", 0);
      }
    }
  }

  private void drawCurrentSequence() {
    DrawingBuffer raw = layers.getLayer("raw ink");
    DrawingBufferRoutines.drawShape(raw, currentSeq.getPoints(), DrawingBufferLayers.DEFAULT_COLOR,
        DrawingBufferLayers.DEFAULT_THICKNESS);
    layers.repaint();
  }

  private static void bug(String what) {
    Debug.out("AntCornerFinder", what);
  }

}
