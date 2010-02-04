package org.six11.util.pen;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.six11.util.Debug;
import org.six11.util.gui.BoundingBox;
import org.six11.util.gui.Components;
import org.six11.util.gui.Strokes;
import org.six11.util.gui.shape.ShapeFactory;

/**
 * 
 * 
 * @author Gabe Johnson <johnsogg@cmu.edu>
 */
public class DrawingBuffer {

  private BufferedImage img;
  public Dimension defaultSize = new Dimension(400, 400);
  private boolean dirty;
  private boolean visible;

  private List<TurtleOp> turtles;
  private BoundingBox bb;

  public static PenState getBasicPen() {
    PenState BASIC_PENCIL = new PenState();
    BASIC_PENCIL.setColor(Color.BLACK);
    BASIC_PENCIL.setThickness(1.2f);
    BASIC_PENCIL.setDown(true);
    return BASIC_PENCIL;
  }

  /**
   * Creates a new drawing buffer. By default a drawing buffer is visible.
   */
  public DrawingBuffer() {
    img = new BufferedImage(defaultSize.width, defaultSize.height, BufferedImage.TYPE_INT_ARGB_PRE);
    this.turtles = new ArrayList<TurtleOp>();
    visible = true;
    dirty = true;
  }

  /**
   * Sets visibility to the given value (and sets the dirty bit if this changes the value).
   */
  public void setVisible(boolean v) {
    if (v != visible) {
      visible = v;
      dirty = true;
    }
  }

  /**
   * Tells you if this buffer is visible or not (by default it is).
   */
  public boolean isVisible() {
    return visible;
  }

  protected void addOp(TurtleOp op) {
    turtles.add(op);
    bb = null;
    dirty = true;
  }

  /**
   * If the 'dirty' flag is set, this completely resets the graphics buffer and draws the entire
   * turtle sequence. If the dirty flag isn't set, this does nothing of interest.
   */
  public void update() {
    if (dirty) {
      // The following block establishes the bounding box so the buffered image is sized correctly
      PenState pen = getBasicPen();
      bb = new BoundingBox();
      List<FilledRegion> regions = new ArrayList<FilledRegion>();
      AffineTransform xform = new AffineTransform();
      List<Object> pointsAndShapes = new ArrayList<Object>();
      for (TurtleOp turtle : turtles) {
        xform = turtle.go(xform, pen, bb, null, regions, pointsAndShapes);
      }
      img = new BufferedImage(bb.getWidthInt(), bb.getHeightInt(), BufferedImage.TYPE_INT_ARGB_PRE);
      Graphics2D g = img.createGraphics();
      g.setTransform(AffineTransform.getTranslateInstance(-bb.getX(), -bb.getY()));
      g.setClip(bb.getRectangleLoose());
      Components.antialias(g);
      drawToGraphics(g);
      dirty = false;
    }
  }

  /**
   * Sets the bounding box, and if the provided graphics object is not null, it draws the turtle
   * sequence and filled regions to the given graphics object.
   */
  public void drawToGraphics(Graphics2D g) {
    PenState pen = getBasicPen();
    List<FilledRegion> regions = new ArrayList<FilledRegion>();
    AffineTransform xform = new AffineTransform();
    List<Object> pointsAndShapes = new ArrayList<Object>();
    if (bb == null) {
      bb = new BoundingBox();
    }
    for (TurtleOp turtle : turtles) { // do a dry run to get the bounding box
      xform = turtle.go(xform, pen, bb, null, regions, pointsAndShapes);
    }
    for (FilledRegion region : regions) {
      GeneralPath path = new GeneralPath();
      path.append(region.getPathIterator(), true);
      if (g != null) {
        g.setColor(region.getColor());
        g.fill(path);
      }
    }
    xform = new AffineTransform();
    for (TurtleOp turtle : turtles) {
      xform = turtle.go(xform, pen, bb, g, regions, pointsAndShapes);
    }
  }

  @SuppressWarnings("unused")
  private static void bug(String what) {
    Debug.out("DrawingBuffer", what);
  }

  /**
   * Returns a cached Image object of this buffer so you don't have to re-draw everything all the
   * time. It will create an Image if necessary.
   */
  public Image getImage() {
    if (turtles.size() > 0 && dirty) {
      update();
    }
    return img;
  }

