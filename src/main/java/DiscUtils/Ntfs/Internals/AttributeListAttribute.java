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

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Ntfs.AttributeList;
import DiscUtils.Ntfs.AttributeListRecord;
import DiscUtils.Ntfs.AttributeRecord;
import DiscUtils.Ntfs.INtfsContext;
import DiscUtils.Streams.Util.StreamUtilities;


/**
 * List of attributes for files that are split over multiple Master File Table
 * entries.
 *
 * Files with lots of attribute data (for example that have become very
 * fragmented) contain
 * this attribute in their 'base' Master File Table entry. This attribute acts
 * as an index,
 * indicating for each attribute in the file, which Master File Table entry
 * contains the
 * attribute.
 */
public final class AttributeListAttribute extends GenericAttribute {
    private final AttributeList _list;

    public AttributeListAttribute(INtfsContext context, AttributeRecord record) {
        super(context, record);
        byte[] content = StreamUtilities.readAll(getContent());
        _list = new AttributeList();
        _list.readFrom(content, 0);
    }

    /**
     * Gets the entries in this attribute list.
     */
    public List<AttributeListEntry> getEntries() {
        List<AttributeListEntry> entries = new ArrayList<>();
        for (AttributeListRecord record : _list) {
            entries.add(new AttributeListEntry(record));
        }
        return entries;
    }
}
