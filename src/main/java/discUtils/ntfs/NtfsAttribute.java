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

package discUtils.ntfs;

import java.io.PrintWriter;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import discUtils.core.IDiagnosticTraceable;
import discUtils.streams.SparseStream;
import discUtils.streams.buffer.BufferStream;
import discUtils.streams.buffer.IBuffer;
import discUtils.streams.buffer.IMappedBuffer;
import discUtils.streams.util.Range;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;

import static java.lang.System.getLogger;


class NtfsAttribute implements IDiagnosticTraceable {

    private static final Logger logger = getLogger(NtfsAttribute.class.getName());

    private IBuffer cachedRawBuffer;

    protected FileRecordReference containingFile;

    protected Map<AttributeReference, AttributeRecord> extents;

    protected File file;

    protected AttributeRecord primaryRecord;

    protected NtfsAttribute(File file, FileRecordReference containingFile, AttributeRecord record) {
        this.file = file;
        this.containingFile = containingFile;
        primaryRecord = record;
        extents = new HashMap<>();
        extents.put(new AttributeReference(containingFile, record.getAttributeId()), primaryRecord);
//if (NonResidentAttributeBuffer.debug) logger.log(Level.DEBUG, "4c: " + extents.size());
    }

    protected String getAttributeTypeName() {
        return switch (primaryRecord.getAttributeType()) {
            case StandardInformation -> "STANDARD INFORMATION";
            case FileName -> "FILE NAME";
            case SecurityDescriptor -> "SECURITY DESCRIPTOR";
            case Data -> "DATA";
            case Bitmap -> "BITMAP";
            case VolumeName -> "VOLUME NAME";
            case VolumeInformation -> "VOLUME INFORMATION";
            case IndexRoot -> "INDEX ROOT";
            case IndexAllocation -> "INDEX ALLOCATION";
            case ObjectId -> "OBJECT ID";
            case ReparsePoint -> "REPARSE POINT";
            case AttributeList -> "ATTRIBUTE LIST";
            default -> "UNKNOWN";
        };
    }

    public long getCompressedDataSize() {
        AttributeRecord firstExtent = getFirstExtent();
        if (!(firstExtent instanceof NonResidentAttributeRecord)) {
            return getFirstExtent().getAllocatedLength();
        }
        return ((NonResidentAttributeRecord) firstExtent).getCompressedDataSize();
    }

    public void setCompressedDataSize(long value) {
        AttributeRecord firstExtent = getFirstExtent();
        if (firstExtent instanceof NonResidentAttributeRecord) {
            ((NonResidentAttributeRecord) firstExtent).setCompressedDataSize(value);
        }
    }

    public int getCompressionUnitSize() {
        AttributeRecord firstExtent = getFirstExtent();
        if (!(firstExtent instanceof NonResidentAttributeRecord)) {
            return 0;
        }
        return ((NonResidentAttributeRecord) firstExtent).getCompressionUnitSize();
    }

    public void setCompressionUnitSize(int value) {
        AttributeRecord firstExtent = getFirstExtent();
        if (firstExtent instanceof NonResidentAttributeRecord) {
            ((NonResidentAttributeRecord) firstExtent).setCompressionUnitSize(value);
        }
    }

    public Map<AttributeReference, AttributeRecord> getExtents() {
        return extents;
    }

    public AttributeRecord getFirstExtent() {
        if (extents != null) {
            for (Map.Entry<AttributeReference, AttributeRecord> extent : extents.entrySet()) {
                AttributeRecord record = extent.getValue();
                if (!(record instanceof NonResidentAttributeRecord)) {
                    // Resident attribute, so there can only be one...
                    return extent.getValue();
                }
                if (record.getStartVcn() == 0) {
                    return extent.getValue();
                }
            }
        }

        throw new IllegalStateException("Attribute with no initial extent");
    }

    public EnumSet<AttributeFlags> getFlags() {
        return primaryRecord.getFlags();
    }

    public void addFlag(AttributeFlags value) {
        primaryRecord.getFlags().add(value);
        cachedRawBuffer = null;
    }

    public short getId() {
        return primaryRecord.getAttributeId();
    }

    public boolean isNonResident() {
        return primaryRecord.isNonResident();
    }

    public AttributeRecord getLastExtent() {
        AttributeRecord last = null;

        if (extents != null) {
            long lastVcn = 0;
            for (Map.Entry<AttributeReference, AttributeRecord> extent : extents.entrySet()) {
                AttributeRecord record = extent.getValue();
                if (!(record instanceof NonResidentAttributeRecord nonResident)) {
                    // Resident attribute, so there can only be one...
                    return extent.getValue();
                }

                if (nonResident.getLastVcn() >= lastVcn) {
                    last = extent.getValue();
                    lastVcn = nonResident.getLastVcn();
                }
            }
        }

        return last;
    }

    public long getLength() {
        return primaryRecord.getDataLength();
    }

    public String getName() {
        return primaryRecord.getName();
    }

    public AttributeRecord getPrimaryRecord() {
        return primaryRecord;
    }

