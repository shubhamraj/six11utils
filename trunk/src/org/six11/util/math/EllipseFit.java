package org.six11.util.math;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static java.lang.Math.atan2;
import static java.lang.Math.sin;
import static java.lang.Math.cos;
import static java.lang.Math.abs;
import static java.lang.Math.signum;
import static java.lang.Math.sqrt;

import org.six11.util.Debug;
import org.six11.util.args.Arguments;
import org.six11.util.gui.BoundingBox;
import org.six11.util.io.FileUtil;
import org.six11.util.pen.Functions;
import org.six11.util.pen.Pt;
import org.six11.util.pen.RotatedEllipse;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * Use this to create an ellipse based on rough data. This implements the approach described in:
 * 
 * Andrew Fitzgibbon, Maurizio Pilu, Robert B. Fisher (1998). ``Direct least Square Fitting of
 * Ellipses''. IEEE Transactions on Pattern Analysis and Machine Intelligence
 */

public class EllipseFit {

  public static void main(String[] in) throws FileNotFoundException, IOException {
    Arguments args = new Arguments();
    args.parseArguments(in);
    Debug.useColor = args.hasFlag("debug-color");

    List<Pt> points = new ArrayList<Pt>();
    String dataString = "1 1 2 3 3 5 5 4";
    if (args.getPositionCount() == 1) {
      dataString = FileUtil.loadStringFromFile(args.getPosition(0));
    } else if (args.getPositionCount() > 1) {
      dataString = "";
      for (int i = 0; i < args.getPositionCount(); i++) {
        String s = args.getPosition(i);
        dataString = dataString + " " + s;
      }
    }

    StringTokenizer tok = new StringTokenizer(dataString, " \n");
    while (tok.hasMoreTokens()) {
      double x = Double.parseDouble(tok.nextToken());
      double y = Double.parseDouble(tok.nextToken());
      points.add(new Pt(x, y));
    }
//    bug("Fit ellipse to points:");
//    for (Pt pt : points) {
//      bug("\t" + Debug.num(pt));
//    }

    ellipseFit(points);
  }

  public static RotatedEllipse ellipseFit(List<Pt> points) {
//    bug("random_data:");
//    for (Pt pt : points) {
//      bug("  " + Debug.num(pt.getX()) + "  " + Debug.num(pt.getY()));
//    }
    Pt mean = Functions.getMean(points);
    BoundingBox bb = new BoundingBox(points);
    double sx = (bb.getMaxX() - bb.getMinX()) / 2.0;
    double sy = (bb.getMaxY() - bb.getMinY()) / 2.0;
    double mx = mean.getX();
    double my = mean.getY();
    double[] x = new double[points.size()];
    double[] y = new double[points.size()];
//    bug("Mean: " + Debug.num(mean));
//    bug("Max x: " + Debug.num(bb.getMaxX()));
//    bug("Min x: " + Debug.num(bb.getMinX()));
//    bug("Scale factors: x: " + Debug.num(sx) + " y: " + Debug.num(sy));
//    bug("normalized data:");
    for (int i = 0; i < points.size(); i++) {
      x[i] = (points.get(i).getX() - mean.getX()) / sx;
      y[i] = (points.get(i).getY() - mean.getY()) / sy;
//      bug("  " + Debug.num(x[i]) + ", " + Debug.num(y[i]));
    }
    double[][] designData = new double[points.size()][6];
    for (int i = 0; i < points.size(); i++) {// @formatter:off
      designData[i] = new double[] {
        x[i] * x[i],
        x[i] * y[i],
        y[i] * y[i],
        x[i],
        y[i],
        1.0
      }; // @formatter:on
    }
    Matrix design = new Matrix(designData);
    Matrix scatter = design.transpose().times(design);
    // @formatter:off
    double[][] cData = new double[][] {
      new double[] { 0.0, 0.0, -2.0, 0.0, 0.0, 0.0 },
      new double[] { 0.0, 1.0, 0.0, 0.0, 0.0, 0.0 },
      new double[] { -2.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
      new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
      new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 } ,
      new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 }
    };
    // @formatter:on
    Matrix c = new Matrix(cData);
    // The next part isn't in the original paper but is by the               tmpA = S(1:3,1:3); 
    // paper authors and they claim it is more numerically stable.           tmpB = S(1:3,4:6); 
    // That's cool but I use it only because the orignal version             tmpC = S(4:6,4:6); 
    // called for generalized eigenvalues, which I haven't found a           tmpD = C(1:3,1:3);
    // Java library for, and I'm sure as hell not going to make one          tmpE = inv(tmpC)*tmpB';
    // myself. So I'm glad they have this other way here.
    Matrix tmpA = scatter.getMatrix(0, 2, 0, 2);
    Matrix tmpB = scatter.getMatrix(0, 2, 3, 5);
    Matrix tmpC = scatter.getMatrix(3, 5, 3, 5);
    Matrix tmpD = c.getMatrix(0, 2, 0, 2);
    Matrix tmpE = tmpC.inverse().times(tmpB.transpose());

//    bugMat("Scatter:", scatter);
//    bugMat("tmpInvC:", tmpInvC);
//    bugMat("tmpA:", tmpA);
//    bugMat("tmpB:", tmpB);
//    bugMat("tmpC:", tmpC);
//    bugMat("tmpD:", tmpD);
//    bugMat("tmpE:", tmpE);

