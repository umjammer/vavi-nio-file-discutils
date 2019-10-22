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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.Stream;


public final class AttributeDefinitions {
    private final Map<AttributeType, AttributeDefinitionRecord> _attrDefs;

    public AttributeDefinitions() {
        _attrDefs = new HashMap<>();
        add(AttributeType.StandardInformation,
            "$STANDARD_INFORMATION",
            EnumSet.of(AttributeTypeFlags.MustBeResident),
            0x30,
            0x48);
        add(AttributeType.AttributeList, "$ATTRIBUTE_LIST", EnumSet.of(AttributeTypeFlags.CanBeNonResident), 0, -1);
        add(AttributeType.FileName,
            "$FILE_NAME",
            EnumSet.of(AttributeTypeFlags.Indexed, AttributeTypeFlags.MustBeResident),
            0x44,
            0x242);
        add(AttributeType.ObjectId, "$OBJECT_ID", EnumSet.of(AttributeTypeFlags.MustBeResident), 0, 0x100);
        add(AttributeType.SecurityDescriptor, "$SECURITY_DESCRIPTOR", EnumSet.of(AttributeTypeFlags.CanBeNonResident), 0x0, -1);
        add(AttributeType.VolumeName, "$VOLUME_NAME", EnumSet.of(AttributeTypeFlags.MustBeResident), 0x2, 0x100);
        add(AttributeType.VolumeInformation, "$VOLUME_INFORMATION", EnumSet.of(AttributeTypeFlags.MustBeResident), 0xC, 0xC);
        add(AttributeType.Data, "$DATA", EnumSet.of(AttributeTypeFlags.None), 0, -1);
        add(AttributeType.IndexRoot, "$INDEX_ROOT", EnumSet.of(AttributeTypeFlags.MustBeResident), 0, -1);
        add(AttributeType.IndexAllocation, "$INDEX_ALLOCATION", EnumSet.of(AttributeTypeFlags.CanBeNonResident), 0, -1);
        add(AttributeType.Bitmap, "$BITMAP", EnumSet.of(AttributeTypeFlags.CanBeNonResident), 0, -1);
        add(AttributeType.ReparsePoint, "$REPARSE_POINT", EnumSet.of(AttributeTypeFlags.CanBeNonResident), 0, 0x4000);
        add(AttributeType.ExtendedAttributesInformation,
            "$EA_INFORMATION",
            EnumSet.of(AttributeTypeFlags.MustBeResident),
            0x8,
            0x8);
        add(AttributeType.ExtendedAttributes, "$EA", EnumSet.of(AttributeTypeFlags.None), 0, 0x10000);
        add(AttributeType.LoggedUtilityStream,
            "$LOGGED_UTILITY_STREAM",
            EnumSet.of(AttributeTypeFlags.CanBeNonResident),
            0,
            0x10000);
    }

    public AttributeDefinitions(File file) {
        _attrDefs = new HashMap<>();
        byte[] buffer = new byte[AttributeDefinitionRecord.Size];

        try (Stream s = file.openStream(AttributeType.Data, null, FileAccess.Read)) {
            while (StreamUtilities.readMaximum(s, buffer, 0, buffer.length) == buffer.length) {
                AttributeDefinitionRecord record = new AttributeDefinitionRecord();
                record.read(buffer, 0);
                // NULL terminator record
                if (record.Type != AttributeType.None) {
                    _attrDefs.put(record.Type, record);
                }

            }
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    public void writeTo(File file) {
        List<AttributeType> attribs = new ArrayList<>(_attrDefs.keySet());
        Collections.sort(attribs);

        try (Stream s = file.openStream(AttributeType.Data, null, FileAccess.ReadWrite)) {
            byte[] buffer;
            for (int i = 0; i < attribs.size(); ++i) {
                buffer = new byte[AttributeDefinitionRecord.Size];
                AttributeDefinitionRecord attrDef = _attrDefs.get(attribs.get(i));
                attrDef.write(buffer, 0);
                s.write(buffer, 0, buffer.length);
            }
            buffer = new byte[AttributeDefinitionRecord.Size];
            s.write(buffer, 0, buffer.length);
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    public AttributeDefinitionRecord lookup(String name) {
        for (AttributeDefinitionRecord record : _attrDefs.values()) {
            if (name.compareTo(record.Name) == 0) {
                return record;
            }
        }
        return null;
    }

    public boolean mustBeResident(AttributeType attributeType) {
        if (_attrDefs.containsKey(attributeType)) {
            AttributeDefinitionRecord record = _attrDefs.get(attributeType);
            return record.Flags.contains(AttributeTypeFlags.MustBeResident);
        }

        return false;
    }

    public boolean isIndexed(AttributeType attributeType) {
        if (_attrDefs.containsKey(attributeType)) {
            AttributeDefinitionRecord record = _attrDefs.get(attributeType);
            return record.Flags.contains(AttributeTypeFlags.Indexed);
        }

        return false;
    }

    private void add(AttributeType attributeType,
                     String name,
                     EnumSet<AttributeTypeFlags> attributeTypeFlags,
                     int minSize,
                     int maxSize) {
        AttributeDefinitionRecord adr = new AttributeDefinitionRecord();
        adr.Type = attributeType;
        adr.Name = name;
        adr.Flags = attributeTypeFlags;
        adr.MinSize = minSize;
        adr.MaxSize = maxSize;
        _attrDefs.put(attributeType, adr);
    }
}
