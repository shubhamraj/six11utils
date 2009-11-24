// $Id$

package org.six11.util.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.six11.util.Debug;

/**
 * 
 * 
 * @author Gabe Johnson <johnsogg@cmu.edu>
 */
public class HttpUtil {

  public static void main(String[] args) {
    if (args.length > 2 && args[0].equals("post")) {
      System.out.println("Posting " + (args.length - 2) + " files to " + args[1]);
      HttpUtil util = new HttpUtil();
      for (int i = 2; i < args.length; i++) {
        try {
          byte[] bytes = FileUtil.getBytesFromFile(new File(args[i]));
          util.postBytes(args[1], bytes);
          System.out.println("Uploaded " + args[i]);
        } catch (IOException ex) {
          System.out.println("FAIL - " + args[i]);
          ex.printStackTrace();
        }
      }
    }
  }

  private void bug(String what) {
    Debug.out("HttpUtil", what);
  }

  public HttpURLConnection initGetConnection(String urlString) throws IOException {
    URL url = new URL(urlString);
    HttpURLConnection ht = (HttpURLConnection) url.openConnection();
    ht.setRequestMethod("GET");
    ht.setUseCaches(false);
    ht.setDoOutput(true);
    ht.setDoInput(true);
    ht.setRequestProperty("Connection", "Keep-Alive");
    return ht;
  }

  public HttpURLConnection initPostConnection(String urlString) throws IOException {
    URL url = new URL(urlString);
    HttpURLConnection ht = (HttpURLConnection) url.openConnection();
    ht.setRequestMethod("POST");
    ht.setUseCaches(false);
    ht.setDoOutput(true);
    ht.setDoInput(true);
    ht.setRequestProperty("Connection", "Keep-Alive");
    return ht;
  }

  public String downloadUrlToString(String fullFileName) throws IOException {
    String ret = "";
    HttpURLConnection con = initGetConnection(fullFileName);
    InputStream response = con.getInputStream();
    ret = StreamUtil.inputStreamToString(response);
    return ret;
  }

  public void postBytes(String urlString, byte[] bytes) throws IOException {
    bug("Posting " + bytes.length + " bytes to " + urlString + "...");
    HttpURLConnection con = initPostConnection(urlString);
    ByteArrayInputStream in = new ByteArrayInputStream(bytes);
    OutputStream out = con.getOutputStream();
    StreamUtil.writeInputStreamToOutputStream(in, out);
    out.flush();
    out.close();
    con.getInputStream();
    bug("Done posting " + bytes.length + " bytes to " + urlString + ".");
  }

}
