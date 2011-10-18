package org.six11.util.solve;

import java.util.List;

import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;

public abstract class Constraint {

  public abstract String getType();
  
  public abstract void accumulateCorrection();
  
  public abstract double measureError();

  public void accumulate(Pt pt, Vec correction) {
    List<Vec> corrections = (List<Vec>) pt.getAttribute(Main.ACCUM_CORRECTION);
    corrections.add(correction);
  }
}
