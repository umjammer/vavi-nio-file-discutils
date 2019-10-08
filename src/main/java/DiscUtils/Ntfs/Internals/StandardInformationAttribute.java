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

package DiscUtils.Ntfs.Internals;

import java.util.EnumSet;

import DiscUtils.Ntfs.AttributeRecord;
import DiscUtils.Ntfs.FileAttributeFlags;
import DiscUtils.Ntfs.INtfsContext;
import DiscUtils.Ntfs.StandardInformation;
import DiscUtils.Streams.Util.StreamUtilities;


/**
 * Representation of an NTFS File Name attribute.
 *
 * The details in this attribute may be inconsistent with similar information in
 * the FileNameAttribute(s) for a file. This attribute is definitive, the
 * FileNameAttribute attribute holds a 'cache' of some of the information.
 */
public final class StandardInformationAttribute extends GenericAttribute {
    private final StandardInformation _si;

    public StandardInformationAttribute(INtfsContext context, AttributeRecord record) {
        super(context, record);
        byte[] content = StreamUtilities.readAll(getContent());
        _si = new StandardInformation();
        _si.readFrom(content, 0);
    }

    /**
     * Gets the Unknown.
     */
    public long getClassId() {
        return _si.ClassId;
    }

    /**
     * Gets the creation time of the file.
     */
    public long getCreationTime() {
        return _si.CreationTime;
    }

    /**
     * Gets the attributes of the file, as stored by NTFS.
     */
    public EnumSet<NtfsFileAttributes> getFileAttributes() {
        return FileAttributeFlags.cast(NtfsFileAttributes.class, _si._FileAttributes);
    }

    /**
     * Gets the last update sequence number of the file (relates to the
     * user-readable journal).
     */
    public long getJournalSequenceNumber() {
        return _si.UpdateSequenceNumber;
    }

    /**
     * Gets the last access time of the file.
     */
    public long getLastAccessTime() {
        return _si.LastAccessTime;
    }

    /**
     * Gets the last time the Master File Table entry for the file was changed.
     */
    public long getMasterFileTableChangedTime() {
        return _si.MftChangedTime;
    }

    /**
     * Gets the maximum number of file versions (normally 0).
     */
    public long getMaxVersions() {
        return _si.MaxVersions;
    }

    /**
     * Gets the modification time of the file.
     */
    public long getModificationTime() {
        return _si.ModificationTime;
    }

    /**
     * Gets the owner identity, for the purposes of quota allocation.
     */
    public long getOwnerId() {
        return _si.OwnerId;
    }

    /**
     * Gets the amount charged to the owners quota for this file.
     */
    public long getQuotaCharged() {
        return _si.QuotaCharged;
    }

    /**
     * Gets the identifier of the Security Descriptor for this file.
     *
     * Security Descriptors are stored in the \$Secure meta-data file.
     */
    public long getSecurityId() {
        return _si.SecurityId;
    }

    /**
     * Gets the version number of the file (normally 0).
     */
    public long getVersion() {
        return _si.Version;
    }
}
