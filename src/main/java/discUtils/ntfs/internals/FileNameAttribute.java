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
import discUtils.ntfs.FileNameNamespace;
import discUtils.ntfs.FileNameRecord;
import discUtils.ntfs.INtfsContext;
import discUtils.streams.util.StreamUtilities;


/**
 * Representation of an NTFS File Name attribute.
 *
 * Each Master File Table entry (MFT Entry) has one of these attributes for each
 * hard link. Files with a long name and a short name will have at least two of
 * these attributes. The details in this attribute may be inconsistent with
 * similar information in the StandardInformationAttribute for a file. The
 * StandardInformation is definitive, this attribute holds a 'cache' of the
 * information.
 */
public final class FileNameAttribute extends GenericAttribute {
    private final FileNameRecord _fnr;

    public FileNameAttribute(INtfsContext context, AttributeRecord record) {
        super(context, record);
        byte[] content = StreamUtilities.readAll(getContent());
        _fnr = new FileNameRecord();
        _fnr.readFrom(content, 0);
    }

    /**
     * Gets the amount of disk space allocated for the file.
     */
    public long getAllocatedSize() {
        return _fnr._allocatedSize;
    }

    /**
     * Gets the creation time of the file.
     */
    public long getCreationTime() {
        return _fnr._creationTime;
    }

    /**
     * Gets the extended attributes size, or a reparse tag, depending on the
     * nature of the file.
     */
    public long getExtendedAttributesSizeOrReparsePointTag() {
        return _fnr._eaSizeOrReparsePointTag;
    }

    /**
     * Gets the attributes of the file, as stored by NTFS.
     */
    public EnumSet<NtfsFileAttributes> getFileAttributes() {
        return FileAttributeFlags.cast(NtfsFileAttributes.class, _fnr._flags);
    }

    /**
     * Gets the name of the file within the parent directory.
     */
    public String getFileName() {
        return _fnr._fileName;
    }

    /**
     * Gets the namespace of the FileName property.
     */
    public NtfsNamespace getFileNameNamespace() {
        return FileNameNamespace.cast(NtfsNamespace.class, _fnr._fileNameNamespace);
    }

    /**
     * Gets the last access time of the file.
     */
    public long getLastAccessTime() {
        return _fnr._lastAccessTime;
    }

    /**
     * Gets the last time the Master File Table entry for the file was changed.
     */
    public long getMasterFileTableChangedTime() {
        return _fnr._mftChangedTime;
    }

    /**
     * Gets the modification time of the file.
     */
    public long getModificationTime() {
        return _fnr._modificationTime;
    }

    /**
     * Gets the reference to the parent directory.
     *
     * This attribute stores the name of a file within a directory, this field
     * provides the link back to the directory.
     */
    public MasterFileTableReference getParentDirectory() {
        return new MasterFileTableReference(_fnr._parentDirectory);
    }

    /**
     * Gets the amount of data stored in the file.
     */
    public long getRealSize() {
        return _fnr._realSize;
    }

}
