package org.six11.util.solve;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import static java.lang.Math.toRadians;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.six11.util.gui.ApplicationFrame;
import org.six11.util.gui.Components;
import org.six11.util.pen.DrawingBuffer;
import org.six11.util.pen.DrawingBufferRoutines;
import org.six11.util.pen.Entropy;
import org.six11.util.pen.MouseThing;
import org.six11.util.pen.Pt;
import org.six11.util.pen.Vec;
import org.six11.util.solve.Manipulator.Param;

import static org.six11.util.solve.Constraint.setPinned;
import static org.six11.util.solve.Constraint.isPinned;
import static org.six11.util.Debug.bug;
import static org.six11.util.Debug.num;

// import static java.lang.Math.toDegrees;

public class TestSolveUI {

  ApplicationFrame af;
  DrawingBuffer buf;
  JComponent canvas;
  JDialog toolBox;
  JPanel editPane;
  Main main;
  Pt nearPt;
  Pt dragPt;
  Pt mousePt;
  Manipulator currentManipulator;
  ActionListener saveManipulatorAction;
  JDialog showAddPointsDialog;
  JTable table;
  MyTableModel tableModel;

  @SuppressWarnings("serial")
  public TestSolveUI(Main m) {
    this.main = m;
    af = new ApplicationFrame("Test Solve UI");
    af.setSize(800, 600);
    buf = new DrawingBuffer();
    toolBox = buildToolBox();
    canvas = new JComponent() {
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
        tableModel.fireTableDataChanged();
        canvas.repaint();
      }
    });
    af.setLayout(new BorderLayout());
    af.add(canvas, BorderLayout.CENTER);
    af.center();
    af.setVisible(true);
    toolBox.setVisible(true);
  }

  private JDialog buildToolBox() {
    JDialog ret = new JDialog(af, ModalityType.MODELESS);
    JPanel buttonPane = new JPanel();
    JButton addPointsButton = new JButton("Add Points...");
    addPointsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        showAddPointsDialog();
      }
    });
    buttonPane.add(addPointsButton);
    final JComboBox addConstraintBox = new JComboBox(createManipulators());
    addConstraintBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        setManipulator((Manipulator) addConstraintBox.getSelectedItem());
      }
    });

    buttonPane.add(addConstraintBox);
    table = new JTable(2, 0);
    tableModel = new MyTableModel();
    table.setModel(tableModel);
    JScrollPane tablePane = new JScrollPane(table);
    table.setFillsViewportHeight(true);
    saveManipulatorAction = new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        if (currentManipulator != null) {
          if (currentManipulator.isNew()) {
            currentManipulator = currentManipulator.makeInstance(main.vars);
            if (currentManipulator.isConstraint()) {
              main.vars.constraints.add(currentManipulator.getConstraint());
              bug("Added constraint to list.");
            }
          }
        }
      }
    };
    editPane = new JPanel();
    editPane.setLayout(new BorderLayout());
    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePane, editPane);
    ret.setLayout(new BorderLayout());
    ret.add(buttonPane, BorderLayout.NORTH);
    ret.add(splitPane, BorderLayout.CENTER);
    ret.setPreferredSize(new Dimension(300, 600));
    ret.pack();
    return ret;
  }

  protected void showAddPointsDialog() {
    if (showAddPointsDialog == null) {
      showAddPointsDialog = new JDialog(af, ModalityType.MODELESS);
      JPanel content = new JPanel();
      JLabel instructions = new JLabel("Point Name:");
      final JTextField ptName = new JTextField(6);
      ptName.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          String val = ptName.getText();
          Pt p = main.mkRandomPoint(canvas);
          main.addPoint(val, p);
          ptName.selectAll();
          canvas.repaint();
        }
      });
      content.add(instructions);
      content.add(ptName);
      showAddPointsDialog.add(content);
      showAddPointsDialog.pack();
      Components.centerComponent(showAddPointsDialog);
    }
    showAddPointsDialog.setVisible(true);
  }

  private void setManipulator(Manipulator manip) {
    this.currentManipulator = manip;
    editPane.removeAll();
    editPane.add(new JLabel("Editor for " + manip.label), BorderLayout.NORTH);
    JPanel paramBox = new JPanel();
    paramBox.setLayout(new GridLayout(0, 2));
    for (Manipulator.Param p : manip.params) {
      paramBox.add(new JLabel(p.helpText));
      JTextField textbox = makeAutosaveTextbox(p);
      textbox.addActionListener(saveManipulatorAction);
      paramBox.add(textbox);
    }
    JScrollPane paramScroller = new JScrollPane(paramBox,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    editPane.add(paramScroller, BorderLayout.CENTER);
    editPane.revalidate();
  }

  private JTextField makeAutosaveTextbox(final Param p) {
    final JTextField ret = new JTextField(6);
    ret.getDocument().addDocumentListener(new DocumentListener() {
      public void changedUpdate(DocumentEvent ev) {
        whack();
      }

      public void insertUpdate(DocumentEvent ev) {
        whack();
      }

      public void removeUpdate(DocumentEvent ev) {
        whack();
      }

      void whack() {
        p.value = ret.getText();
      }
    });
    return ret;
  }

  private Manipulator[] createManipulators() {
    List<Manipulator> men = new ArrayList<Manipulator>();
    men.add(DistanceConstraint.getManipulator());
    men.add(OrientationConstraint.getManipulator());
    men.add(PointOnLineConstraint.getManipulator());
    Manipulator[] ret = men.toArray(new Manipulator[men.size()]);
    return ret;
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
      Pt p1 = main.vars.getPointWithName(currentManipulator.getValue("pointA"));
      Pt p2 = main.vars.getPointWithName(currentManipulator.getValue("pointB"));
      NumericValue dist = new NumericValue(Double.parseDouble(currentManipulator.getValue("dist")));
      if (p1 != null && p2 != null) {
        main.addConstraint(new DistanceConstraint(p1, p2, dist));
      }
    } else if (label.equals(Manipulator.ADD_ANGLE)) {
      Pt p1 = main.vars.getPointWithName(currentManipulator.getValue("pointA"));
      Pt p2 = main.vars.getPointWithName(currentManipulator.getValue("pointB"));
      Pt pF = main.vars.getPointWithName(currentManipulator.getValue("fulcrum"));
      double angle = Double.parseDouble(currentManipulator.getValue("angle"));
      if (p1 != null && p2 != null && pF != null) {
        main.addConstraint(new AngleConstraint(p1, pF, p2, new NumericValue(toRadians(angle))));
      }
    } else if (label.equals(Manipulator.ADD_ORIENTATION)) {
      //  line1start line1end line2start line2end angle
      Pt pA1 = main.vars.getPointWithName(currentManipulator.getValue("line1start"));
      Pt pA2 = main.vars.getPointWithName(currentManipulator.getValue("line1end"));
      Pt pB1 = main.vars.getPointWithName(currentManipulator.getValue("line2start"));
      Pt pB2 = main.vars.getPointWithName(currentManipulator.getValue("line2end"));
      double angle = Double.parseDouble(currentManipulator.getValue("angle"));
      if (pA1 != null && pA2 != null && pB1 != null && pB2 != null) {
        main.addConstraint(new OrientationConstraint(pA1, pA2, pB1, pB2, new NumericValue(
            toRadians(angle))));
      }
    } else if (label.equals(Manipulator.ADD_POINT_AS_LINE_PARAM)) {
      Pt p1 = main.vars.getPointWithName(currentManipulator.getValue("pointA"));
      Pt p2 = main.vars.getPointWithName(currentManipulator.getValue("pointB"));
      NumericValue prop = new NumericValue(Double.parseDouble(currentManipulator
          .getValue("proportion")));
      Pt pT = main.vars.getPointWithName(currentManipulator.getValue("target"));
      if (p1 != null && p2 != null && pT != null) {
        main.addConstraint(new PointAsLineParamConstraint(p1, p2, prop, pT));
      }
    } else if (label.equals(Manipulator.ADD_POINT_ON_LINE)) {
      Pt p1 = main.vars.getPointWithName(currentManipulator.getValue("pointA"));
      Pt p2 = main.vars.getPointWithName(currentManipulator.getValue("pointB"));
      Pt pT = main.vars.getPointWithName(currentManipulator.getValue("target"));
      if (p1 != null && p2 != null && pT != null) {
        main.addConstraint(new PointOnLineConstraint(p1, p2, pT));
      }
    }
    canvas.repaint();
  }

  private Pt getPointWithName(String n) {
    Pt ret = null;
    for (Pt pt : main.vars.points) {
      if (pt.getString("name").equals(n)) {
        ret = pt;
        break;
      }
    }
    return ret;
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
  
  class MyTableModel extends AbstractTableModel {

    public int getColumnCount() {
      return 2;
    }

    public int getRowCount() {
      return main.vars.constraints.size();
    }

    public Object getValueAt(int row, int col) {
      Object ret = null;
      Constraint c = main.vars.constraints.get(row);
      if (col == 0) {
        ret = c.getType();
      } else if (col == 1) {
        ret = num(c.measureError());
      }
      return ret;
    }
  
  }

}
