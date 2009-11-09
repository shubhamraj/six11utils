// $Id$

package org.six11.util.pen;

import junit.framework.TestCase;

/**
 * 
 **/
public class TestStroke extends TestCase {

  Stroke stroke;
  String initial = "initial";
  String mod1 = "modified 1";
  String mod2 = "modified 2";

  protected void setUp() {
    Sequence seq = new Sequence();
    for (int i=1; i <= 10; i++) seq.add(f(i)); // 1..10 inclusive
    stroke = new Stroke(initial, seq);
  }

  protected void tearDown() {
    stroke = null;
  }

  public void testGetSet() {
    Sequence xsquared = stroke.getSequence(initial);
    assertEquals(10, xsquared.size());
    Sequence xcubed = new Sequence();
    for (int i=1; i <= 5; i++) xcubed.add(g(i));
    Sequence shouldBeNull = stroke.getSequence(mod1);
    assertNull(shouldBeNull);
    stroke.setSequence(mod1, xcubed);
    Sequence shouldBeCubed = stroke.getSequence(mod1);
    assertEquals(5, shouldBeCubed.size());
  }

  private static Pt f(double x) {
    return new Pt(x, x * x);
  }

  private static Pt g(double x) {
    return new Pt(x, x * x * x);
  }
  
}
