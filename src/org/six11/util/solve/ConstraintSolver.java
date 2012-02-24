package org.six11.util.solve;

import java.awt.Component;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.six11.util.Debug;
import org.six11.util.args.Arguments;
import org.six11.util.data.Statistics;
import org.six11.util.pen.Entropy;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;

import static org.six11.util.Debug.bug;
import static java.lang.Math.sqrt;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class ConstraintSolver {

  private boolean debugOutput = false;
  private BufferedWriter debugOutWriter = null;
  private String f = "%+#.10f";
  private StringBuilder buf;

  public static interface Listener {
    public void constraintStepDone(State state, int numIterations, double err, int numPoints,
        int numConstraints);
  }

  public static enum State {
    Solved, Unsatisfied, Working;
  }

  public static final String ACCUM_CORRECTION = "accumulated correction";

  private List<Listener> stepListeners;
  private TestSolveUI ui = null;
  protected String msg = null;
  private boolean finished = false;
  protected int fps;
  protected VariableBank vars;
  private Object monitor;
  private State currentState;
  private double residual;
  private int numIterations;
  private boolean paused;

  public static void main(String[] in) throws Exception {
    new ConstraintSolver(in);
  }

  public ConstraintSolver() {
    init();
  }

  public ConstraintSolver(String[] in) throws SecurityException, NoSuchMethodException {
    init();
    Arguments args = new Arguments();
    args.parseArguments(in);
    Debug.useColor = args.hasFlag("use-color");
    if (args.hasValue("fps")) {
      this.fps = Integer.parseInt(args.getValue("fps"));
    }
    if (args.hasFlag("ui")) {
      createUI();
    }
    run();
  }

  private final void init() {
    this.monitor = new Object();
    this.vars = new VariableBank();
    this.stepListeners = new ArrayList<Listener>();
    this.buf = new StringBuilder();
  }

  public void setFrameRate(int frameRate) {
    if (frameRate != fps) {
      bug("Changing framerate to " + frameRate);
    }
    this.fps = frameRate;
  }

  public void setDebugOut(boolean v) {
    this.debugOutput = v;
  }

  public void setDebugOutWriter(BufferedWriter bw) {
    this.debugOutWriter = bw;
  }

  public void createUI() {
    this.ui = new TestSolveUI(this);
  }

  public void addListener(Listener lis) {
    if (!stepListeners.contains(lis)) {
      stepListeners.add(lis);
    }
  }

  public void removeListener(Listener lis) {
    stepListeners.remove(lis);
  }

  protected void fire() {
    for (Listener lis : stepListeners) {
      lis.constraintStepDone(currentState, numIterations, residual, vars.points.size(),
          vars.constraints.size());
    }
  }

  public Pt mkRandomPoint(Component comp) {
    return mkRandomPoint(comp.getWidth(), comp.getHeight());
  }

  public Pt mkRandomPoint(int i, int j) {
    Entropy rand = Entropy.getEntropy();
    return new Pt(rand.getIntBetween(0, i), rand.getIntBetween(0, j));
  }

  public void runInBackground() {
    Runnable runner = new Runnable() {
      public void run() {
        ConstraintSolver.this.run();
      }
    };
    new Thread(runner, "Constraint Solver").start();
  }

  void run() {
    finished = false;
    long naptime = 0;
    if (fps > 0) {
      naptime = (long) (1000.0 / (double) fps);
    } else {
      naptime = 0;
    }
    double prevError = Double.MAX_VALUE;
    double heat = 1.0;
    double heatStep = -0.001;
    numIterations = 0;
    while (true) {
      synchronized (monitor) {
        try {
          if (paused || finished) {
            prevError = Double.MAX_VALUE;
            heat = 1.0;
            residual = Double.MAX_VALUE;
            monitor.wait();
            numIterations = 0;
          }
          double e = step(prevError, heat);
          numIterations = numIterations + 1;
          if (debugOutput && debugOutWriter != null) {
            try {
              debugOutWriter.flush();
            } catch (IOException ex) {
              ex.printStackTrace();
            }
          }
          prevError = e;
          heat = heat + heatStep;
          heat = max(0.1, heat);
          if (!finished) {
            currentState = State.Working;
          }
          if (ui != null) {
            ui.modelChanged();
          }
          if (fps > 0) { // the framerate can change from the UI or from the user program.
            naptime = (long) (1000.0 / (double) fps);
          } else {
            naptime = 0;
          }
          Thread.sleep(naptime);
        } catch (InterruptedException ex) {
          ;
        }
      }
    }
  }

  //  private double calcTotalConstraintError() {
  //    double sum = 0;
  //    for (Constraint c : vars.constraints) {
  //      sum += abs(c.measureError());
  //    }
  //    return sum;
  //  }

  @SuppressWarnings("unchecked")
  private double step(double prevError, double heat) {
    if (debugOutput) {
      buf.setLength(0);
    }
    double totalError = 0;
    try {
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
      Constraint worst = null;
      double worstError = 0;
      for (Constraint c : vars.constraints) {
        c.clearMessages();
        if (heat > 0.6) {
          c.accumulateCorrection(heat);
        } else {
          double e = c.measureError();
          if (Math.abs(e) > Math.abs(worstError)) {
            worst = c;
            worstError = e;
          }
        }
        c.pushLastError();
      }
      for (Constraint c : vars.constraints) {
        if (debugOutput) {
          if (c == worst) {
            buf.append("[" + String.format(f + "] ", c.measureError()));
          } else {
            buf.append(String.format(f + " ", c.measureError()));
          }
        }
      }
      if (worst != null) {
        //        bug("Worst offender: " + worst);
        worst.accumulateCorrection(heat);
      }

      // 3: now all points have some accumulated correction. sum them and update the point's location.
      int numFinished = 0;
      for (Pt pt : vars.points) {
        List<Vec> corrections = (List<Vec>) pt.getAttribute(ACCUM_CORRECTION);
        pt.setBoolean("stable", corrections.size() == 0); // used by the UI
        if (corrections.size() == 0) {
          numFinished = numFinished + 1;
        }
        Vec delta = Vec.sum(corrections.toArray(new Vec[0]));
        double mag = delta.mag();
        totalError = totalError + mag;
        //        double r = random.nextDouble() * heat;
        //        double dampedMag = mag * r;
        double dampedMag = mag;
        // respects the shape of root function:
        if (dampedMag > 1.0) {
          pt.move(delta.getVectorOfMagnitude(sqrt(dampedMag)));
        } else if (dampedMag > 0.0) {
          pt.move(delta);
        }
      }
      residual = totalError;
      if (totalError < 0.0001 || numFinished == vars.points.size()) {
        finished = true;
        currentState = State.Solved;
      }
      fire();
    } catch (Exception ex) {
      // and they say I have a software engineering background.
    }
    if (debugOutput) {
      buf.insert(0, String.format(f + " ", totalError));
      buf.insert(0, (prevError < totalError ? "*WORSE* " : "better! "));
      buf.insert(0, String.format("%#.3f ", heat));
      if (finished) {
        buf.insert(0, "done ");
      }
      if (debugOutWriter == null) {
        System.out.println(buf.toString());
      } else {
        try {
          debugOutWriter.write(buf.toString() + "\n");
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
    return totalError;
  }

  public boolean hasPoints(Pt... pts) {
    boolean ret = true;
    for (Pt pt : pts) {
      if (!vars.points.contains(pt)) {
        ret = false;
        break;
      }
    }
    return ret;
  }

  public List<Pt> getPoints() {
    return vars.points;
  }

  public synchronized void addPoint(String name, Pt pt) {
    if (hasName(pt) && !getName(pt).equals(name)) {
      Debug.stacktrace("warning: do you really want to change the name of this point from "
          + getName(pt) + " to " + name + "?", 10);
    }
    setName(pt, name);
    addPoint(pt);
  }

  public synchronized void addPoint(Pt pt) {
    if (!vars.points.contains(pt)) {
      if (!hasName(pt)) {
        bug("warning: adding a point with no name");
      }
      //      Debug.stacktrace("made point " + pt.getString("name"), 8);
      vars.points.add(pt);
    }
    if (ui != null) {
      ui.modelChanged();
    }
  }

  public List<Constraint> getConstraints() {
    return vars.constraints;
  }

  public VariableBank getVars() {
    return vars;
  }

  public void addConstraint(Constraint c) {
    if (!vars.constraints.contains(c)) {
      vars.constraints.add(c);
      if (ui != null) {
        ui.modelChanged();
      }
    }
  }

  public void removeConstraint(Constraint c) {
    vars.constraints.remove(c);
    if (ui != null) {
      ui.modelChanged();
    }
  }

  public void wakeUp() {
    synchronized (monitor) {
      finished = false;
      currentState = State.Working;
      monitor.notify();
    }
  }

  public State getSolutionState() {
    return currentState;
  }

  public Set<Constraint> removePoint(Pt doomed) {
    vars.points.remove(doomed);
    Set<Constraint> doomedConstraints = new HashSet<Constraint>();
    for (Constraint c : vars.constraints) {
      if (c.involves(doomed)) {
        doomedConstraints.add(c);
      }
    }
    vars.constraints.removeAll(doomedConstraints);
    wakeUp();
    return doomedConstraints;
  }

  public void replacePoint(Pt oldPt, Pt newPt) {
    vars.points.remove(oldPt);
    addPoint(newPt);
    if (!hasName(newPt)) {
      Debug.stacktrace("point has no name", 6);
    }
    for (Constraint c : vars.constraints) {
      if (c.involves(oldPt)) {
        c.replace(oldPt, newPt);
      }
    }
    wakeUp();
  }

  public void replacePoint(Pt oldPt, String name, Pt newPt) {
    setName(newPt, name);
    replacePoint(oldPt, newPt);
  }

  public void clearConstraints() {
    vars.clear();
  }

  /**
   * Sets the name of the point, even if it is not involved in the solver's variables or
   * constraints.
   */
  public static void setName(Pt pt, String name) {
    pt.setString("name", name);
  }

  public static String getName(Pt pt) {
    return pt.getString("name");
  }

  public static boolean hasName(Pt pt) {
    return pt.hasAttribute("name");
  }

  public void setPaused(boolean v) {
    paused = v;
  }

  public boolean isPaused() {
    return paused;
  }
}