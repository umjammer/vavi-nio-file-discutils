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

import static DiscUtils.Core.IFileSystem.TOP_DIRECTORY_ONLY;


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
    public boolean exists() {
        try {
            return _fileSystem.directoryExists(_path);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
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
    public void create() {
        try {
            _fileSystem.createDirectory(_path);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Deletes a directory, even if it's not empty.
     */
    public void delete() {
        try {
            _fileSystem.deleteDirectory(_path, false);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Deletes a directory, with the caller choosing whether to recurse.
     *
     * @param recursive {@code true} to delete all child node, {@code false} to
     *            fail if the directory is not empty.
     */
    public void delete(boolean recursive) {
        try {
            _fileSystem.deleteDirectory(_path, recursive);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Moves a directory and it's contents to a new path.
     *
     * @param destinationDirName The destination directory name.
     */
    public void moveTo(String destinationDirName) {
        try {
            _fileSystem.moveDirectory(_path, destinationDirName);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets all child directories.
     *
     * @return An array of child directories.
     */
    public List<DiscDirectoryInfo> getDirectories() {
        try {
            return _fileSystem.getDirectories(_path)
                    .stream()
                    .map(p -> new DiscDirectoryInfo(_fileSystem, p))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets all child directories matching a search pattern.
     *
     * The search pattern can include the wildcards * (matching 0 or more
     * characters) and ? (matching 1 character).
     *
     * @param pattern The search pattern.
     * @return An array of child directories, or empty if none match.
     */
    public List<DiscDirectoryInfo> getDirectories(String pattern) {
        return getDirectories(pattern, TOP_DIRECTORY_ONLY);
    }

    /**
     * Gets all descendant directories matching a search pattern.
     *
     * The search pattern can include the wildcards * (matching 0 or more
     * characters) and ? (matching 1 character). The option parameter determines
     * whether only immediate children, or all children are returned.
     *
     * @param pattern The search pattern.
     * @param searchOption Whether to search just this directory, or all
     *            children.
     * @return An array of descendant directories, or empty if none match.
     */
    public List<DiscDirectoryInfo> getDirectories(String pattern, String searchOption) {
        try {
            return _fileSystem.getDirectories(_path, pattern, searchOption)
                    .stream()
                    .map(p -> new DiscDirectoryInfo(_fileSystem, p))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets all files.
     *
     * @return An array of files.
     */
    public List<DiscFileInfo> getFiles() {
        try {
            return _fileSystem.getFiles(_path)
                    .stream()
                    .map(p -> new DiscFileInfo(_fileSystem, p))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets all files matching a search pattern.
     *
     * The search pattern can include the wildcards * (matching 0 or more
     * characters) and ? (matching 1 character).
     *
     * @param pattern The search pattern.
     * @return An array of files, or empty if none match.
     */
    public List<DiscFileInfo> getFiles(String pattern) {
        return getFiles(pattern, TOP_DIRECTORY_ONLY);
    }

    /**
     * Gets all descendant files matching a search pattern.
     *
     * The search pattern can include the wildcards * (matching 0 or more
     * characters) and ? (matching 1 character). The option parameter determines
     * whether only immediate children, or all children are returned.
     *
     * @param pattern The search pattern.
     * @param searchOption Whether to search just this directory, or all
     *            children.
     * @return An array of descendant files, or empty if none match.
     */
    public List<DiscFileInfo> getFiles(String pattern, String searchOption) {
        try {
            return _fileSystem.getFiles(_path, pattern, searchOption)
                    .stream()
                    .map(p -> new DiscFileInfo(_fileSystem, p))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets all files and directories in this directory.
     *
     * @return An array of files and directories.
     */
    public List<DiscFileSystemInfo> getFileSystemInfos() {
        try {
            return _fileSystem.getFileSystemEntries(_path)
                    .stream()
                    .map(p -> new DiscFileSystemInfo(_fileSystem, p))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets all files and directories in this directory.
     *
     * The search pattern can include the wildcards * (matching 0 or more
     * characters) and ? (matching 1 character).
     *
     * @param pattern The search pattern.
     * @return An array of files and directories.
     */
    public List<DiscFileSystemInfo> getFileSystemInfos(String pattern) {
        try {
            return _fileSystem.getFileSystemEntries(_path, pattern)
                    .stream()
                    .map(p -> new DiscFileSystemInfo(_fileSystem, p))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

	@Override
	public String toString() {
		return String.format("DiscDirectoryInfo [getFullName()=%s, getDirectories()=%s, getFiles()=%s]", getFullName(),
				getDirectories(), getFiles());
	}
}
