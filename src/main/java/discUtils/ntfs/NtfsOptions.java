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

package discUtils.ntfs;

import discUtils.core.DiscFileSystemOptions;
import discUtils.core.compression.BlockCompressor;


/**
 * Class whose instances hold options controlling how {@link NtfsFileSystem}
 * works.
 */
public final class NtfsOptions extends DiscFileSystemOptions {

    public NtfsOptions() {
        hideMetafiles = true;
        hideHiddenFiles = true;
        hideSystemFiles = true;
        hideDosFileNames = true;
        compressor = new LZNT1();
        readCacheEnabled = true;
        fileLengthFromDirectoryEntries = true;
    }

    /**
     * Gets or sets the compression algorithm used for compressing files.
     */
    private BlockCompressor compressor;

    public BlockCompressor getCompressor() {
        return compressor;
    }

    public void setCompressor(BlockCompressor value) {
        compressor = value;
    }

    /**
     * Gets or sets a value indicating whether file length information comes from
     * directory entries or file data. The default ( {@code true} ) is that file
     * length information is supplied by the directory entry for a file. In some
     * circumstances that information may be inaccurate - specifically for files
     * with multiple hard links, the directory entries are only updated for the hard
     * link used to open the file.Setting this value to {@code false} , will always
     * retrieve the latest information from the underlying NTFS attribute
     * information, which reflects the true size of the file.
     */
    private boolean fileLengthFromDirectoryEntries;

    public boolean getFileLengthFromDirectoryEntries() {
        return fileLengthFromDirectoryEntries;
    }

    public void setFileLengthFromDirectoryEntries(boolean value) {
        fileLengthFromDirectoryEntries = value;
    }

    /**
     * Gets or sets a value indicating whether to hide DOS (8.3-style) file names
     * when enumerating directories.
     */
    private boolean hideDosFileNames;

    public boolean hideDosFileNames() {
        return hideDosFileNames;
    }

    public void setHideDosFileNames(boolean value) {
        hideDosFileNames = value;
    }

    /**
     * Gets or sets a value indicating whether to include hidden files when
     * enumerating directories.
     */
    private boolean hideHiddenFiles;

    public boolean hideHiddenFiles() {
        return hideHiddenFiles;
    }

    public void setHideHiddenFiles(boolean value) {
        hideHiddenFiles = value;
    }

    /**
     * Gets or sets a value indicating whether to include file system meta-files
     * when enumerating directories. Meta-files are those with an MFT (Master File
     * Table) index less than 24.
     */
    private boolean hideMetafiles;

    public boolean hideMetafiles() {
        return hideMetafiles;
    }

    public void setHideMetafiles(boolean value) {
        hideMetafiles = value;
    }

    /**
     * Gets or sets a value indicating whether to include system files when
     * enumerating directories.
     */
    private boolean hideSystemFiles;

    public boolean hideSystemFiles() {
        return hideSystemFiles;
    }

    public void setHideSystemFiles(boolean value) {
        hideSystemFiles = value;
    }

    /**
     * Gets or sets a value indicating whether NTFS-level read caching is used.
     */
    private boolean readCacheEnabled;

    public boolean getReadCacheEnabled() {
        return readCacheEnabled;
    }

    public void setReadCacheEnabled(boolean value) {
        readCacheEnabled = value;
    }

    /**
     * Gets or sets a value indicating whether short (8.3) file names are created
     * automatically.
     */
    private ShortFileNameOption shortNameCreation = ShortFileNameOption.UseVolumeFlag;

    public ShortFileNameOption getShortNameCreation() {
        return shortNameCreation;
    }

    public void setShortNameCreation(ShortFileNameOption value) {
        shortNameCreation = value;
    }

    /**
     * Returns a string representation of the file system options.
     *
     * @return A string of the form Show: XX XX XX.
     */
    public String toString() {
        return "Show: Normal " + (hideMetafiles ? "" : "Meta ") + (hideHiddenFiles ? "" : "Hidden ")
                + (hideSystemFiles ? "" : "System ") + (hideDosFileNames ? "" : "ShortNames ");
    }
}
