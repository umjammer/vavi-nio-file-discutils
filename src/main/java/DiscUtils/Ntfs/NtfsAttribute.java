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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Buffer.BufferStream;
import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Buffer.IMappedBuffer;
import DiscUtils.Streams.Util.Range;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;


class NtfsAttribute implements IDiagnosticTraceable {
    private IBuffer _cachedRawBuffer;

    protected FileRecordReference _containingFile;

    protected Map<AttributeReference, AttributeRecord> _extents;

    protected File _file;

    protected AttributeRecord _primaryRecord;

    protected NtfsAttribute(File file, FileRecordReference containingFile, AttributeRecord record) {
        _file = file;
        _containingFile = containingFile;
        _primaryRecord = record;
        _extents = new HashMap<>();
        _extents.put(new AttributeReference(containingFile, record.getAttributeId()), _primaryRecord);
    }

    protected String getAttributeTypeName() {
        switch (_primaryRecord.getAttributeType()) {
        case StandardInformation:
            return "STANDARD INFORMATION";
        case FileName:
            return "FILE NAME";
        case SecurityDescriptor:
            return "SECURITY DESCRIPTOR";
        case Data:
            return "DATA";
        case Bitmap:
            return "BITMAP";
        case VolumeName:
            return "VOLUME NAME";
        case VolumeInformation:
            return "VOLUME INFORMATION";
        case IndexRoot:
            return "INDEX ROOT";
        case IndexAllocation:
            return "INDEX ALLOCATION";
        case ObjectId:
            return "OBJECT ID";
        case ReparsePoint:
            return "REPARSE POINT";
        case AttributeList:
            return "ATTRIBUTE LIST";
        default:
            return "UNKNOWN";
        }
    }

    public long getCompressedDataSize() {
        AttributeRecord firstExtent = getFirstExtent();
        if (!NonResidentAttributeRecord.class.isInstance(firstExtent)) {
            return getFirstExtent().getAllocatedLength();
        }
        return NonResidentAttributeRecord.class.cast(firstExtent).getCompressedDataSize();
    }

    public void setCompressedDataSize(long value) {
        AttributeRecord firstExtent = getFirstExtent();
        if (NonResidentAttributeRecord.class.isInstance(firstExtent)) {
            NonResidentAttributeRecord.class.cast(firstExtent).setCompressedDataSize(value);
        }
    }

    public int getCompressionUnitSize() {
        AttributeRecord firstExtent = getFirstExtent();
        if (!NonResidentAttributeRecord.class.isInstance(firstExtent)) {
            return 0;
        }
        return NonResidentAttributeRecord.class.cast(firstExtent).getCompressionUnitSize();
    }

    public void setCompressionUnitSize(int value) {
        AttributeRecord firstExtent = getFirstExtent();
        if (NonResidentAttributeRecord.class.isInstance(firstExtent)) {
            NonResidentAttributeRecord.class.cast(firstExtent).setCompressionUnitSize(value);
        }
    }

    public Map<AttributeReference, AttributeRecord> getExtents() {
        return _extents;
    }

    public AttributeRecord getFirstExtent() {
        if (_extents != null) {
            for (Map.Entry<AttributeReference, AttributeRecord> extent : _extents.entrySet()) {
                AttributeRecord record = extent.getValue();
                if (!NonResidentAttributeRecord.class.isInstance(record)) {
                    // Resident attribute, so there can only be one...
                    return extent.getValue();
                }
                if (NonResidentAttributeRecord.class.cast(record).getStartVcn() == 0) {
                    return extent.getValue();
                }
            }
        }

        throw new IllegalStateException("Attribute with no initial extent");
    }

    public EnumSet<AttributeFlags> getFlags() {
        return _primaryRecord.getFlags();
    }

    public void addFlag(AttributeFlags value) {
        _primaryRecord.getFlags().add(value);
        _cachedRawBuffer = null;
    }

    public short getId() {
        return _primaryRecord.getAttributeId();
    }

    public boolean isNonResident() {
        return _primaryRecord.isNonResident();
    }

    public AttributeRecord getLastExtent() {
        AttributeRecord last = null;

        if (_extents != null) {
            long lastVcn = 0;
            for (Map.Entry<AttributeReference, AttributeRecord> extent : _extents.entrySet()) {
                AttributeRecord record = extent.getValue();
                if (!NonResidentAttributeRecord.class.isInstance(record)) {
                    // Resident attribute, so there can only be one...
                    return extent.getValue();
                }

                NonResidentAttributeRecord nonResident = NonResidentAttributeRecord.class.cast(record);
                if (nonResident.getLastVcn() >= lastVcn) {
                    last = extent.getValue();
                    lastVcn = nonResident.getLastVcn();
                }
            }
        }

        return last;
    }

    public long getLength() {
        return _primaryRecord.getDataLength();
    }

    public String getName() {
        return _primaryRecord.getName();
    }

