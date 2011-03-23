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
  private static final String ACTION_GO = "go";
  private static final String DEBUG_LAYER_PREFIX = "Debug layer ";
  public static final String SEGMENT_JUNCTIONS = "junctions";
  private static final String DB_RECENT_INK = "1";
  private static final String DB_CORNER_LAYER = "2";
  private static final String DB_PATCH_DOT_LAYER = "3";
  private static final String DB_SEGMENT_DEBUG_LAYER = "4";
  private static final String DB_SEGMENT_FINAL_LAYER = "5";
  private static final String DB_OLD_INK_LAYER = "6";
  private static final String DB_MERGE_LAYER = "7";

  public static void main(String[] in) {
    Arguments args = new Arguments(in);
    Debug.enabled = args.hasFlag("debug");
    Debug.useColor = args.hasFlag("debug-color");
    Debug.useTime = !args.hasFlag("debug-no-time");
    AntCornerFinder acf = new AntCornerFinder();
  }

  private Sequence currentSeq;
  private DrawingBufferLayers layers;
  private int goCount = 0; // how many times we've pressed go since last pen input
  private List<Sequence> unprocessedSequences;
  private List<Sequence> processedSequences;

  public AntCornerFinder() {
    ApplicationFrame af = new ApplicationFrame("Ant Corner Finder");
    layers = new DrawingBufferLayers();
    layers.addPenListener(this);
    for (int i = 1; i < 10; i++) {
      boolean layerVisible = (i == 1); // only layer 1 is initially visible
      layers.createLayer("" + i, DEBUG_LAYER_PREFIX + i, i + 10, layerVisible);
    }
    unprocessedSequences = new ArrayList<Sequence>();
    processedSequences = new ArrayList<Sequence>();
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
    actions.put(ACTION_GO, new NamedAction("Go", KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0)) {
      public void activate() {
        go();
      }
    });

    // 3. For those actions with keyboard accelerators, register them to the root pane.
    for (Action action : actions.values()) {
      KeyStroke s = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
      if (s != null) {
        rp.registerKeyboardAction(action, s, JComponent.WHEN_IN_FOCUSED_WINDOW);
      }
    }
  }

  private void go() {
    if (goCount == 0) {
      for (Sequence seq : unprocessedSequences) {
        findCorners(seq);
        DrawingBuffer recentInkLayer = layers.getLayer(DB_RECENT_INK);
        DrawingBuffer oldInkLayer = layers.getLayer(DB_OLD_INK_LAYER);
        oldInkLayer.copy(recentInkLayer);
        recentInkLayer.clear();
        layers.clearScribble();
      }
      processedSequences.addAll(unprocessedSequences);
    } else {
      bug("Go again!");
    }
    goCount++;
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
    goCount = 0;
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
        unprocessedSequences.add(currentSeq);
        drawCurrentSequence(currentSeq); // first draw currentSeq to the raw ink layer
        currentSeq = new Sequence();
        break;
    }
  }

  private void findCorners(Sequence seq) {
    assignCurvature(seq); // put a 'curvature' double attribute at every point
    isolateCorners(seq);
    drawJunctions(seq);
    makeAnts(seq);
    mergeSegments(seq);
    layers.repaint();
  }

  private void mergeSegments(Sequence seq) {
    List<Ant> ants = (List<Ant>) seq.getAttribute("ants");
    List<AntSegment> allSegments = new ArrayList<AntSegment>();
    for (Ant ant : ants) {
      SortedSet<AntSegment> segments = ant.getSegments();
      allSegments.addAll(segments);
    }
    while (true) {
      if (mergeSegments(allSegments) == false) {
        break;
      }
    }
    float up = 0f;
    float down = 1f;
    float step = 1f / (float) allSegments.size();
    DrawingBuffer db = layers.getLayer(DB_MERGE_LAYER);
    for (AntSegment seg : allSegments) {
      up = (float) Math.min(1.0, up + step);
      down = (float) Math.max(0, down - step);
      Color color = new Color(up, down, 0f);
      drawSegment(seg, db, color);
    }
  }

  private boolean mergeSegments(List<AntSegment> segments) {
    bug("-------------- merging....");
    int n = segments.size();
    double[] thisErr = new double[n];
    double[] prevErr = new double[n];
    double[] nextErr = new double[n];
    AntSegment[] prevMerge = new AntSegment[n];
    AntSegment[] nextMerge = new AntSegment[n];
    for (int i = 0; i < n; i++) {
      AntSegment seg = segments.get(i);
      Sequence seq = seg.getRawInk();
      if (seg.getType() == Ant.SegType.Line) {
        thisErr[i]  = Functions.getLineError(seg.getLine(), seg.getRawInkSubsequence());
        bug("Error of line segment " + i + " is " + num(thisErr[i]));
      } else {
        thisErr[i] = Functions.getEllipseError(seg.getEllipse(), seg.getRawInkSubsequence());
        bug("Error of ellipse arc " + i + " is " + num(thisErr[i]));
      }
//      int startIdx = Functions.seekByTime(seg.getEarlyPoint(), seq, 0);
//      int endIdx = Functions.seekByTime(seg.getEarlyPoint(), seq, startIdx + 1);
       
    }
    bug("-------------- done merging");
    return false;
  }

  private void makeAnts(Sequence seq) {
    @SuppressWarnings("unchecked")
    List<Ant> ants = new ArrayList<Ant>();
    seq.setAttribute("ants", ants);
    List<Integer> junctions = (List<Integer>) seq.getAttribute(SEGMENT_JUNCTIONS);
    for (int i = 0; i < junctions.size() - 1; i++) {
      makeAnt(seq, junctions, i);
    }
  }

  private void makeAnt(Sequence seq, List<Integer> junctions, int i) {
    int a = junctions.get(i);
    int b = junctions.get(i + 1);
    Ant ant = new Ant(seq, a, b, minSegmentPatchLength, lineErrorThreshold, ellipseErrorThreshold);
    List<Ant> ants = (List<Ant>) seq.getAttribute("ants");
    ants.add(ant);
    drawAnt(ant);
  }

  private void drawSegment(AntSegment seg, DrawingBuffer db, Color color) {
    if (seg.getType() == Ant.SegType.Line) {
      Line line = seg.getLine();
      DrawingBufferRoutines.line(db, line, color, 3.0);
    } else if (seg.getType() == Ant.SegType.EllipticalArc) {
      RotatedEllipse ellie = seg.getEllipse();
      DrawingBufferRoutines.lines(db, ellie.getRestrictedArcPath(), color, 3.0);
    }
  }

  private void drawAnt(Ant ant) {
    DrawingBuffer db = layers.getLayer(DB_PATCH_DOT_LAYER);
    DrawingBufferRoutines.dots(db, ant.getPatchSeq().getPoints(), 2.5, 0.5, Color.BLACK,
        Color.WHITE);
    SortedSet<AntSegment> segments = ant.getSegments();
    db = layers.getLayer(DB_SEGMENT_DEBUG_LAYER);
    DrawingBuffer niceDb = layers.getLayer(DB_SEGMENT_FINAL_LAYER);

    Color black = Color.BLACK;
    Color color;
    for (AntSegment seg : segments) {
      if (seg.getType() == Ant.SegType.Line) {
        color = Color.green.darker();
      } else if (seg.getType() == Ant.SegType.EllipticalArc) {
        color = Color.blue;
      } else {
        color = Color.BLACK;
      }
      drawSegment(seg, db, color);
      drawSegment(seg, niceDb, black);
      //    Color m = Color.magenta.darker().darker();
      //      if (seg.getType() != Ant.SegType.None) {
      //        DrawingBufferRoutines.cross(db, seg.getSegmentStartPoint(), 3, m);
      //        DrawingBufferRoutines.cross(db, seg.getSegmentEndPoint(), 3, m);
      //      }
    }
  }

  private void drawJunctions(Sequence seq) {
    DrawingBuffer db = layers.getLayer(DB_CORNER_LAYER);
    List<Integer> junctions = (List<Integer>) seq.getAttribute(SEGMENT_JUNCTIONS);
    if (junctions == null) {
      bug("No junctions for sequence " + seq.getId());
    } else {
      for (int idx : junctions) {
        Pt pt = seq.get(idx);
        Color c = new Color(1f, 0, 0);
        DrawingBufferRoutines.dot(db, pt, 4.0, 1.0, c, c);
      }
      layers.repaint();
    }
  }

  /**
   * Sets the sequence's SEGMENT_JUNCTIONS attribute, which is a List<Integer> indicating where
   * segment boundaries are. It includes the endpoints of the stroke.
   */
  private void isolateCorners(Sequence seq) {
    // there will be clusters of high curvature. Pick the one in the curvilinear-wise middle.
    int n = seq.size();
    double highCurvatureThreshold = toRadians(highCurvatureThresholdDegrees);
    List<Integer> highCurvature = new ArrayList<Integer>();
    for (int i = 0; i < n; i++) {
      if (abs(seq.get(i).getDouble("curvature")) > highCurvatureThreshold) {
        highCurvature.add(i);
      }
    }
    List<int[]> clusterBoundaries = new ArrayList<int[]>();
    int lastIdx = 0;
    double clusterSize = 0;
    for (int idx : highCurvature) {
      if (clusterBoundaries.size() > 0) {
        double dist = seq.getPathLength(lastIdx, idx);
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
      clusterSize = seq.getPathLength(bounds[0], bounds[2]);
      double dist = 0;
      double target = clusterSize / 2;
      Pt prev = null;
      for (int idx = bounds[0]; idx <= bounds[2]; idx++) {
        Pt here = seq.get(idx);
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
    junctions.add(seq.size() - 1);
    seq.setAttribute(SEGMENT_JUNCTIONS, junctions);
  }

  private void assignCurvature(Sequence seq) {
    int n = seq.size();
    Pt[][] windows = new Pt[n][2];
    for (int i = 0; i < n; i++) {
      windows[i] = Functions.getCurvilinearWindow(seq, i, windowSize);
    }
    for (int i = 0; i < n; i++) {
      Pt me = seq.get(i);
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

  private void drawCurrentSequence(Sequence seq) {
    DrawingBuffer raw = layers.getLayer(DB_RECENT_INK);
    DrawingBufferRoutines.drawShape(raw, seq.getPoints(), DrawingBufferLayers.DEFAULT_COLOR,
        DrawingBufferLayers.DEFAULT_THICKNESS);
    layers.repaint();
  }

  private static void bug(String what) {
    Debug.out("AntCornerFinder", what);
  }

}
