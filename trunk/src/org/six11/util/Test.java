// $Id$

package org.six11.util;

import static java.lang.System.out;
import static org.six11.util.Debug.bug;
import static org.six11.util.Debug.num;

import org.six11.util.pen.Functions;
import org.six11.util.pen.Pt;

public class Test {

  public static void main(String[] in) {
    Pt a = new Pt(10, 20);
    Pt b = new Pt(20, 20);
    Pt c = new Pt(15, 20);
    Pt d = new Pt(15, 21);
    Pt e = new Pt(30, 30);
    
    out.println("Line defined by " + num(a) + " and " + num(b));
    out.println("Is " + num(c) + " on the line? " + Functions.isPointInLineSegment(c, a, b, 0.01));
    out.println("Is " + num(d) + " on the line? " + Functions.isPointInLineSegment(d, a, b, 0.01));
    out.println("Is " + num(e) + " on the line? " + Functions.isPointInLineSegment(e, a, b, 0.01));
    
  }

}