    // [evec_x, eval_x] = eig(inv(tmpD) * (tmpA - tmpB*tmpE));
    Matrix dInv = tmpD.inverse();
    Matrix be = tmpB.times(tmpE);
    Matrix aMinusBE = tmpA.minus(be);
    Matrix eigMeBaby = dInv.times(aMinusBE);
    EigenvalueDecomposition eigenstuff = eigMeBaby.eig();
    double[] evals = eigenstuff.getRealEigenvalues();
    int idxPos = -1;
    double SMALL_NUMBER = 0.00000001; 
    for (int i = 0; i < evals.length; i++) {
      if (evals[i] <= /*0*/ SMALL_NUMBER && !Double.isInfinite(evals[i])) {
        idxPos = i;
      }
//      bug(" -- Eigenvalue " + (i + 1) + ": " + Debug.num(evals[i], 4));
    }
//    bug("The positive eigenvalue is in position " + idxPos);
//    bugMat("Eigenvector matrix", eigenstuff.getV());
    if (idxPos < 0) {
      System.out.println("EllipseFit: idxPos negative. this will ruin your day");
      System.out.println("Eigenvalues are as follows:");
      for (int i=0; i < evals.length; i++) {
        System.out.println("  " + i + ": " + evals[i]);
      }
    }
    Matrix evecX = eigenstuff.getV().getMatrix(0, 2, idxPos, idxPos);
//    bugMat("The eigenvector we want", evecX);

    //    A = [A; evec_y];
    Matrix evecY = tmpE.uminus().times(evecX);
//    bugMat("evec_y", evecY);
    double[] d1 = evecX.getColumnPackedCopy();
    double[] d2 = evecY.getColumnPackedCopy();
    double[] a = new double[d1.length + d2.length];
    System.arraycopy(d1, 0, a, 0, d1.length);
    System.arraycopy(d2, 0, a, d1.length, d2.length);

    double[] par = {
        a[0] * sy * sy,
        a[1] * sx * sy,
        a[2] * sx * sx,
        -2 * a[0] * sy * sy * mx - a[1] * sx * sy * my + a[3] * sx * sy * sy,
        -a[1] * sx * sy * mx - 2 * a[2] * sx * sx * my + a[4] * sx * sx * sy,
        a[0] * sy * sy * mx * mx + a[1] * sx * sy * mx * my + a[2] * sx * sx * my * my - a[3] * sx
            * sy * sy * mx - a[4] * sx * sx * sy * my + a[5] * sx * sx * sy * sy
    };
    double thetaRadians = 0.5 * atan2(par[1], par[0] - par[2]);
    double cosineT = cos(thetaRadians);
    double sineT = sin(thetaRadians);
    double sineSquared = sineT * sineT;
    double cosineSquared = cosineT * cosineT;
    double cosineSine = sineT * cosineT;
    double ao = par[5];
//    bug("ao: " + Debug.num(ao, 4));
    double au = par[3] * cosineT + par[4] * sineT;
//    bug("au: " + Debug.num(au, 4));
    double av = -par[3] * sineT + par[4] * cosineT;
//    bug("av: " + Debug.num(av, 4));
    double auu = par[0] * cosineSquared + par[2] * sineSquared + par[1] * cosineSine;
//    bug("auu: " + Debug.num(auu, 4));
    double avv = par[0] * sineSquared + par[2] * cosineSquared - par[1] * cosineSine;
//    bug("avv: " + Debug.num(avv, 4));