    public IBuffer getRawBuffer() {
        if (cachedRawBuffer == null) {
            if (primaryRecord.isNonResident()) {
                cachedRawBuffer = new NonResidentAttributeBuffer(file, this);
            } else {
                cachedRawBuffer = ((ResidentAttributeRecord) primaryRecord).getDataBuffer();
            }
        }

        return cachedRawBuffer;
    }

    public List<AttributeRecord> getRecords() {
        List<AttributeRecord> records = new ArrayList<>(extents.values());
        records.sort(AttributeRecord.compareStartVcns);
        return records;
    }

    public AttributeReference getReference() {
        return new AttributeReference(containingFile, primaryRecord.getAttributeId());
    }

    public AttributeType getType() {
        return primaryRecord.getAttributeType();
    }

    @Override
    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + getAttributeTypeName() + " ATTRIBUTE (" + (getName() == null ? "No Name" : getName()) + ")");
        writer.println(indent + "  Length: " + primaryRecord.getDataLength() + " bytes");
        if (primaryRecord.getDataLength() == 0) {
            writer.println(indent + "    Data: <none>");
        } else {
            try {
                try (Stream s = open(FileAccess.Read)) {
                    StringBuilder hex = new StringBuilder();
                    byte[] buffer = new byte[32];
                    int numBytes = s.read(buffer, 0, buffer.length);
                    for (int i = 0; i < numBytes; ++i) {
                        hex.append(" %02x".formatted(buffer[i]));
                    }
                    writer.println(indent + "    Data: " + hex + (numBytes < s.getLength() ? "..." : ""));
                }
            } catch (Exception e) {
                logger.log(Level.DEBUG, e.getMessage(), e);
                writer.println(indent + "    Data: <can't read>");
            }
        }
        primaryRecord.dump(writer, indent + "  ");
    }

    public static NtfsAttribute fromRecord(File file, FileRecordReference recordFile, AttributeRecord record) {
        return switch (record.getAttributeType()) {
            case StandardInformation ->
                    new StructuredNtfsAttribute<>(StandardInformation.class, file, recordFile, record);
            case FileName -> new StructuredNtfsAttribute<>(FileNameRecord.class, file, recordFile, record);
            case SecurityDescriptor ->
                    new StructuredNtfsAttribute<>(SecurityDescriptor.class, file, recordFile, record);
            case Data -> new NtfsAttribute(file, recordFile, record);
            case Bitmap -> new NtfsAttribute(file, recordFile, record);
            case VolumeName -> new StructuredNtfsAttribute<>(VolumeName.class, file, recordFile, record);
            case VolumeInformation -> new StructuredNtfsAttribute<>(VolumeInformation.class, file, recordFile, record);
            case IndexRoot -> new NtfsAttribute(file, recordFile, record);
            case IndexAllocation -> new NtfsAttribute(file, recordFile, record);
            case ObjectId -> new StructuredNtfsAttribute<>(ObjectId.class, file, recordFile, record);
            case ReparsePoint -> new StructuredNtfsAttribute<>(ReparsePointRecord.class, file, recordFile, record);
            case AttributeList -> new StructuredNtfsAttribute<>(AttributeList.class, file, recordFile, record);
            default -> new NtfsAttribute(file, recordFile, record);
        };
    }

    public void setExtent(FileRecordReference containingFile, AttributeRecord record) {
        cachedRawBuffer = null;
        this.containingFile = containingFile;
        primaryRecord = record;
        extents.clear();
        extents.put(new AttributeReference(containingFile, record.getAttributeId()), record);
//if (NonResidentAttributeBuffer.debug) logger.log(Level.DEBUG, "4a: " + extents.size());
    }

    public void addExtent(FileRecordReference containingFile, AttributeRecord record) {
        cachedRawBuffer = null;
        extents.put(new AttributeReference(containingFile, record.getAttributeId()), record);
//if (NonResidentAttributeBuffer.debug) logger.log(Level.DEBUG, "4b: " + extents.size() + ", " + record.getAttributeId());
    }

    public void removeExtentCacheSafe(AttributeReference reference) {
        extents.remove(reference);
    }

    public boolean replaceExtent(AttributeReference oldRef, AttributeReference newRef, AttributeRecord record) {
        cachedRawBuffer = null;
        if (extents.remove(oldRef) == null) {
            return false;
        }

        if (oldRef.equals(getReference()) || extents.isEmpty()) {
            primaryRecord = record;
            containingFile = newRef.getFile();
        }

        extents.put(newRef, record);
        return true;
    }

    public List<Range> getClusters() {
        List<Range> result = new ArrayList<>();
        for (Map.Entry<AttributeReference, AttributeRecord> extent : extents.entrySet()) {
            result.addAll(extent.getValue().getClusters());
        }
        return result;
    }

    SparseStream open(FileAccess access) {
        return new BufferStream(getDataBuffer(), access);
    }

    IMappedBuffer getDataBuffer() {
        return new NtfsAttributeBuffer(file, this);
    }

    long offsetToAbsolutePos(long offset) {
        return getDataBuffer().mapPosition(offset);
    }
}
