//
// Copyright (c) 2008-2011, Kenneth Bell
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.
//

package DiscUtils.Common;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Utilities {
    public static String[] wordWrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        int pos = 0;
        while (pos < text.length() - width) {
            int start = Math.min(pos + width, text.length() - 1);
//            int count = start - pos;
            int breakPos = text.lastIndexOf(' ', start/* , count */);
            lines.add(text.substring(pos, breakPos - pos).replaceFirst(" *$", ""));
            while (breakPos < text.length() && text.charAt(breakPos) == ' ') {
                breakPos++;
            }
            pos = breakPos;
        }
        lines.add(text.substring(pos));
        return lines.toArray(new String[0]);
    }

    public static String promptForPassword() {
        System.err.println();
        System.err.print("Password: ");

        String restoreColor = (char) 0x1b + "[" + 00 + "m";
        System.err.print((char) 0x1b + "[" + 37 + "m");
        try (Scanner s = new Scanner(System.in)) {
            return s.nextLine();
        } finally {
            System.err.print(restoreColor);
        }
    }

    public static String approximateDiskSize(long size) {
        if (size > 10 * (1024 * 1024L * 1024)) {
            return (size / (1024 * 1024 * 1024)) + " GiB";
        } else if (size > 10 * (1024 * 1024L)) {
            return (size / (1024 * 1024)) + " MiB";
        } else if (size > 10 * 1024) {
            return (size / 1024) + " KiB";
        } else {
            return size + " B";
        }
    }

    /**
     * @param value {@cs out}
     */
    public static boolean tryParseDiskSize(String size, long[] value) {
        char lastChar = size.charAt(size.length() - 1);
        try {
            if (Character.isDigit(lastChar)) {
                value[0] = Long.parseLong(size);
                return true;
            } else if (lastChar == 'B' && size.length() >= 2) {
                char unitChar = size.charAt(size.length() - 2);
                // suffix is 'B', indicating bytes
                if (Character.isDigit(unitChar)) {
                    value[0] = Long.parseLong(size.substring(0, size.length() - 1));
                    return true;
                }

                // suffix is KB, MB or GB
                long quantity = Long.parseLong(size.substring(0, size.length() - 2));

                switch (unitChar) {
                case 'K':
                    value[0] = quantity * 1024L;
                    return true;
                case 'M':
                    value[0] = quantity * 1024L * 1024L;
                    return true;
                case 'G':
                    value[0] = quantity * 1024L * 1024L * 1024L;
                    return true;
                default:
                    value[0] = 0;
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        value[0] = 0;
        return false;
    }
}
