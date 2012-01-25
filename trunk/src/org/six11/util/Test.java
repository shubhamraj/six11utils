// $Id$

package org.six11.util;

import org.six11.util.args.Arguments;

public class Test {

  public static void main(String[] in) {
    Arguments args = new Arguments(in);
    System.out.println("Do we have the flag --foo? " + args.hasFlag("--foo"));
    System.out.println("How about just foo? " + args.hasFlag("foo"));
    System.out.println("What is the value of the foo flag? " + args.getValue("foo"));
    System.out.println("How many positional params are there? " + args.getPositionCount());
    System.out.println("What's in positional param 2? " + args.getPosition(2));
  }

}
