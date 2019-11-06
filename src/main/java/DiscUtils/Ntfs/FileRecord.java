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
import java.util.List;

import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;


public class FileRecord extends FixupRecordBase {
    private short _firstAttributeOffset;

    private boolean _haveIndex;

    private int _index;

    // Self-reference (on XP+)
    public FileRecord(int sectorSize) {
        super("FILE", sectorSize);
    }

    public FileRecord(int sectorSize, int recordLength, int index) {
        super("FILE", sectorSize, recordLength);
        reInitialize(sectorSize, recordLength, index);
    }

    private int _allocatedSize;

    public int getAllocatedSize() {
        return _allocatedSize;
    }

    public void setAllocatedSize(int value) {
        _allocatedSize = value;
    }

    private List<AttributeRecord> _attributes;

    public List<AttributeRecord> getAttributes() {
        return _attributes;
    }

    public void setAttributes(List<AttributeRecord> value) {
        _attributes = value;
    }

    private FileRecordReference _baseFile;

    public FileRecordReference getBaseFile() {
        return _baseFile;
    }

    public void setBaseFile(FileRecordReference value) {
        _baseFile = value;
    }

    public AttributeRecord getFirstAttribute() {
        return _attributes.size() > 0 ? _attributes.get(0) : null;
    }

    private EnumSet<FileRecordFlags> _flags;

    public EnumSet<FileRecordFlags> getFlags() {
        return _flags;
    }

    public void setFlags(EnumSet<FileRecordFlags> value) {
        _flags = value;
    }

    private short _hardLinkCount;

    public short getHardLinkCount() {
        return _hardLinkCount;
    }

    public void setHardLinkCount(short value) {
        _hardLinkCount = value;
    }

    public boolean getIsMftRecord() {
        return getMasterFileTableIndex() == MasterFileTable.MftIndex ||
               (_baseFile.getMftIndex() == MasterFileTable.MftIndex && _baseFile.getSequenceNumber() != 0);
    }

    private int _loadedIndex;

    public int getLoadedIndex() {
        return _loadedIndex;
    }

    public void setLoadedIndex(int value) {
        _loadedIndex = value;
    }

    private long _logFileSequenceNumber;

    public long getLogFileSequenceNumber() {
        return _logFileSequenceNumber;
    }

    public void setLogFileSequenceNumber(long value) {
        _logFileSequenceNumber = value;
    }

    public int getMasterFileTableIndex() {
        return _haveIndex ? _index : _loadedIndex;
    }

    private short _nextAttributeId;

    public short getNextAttributeId() {
        return _nextAttributeId;
    }

    public void setNextAttributeId(short value) {
        _nextAttributeId = value;
    }

    private int _realSize;

    public int getRealSize() {
        return _realSize;
    }

    public void setRealSize(int value) {
        _realSize = value;
    }

    public FileRecordReference getReference() {
        return new FileRecordReference(getMasterFileTableIndex(), _sequenceNumber);
    }

    private short _sequenceNumber;

    public short getSequenceNumber() {
        return _sequenceNumber;
    }

    public void setSequenceNumber(short value) {
        _sequenceNumber = value;
    }

    static EnumSet<FileAttributeFlags> convertFlags(EnumSet<FileRecordFlags> source) {
        EnumSet<FileAttributeFlags> result = EnumSet.noneOf(FileAttributeFlags.class);

        if (source.contains(FileRecordFlags.IsDirectory)) {
            result.add(FileAttributeFlags.Directory);
        }

        if (source.contains(FileRecordFlags.HasViewIndex)) {
            result.add(FileAttributeFlags.IndexView);
        }

        if (source.contains(FileRecordFlags.IsMetaFile)) {
            result.addAll(EnumSet.of(FileAttributeFlags.Hidden, FileAttributeFlags.System));
        }

        return result;
    }

    public void reInitialize(int sectorSize, int recordLength, int index) {
        initialize("FILE", sectorSize, recordLength);
        _sequenceNumber++;
        _flags = EnumSet.noneOf(FileRecordFlags.class);
        _allocatedSize = recordLength;
        _nextAttributeId = 0;
        _index = index;
        _hardLinkCount = 0;
        _baseFile = new FileRecordReference(0);

        _attributes = new ArrayList<>();
        _haveIndex = true;
    }

    /**
     * Gets an attribute by it's id.
     *
     * @param id The attribute's id.
     * @return The attribute, or {@code null} .
     */
    public AttributeRecord getAttribute(short id) {
        for (AttributeRecord attrRec : getAttributes()) {
            if (attrRec.getAttributeId() == id) {
                return attrRec;
            }
        }

        return null;
    }

