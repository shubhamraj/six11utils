// $Id$

package org.six11.util.pen;

import java.awt.geom.Point2D;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;

import org.six11.util.Debug;

/**
 * My own special point object that does magic tricks, especially when paired with other Pt objects
 * in a Sequence. It helps me do calculations and provides a cleaner syntax than it's parent class.
 * I mean, seriously, I got sick of doing Point2D pt = new Point2D.Double(x, y) -- it's so ugly next
 * to Pt pt = new Pt(x, y);
 * 
 * I can also write on points because of the attributes map.
 **/
public class Pt extends Point2D.Double implements Comparable<Pt> {

  public static int ID_COUNTER = 0;

  protected long time;
  protected Map<String, Object> attribs;
  protected final int id = ID_COUNTER++;

  public Pt() {
    super();
    attribs = new HashMap<String, Object>();
  }

  public Pt(int x, int y) {
    this((double) x, (double) y);
  }

  public Pt(double x, double y) {
    this(x, y, 0);
  }

  public Pt(Vec direction) {
    this(direction.getX(), direction.getY());
  }

  public Pt(MouseEvent ev) {
    this(ev.getPoint().getX(), ev.getPoint().getY(), ev.getWhen());
  }

  public Pt(double x, double y, long time) {
    super(x, y);
    this.time = time;
    Debug.detectNaN(x);
    Debug.detectNaN(y); // TODO: remove these when done debugging. Feb 20 2010
    // attribs = new HashMap<String, Object>();
  }

  public Pt(Point2D source, long time) {
    this(source.getX(), source.getY(), time);
  }

  public int getID() {
    return id;
  }

  public Map<String, Object> getAttribs() {
    if (attribs == null) {
      attribs = new HashMap<String, Object>();
    }
    return attribs;
  }

  /**
   * Scale this point by the given amount.
   */
  public void scale(double amt) {
    setLocation(getX() * amt, getY() * amt);
  }

  public Pt getScaled(double amt) {
    return new Pt(getX() * amt, getY() * amt);
  }

  public int compareTo(Pt other) {
    if (getTime() < other.getTime())
      return -1;
    if (getTime() > other.getTime())
      return 1;
    return 0;
  }

  public boolean isSameLocation(Pt other) {
    return ((Math.abs(getX() - other.getX()) < Functions.EQ_TOL) && (Math
        .abs(getY() - other.getY()) < Functions.EQ_TOL));
  }

  public static Comparator<Pt> sortByX() {
    return new Comparator<Pt>() {
      public int compare(Pt a, Pt b) {
        int ret = 0;
        if (a.getX() < b.getX()) {
          ret = -1;
        } else if (a.getX() > b.getX()) {
          ret = 1;
        } else {
          // x values are the same. to ensure consistent ordering defer to the y value.
          if (a.getY() > b.getY()) {
            ret = 1;
          } else {
            ret = -1;
          }
        }
        return ret;
      }

      public boolean equals(Object obj) {
        return false;
      }
    };
  }

  public int ix() {
    return (int) getX();
  }

  public int iy() {
    return (int) getY();
  }

  public boolean equals(Pt other) {
    // I go through some pain to ensure that 'attribs' is not
    // initialized if it doesn't absolutely need to be.
    boolean basic = (other.compareTo(this) == 0 &&
    /*
     * it used to be this: other.getX() == getX() && other.getY() == getY()
     */
    Functions.eq(this, other, Functions.EQ_TOL));
    boolean advanced = basic ? getAttribs().equals(other.getAttribs()) : false;

    return basic && advanced;
  }
  
  public int hashCode() {
    // this is totally a guess
    int hash = super.hashCode() ^ ((Long) time).hashCode() ^ getAttribs().hashCode();
    return hash;
  }

  @SuppressWarnings("unchecked")
  public Pt copy() {
    Pt twin = new Pt(getX(), getY(), getTime());
    if (attribs == null) {
      twin.attribs = null;
    } else {
      twin.attribs = (HashMap<String, Object>) ((HashMap<String, Object>) attribs).clone();
    }
    return twin;
  }

  public final void setTime(long time) {
    this.time = time;
  }

  public long getTime() {
    return time;
  }

  public void setAttribute(String name, Object value) {
    getAttribs().put(name, value);
  }
  
  public void setBoolean(String name, boolean value) {
    getAttribs().put(name, value);
  }
  
  public boolean getBoolean(String name) {
    return (getAttribs().containsKey(name) && (Boolean) getAttribute(name));
  }

  public void setDouble(String name, double value) {
    // if (name.equals("selection strength")) {
    // Debug.out("Pt", "setting selection strength to " + value);
    // }
    getAttribs().put(name, value);
  }

  public void setString(String name, String value) {
    getAttribs().put(name, value);
  }

  public boolean hasAttribute(String name) {
    return getAttribs().containsKey(name);
  }

  public Object getAttribute(String name) {
    return getAttribs().get(name);
  }

  public void removeAttribute(String name) {
    getAttribs().remove(name);
  }

  public Vec getVec(String name) {
    return (Vec) getAttribute(name);
  }
  
  public void setVec(String name, Vec value) {
    setAttribute(name, value);
  }
  
  public double getDouble(String name) {
    Object shouldBeDouble = getAttribute(name);
    return ((java.lang.Double) shouldBeDouble).doubleValue();
  }

  public String getString(String name) {
    return (String) getAttribute(name);
  }

  public void setMap(String name, Map<?,?> value) {
    getAttribs().put(name, value);
  }

  public Map<?,?> getMap(String name) {
    return (Map<?,?>) getAttribute(name);
  }

  public void setList(String name, List<?> value) {
    getAttribs().put(name, value);
  }

  public List<?> getList(String name) {
    return (List<?>) getAttribute(name);
  }
  
  public void setSequence(String name, Sequence seq) {
    getAttribs().put(name, seq);
  }
  
  public Sequence getSequence(String name) {
    return (Sequence) getAttribute(name);
  }

}
