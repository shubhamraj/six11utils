// $Id: BoundingBox.java 185 2011-11-04 20:18:32Z gabe.johnson@gmail.com $

package org.six11.util;

import static org.six11.util.Debug.bug;
import static org.six11.util.Debug.num;

import java.util.ArrayList;
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
    final ClusterThing<String> clusters = new ClusterThing<String>() {
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
    
    ClusterThing.ClusterFilter<String> filter = new ClusterThing.ClusterFilter<String>() {
      public boolean accepts(Cluster<String> cluster) {
        boolean ret = false;
        double max = clusters.query(cluster.getMax());
        double min = clusters.query(cluster.getMin());
        double ratio = max / min;
        ret = (ratio <= 2.0);
        return ret;
      }
    };
    List<Cluster<String>> groups = clusters.search(filter);
    bug("--");
    bug("Best groups:");
    int counter = 1;
    List<String> best = new ArrayList<String>();
    for (Cluster<String> group : groups) {
      if (group.getMembers().size() > 1) {
        bug("  Group " + counter++ + ": " + num(group.getMembers(), " "));
        best.addAll(group.getMembers());
      }
    }
  }

  public static void report(Cluster<String> cluster, int indent) {
    bug(Debug.spaces(indent) + " " + cluster.getCenter() + " (rank " + cluster.getRank()
        + " radius: " + num(cluster.getRadius()) + ", members: " + cluster.getMembers().size() + ")");
    if (cluster.getChildA() != null) {
      report(cluster.getChildA(), indent + 1);
    }
    if (cluster.getChildB() != null) {
      report(cluster.getChildB(), indent + 1);
    }
  }

}