    /**
     * Gets an unnamed attribute.
     *
     * @param type The attribute type.
     * @return The attribute, or {@code null} .
     */
    public AttributeRecord getAttribute(AttributeType type) {
        return getAttribute(type, null);
    }

    /**
     * Gets an named attribute.
     *
     * @param type The attribute type.
     * @param name The name of the attribute.
     * @return The attribute, or {@code null} .
     */
    public AttributeRecord getAttribute(AttributeType type, String name) {
        for (AttributeRecord attrRec : getAttributes()) {
            if (attrRec.getAttributeType() == type && attrRec.getName().equals(name)) {
                return attrRec;
            }
        }

        return null;
    }

    public String toString() {
        for (AttributeRecord attr : getAttributes()) {
            if (attr.getAttributeType() == AttributeType.FileName) {
                StructuredNtfsAttribute<FileNameRecord> fnAttr = StructuredNtfsAttribute.class
                        .cast(NtfsAttribute.fromRecord(null, new FileRecordReference(0), attr));
                return fnAttr.getContent()._fileName;
            }
        }

        return "No Name";
    }

    /**
     * Creates a new attribute.
     *
     * @param type The type of the new attribute.
     * @param name The name of the new attribute.
     * @param indexed Whether the attribute is marked as indexed.
     * @param flags Flags for the new attribute.
     * @return The id of the new attribute.
     */
    public short createAttribute(AttributeType type, String name, boolean indexed, EnumSet<AttributeFlags> flags) {
        short id = _nextAttributeId++;
        _attributes.add(new ResidentAttributeRecord(type, name, id, indexed, flags));
        Collections.sort(_attributes);
        return id;
    }

    /**
     * Creates a new non-resident attribute.
     *
     * @param type The type of the new attribute.
     * @param name The name of the new attribute.
     * @param flags Flags for the new attribute.
     * @return The id of the new attribute.
     */
    public short createNonResidentAttribute(AttributeType type, String name, EnumSet<AttributeFlags> flags) {
        short id = _nextAttributeId++;
        _attributes.add(new NonResidentAttributeRecord(type, name, id, flags, 0, new ArrayList<>()));
        Collections.sort(_attributes);
        return id;
    }

    /**
     * Creates a new attribute.
     *
     * @param type The type of the new attribute.
     * @param name The name of the new attribute.
     * @param flags Flags for the new attribute.
     * @param firstCluster The first cluster to assign to the attribute.
     * @param numClusters The number of sequential clusters to assign to the
     *            attribute.
     * @param bytesPerCluster The number of bytes in each cluster.
     * @return The id of the new attribute.
     */
    public short createNonResidentAttribute(AttributeType type,
                                            String name,
                                            EnumSet<AttributeFlags> flags,
                                            long firstCluster,
                                            long numClusters,
                                            int bytesPerCluster) {
        short id = _nextAttributeId++;
        _attributes.add(new NonResidentAttributeRecord(type, name, id, flags, firstCluster, numClusters, bytesPerCluster));
        Collections.sort(_attributes);
        return id;
    }

    /**
     * Adds an existing attribute.
     *
     * This method is used to move an attribute between different MFT records.
     *
     * @param attrRec The attribute to add.
     * @return The new Id of the attribute.
     */
    public short addAttribute(AttributeRecord attrRec) {
        attrRec.setAttributeId(_nextAttributeId++);
        _attributes.add(attrRec);
        Collections.sort(_attributes);
        return attrRec.getAttributeId();
    }

    /**
     * Removes an attribute by it's id.
     *
     * @param id The attribute's id.
     */
    public void removeAttribute(short id) {
        for (int i = 0; i < _attributes.size(); ++i) {
            if (_attributes.get(i).getAttributeId() == id) {
                _attributes.remove(i);
                break;
            }
        }
    }

    public void reset() {
        _attributes.clear();
        _flags = EnumSet.noneOf(FileRecordFlags.class);
        _hardLinkCount = 0;
        _nextAttributeId = 0;
        _realSize = 0;
    }

    long getAttributeOffset(short id) {
        int firstAttrPos = (short) MathUtilities.roundUp((_haveIndex ? 0x30 : 0x2A) + getUpdateSequenceSize(), 8);

        int offset = firstAttrPos;
        for (AttributeRecord attr : _attributes) {
            if (attr.getAttributeId() == id) {
                return offset;
            }

            offset += attr.getSize();
        }

        return -1;
    }

