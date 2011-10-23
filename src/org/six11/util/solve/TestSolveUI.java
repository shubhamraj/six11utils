package org.six11.util.solve;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import static java.lang.Math.toRadians;

import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.six11.util.gui.ApplicationFrame;
import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.DrawingBufferRoutines;
import org.six11.util.pen.Entropy;
import org.six11.util.pen.MouseThing;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;
import org.six11.util.solve.Main.Demo;
import static org.six11.util.solve.Constraint.setPinned;
import static org.six11.util.solve.Constraint.isPinned;
import static org.six11.util.Debug.bug;
import static org.six11.util.Debug.num;

// import static java.lang.Math.toDegrees;

public class TestSolveUI {

  ApplicationFrame af;
  DrawingBuffer buf;
  JComponent canvas;
  JPanel paramBox;
  JPanel utils;
  Main main;
  Pt nearPt;
  Pt dragPt;
  Pt mousePt;
  private Manipulator currentManipulator;

  @SuppressWarnings("serial")
  public TestSolveUI(Main m) {
    this.main = m;
    af = new ApplicationFrame("Test Solve UI");
    af.setSize(800, 600);
    buf = new DrawingBuffer();
    final JComboBox options = new JComboBox(m.getDemos().toArray());
    options.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        Demo item = (Demo) options.getSelectedItem();
        item.go();
      }
    });
    canvas = new JComponent() {
      @Override
      protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        g.setColor(Color.WHITE);
        g.fill(getVisibleRect());
        drawBuffer();
        buf.paste(g);
      }
    };
    canvas.addMouseMotionListener(new MouseThing() {
      public void mouseMoved(MouseEvent ev) {
        Pt who = findPoint(new Pt(ev));
        mousePt = new Pt(ev);
        if (nearPt != who) {
          nearPt = who;
        }
        canvas.repaint();
      }

      public void mouseDragged(MouseEvent ev) {
        if (dragPt != null) {
          dragPt.setLocation(ev.getX(), ev.getY());
          canvas.repaint();
        }
      }

    });
    canvas.addMouseListener(new MouseThing() {
      @Override
      public void mousePressed(MouseEvent ev) {
        Pt who = findPoint(new Pt(ev));
        if (mousePt == null) {
          mousePt = new Pt();
        }
        mousePt.setLocation(who);
        dragPt = who;
        canvas.repaint();
      }

      public void mouseClicked(MouseEvent ev) {
        Pt who = findPoint(new Pt(ev));
        setPinned(who, !isPinned(who)); // toggle
        canvas.repaint();
      }

      public void mouseReleased(MouseEvent ev) {
        dragPt = null;
        mousePt = null;
        main.run();
        canvas.repaint();
      }
    });
    af.setLayout(new BorderLayout());
    paramBox = new JPanel();
    paramBox.setLayout(new GridLayout(0, 1));
    utils = new JPanel();
    utils.setLayout(new GridLayout(0, 1));
    JButton go = new JButton("Go");
    ActionListener doYerThing = new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        activateManipulator();
      }
    };
    go.addActionListener(doYerThing);
    Manipulator[] manymani = makeManipulators();
    for (Manipulator man : manymani) {
      for (Manipulator.Param p : man.params) {
        p.editBox.addActionListener(doYerThing);
      }
    }
    final JComboBox manipulators = new JComboBox(manymani);
    manipulators.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        setManipulator((Manipulator) manipulators.getSelectedItem());
      }
    });
    utils.add(options);
    utils.add(manipulators);
    utils.add(paramBox);
    utils.add(go);
    af.add(utils, BorderLayout.EAST);
    af.add(canvas, BorderLayout.CENTER);
    af.center();
    af.setVisible(true);
  }

  protected void activateManipulator() {
    bug("activate manipulator: " + currentManipulator.label);
    String label = currentManipulator.label;
    if (label.equals(Manipulator.ADD_POINT)) {
      String name = currentManipulator.getValue("pointName");
      double x = 0;
      double y = 0;
      try {
        x = Double.parseDouble(currentManipulator.getValue("initialX"));
      } catch (NumberFormatException ex) {
        Entropy rand = Entropy.getEntropy();
        x = rand.getIntBetween(20, canvas.getWidth() - 20);
      }
      try {
        y = Double.parseDouble(currentManipulator.getValue("initialY"));
      } catch (NumberFormatException ex) {
        Entropy rand = Entropy.getEntropy();
        y = rand.getIntBetween(20, canvas.getHeight() - 20);
      }
      Pt loc = new Pt(x, y);
      bug("uh " + num(loc));
      main.addPoint(name, loc);
    } else if (label.equals(Manipulator.ADD_DISTANCE)) {
      Pt p1 = getPointWithName(currentManipulator.getValue("pointA"));
      Pt p2 = getPointWithName(currentManipulator.getValue("pointB"));
      double dist = Double.parseDouble(currentManipulator.getValue("dist"));
      if (p1 != null && p2 != null) {
        main.addConstraint(new DistanceConstraint(p1, p2, dist));
      }
    } else if (label.equals(Manipulator.ADD_ANGLE)) {
      Pt p1 = getPointWithName(currentManipulator.getValue("pointA"));
      Pt p2 = getPointWithName(currentManipulator.getValue("pointB"));
      Pt pF = getPointWithName(currentManipulator.getValue("fulcrum"));
      double angle = Double.parseDouble(currentManipulator.getValue("angle"));
      if (p1 != null && p2 != null && pF != null) {
        main.addConstraint(new AngleConstraint(p1, pF, p2, toRadians(angle)));
      }
    } else if (label.equals(Manipulator.ADD_ORIENTATION)) {
      //  line1start line1end line2start line2end angle
      Pt pA1 = getPointWithName(currentManipulator.getValue("line1start"));
      Pt pA2 = getPointWithName(currentManipulator.getValue("line1end"));
      Pt pB1 = getPointWithName(currentManipulator.getValue("line2start"));
      Pt pB2 = getPointWithName(currentManipulator.getValue("line2end"));
      double angle = Double.parseDouble(currentManipulator.getValue("angle"));
      if (pA1 != null && pA2 != null && pB1 != null && pB2 != null) {
        main.addConstraint(new OrientationConstraint(pA1, pA2, pB1, pB2, toRadians(angle)));
      }
    } else if (label.equals(Manipulator.ADD_POINT_ON_LINE)) {
      Pt p1 = getPointWithName(currentManipulator.getValue("pointA"));
      Pt p2 = getPointWithName(currentManipulator.getValue("pointB"));
      double prop = Double.parseDouble(currentManipulator.getValue("proportion"));
      Pt pT = getPointWithName(currentManipulator.getValue("target"));
      if (p1 != null && p2 != null && pT != null) {
        main.addConstraint(new PointOnLineConstraint(p1, p2, prop, pT));
      }
    }
    canvas.repaint();
  }

  private Pt getPointWithName(String n) {
    Pt ret = null;
    for (Pt pt : main.points) {
      if (pt.getString("name").equals(n)) {
        ret = pt;
        break;
      }
    }
    return ret;
  }

  protected void setManipulator(Manipulator man) {
    paramBox.removeAll();
    for (Manipulator.Param p : man.params) {
      paramBox.add(p.editBox);
    }
    paramBox.revalidate();
    this.currentManipulator = man;
    af.repaint();
  }

  private Manipulator[] makeManipulators() {
    List<Manipulator> out = new ArrayList<Manipulator>();
    // ------------------------------------------------------ add point
    out.add(new Manipulator(Manipulator.ADD_POINT, //
        new Manipulator.Param("pointName", "Point Name", true), //
        new Manipulator.Param("initialX", "Initial X Coordinate", false), //
        new Manipulator.Param("initialY", "Initial Y Coordinate", false)));
    // ------------------------------------------------------ distance constraint
    out.add(new Manipulator(Manipulator.ADD_DISTANCE, //
        new Manipulator.Param("pointA", "First Point Name", true), //
        new Manipulator.Param("pointB", "Second Point Name", true), //
        new Manipulator.Param("dist", "Distance", true)));
    // ------------------------------------------------------ angle constraint
    out.add(new Manipulator(Manipulator.ADD_ANGLE, //
        new Manipulator.Param("pointA", "First Point Name", true), //
        new Manipulator.Param("pointB", "Second Point Name", true), //
        new Manipulator.Param("fulcrum", "Fulcrum Point Name", true), //
        new Manipulator.Param("angle", "Angle (degrees)", true)));
    // ------------------------------------------------------ orientation constraint
    out.add(new Manipulator(Manipulator.ADD_ORIENTATION, //
        new Manipulator.Param("line1start", "Line A Start", true), //
        new Manipulator.Param("line1end", "Line A End", true), //
        new Manipulator.Param("line2start", "Line B Start", true), //
        new Manipulator.Param("line2end", "Line B End", true), //
        new Manipulator.Param("angle", "Angle (degrees)", true)));
    // ------------------------------------------------------ point on line constraint
    out.add(new Manipulator(Manipulator.ADD_POINT_ON_LINE, //
        new Manipulator.Param("pointA", "Line Start", true), //
        new Manipulator.Param("pointB", "Line End", true), //
        new Manipulator.Param("proportion", "Proportion (0..1)", true), //
        new Manipulator.Param("target", "Target (\"midpoint\")", true)));
    return out.toArray(new Manipulator[0]);
  }

  private Pt findPoint(Pt cursor) {
    double best = Double.MAX_VALUE;
    Pt ret = null;
    for (Pt pt : main.getPoints()) {
      double dist = pt.distance(cursor);
      if (dist < best) {
        best = dist;
        ret = pt;
      }
    }
    return ret;
  }

  private void drawBuffer() {
    buf.clear();
    // if there is a mouse point and a near point, it means the user has moved the mouse and 
    // a nearby point is activated. Draw an arrow between them.
    if (mousePt != null && nearPt != null && mousePt.distance(nearPt) > 40) {
      Vec mToN = new Vec(mousePt, nearPt).getUnitVector();
      DrawingBufferRoutines.arrow(buf, mousePt,
          mousePt.getTranslated(mToN, mousePt.distance(nearPt) - 20), 1.4, Color.magenta);
    }

    List<Constraint> constraints = main.getConstraints();
    List<Pt> points = main.getPoints();
    Pt msgCursor = new Pt(12, 12);
    if (main.msg != null && main.msg.length() > 0) {
      DrawingBufferRoutines.text(buf, msgCursor, main.msg, Color.BLACK);
      msgCursor.setLocation(msgCursor.x, msgCursor.y + 20);
    }
    for (Constraint c : constraints) {
      String msg = c.getMessages();
      if (msg.length() > 0) {
        DrawingBufferRoutines.text(buf, msgCursor, msg, Color.BLACK);
        msgCursor.setLocation(msgCursor.x, msgCursor.y + 20);
      }
      c.draw(buf);
    }
    if (main.finished) {
      DrawingBufferRoutines.text(buf, msgCursor, "Solved", Color.GREEN.darker().darker());
    } else {
      DrawingBufferRoutines.text(buf, msgCursor, "Working...", Color.RED.darker());
    }

    for (Pt pt : points) {
      Color fillColor = Color.BLUE;
      if (isPinned(pt)) {
        fillColor = Color.GREEN.darker();
      } else if (pt.hasAttribute("stable") && pt.getBoolean("stable")) {
        fillColor = Color.LIGHT_GRAY;
      }
      DrawingBufferRoutines.text(buf, pt.getTranslated(0, -10), pt.getString("name"),
          Color.GREEN.darker());
      double radius = 5;
      if (pt == nearPt) {
        radius = 10;
      }
      double borderThickness = 1.8;
      Color borderColor = Color.BLACK;
      if (isPinned(pt)) {
        borderThickness = 3.0;
        borderColor = Color.GREEN.darker().darker();
      }
      DrawingBufferRoutines.dot(buf, pt, radius, borderThickness, borderColor, fillColor);
    }
  }

}
