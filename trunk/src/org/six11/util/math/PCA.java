package org.six11.util.math;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

import org.six11.util.Debug;

/**
 * Support class for Principle Components Analysis. This does some light lifting, but the real work
 * is done in the Jama code.
 * 
 * @author Gabe Johnson <johnsogg@cmu.edu>
 */
public class PCA {

  public static void main(String[] args) throws IOException {
    File inFile = new File(args[0]);
    List<List<Double>> data = new ArrayList<List<Double>>();
    BufferedReader br = new BufferedReader(new FileReader(inFile));
    int numElements = -1;
    while (br.ready()) {
      StringTokenizer line = new StringTokenizer(br.readLine());
      List<Double> vec = new ArrayList<Double>();
      data.add(vec);
      while (line.hasMoreTokens()) {
        vec.add(Double.parseDouble(line.nextToken()));
      }
      numElements = vec.size();
    }
    double[][] matrix = new double[data.size()][numElements];
    for (int i = 0; i < data.size(); i++) {
      List<Double> vec = data.get(i);
      for (int j = 0; j < vec.size(); j++) {
        matrix[i][j] = vec.get(j);
      }
    }
    PCA pca = new PCA(matrix);
    int numComponents = pca.getNumComponents();
    bug("There are " + numComponents + " components");
    int k = 2; 
    List<PrincipleComponent> mainComponents = pca.getDominantComponents(k);
    int counter = 1;
    bug("Showing top " + k + " principle components.");
    for (PrincipleComponent pc : mainComponents) {
      bug("Component " + (counter++) + ": " + pc);
    }
  }

  Matrix covMatrix;
  EigenvalueDecomposition eigenstuff;
  double[] eigenvalues;
  Matrix eigenvectors;
  SortedSet<PrincipleComponent> principleComponents;

  public PCA(double[][] input) {
    double[][] cov = getCovariance(input);
    covMatrix = new Matrix(cov);
    eigenstuff = covMatrix.eig();
    eigenvalues = eigenstuff.getRealEigenvalues();
    bug("eigenvalues: " + Debug.num(eigenvalues));
    eigenvectors = eigenstuff.getV();
    bug("eigenvector matrix:");
    eigenvectors.print(6, 3);
    double[][] vecs = eigenvectors.getArray();
    int numComponents = eigenvectors.getColumnDimension(); // same as num rows.
    principleComponents = new TreeSet<PrincipleComponent>();
    for (int i = 0; i < numComponents; i++) {
      double[] eigenvector = new double[numComponents];
      for (int j = 0; j < numComponents; j++) {
        eigenvector[j] = vecs[i][j];
      }
      principleComponents.add(new PrincipleComponent(eigenvalues[i], eigenvector));
    }
  }

  public List<PrincipleComponent> getDominantComponents(int n) {
    List<PrincipleComponent> ret = new ArrayList<PrincipleComponent>();
    int count = 0;
    for (PrincipleComponent pc : principleComponents) {
      ret.add(pc);
      count++;
      if (count >= n) {
        break;
      }
    }
    return ret;
  }

  public int getNumComponents() {
    return eigenvalues.length;
  }

  public static class PrincipleComponent implements Comparable<PrincipleComponent> {
    double eigenValue;
    double[] eigenVector;

    public PrincipleComponent(double eigenValue, double[] eigenVector) {
      this.eigenValue = eigenValue;
      this.eigenVector = eigenVector;
    }

    public int compareTo(PrincipleComponent o) {
      int ret = 0;
      if (eigenValue > o.eigenValue) {
        ret = -1;
      } else if (eigenValue < o.eigenValue) {
        ret = 1;
      }
      return ret;
    }

    public String toString() {
      return "Principle Component, eigenvalue: " + Debug.num(eigenValue) + ", eigenvector: ["
          + Debug.num(eigenVector) + "]";
    }
  }

  public static double[][] getCovariance(double[][] input) {
    int numDataVectors = input.length;
    int n = input[0].length;

    double[] sum = new double[n];
    double[] mean = new double[n];
    for (int i = 0; i < numDataVectors; i++) {
      double[] vec = input[i];
      for (int j = 0; j < n; j++) {
        sum[j] = sum[j] + vec[j];
      }
    }
    for (int i = 0; i < sum.length; i++) {
      mean[i] = sum[i] / numDataVectors;
    }
    double[][] ret = new double[n][n];
    for (int i = 0; i < n; i++) {
      for (int j = i; j < n; j++) {
        double v = getCovariance(input, i, j, mean);
        ret[i][j] = v;
        ret[j][i] = v;
      }
    }
    return ret;
  }

  /**
   * Gives covariance between vectors in an n-dimensional space. The two input arrays store values
   * with the mean already subtracted. Read the code.
   */
  private static double getCovariance(double[][] matrix, int colA, int colB, double[] mean) {
    double sum = 0;
    for (int i = 0; i < matrix.length; i++) {
      double v1 = matrix[i][colA] - mean[colA];
      double v2 = matrix[i][colB] - mean[colB];
      sum = sum + (v1 * v2);
    }
    int n = matrix.length;
    double ret = (sum / (n - 1));
    return ret;
  }

  private static void bug(String what) {
    Debug.out("PCA", what);
  }

}
