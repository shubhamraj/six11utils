package org.six11.util.solve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.six11.util.Debug;
import org.six11.util.pen.Pt;
import static org.six11.util.Debug.bug;
import static org.six11.util.Debug.num;

import org.json.*;

public class JsonIO {

  //  public static void main(String[] args) throws JSONException {
  //    Debug.useColor = false;
  //    JsonIO io = new JsonIO();
  //
  //    Pt a = new Pt(45.3934, 843.23, System.currentTimeMillis() - 238923);
  //    a.setString("name", "point A");
  //    a.setBoolean("pinned", false);
  //
  //    Pt b = new Pt(45.3934, 843.23, System.currentTimeMillis());
  //    b.setString("name", "point B");
  //    b.setBoolean("pinned", true);
  //    b.setDouble("ignoreMe", 734.39);
  //
  //    JSONObject aObj = io.write(a, "name", "pinned");
  //    bug("aObj:");
  //    bug("" + aObj);
  //    Pt aR = io.readPt(aObj, "pinned", "name");
  //    
  //    bug("Reconstructed: " + num(aR) + " named " + aR.getString("name") + " and pinned: " + aR.getBoolean("pinned"));
  //    
  //    List<Pt> ab = new ArrayList<Pt>();
  //    ab.add(a);
  //    ab.add(b);
  //
  //    JSONArray pointArray = io.write(ab, "name", "pinned");
  //    List<Pt> reconst = io.readPoints(pointArray, "pinned", "name");
  //    for (Pt p : reconst) {
  //      bug(num(p) + " name: " + p.getString("name") + ", pinned: " + p.getBoolean("pinned"));
  //    }
  //  }

  public JSONObject write(Pt pt, String... persistedData) throws JSONException {
    JSONObject ret = new JSONObject();
    ret.put("x", pt.getX());
    ret.put("y", pt.getY());
    ret.put("t", pt.getTime());
    Map<String, Object> persist = new HashMap<String, Object>();
    for (String key : persistedData) {
      if (pt.hasAttribute(key)) {
        persist.put(key, pt.getAttribute(key));
      }
    }
    ret.put("data", persist);
    return ret;
  }

  public JSONArray write(List<Pt> points, String... dataNames) throws JSONException {
    JSONArray ret = new JSONArray();
    for (Pt pt : points) {
      ret.put(write(pt, dataNames));
    }
    return ret;
  }

  public Pt readPt(JSONObject obj, String... dataNames) throws JSONException {

    double xLoc = (Double) obj.getDouble("x");
    double yLoc = (Double) obj.getDouble("y");
    long time = (Long) obj.getLong("t");
    Pt pt = new Pt(xLoc, yLoc, time);
    JSONObject data = (JSONObject) obj.get("data");
    for (String key : dataNames) {
      if (data.has(key)) {
        pt.setAttribute(key, data.get(key));
      }
    }
    return pt;
  }

  public List<Pt> readPoints(JSONArray array, String... dataNames) throws JSONException {
    List<Pt> points = new ArrayList<Pt>();
    for (int i = 0; i < array.length(); i++) {
      points.add(readPt((JSONObject) array.get(i), dataNames));
    }
    return points;
  }
}