    void dump(PrintWriter writer, String indent) {
        writer.println(indent + "FILE RECORD (" + toString() + ")");
        writer.println(indent + "              Magic: " + getMagic());
        writer.println(indent + "  Update Seq Offset: " + getUpdateSequenceOffset());
        writer.println(indent + "   Update Seq Count: " + getUpdateSequenceCount());
        writer.println(indent + "  Update Seq Number: " + getUpdateSequenceNumber());
        writer.println(indent + "   Log File Seq Num: " + getLogFileSequenceNumber());
        writer.println(indent + "    Sequence Number: " + getSequenceNumber());
        writer.println(indent + "    Hard Link Count: " + getHardLinkCount());
        writer.println(indent + "              Flags: " + getFlags());
        writer.println(indent + "   Record Real Size: " + getRealSize());
        writer.println(indent + "  Record Alloc Size: " + getAllocatedSize());
        writer.println(indent + "          Base File: " + _baseFile);
        writer.println(indent + "  Next Attribute Id: " + _nextAttributeId);
        writer.println(indent + "    Attribute Count: " + _attributes.size());
        writer.println(indent + "   Index (Self Ref): " + _index);
    }

    protected void read(byte[] buffer, int offset) {
        _logFileSequenceNumber = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x08);
        _sequenceNumber = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x10);
        _hardLinkCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x12);
        _firstAttributeOffset = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x14);
        _flags = FileRecordFlags.valueOf(EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x16));
        _realSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x18);
        _allocatedSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x1C);
        _baseFile = new FileRecordReference(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x20));
        _nextAttributeId = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x28);

        if (getUpdateSequenceOffset() >= 0x30) {
            _index = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x2C);
            _haveIndex = true;
        }

        _attributes = new ArrayList<>();
        int focus = _firstAttributeOffset;
        while (true) {
            int[] length = new int[1];
            AttributeRecord attr = AttributeRecord.fromBytes(buffer, focus, length);
            if (attr == null) {
                break;
            }

            _attributes.add(attr);
            focus += length[0];
        }
    }

    protected short write(byte[] buffer, int offset) {
        short headerEnd = (short) (_haveIndex ? 0x30 : 0x2A);

        _firstAttributeOffset = (short) MathUtilities.roundUp(headerEnd + getUpdateSequenceSize(), 0x08);
        _realSize = calcSize();

        EndianUtilities.writeBytesLittleEndian(_logFileSequenceNumber, buffer, offset + 0x08);
        EndianUtilities.writeBytesLittleEndian(_sequenceNumber, buffer, offset + 0x10);
        EndianUtilities.writeBytesLittleEndian(_hardLinkCount, buffer, offset + 0x12);
        EndianUtilities.writeBytesLittleEndian(_firstAttributeOffset, buffer, offset + 0x14);
        EndianUtilities.writeBytesLittleEndian((short) FileRecordFlags.valueOf(_flags), buffer, offset + 0x16);
        EndianUtilities.writeBytesLittleEndian(_realSize, buffer, offset + 0x18);
        EndianUtilities.writeBytesLittleEndian(_allocatedSize, buffer, offset + 0x1C);
        EndianUtilities.writeBytesLittleEndian(_baseFile.getValue(), buffer, offset + 0x20);
        EndianUtilities.writeBytesLittleEndian(_nextAttributeId, buffer, offset + 0x28);

        if (_haveIndex) {
            EndianUtilities.writeBytesLittleEndian((short) 0, buffer, offset + 0x2A); // Alignment
                                                                                      // field
            EndianUtilities.writeBytesLittleEndian(_index, buffer, offset + 0x2C);
        }

        int pos = _firstAttributeOffset & 0xffff;
        for (AttributeRecord attr : getAttributes()) {
            pos += attr.write(buffer, offset + pos);
        }

        EndianUtilities.writeBytesLittleEndian(0xffffffff, buffer, offset + pos);

        return headerEnd;
    }

    protected int calcSize() {
        int firstAttrPos = (short) MathUtilities.roundUp((_haveIndex ? 0x30 : 0x2A) + getUpdateSequenceSize(), 8);

        int size = firstAttrPos;
        for (AttributeRecord attr : getAttributes()) {
            size += attr.getSize();
        }

        return MathUtilities.roundUp(size + 4, 8); // 0xFFFFFFFF terminator on
                                                   // attributes
    }
}
