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

package discUtils.core;

import java.util.List;

import dotnet4j.security.accessControl.RawSecurityDescriptor;


/**
 * Provides the base class for all file systems that support Windows semantics.
 */
public interface IWindowsFileSystem extends IFileSystem {
    /**
     * Gets the security descriptor associated with the file or directory.
     *
     * @param path The file or directory to inspect.
     * @return The security descriptor.
     */
    RawSecurityDescriptor getSecurity(String path);

    /**
     * Sets the security descriptor associated with the file or directory.
     *
     * @param path The file or directory to change.
     * @param securityDescriptor The new security descriptor.
     */
    void setSecurity(String path, RawSecurityDescriptor securityDescriptor);

    /**
     * Gets the reparse point data associated with a file or directory.
     *
     * @param path The file to query.
     * @return The reparse point information.
     */
    ReparsePoint getReparsePoint(String path);

    /**
     * Sets the reparse point data on a file or directory.
     *
     * @param path The file to set the reparse point on.
     * @param reparsePoint The new reparse point.
     */
    void setReparsePoint(String path, ReparsePoint reparsePoint);

    /**
     * Removes a reparse point from a file or directory, without deleting the
     * file or directory.
     *
     * @param path The path to the file or directory to remove the reparse point
     *            from.
     */
    void removeReparsePoint(String path);

    /**
     * Gets the short name for a given path.
     *
     * @param path The path to convert.
     * @return The short name.
     *         This method only gets the short name for the final part of the
     *         path, to
     *         convert a complete path, call this method repeatedly, once for
     *         each path
     *         segment. If there is no short name for the given path,
     *         {@code null}
     *         is
     *         returned.
     */
    String getShortName(String path);

    /**
     * Sets the short name for a given file or directory.
     *
     * @param path The full path to the file or directory to change.
     * @param shortName The shortName, which should not include a path.
     */
    void setShortName(String path, String shortName);

    /**
     * Gets the standard file information for a file.
     *
     * @param path The full path to the file or directory to query.
     * @return The standard file information.
     */
    WindowsFileInformation getFileStandardInformation(String path);

    /**
     * Sets the standard file information for a file.
     *
     * @param path The full path to the file or directory to query.
     * @param info The standard file information.
     */
    void setFileStandardInformation(String path, WindowsFileInformation info);

    /**
     * Gets the names of the alternate data streams for a file.
     *
     * @param path The path to the file.
     * @return
     *         The list of alternate data streams (or empty, if none). To access
     *         the contents
     *         of the alternate streams, use OpenFile(path + ":" + name, ...).
     */
    List<String> getAlternateDataStreams(String path);

    /**
     * Gets the file id for a given path.
     *
     * @param path The path to get the id of.
     * @return The file id, or -1.
     *         The returned file id uniquely identifies the file, and is shared
     *         by all hard
     *         links to the same file. The value -1 indicates no unique
     *         identifier is
     *         available, and so it can be assumed the file has no hard links.
     */
    long getFileId(String path);

    /**
     * Indicates whether the file is known by other names.
     *
     * @param path The file to inspect.
     * @return
     *         {@code true}
     *         if the file has other names, else
     *         {@code false}
     *         .
     */
    boolean hasHardLinks(String path);
}
