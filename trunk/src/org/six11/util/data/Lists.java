package org.six11.util.data;

import java.util.List;

/**
 * 
 **/
public abstract class Lists {

  public static Object getLast(List list) {
    return list.get(list.size() - 1);
  }
  
  public static void setLast(List list, Object obj) {
    if (list.size() == 0) {
      list.add(obj);
    } else {
      list.set(list.size() - 1, obj);
    }
  }

  public static boolean isLast(List list, Object obj) {
    return (list.size() > 0 && getLast(list).equals(obj));
  }

  /**
   * Same as a.containsAll(b) && b.containsAll(a);
   */
  public static boolean areListsSame(List a, List b) {
    return a.containsAll(b) && b.containsAll(a);
  }

}
