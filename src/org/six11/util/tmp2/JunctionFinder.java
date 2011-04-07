package org.six11.util.tmp2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import static java.lang.Math.max;
import static java.lang.Math.min;

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
import org.six11.util.pen.CardinalSpline;
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
import org.six11.util.spud.ConstraintModel;

import static java.lang.Math.acos;
import static java.lang.Math.ceil;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static java.lang.Math.abs;

@SuppressWarnings("unchecked")
public class JunctionFinder implements PenListener {

  private Color purple = new Color(160, 32, 240);
  // instance members ------------------------------------------------------ - - - -
  private Map<String, Action> actions;
  private Sequence currentSeq;
  private DrawingBufferLayers layers;
  private int goCount = 0; // how many times we've pressed go since last pen input
  private List<Sequence> unprocessedSequences;
  private JunctionFinderDebugThing debugThing;
  private SketchBook sketchBook;
  private Latcher latcher;
  //------------------------------------------------------------------------ - - - -

  //  private List<Sequence> processedSequences;
  //  private SketchBook sketchBook;

  //  ---------------------------------------------------------------------- - - - -
  // param block - keep these together - don't mix with non-params
  //
  public static final double highCurvatureThresholdDegrees = 45;
  public static final double clusterDistanceThreshold = 6;
  public static final double windowSize = 10;
  public static final double lineErrorThreshold = 1.5;
  public static final double ellipseErrorThreshold = 0.5; // TODO: change
  public static final double minPatchSize = 10;
  //
  // end param block
  //  ---------------------------------------------------------------------- - - - -

  private static final String ACTION_PRINT = "print";
  private static final String ACTION_GO = "go";

  public static final String SEGMENT_JUNCTIONS = "junctions"; // List<Integer> : corners
  public static final String SEGMENTS = "segments"; // List<SEGMENTS> : corners

  public static final String DEBUG_LAYER_PREFIX = "Debug layer ";

  public static final String DB_RECENT_INK = "1";
  public static final String DB_JUNCTION_LAYER = "2";
  public static final String DB_DOT_LAYER = "3";
  public static final String DB_SEGMENT_LAYER = "4";
  public static final String DB_LATCH_LAYER = "5";
  public static final String DB_COMPLETE_LAYER = "6";

  // public static final String DB_FOO = "7";

  public static void main(String[] in) {
    Arguments args = new Arguments(in);
    Debug.enabled = args.hasFlag("debug");
    Debug.useColor = args.hasFlag("debug-color");
    Debug.useTime = !args.hasFlag("debug-no-time");
    JunctionFinder jf = new JunctionFinder();
  }

  public JunctionFinder() {
    ApplicationFrame af = new ApplicationFrame("Sketch");
    layers = new DrawingBufferLayers();
    layers.addPenListener(this);
    for (int i = 1; i < 10; i++) {
      boolean layerVisible = (i == 1) || (i == 6); // only these layers are initially visible
      layers.createLayer("" + i, DEBUG_LAYER_PREFIX + i, i + 10, layerVisible);
    }
    unprocessedSequences = new ArrayList<Sequence>();
    latcher = new Latcher(this);
    //    processedSequences = new ArrayList<Sequence>();
    sketchBook = new SketchBook();
    currentSeq = new Sequence();
    createActions(af.getRootPane());
    af.setLayout(new BorderLayout());
    af.add(layers, BorderLayout.CENTER);
    af.setSize(800, 600);
    af.center();
    af.setVisible(true);
    debugThing = new JunctionFinderDebugThing(this);
  }

  public SketchBook getSketchBook() {
    return sketchBook;
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
      List<Segment> batch = new ArrayList<Segment>();
      for (Sequence seq : unprocessedSequences) {
        findCorners(seq);
        batch.addAll((List<Segment>) seq.getAttribute(SEGMENTS));
      }
      sketchBook.record(batch);
      unprocessedSequences.clear();
    } else if (goCount == 1) {
      latcher.latch();
    } else {
      //      bug("goCount for " + goCount + " not implemented.");
    }
    layers.getLayer(DB_RECENT_INK).clear();
    layers.getLayer(DB_COMPLETE_LAYER).clear();
    debugThing
        .drawSegments(layers.getLayer(DB_COMPLETE_LAYER), sketchBook.getAllSegments(), purple);
    layers.repaint();
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
    isolateCorners(seq); // sets the SEGMENT_JUNCTIONS attribute (List<Integer>)
    debugThing.drawJunctions(seq);
    makeSegments(seq); // sets the SEGMENTS attrib (list of Segments)
    debugThing.drawSegments((List<Segment>) seq.getAttribute(SEGMENTS));
  }

  private void makeSegments(Sequence seq) {
    List<Integer> juncts = (List<Integer>) seq.getAttribute(SEGMENT_JUNCTIONS);
    List<Segment> segments = new ArrayList<Segment>();
    for (int i = 0; i < juncts.size() - 1; i++) {
      segments.add(identifySegment(seq, juncts.get(i), juncts.get(i + 1)));
    }
    seq.setAttribute(SEGMENTS, segments);

  }

  private Segment identifySegment(Sequence seq, int i, int j) {
    Segment ret = null;
    double segLength = seq.getPathLength(i, j);
    int numPatches = (int) ceil(segLength / minPatchSize);
    double patchLength = segLength / (double) numPatches;
    List<Pt> patch = Functions.getCurvilinearNormalizedSequence(seq, i, j, patchLength).getPoints();
    // debugThing.drawPoints(patch);
    int a = 0;
    int b = patch.size() - 1;
    Line line = new Line(patch.get(a), patch.get(b));
    double lineError = Functions.getLineError(line, patch, a, b);
    if (lineError < lineErrorThreshold) {
      ret = new LineSegment(patch, i == 0, j == seq.size() - 1);
    } else if (Functions.getEllipseError(patch) < ellipseErrorThreshold) {
      ret = new EllipseArcSegment(patch, i == 0, j == seq.size() - 1);
    } else {
      ret = new CurvySegment(patch, i == 0, j == seq.size() - 1);
    }
    return ret;
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
    Debug.out("JunctionFinder", what);
  }

  public DrawingBufferLayers getLayers() {
    return layers;
  }

  public JunctionFinderDebugThing getDebugThing() {
    return debugThing;
  }

}
