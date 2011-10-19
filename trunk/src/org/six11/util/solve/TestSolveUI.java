package org.six11.util.solve;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.six11.util.gui.ApplicationFrame;
import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.DrawingBufferRoutines;
import org.six11.util.pen.MouseThing;
import org.six11.util.pen.Pt;
import org.six11.util.solve.Main.Demo;

import static org.six11.util.Debug.bug;
import static org.six11.util.Debug.num;
import static java.lang.Math.toDegrees;

public class TestSolveUI {

  ApplicationFrame af;
  DrawingBuffer buf;
  JComponent canvas;
  Main main;
  Pt nearPt;
  Pt dragPt;

  public TestSolveUI(Main m) {
    this.main = m;
    af = new ApplicationFrame("Test Solve UI");
    af.setSize(800, 600);
    buf = new DrawingBuffer();
    JList options = new JList(m.getDemos().toArray());
    options.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent ev) {
        bug("Demo list: ");
        int idx = 0;
        for (Demo d : main.getDemos()) {
          bug("  " + idx + ": "+ d);
          idx++;
        }
        bug("Invoking index " + ev.getFirstIndex() + ": " + main.getDemos().get(ev.getFirstIndex()));
        main.getDemos().get(ev.getFirstIndex()).go();
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
        if (nearPt != who) {
          nearPt = who;
          canvas.repaint();
        }
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
        dragPt = who;
        canvas.repaint();
      }

      public void mouseReleased(MouseEvent ev) {
        dragPt = null;
        main.run();
        canvas.repaint();
      }
    });
    af.setLayout(new BorderLayout());
    JPanel utils = new JPanel();
    utils.add(options);
    af.add(utils, BorderLayout.NORTH);
    af.add(canvas, BorderLayout.CENTER);
    af.center();
    af.setVisible(true);
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
    List<Constraint> constraints = main.getConstraints();
    List<Pt> points = main.getPoints();
    Pt msgCursor = new Pt(12, 12);
    for (Constraint c : constraints) {
      String msg = c.getMessages();
      if (msg.length() > 0) {
        DrawingBufferRoutines.text(buf, msgCursor, msg, Color.BLACK);
        msgCursor.setLocation(msgCursor.x, msgCursor.y + 20);
      }
      c.draw(buf);
    }

    for (Pt pt : points) {
      Color fillColor = Color.BLUE;
      if (pt.hasAttribute("stable") && pt.getBoolean("stable")) {
        fillColor = Color.LIGHT_GRAY;
      }
      DrawingBufferRoutines.text(buf, pt.getTranslated(0, -10), pt.getString("name"), Color.GREEN.darker());
      double radius = 5;
      if (pt == nearPt) {
        radius = 10;
      }
      DrawingBufferRoutines.dot(buf, pt, radius, 0.8, Color.BLACK, fillColor);
    }
  }

}
