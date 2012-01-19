// $Id$

package org.six11.util;

import static org.six11.util.Debug.bug;
import static org.six11.util.Debug.num;

import java.util.ArrayList;
import java.util.List;

import org.six11.util.data.GaussianHat;
import org.six11.util.math.ClusterThing;
import org.six11.util.math.ClusterThing.Cluster;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;

/**
 * 
 * 
 * @author Gabe Johnson <johnsogg@cmu.edu>
 */
public class Test {

  public static void main(String[] args) {
    Pt x = new Pt(1.234234234234234234234, Math.PI);
    bug("Normal: " + num(x));
    bug("Fancy 3: " + num(x, 3));
    bug("Fancy 7: " + num(x, 7));
    bug("Fancy 3: " + num(x, 3));
  }

}