  /**
   * Gives the cached BoundingBox, but it doesn't check to ensure this value has been initialized.
   * To be safe, call drawToGraphics() once with a null input. That will initialize the bounding
   * box.
   */
  public BoundingBox getBoundingBox() {
    return bb;
  }

  /**
   * Pastes this buffer's image to the provided graphics context in the correct location.
   */
  public void paste(Graphics2D g) {
    AffineTransform before = new AffineTransform(g.getTransform());
    if (bb == null) {
      update();
    }
    g.translate(bb.getX(), bb.getY());
    g.drawImage(getImage(), 0, 0, null);
    g.setTransform(before);
  }

  public void forward(double d) {
    addOp(new TurtleOp(AffineTransform.getTranslateInstance(0.0, d)));
  }

  public void turn(double deg) {
    addOp(new TurtleOp(AffineTransform.getRotateInstance(Math.toRadians(deg))));
  }

  public void moveTo(double x, double y) {
    addOp(new TurtleOp(new Point2D.Double(x, y)));
  }

  public void circleTo(double startX, double startY, double midX, double midY, double endX,
      double endY) {
    addOp(new TurtleOp(new Pt(startX, startY), new Pt(midX, midY), new Pt(endX, endY)));
  }

  public void addShape(Shape arbitraryShape) {
    addOp(new TurtleOp(arbitraryShape));
  }

  public void addText(String what, Color color) {
    setFillColor(color);
    setFilling(true);
    addOp(new TurtleOp(what));
    setFilling(false);
  }

  public void up() {
    PenState p = new PenState();
    p.setDown(false);
    addOp(new TurtleOp(p));
  }

  public void down() {
    PenState p = new PenState();
    p.setDown(true);
    addOp(new TurtleOp(p));
  }

  public void setColor(double r, double g, double b, double a) {
    setColor(new Color((float) r, (float) g, (float) b, (float) a));
  }

  public void setColor(Color color) {
    PenState p = new PenState();
    p.setColor(color);
    addOp(new TurtleOp(p));
  }

  public void setThickness(double t) {
    PenState p = new PenState();
    p.setThickness((float) t);
    addOp(new TurtleOp(p));
  }

  public void setFillColor(double r, double g, double b, double a) {
    setFillColor(new Color((float) r, (float) g, (float) b, (float) a));
  }

  public void setFillColor(Color color) {
    PenState p = new PenState();
    p.setFillColor(color);
    addOp(new TurtleOp(p));
  }

  /**
   * Adds a turtle op that starts a filling operation. Make sure to call setFillColor BEFORE calling
   * this, because the setFillColor will be ignored otherwise.
   */
  public void setFilling(boolean f) {
    PenState p = new PenState();
    p.setFilling(f);
    addOp(new TurtleOp(p));
  }

  private static class TurtleOp {

    AffineTransform myTransform;
    PenState myPenState;
    Point2D myMoveTo;
    Point2D circleStart, circleMid, circleEnd;
    Shape arbitraryShape;
    String text;

    public TurtleOp() {
    }

    public TurtleOp(AffineTransform mine) {
      this();
      this.myTransform = mine;
    }

    /**
     * @param penState
     */
    public TurtleOp(PenState penState) {
      this();
      this.myPenState = penState;
    }

    /**
     * A turtle op for moving to a specific location regardless of what the current transform is.
     * 
     * @param tx
     *          A point indicating the x and y translation.
     */
    public TurtleOp(Point2D tx) {
      this();
      this.myMoveTo = tx;
    }

    public TurtleOp(Point2D start, Point2D mid, Point2D end) {
      this();
      this.circleStart = start;
      this.circleMid = mid;
      this.circleEnd = end;
    }

    public TurtleOp(Shape s) {
      this();
      this.arbitraryShape = s;
    }

    public TurtleOp(String s) {
      this();
      this.text = s;
    }

