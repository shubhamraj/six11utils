package org.six11.util.solve;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import static org.six11.util.Debug.bug;

public class Manipulator {

  public static final String ADD_POINT = "Add Point";
  public static final String ADD_DISTANCE = "Add Distance Constraint";
  public static final String ADD_ANGLE = "Add Angle Constraint";
  public static final String ADD_ORIENTATION = "Add Orientation Constraint";
  public static final String ADD_POINT_ON_LINE = "Add Point-On-Line Constraint";

  String label;
  Param[] params;

  public Manipulator(String label, Param... params) {
    this.label = label;
    this.params = params;
  }

  public String toString() {
    return label;
  }

  public static class Param {

    boolean required;
    String helpText;
    String key;
    JTextField editBox;

    public Param(String key, String helpText, boolean required) {
      this.key = key;
      this.helpText = helpText;
      this.required = required;
      this.editBox = createEditBox();
    }

    private JTextField createEditBox() {
      final JTextField ret = new JTextField(12);
      ret.addFocusListener(new FocusListener() {
        public void focusGained(FocusEvent arg0) {
          ret.selectAll();
        }

        public void focusLost(FocusEvent arg0) {
        }

      });
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
          Runnable runner = new Runnable() {
            public void run() {
              if (ret.getText().length() == 0) {
                ret.setText(helpText);
              } else if (ret.getText().length() > helpText.length()
                  && ret.getText().startsWith(helpText)) {
                ret.setText(ret.getText().substring(helpText.length()));
              }
              if (ret.getText().equals(helpText)) {
                ret.setForeground(Color.LIGHT_GRAY);
              } else {
                ret.setForeground(Color.BLACK);
              }
            }
          };
          SwingUtilities.invokeLater(runner);
        }
      });
      ret.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          ret.selectAll();
        }        
      });
      ret.setText(helpText);
      ret.setForeground(Color.LIGHT_GRAY);
      ret.setFont(new Font("Dialog", required ? Font.BOLD : Font.PLAIN, 16));
      return ret;
    }
  }

  public String getValue(String key) {
    String ret = "Unknown";
    for (Param p : params) {
      if (p.key.equals(key)) {
        ret = p.editBox.getText();
        break;
      }
    }
    return ret;
  }

}
