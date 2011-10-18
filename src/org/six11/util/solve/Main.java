package org.six11.util.solve;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;

import org.six11.util.Debug;
import org.six11.util.args.Arguments;
import org.six11.util.pen.Entropy;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;

import static org.six11.util.Debug.bug;
import static java.lang.Math.toRadians;

public class Main {

  public static final String ACCUM_CORRECTION = "accumulated correction";
  TestSolveUI ui = null;
  List<Pt> points;
  List<Constraint> constraints;
  boolean finished = false;
  int fps;
  /**
   * @param args
   */
  public static void main(String[] in) {
    new Main(in);
  }

  public Main(String[] in) {
    points = new ArrayList<Pt>();
    constraints = new ArrayList<Constraint>();
    Arguments args = new Arguments();
    args.parseArguments(in);
    if (args.hasFlag("ui")) {
      ui = new TestSolveUI(this);
    }
    Debug.useColor = args.hasFlag("use-color");
    this.fps = 10;
    if (args.hasValue("fps")) {
      this.fps = Integer.parseInt(args.getValue("fps"));
    }
    Entropy.setSeed(System.currentTimeMillis());
    String test = "angleTest";
    if (args.hasValue("test")) {
      test = args.getValue("test");
    }
    if ("distanceTest".equals(test)) {
      initDistanceTest();
    } else if ("angleTest".equals(test)) {
      initAngleTest();
    }
    run();
  }

  private void initDistanceTest() {
    bug("This test establishes three points at random locations and enforces three distance constraints.");
    bug("An equilateral triangle should emerge.");

    Pt ptA = mkRandomPoint(800, 600);
    Pt ptB = mkRandomPoint(800, 600);
    Pt ptC = mkRandomPoint(800, 600);
    addPoint("A", ptA);
    addPoint("B", ptB);
    addPoint("C", ptC);
    Constraint ab = new DistanceConstraint(ptA, ptB, 350.0);
    Constraint bc = new DistanceConstraint(ptB, ptC, 350.0);
    Constraint ca = new DistanceConstraint(ptC, ptA, 350.0);
    addConstraint(ab);
    addConstraint(bc);
    addConstraint(ca);
  }

  private Pt mkRandomPoint(int i, int j) {
    Entropy rand = Entropy.getEntropy();
    return new Pt(rand.getIntBetween(0, i), rand.getIntBetween(0, j));
  }

  private void initAngleTest() {
    Pt ptA = mkRandomPoint(800, 600);
    Pt ptB = mkRandomPoint(800, 600);
    Pt ptC = mkRandomPoint(800, 600);
    addPoint("A", ptA);
    addPoint("B", ptB);
    addPoint("C", ptC);
    Constraint abc = new AngleConstraint(ptA, ptB, ptC, toRadians(90.0));
    addConstraint(abc);
  }

  void run() {
    Runnable runner = new Runnable() {
      @Override
      public void run() {
        finished = false;
        long naptime = 0;
        if (fps > 0) {
          naptime = (long) (1000.0 / (double) fps);
        }

        bug("Loop is using " + naptime + " sleepy time");
        //    naptime = naptime + 800;
        long start = System.currentTimeMillis();
        while (!finished) {
          step();
          if (ui != null) {
            bug("telling canvas to redraw...");
            ui.canvas.repaint();
          }
          try {
            Thread.sleep(naptime);
          } catch (InterruptedException ex) {
            ;
          }
        }
        long elapsed = System.currentTimeMillis() - start;
        bug("System solved in " + elapsed + " ms");               
      }
    };
    if (EventQueue.isDispatchThread()) {
      new Thread(runner).start();
    } else {
      runner.run();
    }
  }

  private void step() {
    // 1: clear any current correction values
    for (Pt pt : points) {
      List<Vec> corrections = (List<Vec>) pt.getAttribute(ACCUM_CORRECTION);
      if (corrections == null) {
        pt.setAttribute(ACCUM_CORRECTION, new ArrayList<Vec>());
        corrections = (List<Vec>) pt.getAttribute(ACCUM_CORRECTION);
      }
      corrections.clear();
    }

    // 2: poll all constraints and have them add correction vectors to each point
    for (Constraint c : constraints) {
      c.accumulateCorrection();
    }

    // 3: now all points have some accumulated correction. sum them and update the point's location.
    int numFinished = 0;
    for (Pt pt : points) {
      List<Vec> corrections = (List<Vec>) pt.getAttribute(ACCUM_CORRECTION);
      pt.setBoolean("stable", corrections.size() == 0);
      if (corrections.size() == 0) {
        numFinished = numFinished + 1;
      }
      Vec delta = Vec.sum(corrections.toArray(new Vec[0]));
      pt.setLocation(pt.getX() + delta.getX(), pt.getY() + delta.getY());
    }
    if (numFinished == points.size()) {
      finished = true;
      bug("All points are in satisfactory locations!");
    }
  }

  public List<Pt> getPoints() {
    return points;
  }

  private void addPoint(String name, Pt pt) {
    pt.setAttribute("name", name);
    points.add(pt);
  }

  public List<Constraint> getConstraints() {
    return constraints;
  }

  private void addConstraint(Constraint c) {
    constraints.add(c);
  }
}
