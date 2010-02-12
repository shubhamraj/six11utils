// $Id$

package org.six11.util.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 **/
public abstract class Lists {

  public static Object getLast(List<?> list) {
    return list.get(list.size() - 1);
  }

  public static void setLast(List<Object> list, Object obj) {
    if (list.size() == 0) {
      list.add(obj);
    } else {
      list.set(list.size() - 1, obj);
    }
  }

  public static boolean isLast(List<?> list, Object obj) {
    return (list.size() > 0 && getLast(list).equals(obj));
  }

  /**
   * Same as a.containsAll(b) && b.containsAll(a);
   */
  public static boolean areListsSame(List<?> a, List<?> b) {
    return a.containsAll(b) && b.containsAll(a);
  }

  /**
   * Computes out[i] = in[i] - in[i-1], where out[0] = 0. The return list is the same size as the
   * input list.
   */
  public static List<Double> getDeltaList(List<Double> in) {
    List<Double> ret = new ArrayList<Double>();
    ret.add(0.0);
    for (int i = 1; i < in.size(); i++) {
      ret.add(in.get(i) - in.get(i - 1));
    }
    return ret;
  }

}
