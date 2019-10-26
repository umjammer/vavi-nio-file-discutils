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

package DiscUtils.Core.Internal;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import DiscUtils.Core.UnixFileType;
import DiscUtils.Core.CoreCompat.FileAttributes;


public class Utilities {

    /**
     * Indicates if two ranges overlap. The type of the ordinals.
     *
     * @param xFirst The lowest ordinal of the first range (inclusive).
     * @param xLast The highest ordinal of the first range (exclusive).
     * @param yFirst The lowest ordinal of the second range (inclusive).
     * @param yLast The highest ordinal of the second range (exclusive).
     * @return {@code true} if the ranges overlap, else {@code false} .
     */
    public static <T extends Comparable<T>> boolean rangesOverlap(T xFirst, T xLast, T yFirst, T yLast) {
        return !((xLast.compareTo(yFirst) <= 0) || (xFirst.compareTo(yLast) >= 0));
    }

    public static boolean isAllZeros(byte[] buffer, int offset, int count) {
        int end = offset + count;
        for (int i = offset; i < end; ++i) {
            if (buffer[i] != 0) {
                return false;
            }

        }
        return true;
    }

    public static boolean isPowerOfTwo(int val) {
        if (val == 0) {
            return false;
        }

        while ((val & 1) != 1) {
            val >>>= 1;
        }
        return val == 1;
    }

    public static boolean isPowerOfTwo(long val) {
        if (val == 0) {
            return false;
        }

        while ((val & 1) != 1) {
            val >>>= 1;
        }
        return val == 1;
    }

    public static boolean areEqual(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        for (int i = 0; i < a.length; ++i) {
            if (a[i] != b[i]) {
                return false;
            }

        }
        return true;
    }

    public static short bitSwap(short value) {
        return (short) (((value & 0x00FF) << 8) | ((value & 0xFF00) >> 8));
    }

    public static int bitSwap(int value) {
        return ((value & 0xFF) << 24) | ((value & 0xFF00) << 8) | ((value & 0x00FF0000) >> 8) | ((value & 0xFF000000) >> 24);
    }

    public static long bitSwap(long value) {
        return ((long) bitSwap((int) (value & 0xFFFFFFFF)) << 32) | bitSwap((int) (value >> 32));
    }

    /**
     * Extracts the directory part of a path.
     *
     * @param path The path to process.
     * @return The directory part.
     */
    public static String getDirectoryFromPath(String path) {
        String trimmed = path.replaceFirst(escapeForRegex("\\*$"), "");
        int index = trimmed.lastIndexOf('\\');
        if (index < 0) {
            return ""; // No directory, just a file name
        }

        return trimmed.substring(0, index);
    }

    /**
     * Extracts the file part of a path.
     *
     * @param path The path to process.
     * @return The file part of the path.
     */
    public static String getFileFromPath(String path) {
        String trimmed = path.replaceFirst(escapeForRegex("\\*$"), "");
        int index = trimmed.lastIndexOf('\\');
        if (index < 0) {
            return trimmed;
        }

        return trimmed.substring(index + 1);
    }

    // No directory, just a file name
    /**
     * Combines two paths.
     *
     * @param a The first part of the path.
     * @param b The second part of the path.
     * @return The combined path.
     */
    public static String combinePaths(String a, String b) {
        if (Objects.isNull(a) || a.isEmpty() || (b.length() > 0 && b.charAt(0) == '\\')) {
            return b;
        }

        if (Objects.isNull(b) || b.isEmpty()) {
            return a;
        }

        return a.replaceFirst(escapeForRegex("\\*$"), "") + '\\' + b.replaceFirst(escapeForRegex("^\\*"), "");
    }

    /**
     * Resolves a relative path into an absolute one.
     *
     * @param basePath The base path to resolve from.
     * @param relativePath The relative path.
     * @return The absolute path. If no {@code basePath} is specified then
     *         relativePath is returned as-is. If {@code relativePath}
     *
     *         contains more '..' characters than the base path contains levels
     *         of
     *         directory, the resultant string be the root drive followed by the
     *         file name. If no the basePath starts with '\' (no drive
     *         specified)
     *         then the returned path will also start with '\'. For example:
     *         (\TEMP\Foo.txt, ..\..\Bar.txt) gives (\Bar.txt).
     */
    public static String resolveRelativePath(String basePath, String relativePath) {
        if (Objects.isNull(basePath) || basePath.isEmpty()) {
            return relativePath;
        }

        if (!basePath.endsWith("\\")) {
            Path parent = Paths.get(basePath).getParent();
            basePath = parent != null ? parent.toString() : "\\"; // TODO check
        }

        String merged = Paths.get(basePath, relativePath).toAbsolutePath().toString();

        if (basePath.startsWith("\\") && merged.length() > 2 && merged.charAt(1) == ':') {
            return merged.substring(2);
        }

        return merged;
    }

    public static String resolvePath(String basePath, String path) {
        if (!path.startsWith("\\")) {
            return resolveRelativePath(basePath, path);
        }

        return path;
    }

