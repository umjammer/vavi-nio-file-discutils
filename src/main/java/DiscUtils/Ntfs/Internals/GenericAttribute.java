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
import DiscUtils.Ntfs.AttributeType;
import DiscUtils.Ntfs.INtfsContext;
import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Buffer.SubBuffer;


/**
 * Base class for all attributes within Master File Table entries.
 *
 * More specialized base classes are provided for known attribute types.
 */
public abstract class GenericAttribute {
    private final INtfsContext _context;

    private final AttributeRecord _record;

    public GenericAttribute(INtfsContext context, AttributeRecord record) {
        _context = context;
        _record = record;
    }

    /**
     * Gets the type of the attribute.
     */
    public AttributeType getAttributeType() {
        return _record.getAttributeType();
    }

    /**
     * Gets a buffer that can access the content of the attribute.
     */
    public IBuffer getContent() {
        IBuffer rawBuffer = _record.getReadOnlyDataBuffer(_context);
        return new SubBuffer(rawBuffer, 0, _record.getDataLength());
    }

    /**
     * Gets the amount of valid data in the attribute's content.
     */
    public long getContentLength() {
        return _record.getDataLength();
    }

    /**
     * Gets the flags indicating how the content of the attribute is stored.
     */
    public EnumSet<AttributeFlags> getFlags() {
        return AttributeFlags.valueOf((int) DiscUtils.Ntfs.AttributeFlags.valueOf(_record.getFlags()));
    }

    /**
     * Gets the unique id of the attribute.
     */
    public int getIdentifier() {
        return _record.getAttributeId();
    }

    /**
     * Gets a value indicating whether the attribute content is stored in the
     * MFT record itself.
     */
    public boolean getIsResident() {
        return !_record.isNonResident();
    }

    /**
     * Gets the name of the attribute (if any).
     */
    public String getName() {
        return _record.getName();
    }

    public static GenericAttribute fromAttributeRecord(INtfsContext context, AttributeRecord record) {
        switch (record.getAttributeType()) {
        case AttributeList:
            return new AttributeListAttribute(context, record);
        case FileName:
            return new FileNameAttribute(context, record);
        case StandardInformation:
            return new StandardInformationAttribute(context, record);
        default:
            return new UnknownAttribute(context, record);
        }
    }
}
