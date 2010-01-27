// $Id$

package org.six11.util.gui;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;

import org.six11.util.Debug;

/**
 * A handy class for determining the bounding box of 2D objects--just give it some points and it
 * will give you the bounding box. This class is only necessary because the Java Rectangle classes
 * include the origin in the rectangle by default, which isn't what we want at all.
 **/
public class BoundingBox {
  boolean hasX = false;
  double minX;
  double maxX;

  boolean hasY = false;
  double minY;
  double maxY;

  /**
   * Gives you the minimum X value.
   */
  public double getX() {
    if (!hasX)
      bug("There's no real x value yet!");
    return minX;
  }

  /**
   * The total width, calculated by the difference of the max and min X values.
   */
  public double getWidth() {
    if (!hasX)
      bug("There's no real x value yet!");
    return maxX - minX;
  }

  /**
   * Returns the integer form of the width, plus one. I am not sure why the +1 is necessary, but it
   * probably has something to do with rounding error.
   */
  public int getWidthInt() {
//    return (int) Math.ceil(getWidth()) + 1;
    return (int) Math.ceil(getWidth());
  }

  /**
   * Like getWidthInt, but for height.
   */
  public int getHeightInt() {
//    return (int) Math.ceil(getHeight()) + 1;    
    return (int) Math.ceil(getHeight());
  }

  /**
   * Gives you the minimum Y value.
   */
  public double getY() {
    if (!hasY)
      bug("There's no real y value yet!");
    return minY;
  }

  /**
   * The total height, calculated from the difference of the max and min Y values.
   */
  public double getHeight() {
    if (!hasY)
      bug("There's no real y value yet!");
    return maxY - minY;
  }

  /**
   * The distance from one corner to the opposite.
   */
  public double getDiagonal() {
    return Math.hypot(getWidth(), getHeight());
  }

  /**
   * Forces this bounding box to include the extents of the given bounding box.
   */
  public void add(BoundingBox other) {
    add(other.getRectangle());
  }

  /**
   * Forces the bounding box to include the extents of the given rectangle.
   */
  public void add(Rectangle2D rec) {
    add(new Point2D.Double(rec.getX(), rec.getY()));
    add(new Point2D.Double(rec.getX() + rec.getWidth(), rec.getY() + rec.getHeight()));
  }

  /**
   * Forces the bounding box to include points within the given padding distance of the provided
   * point (using manhattan block distance).
   */
  public void add(Point2D pt, double padding) {
    add(new Point2D.Double(pt.getX() + padding, pt.getY() + padding));
    add(new Point2D.Double(pt.getX() - padding, pt.getY() + padding));
    add(new Point2D.Double(pt.getX() + padding, pt.getY() - padding));
    add(new Point2D.Double(pt.getX() - padding, pt.getY() - padding));
  }

  /**
   * Forces the bounding box to include the following point.
   */
  public void add(Point2D pt) {
    if (!hasX) {
      maxX = pt.getX();
      minX = maxX;
      hasX = true;
    } else {
      maxX = Math.max(maxX, pt.getX());
      minX = Math.min(minX, pt.getX());
    }

    if (!hasY) {
      maxY = pt.getY();
      minY = maxY;
      hasY = true;
    } else {
      maxY = Math.max(maxY, pt.getY());
      minY = Math.min(minY, pt.getY());
    }
  }

  /**
   * Returns a rectangle that exactly covers the bounding box.
   */
  public Rectangle2D getRectangle() {
    return new Rectangle2D.Double(getX(), getY(), getWidth(), getHeight());
  }

  /**
   * Returns an integer-defined rectangle that is at least as big as the double form. This takes the
   * floor function for the X and Y coordinates, and uses getWidthInt/getHeightInt for the rectangle
   * sizes.
   */
  public Rectangle2D getRectangleLoose() {
    return new Rectangle2D.Double(Math.floor(getX()), Math.floor(getY()), getWidthInt(),
        getHeightInt());
  }

  public void bug(String what) {
    Debug.out("BoundingBox", what);
  }

}
