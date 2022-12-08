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

package discUtils.vhd;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import discUtils.core.Geometry;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


/**
 * Provides read access to detailed information about a VHD file.
 */
public class DiskImageFileInfo {

    private final Footer footer;

    private final DynamicHeader header;

    private final Stream vhdStream;

    public DiskImageFileInfo(Footer footer, DynamicHeader header, Stream vhdStream) {
        this.footer = footer;
        this.header = header;
        this.vhdStream = vhdStream;
    }

    /**
     * Gets the cookie indicating this is a VHD file (should be "conectix").
     */
    public String getCookie() {
        return footer.cookie;
    }

    /**
     * Gets the time the file was created (note: this is not the modification
     * time).
     */
    public long getCreationTimestamp() {
        return footer.timestamp;
    }

    /**
     * Gets the application used to create the file.
     */
    public String getCreatorApp() {
        return footer.creatorApp;
    }

    /**
     * Gets the host operating system of the application used to create the
     * file.
     */
    public String getCreatorHostOS() {
        return footer.creatorHostOS;
    }

    /**
     * Gets the version of the application used to create the file, packed as an
     * integer.
     */
    public int getCreatorVersion() {
        return footer.creatorVersion;
    }

    /**
     * Gets the current size of the disk (in bytes).
     */
    public long getCurrentSize() {
        return footer.currentSize;
    }

    /**
     * Gets the type of the disk.
     */
    public FileType getDiskType() {
        return footer.diskType;
    }

    /**
     * Gets the number of sparse blocks the file is divided into.
     */
    public long getDynamicBlockCount() {
        return header.maxTableEntries;
    }

    /**
     * Gets the size of a sparse allocation block, in bytes.
     */
    public long getDynamicBlockSize() {
        return header.blockSize;
    }

    /**
     * Gets the checksum value of the dynamic header structure.
     */
    public int getDynamicChecksum() {
        return header.checksum;
    }

    /**
     * Gets the cookie indicating a dynamic disk header (should be "cxsparse").
     */
    public String getDynamicCookie() {
        return header.cookie;
    }

    /**
     * Gets the version of the dynamic header structure, packed as an integer.
     */
    public int getDynamicHeaderVersion() {
        return header.headerVersion;
    }

    /**
     * Gets the stored paths to the parent file (for differencing disks).
     */
    public List<String> getDynamicParentLocators() {
        List<String> vals = new ArrayList<>(8);
        for (ParentLocator pl : header.parentLocators) {
            if (ParentLocator.PlatformCodeWindowsAbsoluteUnicode.equals(pl.platformCode) ||
                ParentLocator.PlatformCodeWindowsRelativeUnicode.equals(pl.platformCode)) {
                vhdStream.position(pl.platformDataOffset);
                byte[] buffer = StreamUtilities.readExact(vhdStream, pl.platformDataLength);
                vals.add(new String(buffer, StandardCharsets.UTF_16LE));
            }

        }
        return vals;
    }

    /**
     * Gets the modification timestamp of the parent file (for differencing
     * disks).
     */
    public long getDynamicParentTimestamp() {
        return header.parentTimestamp;
    }

    /**
     * Gets the unicode name of the parent file (for differencing disks).
     */
    public String getDynamicParentUnicodeName() {
        return header.parentUnicodeName;
    }

    /**
     * Gets the unique id of the parent file (for differencing disks).
     */
    public UUID getDynamicParentUniqueId() {
        return header.parentUniqueId;
    }

    /**
     * Gets the features bit field.
     */
    public int getFeatures() {
        return footer.features;
    }

    /**
     * Gets the file format version packed as an integer.
     */
    public int getFileFormatVersion() {
        return footer.fileFormatVersion;
    }

    /**
     * Gets the checksum of the file's 'footer'.
     */
    public int getFooterChecksum() {
        return footer.checksum;
    }

    /**
     * Gets the geometry of the disk.
     */
    public Geometry getGeometry() {
        return footer.geometry;
    }

    /**
     * Gets the original size of the disk (in bytes).
     */
    public long getOriginalSize() {
        return footer.originalSize;
    }

    /**
     * Gets a flag indicating if the disk has associated saved VM memory state.
     */
    public byte getSavedState() {
        return footer.savedState;
    }

    /**
     * Gets the unique identity of this disk.
     */
    public UUID getUniqueId() {
        return footer.uniqueId;
    }
}