    public AffineTransform go(AffineTransform xform, PenState pen, BoundingBox bb, Graphics2D g,
        List<FilledRegion> regions, List<Object> pointsAndShapes) {
      AffineTransform change = xform;
      boolean linearMovement = false;
      boolean circularMovement = false;
      boolean shapeMovement = false;

      if (myPenState != null) {
        if (myPenState.changeFilling) {
          // off -> on = add a new FilledRegion
          if (myPenState.filling) {
            regions.add(new FilledRegion(pen.fillColor));
          }
        }
        if (myPenState.changeDown) {
          if (myPenState.down) {
            pointsAndShapes.clear();
          } else { // pen lifted up
            if (g != null && pointsAndShapes.size() > 0) {
              g.setColor(pen.color);
              g.setStroke(Strokes.get(pen.thickness));
              GeneralPath gp = new GeneralPath();
              boolean first = true;
              for (Object obj : pointsAndShapes) {
                if (obj instanceof Shape) {
                  gp.append((Shape) obj, true);
                } else {
                  Point2D pt = (Point2D.Double) obj;
                  if (first) {
                    gp.moveTo(pt.getX(), pt.getY());
                    first = false;
                  } else {
                    gp.lineTo(pt.getX(), pt.getY());
                  }
                }
              }
              g.draw(gp);
            }
          }
        }
        pen.incorporate(myPenState);
      } else if (myTransform != null && !myTransform.isIdentity()) {
        change = new AffineTransform(myTransform);
        change.preConcatenate(xform);
        linearMovement = true;
      } else if (myMoveTo != null) {
        change = AffineTransform.getTranslateInstance(myMoveTo.getX(), myMoveTo.getY());
        linearMovement = true;
      } else if (circleEnd != null && circleMid != null && circleStart != null) {
        change = AffineTransform.getTranslateInstance(circleEnd.getX(), circleEnd.getY());
        circularMovement = true;
      } else if (arbitraryShape != null) {
        change = new AffineTransform();
        shapeMovement = true;
      } else if (text != null) {
        change = new AffineTransform();
        Graphics2D playground = g;
        if (playground == null) {
          BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
          playground = (Graphics2D) image.getGraphics();
        }
        FontRenderContext frc = playground.getFontRenderContext();
        TextLayout tl = new TextLayout(text, new Font("Monospaced", Font.PLAIN, 9), frc);
        arbitraryShape = xform.createTransformedShape(tl.getOutline(null));
        shapeMovement = true;
      }

      if (linearMovement && pen.down) {
        double x1, y1, x2, y2;
        x1 = xform.getTranslateX();
        y1 = xform.getTranslateY();
        bb.add(new Point2D.Double(x1, y1), (double) pen.thickness);
        if (pen.filling) {
          regions.get(regions.size() - 1).addPoint(x1, y1);
        }
        x2 = change.getTranslateX();
        y2 = change.getTranslateY();
        bb.add(new Point2D.Double(x2, y2), (double) pen.thickness);
        if (pen.filling) {
          regions.get(regions.size() - 1).addPoint(x2, y2);
        }

        if (g != null && ((Math.abs(x2 - x1) > 0.0) || (Math.abs(y2 - y1) > 0.0))) {
          Point2D pt1 = new Point2D.Double(x1, y1);
          Point2D pt2 = new Point2D.Double(x2, y2);
          if (pointsAndShapes.size() == 0) {
            pointsAndShapes.add(pt1);
            pointsAndShapes.add(pt2);
          } else {
            if (pointsAndShapes.get(pointsAndShapes.size() - 1) instanceof Point2D) {
              Point2D prevPt = (Point2D) pointsAndShapes.get(pointsAndShapes.size() - 1);
              if (prevPt.distance(pt1) > 0.0) {
                pointsAndShapes.add(pt1);
              }
            }
            if (pointsAndShapes.get(pointsAndShapes.size() - 1) instanceof Point2D) {
              Point2D prevPt = (Point2D) pointsAndShapes.get(pointsAndShapes.size() - 1);
              if (prevPt.distance(pt2) > 0.0) {
                pointsAndShapes.add(pt2);
              }
            }
          }
        }
      }
      if ((circularMovement || shapeMovement) && pen.down) {
        Shape shape = null;
        if (circularMovement) {
          Pt s = new Pt(circleStart, 0);
          Pt mid = new Pt(circleMid, 0);
          Pt e = new Pt(circleEnd, 0);
          shape = ShapeFactory.makeArc(s, mid, e);
        } else { // shapeMovement!
          shape = arbitraryShape;
        }

        bb.add(shape.getBounds2D(), (double) pen.thickness);
        if (pen.filling) {
          regions.get(regions.size() - 1).addShape(shape);
        }
        if (g != null && text == null) {
          pointsAndShapes.add(shape);
        }
      }
      return change;
    }

