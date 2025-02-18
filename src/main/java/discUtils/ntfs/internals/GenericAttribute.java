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
import discUtils.ntfs.AttributeType;
import discUtils.ntfs.INtfsContext;
import discUtils.streams.buffer.IBuffer;
import discUtils.streams.buffer.SubBuffer;


/**
 * base class for all attributes within Master File Table entries.
 *
 * More specialized base classes are provided for known attribute types.
 */
public abstract class GenericAttribute {

    private final INtfsContext context;

    private final AttributeRecord record;

    public GenericAttribute(INtfsContext context, AttributeRecord record) {
        this.context = context;
        this.record = record;
    }

    /**
     * Gets the type of the attribute.
     */
    public AttributeType getAttributeType() {
        return record.getAttributeType();
    }

    /**
     * Gets a buffer that can access the content of the attribute.
     */
    public IBuffer getContent() {
        IBuffer rawBuffer = record.getReadOnlyDataBuffer(context);
        return new SubBuffer(rawBuffer, 0, record.getDataLength());
    }

    /**
     * Gets the amount of valid data in the attribute's content.
     */
    public long getContentLength() {
        return record.getDataLength();
    }

    /**
     * Gets the flags indicating how the content of the attribute is stored.
     */
    public EnumSet<AttributeFlags> getFlags() {
        return AttributeFlags.valueOf((int) discUtils.ntfs.AttributeFlags.valueOf(record.getFlags()));
    }

    /**
     * Gets the unique id of the attribute.
     */
    public int getIdentifier() {
        return record.getAttributeId();
    }

    /**
     * Gets a value indicating whether the attribute content is stored in the
     * MFT record itself.
     */
    public boolean getIsResident() {
        return !record.isNonResident();
    }

    /**
     * Gets the name of the attribute (if any).
     */
    public String getName() {
        return record.getName();
    }

    public static GenericAttribute fromAttributeRecord(INtfsContext context, AttributeRecord record) {
        return switch (record.getAttributeType()) {
            case AttributeList -> new AttributeListAttribute(context, record);
            case FileName -> new FileNameAttribute(context, record);
            case StandardInformation -> new StandardInformationAttribute(context, record);
            default -> new UnknownAttribute(context, record);
        };
    }
}
