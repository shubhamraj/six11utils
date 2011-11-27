package org.six11.util.solve;

import java.awt.Component;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.six11.util.Debug;
import org.six11.util.args.Arguments;
import org.six11.util.data.Statistics;
import org.six11.util.pen.Entropy;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;

import static org.six11.util.Debug.bug;
import static org.six11.util.Debug.num;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

public class ConstraintSolver {

  public static interface Listener {
    public void constraintStepDone();
  }

  public static enum State {
    Solved, Unsatisfied, Working;
  }

  public static final String ACCUM_CORRECTION = "accumulated correction";
  private static final int MAX_ITERATION_N = 100;
  private static final double MIN_ITERATION_VARIATION = 0.01;

  private List<Listener> stepListeners;
  private TestSolveUI ui = null;
  protected String msg = null;
  private boolean finished = false;
  protected int fps;
  protected VariableBank vars;
  private Object monitor;
  private State currentState;

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
    this.fps = 60;
    this.stepListeners = new ArrayList<Listener>();
    Entropy.setSeed(System.currentTimeMillis());
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
      lis.constraintStepDone();
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
    new Thread(runner).start();
  }

  void run() {
    bug("Entering run(). You should only see this one time!");
    finished = false;
    long naptime = 0;
    if (fps > 0) {
      naptime = (long) (1000.0 / (double) fps);
    } else {
      naptime = 0;
    }
    Statistics errorStats = new Statistics();
    errorStats.setMaximumN(MAX_ITERATION_N);
    double prevError = Double.MAX_VALUE;
    while (true) {
      synchronized (monitor) {
        try {
          if (finished) {
            errorStats.clear();
            prevError = Double.MAX_VALUE;
            monitor.wait();
          }
          double e = step();
          errorStats.addData(prevError - e);
          prevError = e;
          if (errorStats.getN() == MAX_ITERATION_N
              && errorStats.getVariance() < MIN_ITERATION_VARIATION) {
            bug("Can't find a stable solution, so I give up.");
            bug("        n: " + errorStats.getN());
            bug("  std_dev: " + errorStats.getStdDev());
            bug("  variate: " + errorStats.getVariance());
            bug("     mean: " + errorStats.getMean());
            bug("   median: " + errorStats.getMedian());
            bug("    error: " + calcTotalConstraintError());
            bug("   values: " + num(errorStats.getDataList(), " "));
            currentState = State.Unsatisfied;
            finished = true;
          }
          if (!finished) {
            currentState = State.Working;
          }
          if (ui != null) {
            ui.modelChanged();
          }
          if (fps > 0) { // the framerate can change
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

  private double calcTotalConstraintError() {
    double sum = 0;
    for (Constraint c : vars.constraints) {
      sum += abs(c.measureError());
    }
    return sum;
  }

  @SuppressWarnings("unchecked")
  private double step() {

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
        totalError = totalError + mag;
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
        currentState = State.Solved;
      }
      fire();
    } catch (Exception ex) {
      // and they say I have a software engineering background.
    }
    return totalError;
  }

  public List<Pt> getPoints() {
    return vars.points;
  }

  public synchronized void addPoint(String name, Pt pt) {
    if (!vars.points.contains(pt)) {
      pt.setAttribute("name", name);
      vars.points.add(pt);
      if (ui != null) {
        ui.modelChanged();
      }
    }
  }

  public List<Constraint> getConstraints() {
    return vars.constraints;
  }

  public VariableBank getVars() {
    return vars;
  }

  public void addConstraint(Constraint c) {
    vars.constraints.add(c);
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

  public void removePoint(Pt doomed) {
    vars.points.remove(doomed);
    Set<Constraint> doomedConstraints = new HashSet<Constraint>();
    for (Constraint c : vars.constraints) {
      if (c.involves(doomed)) {
        doomedConstraints.add(c);
      }
    }
    vars.constraints.removeAll(doomedConstraints);
    wakeUp();
  }

  public void replacePoint(Pt oldPt, String name, Pt newPt) {
    vars.points.remove(oldPt);
    addPoint(name, newPt);
    if (newPt.getString("name") == null) {
      Debug.stacktrace("point has no name", 6);
    }
    for (Constraint c : vars.constraints) {
      if (c.involves(oldPt)) {
        c.replace(oldPt, newPt);
      }
    }
    wakeUp();
  }

  public void clearConstraints() {
    vars.clear();
  }
}
