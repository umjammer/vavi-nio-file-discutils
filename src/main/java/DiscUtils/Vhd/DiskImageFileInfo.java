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

package DiscUtils.Vhd;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import DiscUtils.Core.Geometry;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * Provides read access to detailed information about a VHD file.
 */
public class DiskImageFileInfo {
    private final Footer _footer;

    private final DynamicHeader _header;

    private final Stream _vhdStream;

    public DiskImageFileInfo(Footer footer, DynamicHeader header, Stream vhdStream) {
        _footer = footer;
        _header = header;
        _vhdStream = vhdStream;
    }

    /**
     * Gets the cookie indicating this is a VHD file (should be "conectix").
     */
    public String getCookie() {
        return _footer.Cookie;
    }

    /**
     * Gets the time the file was created (note: this is not the modification
     * time).
     */
    public long getCreationTimestamp() {
        return _footer.Timestamp;
    }

    /**
     * Gets the application used to create the file.
     */
    public String getCreatorApp() {
        return _footer.CreatorApp;
    }

    /**
     * Gets the host operating system of the application used to create the
     * file.
     */
    public String getCreatorHostOS() {
        return _footer.CreatorHostOS;
    }

    /**
     * Gets the version of the application used to create the file, packed as an
     * integer.
     */
    public int getCreatorVersion() {
        return _footer.CreatorVersion;
    }

    /**
     * Gets the current size of the disk (in bytes).
     */
    public long getCurrentSize() {
        return _footer.CurrentSize;
    }

    /**
     * Gets the type of the disk.
     */
    public FileType getDiskType() {
        return _footer.DiskType;
    }

    /**
     * Gets the number of sparse blocks the file is divided into.
     */
    public long getDynamicBlockCount() {
        return _header.MaxTableEntries;
    }

    /**
     * Gets the size of a sparse allocation block, in bytes.
     */
    public long getDynamicBlockSize() {
        return _header.BlockSize;
    }

    /**
     * Gets the checksum value of the dynamic header structure.
     */
    public int getDynamicChecksum() {
        return _header.Checksum;
    }

    /**
     * Gets the cookie indicating a dynamic disk header (should be "cxsparse").
     */
    public String getDynamicCookie() {
        return _header.Cookie;
    }

    /**
     * Gets the version of the dynamic header structure, packed as an integer.
     */
    public int getDynamicHeaderVersion() {
        return _header.HeaderVersion;
    }

    /**
     * Gets the stored paths to the parent file (for differencing disks).
     */
    public List<String> getDynamicParentLocators() {
        List<String> vals = new ArrayList<>(8);
        for (ParentLocator pl : _header.ParentLocators) {
            if (ParentLocator.PlatformCodeWindowsAbsoluteUnicode.equals(pl.PlatformCode) ||
                ParentLocator.PlatformCodeWindowsRelativeUnicode.equals(pl.PlatformCode)) {
                _vhdStream.setPosition(pl.PlatformDataOffset);
                byte[] buffer = StreamUtilities.readExact(_vhdStream, pl.PlatformDataLength);
                vals.add(new String(buffer, Charset.forName("UTF-16LE")));
            }

        }
        return vals;
    }

    /**
     * Gets the modification timestamp of the parent file (for differencing
     * disks).
     */
    public long getDynamicParentTimestamp() {
        return _header.ParentTimestamp;
    }

    /**
     * Gets the unicode name of the parent file (for differencing disks).
     */
    public String getDynamicParentUnicodeName() {
        return _header.ParentUnicodeName;
    }

    /**
     * Gets the unique id of the parent file (for differencing disks).
     */
    public UUID getDynamicParentUniqueId() {
        return _header.ParentUniqueId;
    }

    /**
     * Gets the Features bit field.
     */
    public int getFeatures() {
        return _footer.Features;
    }

    /**
     * Gets the file format version packed as an integer.
     */
    public int getFileFormatVersion() {
        return _footer.FileFormatVersion;
    }

    /**
     * Gets the checksum of the file's 'footer'.
     */
    public int getFooterChecksum() {
        return _footer.Checksum;
    }

    /**
     * Gets the geometry of the disk.
     */
    public Geometry getGeometry() {
        return _footer.Geometry;
    }

    /**
     * Gets the original size of the disk (in bytes).
     */
    public long getOriginalSize() {
        return _footer.OriginalSize;
    }

    /**
     * Gets a flag indicating if the disk has associated saved VM memory state.
     */
    public byte getSavedState() {
        return _footer.SavedState;
    }

    /**
     * Gets the unique identity of this disk.
     */
    public UUID getUniqueId() {
        return _footer.UniqueId;
    }

}
