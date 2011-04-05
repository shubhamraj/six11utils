package org.six11.util.tmp;

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

import static java.lang.Math.acos;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static java.lang.Math.abs;

@SuppressWarnings("unchecked")
public class AntCornerFinder implements PenListener {

  //  ------------------------------------------------------------------------ - - - -
  // param block - keep these together - don't mix with non-params
  //
  public static final double highCurvatureThresholdDegrees = 45;
  public static final double maxSplineDeviationThreshold = 8;
  public static final double mergeImprovementMult = 1.2;
  // these are subject to zooming. e.g. if you zoom in by 2x, use windowSize/2, errorThresh/2, etc
  public static final double windowSize = 10;
  public static final double clusterDistanceThreshold = 6;
  public static final double minSegmentPatchLength = 12; // will be adjusted downward for short (<80px) segs.
  public static final double lineErrorThreshold = 1.5;
  public static final double ellipseErrorThreshold = 0.7;
  public static final double maxLatchRadius = 60.0;
  public static final double latchNumerator = 10.0;

  //
  // end param block
  //  ------------------------------------------------------------------------ - - - -

  private Map<String, Action> actions;
  private static final String ACTION_PRINT = "print";
  private static final String ACTION_GO = "go";
  public static final String DEBUG_LAYER_PREFIX = "Debug layer ";

  private static final String DB_RECENT_INK = "1";
  public static final String DB_CORNER_LAYER = "2";
  public static final String DB_PATCH_DOT_LAYER = "3";
  public static final String DB_SEGMENT_DEBUG_LAYER = "4";
  public static final String DB_SEGMENT_FINAL_LAYER = "5";
  public static final String DB_OLD_INK_LAYER = "6";
  public static final String DB_MERGE_LAYER = "7";
  public static final String DB_LATCH = "8";

  // constants used as keys in Sequence objects
  public static final String SEGMENT_JUNCTIONS = "junctions"; // List<Integer> : corners
  public static final String SEQUENCE_SEGMENTS = "segments"; // List<AntSegment> : segments
  public static final String POINT_SEGMENTS = "point segments"; // List<AntSegment> : segs for Pt

  public static void main(String[] in) {
    Arguments args = new Arguments(in);
    Debug.enabled = args.hasFlag("debug");
    Debug.useColor = args.hasFlag("debug-color");
    Debug.useTime = !args.hasFlag("debug-no-time");
    AntCornerFinder acf = new AntCornerFinder();
  }

  private Latcher latcher;
  private Sequence currentSeq;
  private DrawingBufferLayers layers;
  private int goCount = 0; // how many times we've pressed go since last pen input
  private List<Sequence> unprocessedSequences;
  //  private List<Sequence> processedSequences;
  private SketchBook sketchBook;