    public AttributeRecord getPrimaryRecord() {
        return _primaryRecord;
    }

    public IBuffer getRawBuffer() {
        if (_cachedRawBuffer == null) {
            if (_primaryRecord.isNonResident()) {
                _cachedRawBuffer = new NonResidentAttributeBuffer(_file, this);
            } else {
                _cachedRawBuffer = ((ResidentAttributeRecord) _primaryRecord).getDataBuffer();
            }
        }

        return _cachedRawBuffer;
    }

    public List<AttributeRecord> getRecords() {
        List<AttributeRecord> records = new ArrayList<>(_extents.values());
        Collections.sort(records, AttributeRecord.compareStartVcns);
        return records;
    }

    public AttributeReference getReference() {
        return new AttributeReference(_containingFile, _primaryRecord.getAttributeId());
    }

    public AttributeType getType() {
        return _primaryRecord.getAttributeType();
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + getAttributeTypeName() + " ATTRIBUTE (" + (getName() == null ? "No Name" : getName()) + ")");
        writer.println(indent + "  Length: " + _primaryRecord.getDataLength() + " bytes");
        if (_primaryRecord.getDataLength() == 0) {
            writer.println(indent + "    Data: <none>");
        } else {
            try {
                try (Stream s = open(FileAccess.Read)) {
                    String hex = "";
                    byte[] buffer = new byte[32];
                    int numBytes = s.read(buffer, 0, buffer.length);
                    for (int i = 0; i < numBytes; ++i) {
                        hex = hex + String.format(" %02x", buffer[i]);
                    }
                    writer.println(indent + "    Data: " + hex + (numBytes < s.getLength() ? "..." : ""));
                }
            } catch (Exception e) {
                e.printStackTrace();
                writer.println(indent + "    Data: <can't read>");
            }
        }
        _primaryRecord.dump(writer, indent + "  ");
    }

    public static NtfsAttribute fromRecord(File file, FileRecordReference recordFile, AttributeRecord record) {
        switch (record.getAttributeType()) {
        case StandardInformation:
            return new StructuredNtfsAttribute<>(StandardInformation.class, file, recordFile, record);
        case FileName:
            return new StructuredNtfsAttribute<>(FileNameRecord.class, file, recordFile, record);
        case SecurityDescriptor:
            return new StructuredNtfsAttribute<>(SecurityDescriptor.class, file, recordFile, record);
        case Data:
            return new NtfsAttribute(file, recordFile, record);
        case Bitmap:
            return new NtfsAttribute(file, recordFile, record);
        case VolumeName:
            return new StructuredNtfsAttribute<>(VolumeName.class, file, recordFile, record);
        case VolumeInformation:
            return new StructuredNtfsAttribute<>(VolumeInformation.class, file, recordFile, record);
        case IndexRoot:
            return new NtfsAttribute(file, recordFile, record);
        case IndexAllocation:
            return new NtfsAttribute(file, recordFile, record);
        case ObjectId:
            return new StructuredNtfsAttribute<>(ObjectId.class, file, recordFile, record);
        case ReparsePoint:
            return new StructuredNtfsAttribute<>(ReparsePointRecord.class, file, recordFile, record);
        case AttributeList:
            return new StructuredNtfsAttribute<>(AttributeList.class, file, recordFile, record);
        default:
            return new NtfsAttribute(file, recordFile, record);
        }
    }

    public void setExtent(FileRecordReference containingFile, AttributeRecord record) {
        _cachedRawBuffer = null;
        _containingFile = containingFile;
        _primaryRecord = record;
        _extents.clear();
        _extents.put(new AttributeReference(containingFile, record.getAttributeId()), record);
    }

    public void addExtent(FileRecordReference containingFile, AttributeRecord record) {
        _cachedRawBuffer = null;
        _extents.put(new AttributeReference(containingFile, record.getAttributeId()), record);
    }

    public void removeExtentCacheSafe(AttributeReference reference) {
        _extents.remove(reference);
    }

    public boolean replaceExtent(AttributeReference oldRef, AttributeReference newRef, AttributeRecord record) {
        _cachedRawBuffer = null;
        if (_extents.remove(oldRef) == null) {
            return false;
        }

        if (oldRef.equals(getReference()) || _extents.size() == 0) {
            _primaryRecord = record;
            _containingFile = newRef.getFile();
        }

        _extents.put(newRef, record);
        return true;
    }

    public List<Range> getClusters() {
        List<Range> result = new ArrayList<>();
        for (Map.Entry<AttributeReference, AttributeRecord> extent : _extents.entrySet()) {
            result.addAll(extent.getValue().getClusters());
        }
        return result;
    }

    SparseStream open(FileAccess access) {
        return new BufferStream(getDataBuffer(), access);
    }

    IMappedBuffer getDataBuffer() {
        return new NtfsAttributeBuffer(_file, this);
    }

    long offsetToAbsolutePos(long offset) {
        return getDataBuffer().mapPosition(offset);
    }
}