    public static String makeRelativePath(String path, String basePath) {
        List<String> pathElements = Arrays.stream(path.split(Utilities.escapeForRegex("\\")))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toList());
        List<String> basePathElements = Arrays.stream(basePath.split(Utilities.escapeForRegex("\\")))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toList());

        if (!basePath.endsWith("\\") && basePathElements.size() > 0) {
            basePathElements.remove(basePathElements.size() - 1);
        }

        // Find first part of paths that don't match
        int i = 0;
        while (i < Math.min(pathElements.size() - 1, basePathElements.size())) {
            if (!pathElements.get(i).toUpperCase().equals(basePathElements.get(i).toUpperCase())) {
                break;
            }

            ++i;
        }
        // For each remaining part of the base path, insert '..'
        StringBuilder result = new StringBuilder();
        if (i == basePathElements.size()) {
            result.append("." + '\\');
        } else if (i < basePathElements.size()) {
            for (int j = 0; j < basePathElements.size() - i; ++j) {
                result.append(".." + '\\');
            }
        }

        for (int j = i; j < pathElements.size() - 1; ++j) {
            // For each remaining part of the path, add the path element
            result.append(pathElements.get(j));
            result.append("\\");
        }
        result.append(pathElements.get(pathElements.size() - 1));
        // If the target was a directory, put the terminator back
        if (path.endsWith("\\")) {
            result.append("\\");
        }

        return result.toString();
    }

    /**
     * Indicates if a file name matches the 8.3 pattern.
     *
     * @param name The name to test.
     * @return {@code true} if the name is 8.3, otherwise {@code false} .
     */
    public static boolean is8Dot3(String name) {
        if (name.length() > 12) {
            return false;
        }

        String[] split = name.split("\\.");
        if (split.length > 2 || split.length < 1) {
            return false;
        }

        if (split[0].length() > 8) {
            return false;
        }

        for (Character ch : split[0].toCharArray()) {
            if (!is8Dot3Char(ch)) {
                return false;
            }
        }
        if (split.length > 1) {
            if (split[1].length() > 3) {
                return false;
            }

            for (char ch : split[1].toCharArray()) {
                if (!is8Dot3Char(ch)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean is8Dot3Char(char ch) {
        return (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || "_^$~!#%£-{}()@'`&".indexOf(ch) != -1;
    }

    /**
     * Converts a 'standard' wildcard file/path specification into a regular
     * expression.
     *
     * @param pattern The wildcard pattern to convert.
     * @return The resultant regular expression. The wildcard * (star) matches
     *         zero
     *         or more characters (including '.'), and ? (question mark) matches
     *         precisely one character (except '.').
     */
    public static Pattern convertWildcardsToRegEx(String pattern) {
        if (!pattern.contains(".")) {
            pattern += ".";
        }

        String query = "^" + pattern.replaceAll("\\*", ".*").replaceAll("\\?", "[^.]") + "$";
        return Pattern.compile(query, Pattern.CASE_INSENSITIVE);
    }

    public static EnumSet<FileAttributes> fileAttributesFromUnixFileType(UnixFileType fileType) {
        switch (fileType) {
        case Fifo:
            return EnumSet.of(FileAttributes.Device, FileAttributes.System);
        case Character:
            return EnumSet.of(FileAttributes.Device, FileAttributes.System);
        case Directory:
            return EnumSet.of(FileAttributes.Directory);
        case Block:
            return EnumSet.of(FileAttributes.Device, FileAttributes.System);
        case Regular:
            return EnumSet.of(FileAttributes.Normal);
        case Link:
            return EnumSet.of(FileAttributes.ReparsePoint);
        case Socket:
            return EnumSet.of(FileAttributes.Device, FileAttributes.System);
        default:
            return EnumSet.noneOf(FileAttributes.class);
        }
    }

    public static int getCombinedHashCode(Object... objs) {
        int result = 0;
        assert objs.length > 1;
        int hash = combineHashCode(toHashCode(objs[0]), toHashCode(objs[1]));
        for (int i = 2; i < objs.length; i++) {
            result = combineHashCode(hash, toHashCode(objs[i]));
            hash = result;
        }
        return result;
    }

    private static int toHashCode(Object o) {
        if (Integer.TYPE.isInstance(o)) {
            return Integer.TYPE.cast(o);
        } else if (Integer.class.isInstance(o)) {
            return Integer.class.cast(o);
        } else if (Byte.TYPE.isInstance(o)) {
            return Byte.hashCode(Byte.TYPE.cast(o));
        } else if (Byte.class.isInstance(o)) {
            return Byte.hashCode(Byte.class.cast(o));
        } else if (Character.TYPE.isInstance(o)) {
            return Character.hashCode(Character.TYPE.cast(o));
        } else if (Character.class.isInstance(o)) {
            return Character.hashCode(Character.class.cast(o));
        } else if (Short.TYPE.isInstance(o)) {
            return Short.hashCode(Short.TYPE.cast(o));
        } else if (Short.class.isInstance(o)) {
            return Short.hashCode(Short.class.cast(o));
        } else if (Long.TYPE.isInstance(o)) {
            return Long.hashCode(Long.TYPE.cast(o));
        } else if (Long.class.isInstance(o)) {
            return Long.hashCode(Long.class.cast(o));
        } else {
            return o.hashCode();
        }
    }

    private static int combineHashCode(int a, int b) {
        return 997 * a ^ 991 * b;
    }

    /** currently only '\' is replaced */
    public static String escapeForRegex(String separator) {
        return separator.replace("\\", "\\\\");
    }

    /**
     * @param s1 nullable
     * @param s2 nullable
     */
    public static int compareTo(String s1, String s2, boolean ignoreCase) {
        if (s1 == null) {
            if (s2 == null) {
                return 0;
            } else {
                return -1;
            }
        }
        if (s2 == null) {
            return 1;
        }
        if (ignoreCase) {
            return s1.compareToIgnoreCase(s2);
        } else {
            return s1.compareTo(s2);
        }
    }

    /**
     * @param obj1 nullable
     * @param obj2 nullable
     */
    public static boolean equals(Object obj1, Object obj2) {
        if (obj1 == null) {
            return obj2 == null;
        } else {
            return obj1.equals(obj2);
        }
    }
}