    @SuppressWarnings("unused")
    private static void bug(String what) {
      Debug.out("TurtleOp", what);
    }
  }

  public static class PenState {

    boolean down;
    float thickness;
    Color color;
    boolean filling;
    Color fillColor;

    boolean changeDown;
    boolean changeThickness;
    boolean changeColor;
    boolean changeFilling;
    boolean changeFillColor;

    public PenState() {

    }

    public void incorporate(PenState target) {
      if (target.changeDown) {
        this.down = target.down;
      }
      if (target.changeThickness) {
        this.thickness = target.thickness;
      }
      if (target.changeColor) {
        this.color = target.color;
      }
      if (target.changeFilling) {
        this.filling = target.filling;
      }
      if (target.changeFillColor) {
        this.fillColor = target.fillColor;
        if (filling) {
          // this is an inventive way to 'fix' a bug.
          System.out.println("Warning: setFillColor called while filling=true. This is not "
              + "what you want. Use setFillColor *before* calling setFilling(true).");
        }
      }
    }

    public String toString() {
      return "down: " + down + (changeDown ? "* " : " ") + "thickness: " + thickness
          + (changeThickness ? "* " : " ") + "color: " + color + (changeColor ? "* " : " ")
          + "filling: " + filling + (changeFilling ? "* " : " ") + "fillColor: " + fillColor
          + (changeFillColor ? "* " : " ");
    }

    public void setDown(boolean v) {
      changeDown = true;
      down = v;
    }

    public void setThickness(float t) {
      changeThickness = true;
      thickness = t;
    }

    public void setColor(Color c) {
      changeColor = true;
      color = c;
    }

    public void setFillColor(Color c) {
      changeFillColor = true;
      fillColor = c;
    }

    public void setFilling(boolean f) {
      changeFilling = true;
      filling = f;
    }
  }

  private static class FilledRegion {

    private Color color;
    private List<Object> points;
    private BoundingBox bb;
    private boolean dirty;
    private PathIterator pathIterator;

    public FilledRegion(Color c) {
      this.color = c;
      this.points = new ArrayList<Object>(); // contains either Point2D or Shape objects
      bb = new BoundingBox();
    }

    /**
     * Adds a point to this filled region in order. It will silently fail to do this if the given
     * point is the same as the most recently added point.
     */
    public void addPoint(double x, double y) {
      if (points.size() > 0) {
        // ensure we didn't just add this point.
        if (points.get(points.size() - 1) instanceof Point2D) {
          Point2D prev = (Point2D) points.get(points.size() - 1);
          if (prev.getX() == x && prev.getY() == y) {
            return;
          }
        }
      }
      Point2D pt = new Point2D.Double(x, y);
      dirty = true;
      bb.add(pt);
      points.add(pt);
    }

    public void addShape(Shape shape) {
      Rectangle2D bounds = shape.getBounds2D();
      bb.add(bounds);
      points.add(shape);
      dirty = true;
    }

    public Color getColor() {
      return color;
    }

    @SuppressWarnings("unused")
    private static void bug(String what) {
      Debug.out("FilledRegion", what);
    }

    public PathIterator getPathIterator() {
      if (dirty || pathIterator == null) {
        GeneralPath gpi = new GeneralPath();
        boolean first = true;
        for (Object obj : points) {
          if (obj instanceof Point2D) {
            Point2D pt = (Point2D) obj;
            if (first) {
              gpi.moveTo(pt.getX(), pt.getY());
              first = false;
            } else {
              gpi.lineTo(pt.getX(), pt.getY());
            }
          } else if (obj instanceof Shape) {
            Shape shape = (Shape) obj;
            if (first) {
              gpi.append(shape, true);
            }
          }
        }
        pathIterator = gpi.getPathIterator(null);
        dirty = false;
      }
      return pathIterator;
    }
  }
}
