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
    Debug.useColor = false;
    Pt pt = new Pt(50.0, 60.0);
    bug("Point is " + num(pt) + " with hash code: " + pt.hashCode());
    GaussianHat hat = new GaussianHat(5, 3);
    for (int i = 0; i < 10; i++) {
      bug("Point is " + num(pt) + " with hash code: " + pt.hashCode());
      double randX = hat.getDouble();
      double randY = hat.getDouble();
      pt.move(new Vec(randX, randY));
    }
  }

}
