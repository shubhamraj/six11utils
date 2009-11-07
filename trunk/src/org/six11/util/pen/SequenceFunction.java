package org.six11.util.pen;

/**
 * A function that you can use on a sequence to transform points.
 **/
public abstract class SequenceFunction {
  
  public abstract Pt transform(Pt in);

  public static class Translate extends SequenceFunction {
    double dx, dy;
    
    public Translate(double dx, double dy) {
      this.dx = dx;
      this.dy = dy;
    }
    
    public Pt transform(Pt in) {
      return new Pt(in.getX() + dx, in.getY() + dy);
    }
  }
}
