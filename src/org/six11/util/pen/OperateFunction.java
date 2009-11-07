package org.six11.util.pen;

public interface OperateFunction {
  public void beginOperation(Pt penLocation);
  public void operate(Sequence seq, Pt penLocation);
  public void endOperation();
}
