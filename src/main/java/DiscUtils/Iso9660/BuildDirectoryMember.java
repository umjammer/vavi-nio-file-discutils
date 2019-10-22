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

package DiscUtils.Iso9660;

import java.nio.charset.Charset;
import java.util.Comparator;


/**
 * Provides the base class for
 * {@link BuildFileInfo}
 * and
 *
 * {@link BuildDirectoryInfo}
 * objects that will be built into an
 * ISO image.
 * Instances of this class have two names, a
 * {@link #__Name}
 * ,
 * which is the full-length Joliet name and a
 * {@link #__ShortName}
 * ,
 * which is the strictly compliant ISO 9660 name.
 */
public abstract class BuildDirectoryMember {
    public static final Comparator<BuildDirectoryMember> SortedComparison = new DirectorySortedComparison();

    /**
     * Initializes a new instance of the BuildDirectoryMember class.
     *
     * @param name The Joliet compliant name of the file or directory.
     * @param shortName The ISO 9660 compliant name of the file or directory.
     */
    protected BuildDirectoryMember(String name, String shortName) {
        __Name = name;
        __ShortName = shortName;
        setCreationTime(System.currentTimeMillis());
    }

    /**
     * Gets or sets the creation date for the file or directory, in UTC.
     */
    private long __CreationTime;

    public long getCreationTime() {
        return __CreationTime;
    }

    public void setCreationTime(long value) {
        __CreationTime = value;
    }

    /**
     * Gets the Joliet compliant name of the file or directory.
     */
    private String __Name;

    public String getName() {
        return __Name;
    }

    /**
     * Gets the parent directory, or
     * {@code null}
     * if this is the root directory.
     */
    public abstract BuildDirectoryInfo getParent();

    /**
     * Gets the ISO 9660 compliant name of the file or directory.
     */
    private String __ShortName;

    public String getShortName() {
        return __ShortName;
    }

    public String pickName(String nameOverride, Charset enc) {
        if (nameOverride != null) {
            return nameOverride;
        }

        return enc.equals(Charset.forName("ASCII")) ? getShortName() : getName();
    }

    public abstract long getDataSize(Charset enc);

    public int getDirectoryRecordSize(Charset enc) {
        return DirectoryRecord.calcLength(pickName(null, enc), enc);
    }

    private static class DirectorySortedComparison implements Comparator<BuildDirectoryMember> {
        public int compare(BuildDirectoryMember x, BuildDirectoryMember y) {
            String[] xParts = x.getName().split("\\.;");
            String[] yParts = y.getName().split("\\.;");
            String xPart;
            String yPart;
            for (int i = 0; i < 2; ++i) {
                xPart = xParts.length > i ? xParts[i] : "";
                yPart = yParts.length > i ? yParts[i] : "";
                int val = comparePart(xPart, yPart, ' ');
                if (val != 0) {
                    return val;
                }
            }
            xPart = xParts.length > 2 ? xParts[2] : "";
            yPart = yParts.length > 2 ? yParts[2] : "";
            return comparePartBackwards(xPart, yPart, '0');
        }

        private static int comparePart(String x, String y, char padChar) {
            int max = Math.max(x.length(), y.length());
            for (int i = 0; i < max; ++i) {
                char xChar = i < x.length() ? x.charAt(i) : padChar;
                char yChar = i < y.length() ? y.charAt(i) : padChar;
                if (xChar != yChar) {
                    return xChar - yChar;
                }
            }
            return 0;
        }

        private static int comparePartBackwards(String x, String y, char padChar) {
            int max = Math.max(x.length(), y.length());
            int xPad = max - x.length();
            int yPad = max - y.length();
            for (int i = 0; i < max; ++i) {
                char xChar = i >= xPad ? x.charAt(i - xPad) : padChar;
                char yChar = i >= yPad ? y.charAt(i - yPad) : padChar;
                if (xChar != yChar) {
                    return yChar - xChar;
                }
            }
            return 0;
        }
    }
}

// Note: Version numbers are in DESCENDING order!
