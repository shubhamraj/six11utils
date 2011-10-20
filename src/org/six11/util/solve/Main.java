package org.six11.util.solve;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.six11.util.Debug;
import org.six11.util.args.Arguments;
import org.six11.util.pen.Entropy;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;

import static org.six11.util.Debug.bug;
import static org.six11.util.Debug.num;
import static java.lang.Math.toRadians;

public class Main {

  public class Demo {
    String label;
    Method method;
    public Demo(String label, Method method) {
      this.label = label;
      this.method = method;
    }
    public void go() {
      try {
        points.clear();
        constraints.clear();
        finished = false;
        bug("Executing " + label + " = " + method.getName());
        method.invoke(Main.this);
        ui.canvas.repaint();
      } catch (Exception ex) {
        ex.printStackTrace();
      } 
    }
    
    public String toString() {
      return label;
    }

  }

  public static final String ACCUM_CORRECTION = "accumulated correction";
  List<Demo> demos;
  TestSolveUI ui = null;
  List<Pt> points;
  List<Constraint> constraints;
  boolean finished = false;
  int fps;

  public static void main(String[] in) throws Exception {
    new Main(in);
  }

  public Main(String[] in) throws SecurityException, NoSuchMethodException {
    points = new ArrayList<Pt>();
    constraints = new ArrayList<Constraint>();
    Arguments args = new Arguments();
    args.parseArguments(in);
    demos = new ArrayList<Demo>();

    Debug.useColor = args.hasFlag("use-color");
    this.fps = 30;
    if (args.hasValue("fps")) {
      this.fps = Integer.parseInt(args.getValue("fps"));
    }
    Entropy.setSeed(System.currentTimeMillis());
    String test = "pinTest";
    if (args.hasValue("test")) {
      test = args.getValue("test");
    }
    demos.add(new Demo("distanceTest", this.getClass().getMethod("initDistanceTest")));
    demos.add(new Demo("angleTest", this.getClass().getMethod("initAngleTest")));
    demos.add(new Demo("destAndAngleTest", this.getClass().getMethod("initDestAndAngleTest")));
    demos.add(new Demo("orientationTest", this.getClass().getMethod("initOrientationTest")));
    demos.add(new Demo("pinTest", this.getClass().getMethod("initPinTest")));
//    if ("distanceTest".equals(test)) {
//      initDistanceTest();
//    } else if ("angleTest".equals(test)) {
//      initAngleTest();
//    } else if ("destAndAngleTest".equals(test)) {
//      initDestAndAngleTest();
//    } else if ("orientationTest".equals(test)) {
//      double degrees = 45;
//      if (args.hasValue("degrees")) {
//        degrees = Double.parseDouble(args.getValue("degrees"));
//      }
//      initOrientationTest(degrees);
//    } else if ("pinTest".equals(test)) {
//      initPinTest();
//    }
    if (args.hasFlag("ui")) {
      ui = new TestSolveUI(this);
    }
    run();
  }
  
  public List<Demo> getDemos() {
    return demos;
  }

  public void initPinTest() {
    Pt ptA = mkRandomPoint(800, 600);
    Pt ptB = mkRandomPoint(800, 600);
    Pt ptC = mkRandomPoint(800, 600);
    Pt ptD = mkRandomPoint(800, 600);
    addPoint("A", ptA);
    addPoint("B", ptB);
    addPoint("C", ptC);
    addPoint("D", ptD);
    addConstraint(new PinConstraint(ptA, new Pt(200, 200)));
    addConstraint(new PinConstraint(ptB, new Pt(600, 200)));
    addConstraint(new PinConstraint(ptC, new Pt(600, 400)));
    addConstraint(new PinConstraint(ptD, new Pt(200, 400)));
  }

  public void initOrientationTest() {
    Pt ptA = mkRandomPoint(800, 600);
    Pt ptB = mkRandomPoint(800, 600);
    Pt ptC = mkRandomPoint(800, 600);
    Pt ptD = mkRandomPoint(800, 600);
    addPoint("A", ptA);
    addPoint("B", ptB);
    addPoint("C", ptC);
    addPoint("D", ptD);
    Constraint orient = new OrientationConstraint(ptA, ptB, ptC, ptD, toRadians(90));
    addConstraint(orient);
  }

  public void initDestAndAngleTest() {
    // TODO Auto-generated method stub
    bug("Four points, three of which are part of a square, and the other is dragged away a bit. There");
    bug("are two topographic solutions to this depending on the start conditions. The weird angle must");
    bug("be 45 degrees, but it could be close to the opposite corner, or far from it.");
    Pt ptA = mkRandomPoint(800, 600);
    Pt ptB = mkRandomPoint(800, 600);
    Pt ptC = mkRandomPoint(800, 600);
    Pt ptD = mkRandomPoint(800, 600);
    addPoint("A", ptA);
    addPoint("B", ptB);
    addPoint("C", ptC);
    addPoint("D", ptD);
    Constraint ab = new DistanceConstraint(ptA, ptB, 300.0);
    Constraint bc = new DistanceConstraint(ptB, ptC, 300.0);
    Constraint abc = new AngleConstraint(ptA, ptB, ptC, toRadians(90.0));
    Constraint adc = new AngleConstraint(ptA, ptD, ptC, toRadians(45.0));
    addConstraint(ab);
    addConstraint(bc);
    addConstraint(abc);
    addConstraint(adc);
  }

  public void initDistanceTest() {
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

  public void initAngleTest() {
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
        //      naptime = naptime + 800;
//        if (naptime > 0) {
//          bug("Loop will sleep for " + naptime + " ms between steps.");
//        } else {
//          bug("Loop going full blast.");
//        }

        //        long start = System.currentTimeMillis();
        while (!finished) {
          step();
          if (ui != null) {
            ui.canvas.repaint();
          }
          try {
            Thread.sleep(naptime);
          } catch (InterruptedException ex) {
            ;
          }
        }
        //        long elapsed = System.currentTimeMillis() - start;
        //        bug("System solved in " + elapsed + " ms");               
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
      c.clearMessages();
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
