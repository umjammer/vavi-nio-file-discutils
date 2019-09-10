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
import java.nio.file.attribute.BasicFileAttributes;

import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.FileMode;
import moe.yo3explorer.dotnetio4j.Stream;


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
    public boolean getExists() throws IOException {
        return getFileSystem().fileExists(getPath());
    }

    /**
    * Gets or sets a value indicating whether the file is read-only.
    */
    public boolean getIsReadOnly() {
        return getAttributes(). & BasicFileAttributes.ReadOnly;
    }

    public void setIsReadOnly(boolean value) {
        if (value) {
            setAttributes(getAttributes() | BasicFileAttributes.ReadOnly);
        } else {
            setAttributes(getAttributes() & ~BasicFileAttributes.ReadOnly);
        }
    }

    /**
     * Gets the length of the current file in bytes.
     */
    public long getLength() throws IOException {
        return getFileSystem().getFileLength(getPath());
    }

    /**
     * Deletes a file.
     */
    public void delete() throws IOException {
        getFileSystem().deleteFile(getPath());
    }

    /**
     * Creates a
     * {@link #StreamWriter}
     * that appends text to the file represented by this
     * {@link #DiscFileInfo}
     * .
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
    public void copyTo(String destinationFileName) throws IOException {
        copyTo(destinationFileName, false);
    }

    /**
     * Copies an existing file to a new file, allowing overwriting of an
     * existing file.
     * 
     * @param destinationFileName The destination file.
     * @param overwrite Whether to permit over-writing of an existing file.
     */
    public void copyTo(String destinationFileName, boolean overwrite) throws IOException {
        getFileSystem().copyFile(getPath(), destinationFileName, overwrite);
    }

    /**
     * Creates a new file for reading and writing.
     * 
     * @return The newly created stream.
     */
    public Stream create() throws IOException {
        return open(FileMode.Create);
    }

    /**
     * Creates a new
     * {@link #StreamWriter}
     * that writes a new text file.
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
    public void moveTo(String destinationFileName) throws IOException {
        getFileSystem().moveFile(getPath(), destinationFileName);
    }

    /**
     * Opens the current file.
     * 
     * @param mode The file mode for the created stream.
     * @return The newly created stream.Read-only file systems only support
     *         {@code FileMode.Open}
     *         .
     */
    public Stream open(FileMode mode) throws IOException {
        return getFileSystem().openFile(getPath(), mode);
    }

    /**
     * Opens the current file.
     * 
     * @param mode The file mode for the created stream.
     * @param access The access permissions for the created stream.
     * @return The newly created stream.Read-only file systems only support
     *         {@code FileMode.Open}
     *         and
     *         {@code FileAccess.Read}
     *         .
     */
    public Stream open(FileMode mode, FileAccess access) throws IOException {
        return getFileSystem().openFile(getPath(), mode, access);
    }

    /**
     * Opens an existing file for read-only access.
     * 
     * @return The newly created stream.
     */
    public Stream openRead() throws IOException {
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
    public Stream openWrite() throws IOException {
        return open(FileMode.Open, FileAccess.Write);
    }

}
