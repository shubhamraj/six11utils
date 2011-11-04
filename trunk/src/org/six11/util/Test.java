package org.six11.util;

import static org.six11.util.Debug.bug;
import static org.six11.util.Debug.num;

import java.util.List;

import org.six11.util.math.ClusterThing;
import org.six11.util.math.ClusterThing.Cluster;

/**
 * 
 * 
 * @author Gabe Johnson <johnsogg@cmu.edu>
 */
public class Test {

  public static void main(String[] args) {
    Debug.useColor = false;
    ClusterThing<String> clusters = new ClusterThing<String>() {
      public double query(String thing) {
        double ret = 0;
        try {
          ret = Double.parseDouble(thing);
        } catch (NumberFormatException ex) {
          ret = 0;
        }
        return ret;
      }
    };
    for (String v : args) {
      clusters.add(v);
    }
    clusters.computeClusters();
    double radius = clusters.getRadius();
    bug("Radius of whole thing: " + num(radius));
    Cluster<String> root = clusters.getRootCluster();
    report(root, 0);
  }
  
  public static void report(Cluster<String> cluster, int indent) {
    bug(Debug.spaces(indent) + " " + cluster.getCenter() + " (rank " + cluster.getRank() + ", radius: " + num(cluster.getRadius()) + ", members: " + cluster.getChildCount() + ")");
    if (cluster.getChildA() != null) {
      report(cluster.getChildA(), indent+1);
    }
    if (cluster.getChildB() != null) {
      report(cluster.getChildB(), indent+1);
    }
  }
  

}
