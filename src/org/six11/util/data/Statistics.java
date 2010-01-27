// $Id$

package org.six11.util.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.six11.util.Debug;

/**
 * 
 **/
public class Statistics {
  private boolean dirty = false;
  private List<Double> data;

  private double average;
  private double sum;
  private double variance;
  private double variation;
  private double min;
  private double max;
  private double median;

  public Statistics() {
    data = new ArrayList<Double>();
  }

  public void addData(double d) {
    data.add(d);
    dirty = true;
  }

  public double getMax() {
    calc();
    return max;
  }

  public double getMin() {
    calc();
    return min;
  }

  public int getN() {
    return data.size();
  }

  public double getMean() {
    calc();
    return average;
  }

  public double getSum() {
    calc();
    return sum;
  }

  public double getVariation() {
    calc();
    return variation;
  }

  public double getVariance() {
    calc();
    return variance;
  }
  
  public double getMedian() {
    calc();
    return median;
  }

  public double getStdDev() {
    calc();
    return Math.sqrt(variance);
  }

  public double getStdDevDistance(double x) {
    calc();
    return (x - getMean()) / getStdDev();
  }

  public void calc() {
    if (dirty) {
      double n = (double) data.size();
      max = Double.MIN_VALUE;
      min = Double.MAX_VALUE;
      sum = 0.0;
      variation = 0.0;
      for (double d : data) {
        sum += d;
        min = Math.min(d, min);
        max = Math.max(d, max);
      }
      average = sum / n;
      for (double d : data) {
        variation += Math.pow(d - average, 2.0);
      }
      variance = variation / n;
      
      // calculate median
      List<Double> copy = new ArrayList<Double>(data);
      Collections.sort(copy);
      if ((copy.size() % 2) == 0) {
        median = copy.get(copy.size() / 2);
      } else {
        double v1 = copy.get(copy.size() / 2);
        double v2 = copy.get((copy.size() / 2) + 1);
        median = (v1 + v2) / 2.0;
      } 
    }
    dirty = false;
  }

  @SuppressWarnings("unused")
  private static void bug(String what) {
    Debug.out("Statistics", what);
  }

  public static double minimum(double... vals) {
    double ret = Double.MAX_VALUE;
    for (double d : vals) {
      ret = Math.min(ret, d);
    }
    return ret;
  }

  public static double maximum(double... vals) {
    double ret = Double.MIN_VALUE;
    for (double d : vals) {
      ret = Math.max(ret, d);
    }
    return ret;
  }

  public static double mean(double... vals) {
    double ret = 0d;
    double count = 0d;
    for (double v : vals) {
      count += v;
    }
    if (vals.length > 0) {
      ret = count / vals.length;
    }
    return ret;
  }

}
