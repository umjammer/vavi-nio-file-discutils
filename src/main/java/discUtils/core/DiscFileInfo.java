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

import java.io.IOException;
import java.util.EnumSet;

import discUtils.core.coreCompat.FileAttributes;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.Stream;
import dotnet4j.io.StreamReader;
import dotnet4j.io.StreamWriter;


/**
 * Provides information about a file on a disc.
 */
public final class DiscFileInfo extends DiscFileSystemInfo {

    public DiscFileInfo(DiscFileSystem fileSystem, String path) {
        super(fileSystem, path);
    }

    /**
     * Gets an instance of the parent directory.
     */
    public DiscDirectoryInfo getDirectory() {
        return getParent();
    }

    /**
     * Gets a string representing the directory's full path.
     */
    public String getDirectoryName() {
        return getDirectory().getFullName();
    }

    /**
     * Gets a value indicating whether the file exists.
     */
    @Override public boolean exists() {
        try {
            return fileSystem.fileExists(path);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets or sets a value indicating whether the file is read-only.
     */
    public boolean isReadOnly() {
        return getAttributes().contains(FileAttributes.ReadOnly);
    }

    public void setReadOnly(boolean value) {
        EnumSet<FileAttributes> atributes = getAttributes();
        if (value) {
            atributes.add(FileAttributes.ReadOnly);
        } else {
            atributes.remove(FileAttributes.ReadOnly);
        }
        setAttributes(atributes);
    }

    /**
     * Gets the length of the current file in bytes.
     */
    public long getLength() {
        try {
            return fileSystem.getFileLength(path);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Deletes a file.
     */
    @Override public void delete() {
        try {
            fileSystem.deleteFile(path);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Creates a {@link StreamWriter} that appends text to the file represented
     * by this {@link #DiscFileInfo} .
     *
     * @return The newly created writer.
     */
    public StreamWriter appendText() {
        return new StreamWriter(open(FileMode.Append));
    }

    /**
     * Copies an existing file to a new file.
     *
     * @param destinationFileName The destination file.
     */
    public void copyTo(String destinationFileName) {
        copyTo(destinationFileName, false);
    }

    /**
     * Copies an existing file to a new file, allowing overwriting of an
     * existing file.
     *
     * @param destinationFileName The destination file.
     * @param overwrite Whether to permit over-writing of an existing file.
     */
    public void copyTo(String destinationFileName, boolean overwrite) {
        try {
            fileSystem.copyFile(path, destinationFileName, overwrite);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Creates a new file for reading and writing.
     *
     * @return The newly created stream.
     */
    public Stream create() {
        return open(FileMode.Create);
    }

    /**
     * Creates a new {@link StreamWriter} that writes a new text file.
     *
     * @return A new stream writer that can write to the file contents.
     */
    public StreamWriter createText() {
        return new StreamWriter(open(FileMode.Create));
    }

    /**
     * Moves a file to a new location.
     *
     * @param destinationFileName The new name of the file.
     */
    public void moveTo(String destinationFileName) {
        try {
            fileSystem.moveFile(path, destinationFileName);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Opens the current file.
     *
     * @param mode The file mode for the created stream.
     * @return The newly created stream.read-only file systems only support
     *         {@code FileMode.Open} .
     */
    public Stream open(FileMode mode) {
        try {
            return fileSystem.openFile(path, mode);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Opens the current file.
     *
     * @param mode The file mode for the created stream.
     * @param access The access permissions for the created stream.
     * @return The newly created stream.read-only file systems only support
     *         {@code FileMode.Open} and {@code FileAccess.Read} .
     */
    public Stream open(FileMode mode, FileAccess access) {
        try {
            return fileSystem.openFile(path, mode, access);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Opens an existing file for read-only access.
     *
     * @return The newly created stream.
     */
    public Stream openRead() {
        return open(FileMode.Open, FileAccess.Read);
    }

    /**
     * Opens an existing file for reading as UTF-8 text.
     *
     * @return The newly created reader.
     */
    public StreamReader openText() {
        return new StreamReader(openRead());
    }

    /**
     * Opens a file for writing.
     *
     * @return The newly created stream.
     */
    public Stream openWrite() {
        return open(FileMode.Open, FileAccess.Write);
    }

    @Override
    public String toString() {
        return "DiscFileInfo{" +
                "path='" + path + '\'' +
                '}';
    }
}
