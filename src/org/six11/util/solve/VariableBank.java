package org.six11.util.solve;

import java.util.ArrayList;
import java.util.List;

import org.six11.util.pen.Pt;

public class VariableBank {

  List<Pt> points;
  List<Constraint> constraints;
  
  public VariableBank() {
    points = new ArrayList<Pt>();
    constraints = new ArrayList<Constraint>();
  }

  public void clear() {
    points.clear();
    constraints.clear();
  }
  
  public Pt getPointWithName(String n) {
    Pt ret = null;
    for (Pt pt : points) {
      if (pt.getString("name").equals(n)) {
        ret = pt;
        break;
      }
    }
    return ret;
  }
}
