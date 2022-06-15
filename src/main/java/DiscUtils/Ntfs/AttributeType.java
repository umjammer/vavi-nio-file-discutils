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

package DiscUtils.Ntfs;

import java.util.Arrays;


/**
 * Enumeration of NTFS file attribute types. Normally applications only create
 * Data attributes.
 */
public enum AttributeType {
    /**
     * No type specified.
     */
    None(0x00),
    /**
     * NTFS Standard Information.
     */
    StandardInformation(0x10),
    /**
     * Attribute list, that holds a list of attribute locations for files with a
     * large attribute set.
     */
    AttributeList(0x20),
    /**
     * FileName information, one per hard link.
     */
    FileName(0x30),
    /**
     * Distributed Link Tracking object identity.
     */
    ObjectId(0x40),
    /**
     * Legacy Security Descriptor attribute.
     */
    SecurityDescriptor(0x50),
    /**
     * The name of the NTFS volume.
     */
    VolumeName(0x60),
    /**
     * Information about the NTFS volume.
     */
    VolumeInformation(0x70),
    /**
     * File contents(0x00), a file may have multiple data attributes (default is
     * unnamed).
     */
    Data(0x80),
    /**
     * Root information for directories and other NTFS index's.
     */
    IndexRoot(0x90),
    /**
     * For 'large' directories and other NTFS index's, the index contents.
     */
    IndexAllocation(0xa0),
    /**
     * Bitmask of allocated clusters, records, etc - typically used in indexes.
     */
    Bitmap(0xb0),
    /**
     * ReparsePoint information.
     */
    ReparsePoint(0xc0),
    /**
     * Extended Attributes meta-information.
     */
    ExtendedAttributesInformation(0xd0),
    /**
     * Extended Attributes data.
     */
    ExtendedAttributes(0xe0),
    /**
     * Legacy attribute type from NT (not used).
     */
    PropertySet(0xf0),
    /**
     * Encrypted File System (EFS) data.
     */
    LoggedUtilityStream(0x100);

    private final int value;

    public int getValue() {
        return value;
    }

    AttributeType(int value) {
        this.value = value;
    }

    public static AttributeType valueOf(int value) {
        return Arrays.stream(values()).filter(e -> e.getValue() == value).findFirst().get();
    }
}
