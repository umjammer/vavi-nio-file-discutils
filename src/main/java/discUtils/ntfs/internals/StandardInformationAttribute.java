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

package discUtils.ntfs.internals;

import java.util.EnumSet;

import discUtils.ntfs.AttributeRecord;
import discUtils.ntfs.FileAttributeFlags;
import discUtils.ntfs.INtfsContext;
import discUtils.ntfs.StandardInformation;
import discUtils.streams.util.StreamUtilities;


/**
 * Representation of an NTFS File Name attribute.
 *
 * The details in this attribute may be inconsistent with similar information in
 * the FileNameAttribute(s) for a file. This attribute is definitive, the
 * FileNameAttribute attribute holds a 'cache' of some of the information.
 */
public final class StandardInformationAttribute extends GenericAttribute {

    private final StandardInformation si;

    public StandardInformationAttribute(INtfsContext context, AttributeRecord record) {
        super(context, record);
        byte[] content = StreamUtilities.readAll(getContent());
        si = new StandardInformation();
        si.readFrom(content, 0);
    }

    /**
     * Gets the Unknown.
     */
    public long getClassId() {
        return si.classId;
    }

    /**
     * Gets the creation time of the file.
     */
    public long getCreationTime() {
        return si.creationTime;
    }

    /**
     * Gets the attributes of the file, as stored by NTFS.
     */
    public EnumSet<NtfsFileAttributes> getFileAttributes() {
        return FileAttributeFlags.cast(NtfsFileAttributes.class, si.fileAttributeFlags);
    }

    /**
     * Gets the last update sequence number of the file (relates to the
     * user-readable journal).
     */
    public long getJournalSequenceNumber() {
        return si.updateSequenceNumber;
    }

    /**
     * Gets the last access time of the file.
     */
    public long getLastAccessTime() {
        return si.lastAccessTime;
    }

    /**
     * Gets the last time the Master File Table entry for the file was changed.
     */
    public long getMasterFileTableChangedTime() {
        return si.mftChangedTime;
    }

    /**
     * Gets the maximum number of file versions (normally 0).
     */
    public long getMaxVersions() {
        return si.maxVersions;
    }

    /**
     * Gets the modification time of the file.
     */
    public long getModificationTime() {
        return si.modificationTime;
    }

    /**
     * Gets the owner identity, for the purposes of quota allocation.
     */
    public long getOwnerId() {
        return si.ownerId;
    }

    /**
     * Gets the amount charged to the owners quota for this file.
     */
    public long getQuotaCharged() {
        return si.quotaCharged;
    }

    /**
     * Gets the identifier of the Security Descriptor for this file.
     *
     * Security Descriptors are stored in the \$Secure meta-data file.
     */
    public long getSecurityId() {
        return si.securityId;
    }

    /**
     * Gets the version number of the file (normally 0).
     */
    public long getVersion() {
        return si.version;
    }
}
