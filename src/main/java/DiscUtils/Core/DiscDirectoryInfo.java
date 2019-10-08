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

package DiscUtils.Core;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Provides information about a directory on a disc.
 *
 * This class allows navigation of the disc directory/file hierarchy.
 */
public final class DiscDirectoryInfo extends DiscFileSystemInfo {
    /**
     * Initializes a new instance of the DiscDirectoryInfo class.
     *
     * @param fileSystem The file system the directory info relates to.
     * @param path The path within the file system of the directory.
     */
    public DiscDirectoryInfo(DiscFileSystem fileSystem, String path) {
        super(fileSystem, path);
    }

    /**
     * Gets a value indicating whether the directory exists.
     */
    public boolean getExists() throws IOException {
        return getFileSystem().directoryExists(getPath());
    }

    /**
     * Gets the full path of the directory.
     */
    public String getFullName() {
        return super.getFullName() + "\\";
    }

    /**
     * Creates a directory.
     */
    public void create() throws IOException {
        getFileSystem().createDirectory(getPath());
    }

    /**
     * Deletes a directory, even if it's not empty.
     */
    public void delete() throws IOException {
        getFileSystem().deleteDirectory(getPath(), false);
    }

    /**
     * Deletes a directory, with the caller choosing whether to recurse.
     *
     * @param recursive
     *            {@code true}
     *            to delete all child node,
     *            {@code false}
     *            to fail if the directory is not empty.
     */
    public void delete(boolean recursive) throws IOException {
        getFileSystem().deleteDirectory(getPath(), recursive);
    }

    /**
     * Moves a directory and it's contents to a new path.
     *
     * @param destinationDirName The destination directory name.
     */
    public void moveTo(String destinationDirName) throws IOException {
        getFileSystem().moveDirectory(getPath(), destinationDirName);
    }

    /**
     * Gets all child directories.
     *
     * @return An array of child directories.
     */
    public List<DiscDirectoryInfo> getDirectories() throws IOException {
        return getFileSystem().getDirectories(getPath()).stream().map(p -> {
            return new DiscDirectoryInfo(getFileSystem(), p);
        }).collect(Collectors.toList());
    }

    /**
     * Gets all child directories matching a search pattern.
     *
     * @param pattern The search pattern.
     * @return An array of child directories, or empty if none match.The search
     *         pattern can include the wildcards * (matching 0 or more
     *         characters)
     *         and ? (matching 1 character).
     */
    public List<DiscDirectoryInfo> getDirectories(String pattern) throws IOException {
        return getDirectories(pattern, "TopDirectoryOnly");
    }

    /**
     * Gets all descendant directories matching a search pattern.
     *
     * @param pattern The search pattern.
     * @param searchOption Whether to search just this directory, or all
     *            children.
     * @return An array of descendant directories, or empty if none match.The
     *         search pattern can include the wildcards * (matching 0 or more
     *         characters)
     *         and ? (matching 1 character). The option parameter determines
     *         whether only immediate
     *         children, or all children are returned.
     */
    public List<DiscDirectoryInfo> getDirectories(String pattern, String searchOption) throws IOException {
        return getFileSystem().getDirectories(getPath(), pattern, searchOption).stream().map(p -> {
            return new DiscDirectoryInfo(getFileSystem(), p);
        }).collect(Collectors.toList());
    }

    /**
     * Gets all files.
     *
     * @return An array of files.
     */
    public List<DiscDirectoryInfo> getFiles() throws IOException {
        return getFileSystem().getFiles(getPath()).stream().map(p -> {
            return new DiscDirectoryInfo(getFileSystem(), p);
        }).collect(Collectors.toList());
    }

    /**
     * Gets all files matching a search pattern.
     *
     * @param pattern The search pattern.
     * @return An array of files, or empty if none match.The search pattern can
     *         include the wildcards * (matching 0 or more characters)
     *         and ? (matching 1 character).
     */
    public List<DiscDirectoryInfo> getFiles(String pattern) throws IOException {
        return getFiles(pattern, "TopDirectoryOnly");
    }

    /**
     * Gets all descendant files matching a search pattern.
     *
     * @param pattern The search pattern.
     * @param searchOption Whether to search just this directory, or all
     *            children.
     * @return An array of descendant files, or empty if none match.The search
     *         pattern can include the wildcards * (matching 0 or more
     *         characters)
     *         and ? (matching 1 character). The option parameter determines
     *         whether only immediate
     *         children, or all children are returned.
     */
    public List<DiscDirectoryInfo> getFiles(String pattern, String searchOption) throws IOException {
        return getFileSystem().getFiles(getPath(), pattern, searchOption).stream().map(p -> {
            return new DiscDirectoryInfo(getFileSystem(), p);
        }).collect(Collectors.toList());
    }

    /**
     * Gets all files and directories in this directory.
     *
     * @return An array of files and directories.
     */
    public List<DiscDirectoryInfo> getFileSystemInfos() throws IOException {
        return getFileSystem().getFileSystemEntries(getPath()).stream().map(p -> {
            return new DiscDirectoryInfo(getFileSystem(), p);
        }).collect(Collectors.toList());
    }

    /**
     * Gets all files and directories in this directory.
     *
     * @param pattern The search pattern.
     * @return An array of files and directories.The search pattern can include
     *         the wildcards * (matching 0 or more characters)
     *         and ? (matching 1 character).
     */
    public List<DiscDirectoryInfo> getFileSystemInfos(String pattern) throws IOException {
        return getFileSystem().getFileSystemEntries(getPath(), pattern).stream().map(p -> {
            return new DiscDirectoryInfo(getFileSystem(), p);
        }).collect(Collectors.toList());
    }
}
