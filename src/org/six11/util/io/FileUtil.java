// $Id$

package org.six11.util.io;

import org.six11.util.Debug;

import java.util.List;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.io.*;

/**
 * 
 **/
public abstract class FileUtil {

  public static String loadStringFromFile(String fileName) throws FileNotFoundException,
      IOException {
    return loadStringFromFile(new File(fileName));
  }

  public static void writeStringToFile(File file, String contents, boolean append) {
    BufferedWriter out;
    try {
      out = new BufferedWriter(new FileWriter(file, append));
      out.write(contents);
      out.close();
    } catch (IOException ex) {
      ex.printStackTrace();
      bug("Continuing without re-throwing above exception.");
    }
  }

  public static void writeStringToFile(String fileName, String contents, boolean append) {
    writeStringToFile(new File(fileName), contents, append);
  }

  private static void bug(String what) {
    Debug.out("FileUtil", what);
  }

  /**
   * The following function was taken from
   * http://www.java-tips.org/java-se-tips/java.io/reading-a-file-into-a-byte-array.html.
   */
  public static byte[] getBytesFromFile(File file) throws IOException {
    InputStream is = new FileInputStream(file);
    long length = file.length();

    if (length > Integer.MAX_VALUE) {
      return new byte[0];
    }
    byte[] bytes = new byte[(int) length];
    int offset = 0;
    int numRead = 0;
    while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
      offset += numRead;
    }
    if (offset < bytes.length) {
      throw new IOException("Could not completely read file " + file.getName());
    }
    is.close();
    return bytes;
  }

  public static String loadStringFromFile(File f) {
    try {
      BufferedReader in = new BufferedReader(new FileReader(f));
      String line;
      StringBuffer allText = new StringBuffer();
      while (in.ready()) {
        line = in.readLine();
        allText.append(line + "\n");
      }
      return allText.toString();
    } catch (Exception ex) {
      if (!f.exists()) {
//        Debug.out("FileUtil", f.getName() + ": no such file");
        throw new RuntimeException(f.getName() + ": no such file");
      }
      if (!f.canRead()) {
//        Debug.out("FileUtil", f.getName() + ": can not read");
        throw new RuntimeException(f.getName() + ": can not read");
      }
      ex.printStackTrace();
      return "";
    }
  }

  /**
   * Create a JFileChooser in a given directory that accepts files of a certain type. 'dir' is a
   * File object, but it could be null (in which case we just open the default dir). If it is
   * non-null, it is either a file or a directory. In either case we will use the deepest directory
   * as the place to look. In other words, /home/billybob/mystuff/mything.txt and
   * /home/billybob/mystuff will both resolve the the mystuff directory.
   */
  public static JFileChooser makeFileChooser(File dir, String suffix, final String description) {
    JFileChooser ret = new JFileChooser(dir);
    final String suffixWithDot = suffix.startsWith(".") ? suffix : "." + suffix;
    FileFilter filter = new FileFilter() {
      public boolean accept(File f) {
        return (f != null && (f.getName().endsWith(suffixWithDot) || f.isDirectory()));
      }

      public String getDescription() {
        return description;
      }
    };
    ret.setFileFilter(filter);
    return ret;
  }

  public static class FileFinder {
    List<File> dirs;

    public FileFinder(List<File> dirs) {
      this.dirs = dirs;
      for (File d : dirs) {
        if (!d.exists() || !d.canRead()) {
//          Debug.out("FileUtil", "Can't read path element: " + d.getAbsolutePath());
          throw new RuntimeException(d.getAbsolutePath() + " not readable");
        }
      }
    }

    public File search(String fileName) {
      File ret = null;
      File f;
      for (File d : dirs) {
        f = new File(d, fileName);
        if (f.exists() && f.canRead()) {
          ret = f;
          break;
        }
      }
      return ret;
    }
  }

  public static String getPath(String file) {
    String p = new File(file).getParentFile().getPath();
    return p;
  }

}
