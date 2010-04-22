// $Id$

package org.six11.util.pen;

import java.util.Map;
import java.util.HashMap;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;

/**
 * 
 **/
public class Stroke implements Shape {
  public final static String RAW = "raw";

  Map<String, Sequence> sequences;
  String defaultSequence = RAW;
  
  public Stroke(Sequence seq) {
    sequences = new HashMap<String, Sequence>();
    sequences.put(RAW, seq);
  }

  public Stroke(String name, Sequence seq) {
    sequences = new HashMap<String, Sequence>();
    sequences.put(name, seq);
  }

  public Sequence getSequence(String name) {
    return sequences.get(name);
  }

  public void setSequence(String name, Sequence seq) {
    sequences.put(name, seq);
  }

  public Sequence getDefault() {
    return getSequence(defaultSequence);
  }

  public void setDefault(String what) {
    this.defaultSequence = what;
  }

  public Rectangle getBounds() {
    return getDefault().getBounds();
  }

  public Rectangle2D getBounds2D() {
    return getDefault().getBounds2D();
  }

  public boolean contains(double x, double y) {
    return getDefault().contains(x, y);
  }

  public boolean contains(Point2D p) {
    return getDefault().contains(p);
  }

  public boolean intersects(double x, double y, double w, double h) {
    return getDefault().intersects(x, y, w, h);
  }

  public boolean intersects(Rectangle2D rec) {
    return getDefault().intersects(rec);
  }

  public boolean contains(double x, double y, double w, double h) {
    return getDefault().contains(x, y, w, h);
  }

  public boolean contains(Rectangle2D rec) {
    return getDefault().contains(rec);
  }

  public PathIterator getPathIterator(AffineTransform affine) {
    return getDefault().getPathIterator(affine);
  }

  public PathIterator getPathIterator(AffineTransform affine, double flatness) {
    return getDefault().getPathIterator(affine, flatness);
  }

//  /*
//   * @deprecated
//   */
//  public void draw(Graphics2D g) {
//    System.out.println("Stroke.draw deprecated");
////    getDefault().draw(g);
//  }

}
