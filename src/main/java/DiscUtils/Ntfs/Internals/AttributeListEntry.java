//
// Translated by CS2J (http://www.cs2j.com): 2019/07/15 9:43:07
//

package DiscUtils.Ntfs.Internals;

import DiscUtils.Ntfs.AttributeListRecord;
import DiscUtils.Ntfs.AttributeType;

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
/**
* Represents an entry in an AttributeList attribute.
* Each instance of this class points to the actual Master File Table
* entry that contains the attribute.  It is used for files split over multiple
* Master File Table entries.
*/
public final class AttributeListEntry
{
    private final AttributeListRecord _record;
    public AttributeListEntry(AttributeListRecord record) {
        _record = record;
    }

    /**
    * Gets the identifier of the attribute.
    */
    public int getAttributeIdentifier() {
        return _record.AttributeId;
    }

    /**
    * Gets the name of the attribute (if any).
    */
    public String getAttributeName() {
        return _record.Name;
    }

    /**
    * Gets the type of the attribute.
    */
    public AttributeType getAttributeType() {
        return _record.Type;
    }

    /**
    * Gets the first cluster represented in this attribute (normally 0).
    *
    * For very fragmented files, it can be necessary to split a single attribute
    * over multiple Master File Table entries.  This is achieved with multiple attributes
    * with the same name and type (one per Master File Table entry), with this field
    * determining the logical order of the attributes.
    *
    * The number is the first 'virtual' cluster present (i.e. divide the file's content
    * into 'cluster' sized chunks, this is the first of those clusters logically
    * represented in the attribute).
    */
    public long getFirstFileCluster() {
        return _record.StartVcn;
    }

    /**
    * Gets the Master File Table entry that contains the attribute.
    */
    public MasterFileTableReference getMasterFileTableEntry() {
        return new MasterFileTableReference(_record.BaseFileReference);
    }

}