    double tuCenter = -au / (2 * auu);
    double tvCenter = -av / (2 * avv);
    double wCenter = ao - auu * tuCenter * tuCenter - avv * tvCenter * tvCenter;
    double uCenter = tuCenter * cosineT - tvCenter * sineT;
    double vCenter = tuCenter * sineT + tvCenter * cosineT;
    double ru = -wCenter / auu;
    double rv = -wCenter / avv;
    ru = sqrt(abs(ru)) * signum(ru);
    rv = sqrt(abs(rv)) * signum(rv);

//    bug("wCenter: " + Debug.num(wCenter, 4));
//    bug("uCenter: " + Debug.num(uCenter, 4));
//    bug("vCenter: " + Debug.num(vCenter, 4));
//    bug("ru: " + Debug.num(ru, 4));
//    bug("rv: " + Debug.num(rv, 4));
//    bug("thetaRadians: " + Debug.num(thetaRadians, 4));
    // IMPORTANT NOTE: I introduced a unary minus to the thetaRadians variable that was not in the
    // original Matlab code. I suspect that Matlab and Java have opposite notions of up and down
    // and which way is 'positive' for angles. Negating the angle works in Java.
    RotatedEllipse ellipse = new RotatedEllipse(new Pt(uCenter, vCenter), ru, rv, -thetaRadians);
//    drawThing(points, ellipse, bb);
    return ellipse;
  }
//
//  private static void drawThing(final List<Pt> points, final RotatedEllipse ellipse, BoundingBox bb) {
//    ApplicationFrame af = new ApplicationFrame("EllipseFit Test");
//    final RotatedEllipse ellipse2 = ellipse.copy();
//    ellipse2.setRotation(-ellipse.getRotation());
//    af.setSize(800, 800);
//    af.center();
//    double tx = (bb.getMinX() < 0) ? -bb.getMinX() : 0;
//    double ty = (bb.getMinY() < 0) ? -bb.getMinY() : 0;
//    if (tx > 0 || ty > 0) {
//      for (Pt pt : points) {
//        pt.setLocation(pt.getX() + tx + 6, pt.getY() + ty + 6);
//      }
//      ellipse.translate(tx + 6, ty + 6);
//      ellipse2.translate(tx + 6, ty + 6);
//    }
//
//    JComponent drawMe = new JComponent() {
//      public void paintComponent(Graphics g1) {
//        Graphics2D g = (Graphics2D) g1;
//        g.setColor(Color.GRAY);
//        g.setStroke(new BasicStroke(0.8f));
//        Components.antialias(g);
//        for (Pt pt : points) {
//          g.draw(new Circle(pt.getX(), pt.getY(), 3));
//        }
//        g.setStroke(new BasicStroke(3.0f));
//        g.setColor(Color.BLUE);
//        g.draw(new ShapeFactory.RotatedEllipseShape(ellipse, 200));
//        g.setColor(Color.RED);
//        g.draw(new ShapeFactory.RotatedEllipseShape(ellipse2, 200));
//
//      }
//    };
//    af.setLayout(new BorderLayout());
//    af.add(BorderLayout.CENTER, drawMe);
//    af.setVisible(true);
//  }

//  private static void bugMat(String what, Matrix mat) {
//    bug(what);
//    mat.print(7, 4);
//  }
//
//  private static void bug(String what) {
//    //    Debug.out("EllipseFit", what);
//  }

}
