package org.six11.util.solve;

import static org.six11.util.Debug.num;
public class NumericValue {

  private double val;
  String variableName;

  public NumericValue(double val) {
    this.val = val;
    this.variableName = null;
  }
  
  public void setVariableName(String n) {
    this.variableName = n;
  }
  
  public String getVariableName() {
    return variableName;
  }
  
  public String toString() {
    String ret = variableName;
    if (ret == null) {
      ret = "" + num(val, 4);
    }
    return ret;
  }
  
  public double getValue() {
    return val;
  }
}
