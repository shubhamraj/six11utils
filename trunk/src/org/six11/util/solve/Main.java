package org.six11.util.solve;

import java.awt.Component;
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
import org.six11.util.solve.Main.Demo;

import static org.six11.util.Debug.bug;
import static org.six11.util.Debug.num;
import static java.lang.Math.toRadians;
import static java.lang.Math.sqrt;

public class Main {

  public class Demo {
    String label;
    String about;
    Method method;
    boolean initialized = false;
    List<Pt> demoPoints;
    List<Constraint> demoConstraints;

    public Demo(String label, String about, Method method) {
      this.label = label;
      this.about = about;
      this.method = method;
      this.demoPoints = new ArrayList<Pt>();
      this.demoConstraints = new ArrayList<Constraint>();
    }

    public void go() {
      // I'm aware this business with the reflection is wonky.
      // It evolved from something else. If I were to rewrite this,
      // obviously things would be cleaner.
      try {
        vars.clear();
        msg = about;
        if (initialized) {
          for (Pt pt : demoPoints) {
            Pt rand = mkRandomPoint(ui.canvas);
            pt.setLocation(rand.x, rand.y);
          }
          vars.points.addAll(demoPoints);
          vars.constraints.addAll(demoConstraints);
        } else {
          method.invoke(Main.this);
          demoPoints.addAll(vars.points);
          demoConstraints.addAll(vars.constraints);
          initialized = true;
        }
        finished = false;
        bug("Executing " + label + " = " + method.getName());
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
  
  String msg = null;
  boolean finished = false;
  int fps;
  Demo currentDemo;
  VariableBank vars;

  public static void main(String[] in) throws Exception {
    new Main(in);
  }

  public Main(String[] in) throws SecurityException, NoSuchMethodException {
    vars = new VariableBank();
    Arguments args = new Arguments();
    args.parseArguments(in);
    demos = new ArrayList<Demo>();

    Debug.useColor = args.hasFlag("use-color");
    this.fps = 30;
    if (args.hasValue("fps")) {
      this.fps = Integer.parseInt(args.getValue("fps"));
    }
    Entropy.setSeed(System.currentTimeMillis());

    demos.add(new Demo("Playground", "Add points and constraints using the interface above", this
        .getClass().getMethod("initBlank")));
    demos.add(new Demo("Distance", "Points constrained to be a constant distance apart.", this
        .getClass().getMethod("initDistanceTest")));
    demos.add(new Demo("Angle",
        "Two lines co-terminate and are constrained to be at right angles. The fulcrum is static.",
        this.getClass().getMethod("initAngleTest")));
    demos.add(new Demo("Distance and Angle",
        "Two constant-length segments, a 90-degree angle, and a 45-degree angle", this.getClass()
            .getMethod("initDestAndAngleTest")));
    demos.add(new Demo("Orientation", "Line segments try to remain perpendicular to each other.",
        this.getClass().getMethod("initOrientationTest")));
    demos.add(new Demo("Location",
        "Points constrained to specific locations. Pin them to override.", this.getClass()
            .getMethod("initPinTest")));
    demos.add(new Demo("Point As Line Parameter",
        "Inner points are between the endpoints, using different parameters (0.5 is midpoint)",
        this.getClass().getMethod("initPointOnLineTest")));
    demos.add(new Demo("Quadrilateral",
        "Outer points (NW, NE etc) use midpoints to define inner points (N, E, etc), and are "
            + "connected. Note: inner lines form a parallelogram.", this.getClass().getMethod(
            "initQuadrilateralTest")));
    demos
        .add(new Demo(
            "Point on line",
            "Constrains a point to appear somewhere on a line. It does not have to be between the endpoints.",
            this.getClass().getMethod("initPointOnLine")));

    currentDemo = demos.get(0);

    if (args.hasFlag("ui")) {
      ui = new TestSolveUI(this);
    }

    currentDemo.go();
  }

  public List<Demo> getDemos() {
    return demos;
  }

  public void initPointOnLine() {
    Pt a = mkRandomPoint(ui.canvas);
    Pt b = mkRandomPoint(ui.canvas);
    Pt m = mkRandomPoint(ui.canvas);
    addPoint("A", a);
    addPoint("B", b);
    addPoint("M", m);
    addConstraint(new PointOnLineConstraint(a, b, m));
  }

  public void initBlank() {

  }

  public void initQuadrilateralTest() {
    Pt nw = new Pt(200, 200);
    Pt sw = new Pt(230, 520);
    Pt se = new Pt(500, 460);
    Pt ne = new Pt(475, 180);
    Pt n = new Pt(0, 0);
    Pt s = new Pt(0, 0);
    Pt e = new Pt(0, 0);
    Pt w = new Pt(0, 0);
    Pt innerNW = new Pt(0, 0);
    Pt innerSW = new Pt(0, 0);
    Pt innerNE = new Pt(0, 0);
    Pt innerSE = new Pt(0, 0);
    addPoint("NW", nw);
    addPoint("SW", sw);
    addPoint("SE", se);
    addPoint("NE", ne);
    addPoint("N", n);
    addPoint("S", s);
    addPoint("E", e);
    addPoint("W", w);
    addPoint("InnerNW", innerNW);
    addPoint("InnerSW", innerSW);
    addPoint("InnerSE", innerSE);
    addPoint("InnerNE", innerNE);
    NumericValue amt = new NumericValue(0.5);
    addConstraint(new PointAsLineParamConstraint(nw, ne, amt, n));
    addConstraint(new PointAsLineParamConstraint(nw, sw, amt, w));
    addConstraint(new PointAsLineParamConstraint(se, sw, amt, s));
    addConstraint(new PointAsLineParamConstraint(se, ne, amt, e));

    addConstraint(new PointAsLineParamConstraint(n, e, amt, innerNE));
    addConstraint(new PointAsLineParamConstraint(e, s, amt, innerSE));
    addConstraint(new PointAsLineParamConstraint(s, w, amt, innerSW));
    addConstraint(new PointAsLineParamConstraint(w, n, amt, innerNW));
  }

  public void initPointOnLineTest() {
    String[] names = new String[] {
        "A", "B", // line 0 
        "C", "D", // line 1
        "E", "F", // line 2
        "G", "H", // line 3
        "I", "J", // line 4
        "K", "L" // line 5
    };
    NumericValue factor = new NumericValue(0.2);
    double factorIncr = 0.8 / ((double) names.length / 2.0);
    for (int i = 0; i < names.length; i = i + 2) {
      Pt one = mkRandomPoint(ui.canvas);
      Pt two = mkRandomPoint(ui.canvas);
      Pt mid = new Pt(0, 0);
      addPoint(names[i], one);
      addPoint(names[i + 1], two);
      addPoint(names[i] + "-" + names[i + 1], mid);
      addConstraint(new PointAsLineParamConstraint(one, two, factor, mid));
      factor = new NumericValue(factor.getValue() + factorIncr);
    }
  }

  public void initPinTest() {
    Pt ptA = mkRandomPoint(ui.canvas);
    Pt ptB = mkRandomPoint(ui.canvas);
    Pt ptC = mkRandomPoint(ui.canvas);
    Pt ptD = mkRandomPoint(ui.canvas);
    addPoint("A", ptA);
    addPoint("B", ptB);
    addPoint("C", ptC);
    addPoint("D", ptD);
    addConstraint(new LocationConstraint(ptA, new Pt(200, 200)));
    addConstraint(new LocationConstraint(ptB, new Pt(600, 200)));
    addConstraint(new LocationConstraint(ptC, new Pt(600, 400)));
    addConstraint(new LocationConstraint(ptD, new Pt(200, 400)));
  }

  public void initOrientationTest() {
    Pt ptA = mkRandomPoint(ui.canvas);
    Pt ptB = mkRandomPoint(ui.canvas);
    Pt ptC = mkRandomPoint(ui.canvas);
    Pt ptD = mkRandomPoint(ui.canvas);
    addPoint("A", ptA);
    addPoint("B", ptB);
    addPoint("C", ptC);
    addPoint("D", ptD);
    Constraint orient = new OrientationConstraint(ptA, ptB, ptC, ptD, new NumericValue(toRadians(90)));
    addConstraint(orient);
  }

  public void initDestAndAngleTest() {
    bug("Four points, three of which are part of a square, and the other is dragged away a bit. There");
    bug("are two topographic solutions to this depending on the start conditions. The weird angle must");
    bug("be 45 degrees, but it could be close to the opposite corner, or far from it.");
    Pt ptA = mkRandomPoint(ui.canvas);
    Pt ptB = mkRandomPoint(ui.canvas);
    Pt ptC = mkRandomPoint(ui.canvas);
    Pt ptD = mkRandomPoint(ui.canvas);
    addPoint("A", ptA);
    addPoint("B", ptB);
    addPoint("C", ptC);
    addPoint("D", ptD);
    NumericValue amt = new NumericValue(300.0);
    amt.setVariableName("x");
    Constraint ab = new DistanceConstraint(ptA, ptB, amt);
    Constraint bc = new DistanceConstraint(ptB, ptC, amt);
    Constraint abc = new AngleConstraint(ptA, ptB, ptC, new NumericValue(toRadians(90.0)));
    Constraint adc = new AngleConstraint(ptA, ptD, ptC, new NumericValue(toRadians(45.0)));
    addConstraint(ab);
    addConstraint(bc);
    addConstraint(abc);
    addConstraint(adc);
  }

  public void initDistanceTest() {
    Pt ptA = mkRandomPoint(ui.canvas);
    Pt ptB = mkRandomPoint(ui.canvas);
    Pt ptC = mkRandomPoint(ui.canvas);
    addPoint("A", ptA);
    addPoint("B", ptB);
    addPoint("C", ptC);
    NumericValue amt = new NumericValue(350.0);
    amt.setVariableName("sideLen");
    Constraint ab = new DistanceConstraint(ptA, ptB, amt);
    Constraint bc = new DistanceConstraint(ptB, ptC, amt);
    Constraint ca = new DistanceConstraint(ptC, ptA, amt);
    addConstraint(ab);
    addConstraint(bc);
    addConstraint(ca);
  }

  public Pt mkRandomPoint(Component comp) {
    return mkRandomPoint(comp.getWidth(), comp.getHeight());
  }

  public Pt mkRandomPoint(int i, int j) {
    Entropy rand = Entropy.getEntropy();
    return new Pt(rand.getIntBetween(0, i), rand.getIntBetween(0, j));
  }

  public void initAngleTest() {
    Pt ptA = mkRandomPoint(ui.canvas);
    Pt ptB = mkRandomPoint(ui.canvas);
    Pt ptC = mkRandomPoint(ui.canvas);
    addPoint("A", ptA);
    addPoint("B", ptB);
    addPoint("C", ptC);
    Constraint abc = new AngleConstraint(ptA, ptB, ptC, new NumericValue(toRadians(90.0)));
    addConstraint(abc);
  }

  void run() {
    Runnable runner = new Runnable() {
      public void run() {
        finished = false;
        long naptime = 0;
        if (fps > 0) {
          naptime = (long) (1000.0 / (double) fps);
        }
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
        bug("Iterative solver finished in thread " + Thread.currentThread().getId());
      }
    };
    if (EventQueue.isDispatchThread()) {
      // TODO: this is dumb. Should have a single thread handle all of this. It is possible
      // for two or more threads to be solving the system at the same time. This leads
      // to race conditions and concurrent modification issues.
      Thread thread = new Thread(runner);
      bug("Starting thread: " + thread.getId());
      thread.start();
    } else {
      runner.run();
    }
  }

  private void step() {
    // 1: clear any current correction values
    for (Pt pt : vars.points) {
      List<Vec> corrections = (List<Vec>) pt.getAttribute(ACCUM_CORRECTION);
      if (corrections == null) {
        pt.setAttribute(ACCUM_CORRECTION, new ArrayList<Vec>());
        corrections = (List<Vec>) pt.getAttribute(ACCUM_CORRECTION);
      }
      corrections.clear();
    }

    // 2: poll all constraints and have them add correction vectors to each point
    for (Constraint c : vars.constraints) {
      c.clearMessages();
      c.accumulateCorrection();
    }

    // 3: now all points have some accumulated correction. sum them and update the point's location.
    int numFinished = 0;
    for (Pt pt : vars.points) {
      List<Vec> corrections = (List<Vec>) pt.getAttribute(ACCUM_CORRECTION);
      pt.setBoolean("stable", corrections.size() == 0);
      if (corrections.size() == 0) {
        numFinished = numFinished + 1;
      }
      Vec delta = Vec.sum(corrections.toArray(new Vec[0]));
      double mag = delta.mag();
      if (mag > 1.0) {
        pt.move(delta.getVectorOfMagnitude(sqrt(mag)));
      } else if (mag > 0.0) {
        if (Entropy.getEntropy().getBoolean()) {
          pt.move(delta);
        }
      }
    }
    if (numFinished == vars.points.size()) {
      finished = true;
    }
  }

  public List<Pt> getPoints() {
    return vars.points;
  }

  public void addPoint(String name, Pt pt) {
    pt.setAttribute("name", name);
    vars.points.add(pt);
  }

  public List<Constraint> getConstraints() {
    return vars.constraints;
  }
  
  public VariableBank getVars() {
    return vars;
  }

  public void addConstraint(Constraint c) {
    vars.constraints.add(c);
  }
}