  public AntCornerFinder() {
    ApplicationFrame af = new ApplicationFrame("Ant Corner Finder");
    layers = new DrawingBufferLayers();
    layers.addPenListener(this);
    for (int i = 1; i < 10; i++) {
      boolean layerVisible = (i == 1); // only layer 1 is initially visible
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
      sketchBook.addBatch(unprocessedSequences);
      unprocessedSequences.clear();
    } else if (goCount == 1) {
      latcher.latch();
    } else {
      bug("goCount for " + goCount + " not implemented.");
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
    splinify(seq);
    drawSegments(seq);
    layers.repaint();
  }

  private void drawSegments(Sequence seq) {
    @SuppressWarnings("unchecked")
    List<AntSegment> allSegments = (List<AntSegment>) seq.getAttribute(SEQUENCE_SEGMENTS);
    DrawingBuffer db = layers.getLayer(DB_SEGMENT_FINAL_LAYER);
    for (AntSegment seg : allSegments) {
      drawSegment(seg, db, Color.BLACK, 3.0);
    }
  }

  private void splinify(Sequence seq) {
    List<AntSegment> allSegments = (List<AntSegment>) seq.getAttribute(SEQUENCE_SEGMENTS);
    List<Integer> junctions = (List<Integer>) seq.getAttribute(SEGMENT_JUNCTIONS);
    List<AntSegment> arcs = new ArrayList<AntSegment>();
    int junctionCounter = 1;
    for (int i = 0; i < allSegments.size(); i++) {
      AntSegment seg = allSegments.get(i);
      if (seg.getType() == Ant.SegType.EllipticalArc) {
        arcs.add(seg);
      }
      if (seg.getLatePoint().isSameLocation(seq.get(junctions.get(junctionCounter)))) {
        if (arcs.size() > 0) {
          List<Pt> spline = splinifySegments(arcs);
          for (AntSegment arcSeg : arcs) {
            arcSeg.setSpline(spline);
          }
        }
        arcs.clear();
        junctionCounter++;
      }
    }
  }

  private List<Pt> splinifySegments(List<AntSegment> arcs) {
    Sequence raw = arcs.get(0).getRawInk();
    Pt early = arcs.get(0).getEarlyPoint();
    Pt late = arcs.get(arcs.size() - 1).getLatePoint();
    int earlyIdx = Functions.seekByTime(early, raw.getPoints(), 0);
    int lateIdx = Functions.seekByTime(late, raw.getPoints(), earlyIdx);
    Sequence rawRegion = raw.getSubSequence(earlyIdx, lateIdx + 1);
    int numPatches = (int) Math.floor(rawRegion.length() / minSegmentPatchLength);
    double patchLength = rawRegion.length() / (double) numPatches;
    Sequence patchRegion = Functions.getCurvilinearNormalizedSequence(rawRegion, 0,
        rawRegion.size() - 1, patchLength);
    double tightness = 1.0;
    double maxSegLen = 4.0;
    List<Pt> ctrl = CardinalSpline.subdivideControlPoints(patchRegion.getPoints(),
        maxSplineDeviationThreshold, tightness, maxSegLen);
    List<Pt> spline = CardinalSpline.interpolateCardinal(ctrl, tightness, maxSegLen);
    DrawingBuffer db = layers.getLayer(DB_CORNER_LAYER);
    DrawingBufferRoutines.dots(db, ctrl, 4.0, 0.6, Color.BLACK, new Color(1.0f, 0.4f, 0.8f));
    return spline;
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
    // clear the 'ants' attrib because it is no longer a thing
    //    seq.setAttribute("ants", null);
    seq.setAttribute(SEQUENCE_SEGMENTS, allSegments);
  }

  private void tmpBugSegments(String msg, List<AntSegment> segs) {
    bug(msg);
    for (AntSegment seg : segs) {
      bug("  seq " + seg.getRawInk().getId() + " " + seg.getEarlyPointIndex() + "--"
          + seg.getLatePointIndex() + "\t" + seg.getType() + " "
          + (seg.hasSpline() ? "[splinified]" : ""));
    }
  }

  private boolean mergeSegments(List<AntSegment> segments) {
    boolean ret = false;
    int n = segments.size();
    double[] thisErr = new double[n];
    double[] mergedErr = new double[n - 1];
    AntSegment[] merged = new AntSegment[n - 1];
    for (int i = 0; i < (n - 1); i++) {
      AntSegment here = segments.get(i);
      AntSegment there = segments.get(i + 1);
      Sequence seq = here.getRawInk();
      if (here.getType() == Ant.SegType.Line) {
        thisErr[i] = Functions.getLineError(here.getLine(), here.getRawInkSubsequence());
      } else {
        thisErr[i] = Functions.getEllipseError(here.getEllipse(), here.getRawInkSubsequence());
      }
      int startIdx = Functions.seekByTime(here.getEarlyPoint(), seq.getPoints(), 0);
      int endIdx = Functions.seekByTime(there.getLatePoint(), seq.getPoints(), startIdx + 1);
      Sequence mergeSeq = seq.getSubSequence(startIdx, endIdx + 1);
      Line mergeLine = new Line(seq.get(startIdx), seq.get(endIdx));
      RotatedEllipse mergeEllipse = Functions.createEllipse(mergeSeq.getPoints());
      double mergeLineError = Functions.getLineError(mergeLine, mergeSeq);
      double mergeEllipseError = mergeEllipse == null ? Double.MAX_VALUE : Functions
          .getEllipseError(mergeEllipse, mergeSeq);
      if (mergeLineError <= mergeEllipseError) {
        merged[i] = AntSegment.makeLineSegment(seq.get(startIdx), seq.get(endIdx), seq);
        mergedErr[i] = mergeLineError;
      } else {
        merged[i] = AntSegment
            .makeArcSegment(mergeEllipse, seq.get(startIdx), seq.get(endIdx), seq);
        mergedErr[i] = mergeEllipseError;
      }
    }
    // pick best candidate for merging
    int bestIndex = -1; // which one to replace
    double bestMergeError = Double.MAX_VALUE;
    String mergeString = "";
    for (int i = 0; i < (n - 1); i++) {
      if (mergedErr[i] < bestMergeError) {
        double nomergeError = thisErr[i] + thisErr[i + 1];
        if (nomergeError * mergeImprovementMult > mergedErr[i]) { // merging is a sloppy improvement
          bestIndex = i;
          bestMergeError = mergedErr[i];
        }
      }
    }
    // if bestIndex is non-negative, swap in the new segment, update the input list, set ret=true
    if (bestIndex >= 0) {
      bug(mergeString);
      Sequence whichSequence = segments.get(bestIndex).getRawInk();
      List<Integer> junctions = (List<Integer>) whichSequence.getAttribute(SEGMENT_JUNCTIONS);
      AntSegment mondoSeg = merged[bestIndex];
      int newSegStart = mondoSeg.getEarlyPointIndex();
      int newSegEnd = mondoSeg.getLatePointIndex();
      List<Integer> doomed = new ArrayList<Integer>();
      for (int junction : junctions) {
        if (junction > newSegStart && junction < newSegEnd) {
          doomed.add(junction);
          bug("Will remove junction " + junction + " from list of corners");
        }
      }
      junctions.removeAll(doomed);
      segments.remove(bestIndex);
      segments.remove(bestIndex); // ^^ clean up ants
      segments.add(bestIndex, merged[bestIndex]);
      ret = true;
    }
    return ret;
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
    //    drawAnt(ant);
  }

  public void drawSegment(AntSegment seg, DrawingBuffer db, Color color, double thickness) {
    Set<List<Pt>> splinesDrawn = new HashSet<List<Pt>>();
    if (seg.getType() == Ant.SegType.Line) {
      Line line = seg.getLine();
      DrawingBufferRoutines.line(db, line, color, thickness);
    } else if (seg.getType() == Ant.SegType.EllipticalArc) {
      if (seg.hasSpline()) {
        if (!splinesDrawn.contains(seg.getSpline())) {
          DrawingBufferRoutines.lines(db, seg.getSpline(), color, thickness);
          splinesDrawn.add(seg.getSpline());
        }
      } else {
        bug("Drawing ellipse, but I shouldn't because I should use the spline. Fail.");
        RotatedEllipse ellie = seg.getEllipse();
        DrawingBufferRoutines.lines(db, ellie.getRestrictedArcPath(), color, thickness);
      }
    }
  }

  //  private void drawAnt(Ant ant) {
  //    DrawingBuffer db = layers.getLayer(DB_PATCH_DOT_LAYER);
  //    DrawingBufferRoutines.dots(db, ant.getPatchSeq().getPoints(), 2.5, 0.5, Color.BLACK,
  //        Color.WHITE);
  //    SortedSet<AntSegment> segments = ant.getSegments();
  //    db = layers.getLayer(DB_SEGMENT_DEBUG_LAYER);
  //    DrawingBuffer niceDb = layers.getLayer(DB_SEGMENT_FINAL_LAYER);
  //
  //    Color black = Color.BLACK;
  //    Color color;
  //    for (AntSegment seg : segments) {
  //      if (seg.getType() == Ant.SegType.Line) {
  //        color = Color.green.darker();
  //      } else if (seg.getType() == Ant.SegType.EllipticalArc) {
  //        color = Color.blue;
  //      } else {
  //        color = Color.BLACK;
  //      }
  //      drawSegment(seg, db, color);
  //      drawSegment(seg, niceDb, black);
  //    }
  //  }

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

  public SketchBook getSketchBook() {
    return sketchBook;
  }

  public DrawingBufferLayers getLayers() {
    return layers;
  }

}