// $Id$

package org.six11.util.pen;

import java.util.Comparator;

import org.six11.util.Debug;

/**
 * Represents a 2D vector from one point to another. This gives you access to useful Vector stuff
 * like magnitude, and via the Functions class, other stuff like dot products.
 */
public class Vec {

  public static final int DIR_LEFT = 1;
  public static final int DIR_RIGHT = 2;

  private double x;
  private double y;

  public Vec(double xComponent, double yComponent) {
    this.x = xComponent;
    this.y = yComponent;
  }

  /**
   * Create a new vector whose location is b - a.
   */
  public Vec(Pt a, Pt b) {
    this(b.getX() - a.getX(), b.getY() - a.getY());
  }

  public Vec(Line line) {
    this(line.getEnd(), line.getStart());
  }

  public Vec(Pt source) {
    this(source.getX(), source.getY());
  }

  public Vec(Vec source) {
    this.x = source.x;
    this.y = source.y;
  }

  /**
   * Gives you a comparator to order vectors based on angle. A nonzero vector's angle can be
   * compared to a 'true north'. This angle can then be used to order vectors. Note that two angles
   * that are very close to one another but are on opposite sides of the true north will be treated
   * as being far apart, for this comparison.
   */
  public static Comparator<Vec> sortByAngle(final Vec trueNorth) {
    return new Comparator<Vec>() {
      public int compare(Vec a, Vec b) {
        int ret = 0;
        double angleA = Functions.getAngleBetween(trueNorth, a);
        double angleB = Functions.getAngleBetween(trueNorth, b);
        if (angleA < angleB) {
          ret = -1;
        } else if (angleA > angleB) {
          ret = 1;
        }
        return ret;
      }
    };
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public double mag() {
    return Math.sqrt(magSquared());
  }

  public double magSquared() {
    return (x * x) + (y * y);
  }

  /**
   * Returns the point that you get by moving away from 'pt' using this vector.
   */
  public Pt add(Pt pt) {
    return Functions.getEndPoint(pt, this);
  }

  public Vec getVectorOfMagnitude(double m) {
    return Functions.getVectorOfMagnitude(this, m);
  }

  public Vec getUnitVector() {
    return getVectorOfMagnitude(1d);
  }
  
  public Vec getScaled(double scale) {
    return new Vec(x * scale, y * scale);
  }

  public Vec getNormal() {
    return new Vec(y, -1.0 * x);
  }

  public Vec getFlip() {
    return new Vec(-x, -y);
  }

  public double dot(Vec other) {
    return getX() * other.getX() + getY() * other.getY();
  }
}
