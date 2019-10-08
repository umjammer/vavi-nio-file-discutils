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

package DiscUtils.Core.Vfs;

import java.util.Collection;


/**
 * Interface implemented by classes representing a directory.
 * Concrete type representing directory entries.Concrete type representing
 * files.
 */
public interface IVfsDirectory<TDirEntry extends VfsDirEntry, TFile extends IVfsFile> extends IVfsFile {
    /**
     * Gets all of the directory entries.
     */
    Collection<TDirEntry> getAllEntries();

    /**
     * Gets a self-reference, if available.
     */
    TDirEntry getSelf();

    /**
     * Gets a specific directory entry, by name.
     *
     * @param name The name of the directory entry.
     * @return The directory entry, or
     *         {@code null}
     *         if not found.
     */
    TDirEntry getEntryByName(String name);

    /**
     * Creates a new file.
     *
     * @param name The name of the file (relative to this directory).
     * @return The newly created file.
     */
    TDirEntry createNewFile(String name);
}
