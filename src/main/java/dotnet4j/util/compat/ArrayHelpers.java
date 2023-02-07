/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package dotnet4j.util.compat;


/**
 * ArrayHelpers.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-17 nsano initial version <br>
 */
public class ArrayHelpers {

    private ArrayHelpers() {}

    public static boolean isArrayNullOrEmpty(byte[] a) {
        return a == null || a.length == 0;
    }

    public static int[] reverse(int[] x) {
        for (int i = 0; i < x.length / 2; i++) {
            int r = x.length - 1 - i;
            int t = x[r];
            x[r] = x[i];
            x[i] = t;
        }
        return x;
    }

    public static byte[] reverse(byte[] x) {
        for (int i = 0; i < x.length / 2; i++) {
            int r = x.length - 1 - i;
            byte t = x[r];
            x[r] = x[i];
            x[i] = t;
        }
        return x;
    }

    public static long[] reverse(long[] x) {
        for (int i = 0; i < x.length / 2; i++) {
            int r = x.length - 1 - i;
            long t = x[r];
            x[r] = x[i];
            x[i] = t;
        }
        return x;
    }
}
