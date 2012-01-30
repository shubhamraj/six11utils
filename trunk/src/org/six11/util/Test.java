// $Id$

package org.six11.util;

import static java.lang.System.out;

public class Test {

  public static void main(String[] in) {
    out.println("Modulus test");
    int n = 5;
    for (int i=-10; i < n + 10; i++) {
      out.println(String.format("%d @ %d = %d", i, n, wrapMod(i, n)));
    }
  }
  
  public static int wrapMod(int i, int n) {
    int res = i % n;
    if (res < 0) {
      res = n + res;
    }
    return res;
  }

}
