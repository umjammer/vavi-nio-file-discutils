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

package discUtils.core.internal;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import dotnet4j.util.compat.StringUtilities;


/**
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class Utilities {

    /**
     * Indicates if two ranges overlap. The type of the ordinals.
     *
     * @param xFirst The lowest ordinal of the first range (inclusive).
     * @param xLast The highest ordinal of the first range (exclusive).
     * @param yFirst The lowest ordinal of the second range (inclusive).
     * @param yLast The highest ordinal of the second range (exclusive).
     * @return {@code true} if the ranges overlap, else {@code false}.
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

    public static short bitSwap(short value) {
        return (short) (((value & 0x00FF) << 8) | ((value & 0xFF00) >> 8));
    }

    public static int bitSwap(int value) {
        return ((value & 0xFF) << 24) | ((value & 0xFF00) << 8) | ((value & 0x00FF0000) >> 8) | ((value & 0xFF000000) >> 24);
    }

    public static long bitSwap(long value) {
        return ((long) bitSwap((int) (value & 0xFFFFFFFFL)) << 32) | bitSwap((int) (value >> 32));
    }

    private static final String FS = File.separator;
    private static final char FSC = File.separatorChar;

    /**
     * Extracts the directory part of a path.
     *
     * @param path The path to process.
     * @return The directory part.
     */
    public static String getDirectoryFromPath(String path) {
        String trimmed = path.replaceFirst(StringUtilities.escapeForRegex(FS + "*$"), "");
        int index = trimmed.lastIndexOf(FSC);
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
        String trimmed = path.replaceFirst(StringUtilities.escapeForRegex(FS + "*$"), "");
        int index = trimmed.lastIndexOf(FSC);
        if (index < 0) {
            return trimmed; // No directory, just a file name
        }

        return trimmed.substring(index + 1);
    }

    /**
     * Combines two paths.
     *
     * @param a The first part of the path.
     * @param b The second part of the path.
     * @return The combined path.
     */
    public static String combinePaths(String a, String b) {
        if (Objects.isNull(a) || a.isEmpty() || (b.length() > 0 && b.charAt(0) == FSC)) {
            return b;
        }

        if (b.isEmpty()) {
            return a;
        }

        return a.replaceFirst(StringUtilities.escapeForRegex(FS + "*$"), "") + FSC +
               b.replaceFirst(StringUtilities.escapeForRegex("^" + FS + "*"), "");
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
     *         of directory, the resultant string be the root drive followed by
     *         the file name. If no the basePath starts with '\' (no drive
     *         specified) then the returned path will also start with '\'. For
     *         example: (\TEMP\Foo.txt, ..\..\Bar.txt) gives (\Bar.txt).
     */
    public static String resolveRelativePath(String basePath, String relativePath) {
        if (Objects.isNull(basePath) || basePath.isEmpty()) {
            return relativePath;
        }

        if (!basePath.endsWith(FS)) {
            basePath = getDirectoryFromPath(basePath);
        }

        String merged = Paths.get(combinePaths(basePath, relativePath).replace("\\", "/"))
                .normalize()
                .toString()
                .replace("/", FS);

        if (basePath.startsWith(FS) && merged.length() > 2 && merged.charAt(1) == ':') {
            return merged.substring(2);
        }

        return merged;
    }

    public static String resolvePath(String basePath, String path) {
        if (!path.startsWith(FS)) {
            return resolveRelativePath(basePath, path);
        }

        return path;
    }

    public static String makeRelativePath(String path, String basePath) {
        List<String> pathElements = Arrays.stream(path.split(StringUtilities.escapeForRegex(FS)))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toList());
        List<String> basePathElements = Arrays.stream(basePath.split(StringUtilities.escapeForRegex(FS)))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toList());

        if (!basePath.endsWith(FS) && basePathElements.size() > 0) {
            basePathElements.remove(basePathElements.size() - 1);
        }

        // Find first part of paths that don't match
        int i = 0;
        while (i < Math.min(pathElements.size() - 1, basePathElements.size())) {
            if (!pathElements.get(i).equalsIgnoreCase(basePathElements.get(i))) {
                break;
            }

            ++i;
        }
        // For each remaining part of the base path, insert '..'
        StringBuilder result = new StringBuilder();
        if (i == basePathElements.size()) {
            result.append(".").append(FSC);
        } else if (i < basePathElements.size()) {
            for (int j = 0; j < basePathElements.size() - i; ++j) {
                result.append("..").append(FSC);
            }
        }

        for (int j = i; j < pathElements.size() - 1; ++j) {
            // For each remaining part of the path, add the path element
            result.append(pathElements.get(j));
            result.append(FS);
        }
        result.append(pathElements.get(pathElements.size() - 1));
        // If the target was a directory, put the terminator back
        if (path.endsWith(FS)) {
            result.append(FS);
        }

        return result.toString();
    }

    /**
     * Indicates if a file name matches the 8.3 pattern.
     *
     * @param name The name to test.
     * @return {@code true} if the name is 8.3, otherwise {@code false}.
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
     *         zero or more characters (including '.'), and ? (question mark)
     *         matches precisely one character (except '.').
     */
    public static Pattern convertWildcardsToRegEx(String pattern) {
        if (!pattern.contains(".")) {
            pattern += ".";
        }
        pattern = pattern.replace(".", "\\.");

        String query = "^" + pattern.replaceAll("\\*", ".*").replaceAll("\\?", "[^.]") + "$";
        return Pattern.compile(query, Pattern.CASE_INSENSITIVE);
    }

    private static final Pattern envPattern = Pattern.compile("%(\\w+)%");

    // Environment#
    public static String expandEnvironmentVariables(String value) {
        Matcher matcher = envPattern.matcher(value);
        while (matcher.find()) {
            String envKey = matcher.group(1);
            value = value.replace(matcher.group(0), System.getenv(envKey));
        }
        return value;
    }
}
