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

    private int __AllocatedSize;

    public int getAllocatedSize() {
        return __AllocatedSize;
    }

    public void setAllocatedSize(int value) {
        __AllocatedSize = value;
    }

    private List<AttributeRecord> __Attributes;

    public List<AttributeRecord> getAttributes() {
        return __Attributes;
    }

    public void setAttributes(List<AttributeRecord> value) {
        __Attributes = value;
    }

    private FileRecordReference __BaseFile = new FileRecordReference();

    public FileRecordReference getBaseFile() {
        return __BaseFile;
    }

    public void setBaseFile(FileRecordReference value) {
        __BaseFile = value;
    }

    public AttributeRecord getFirstAttribute() {
        return getAttributes().size() > 0 ? getAttributes().get(0) : null;
    }

    private EnumSet<FileRecordFlags> __Flags;

    public EnumSet<FileRecordFlags> getFlags() {
        return __Flags;
    }

    public void setFlags(EnumSet<FileRecordFlags> value) {
        __Flags = value;
    }

    private short __HardLinkCount;

    public short getHardLinkCount() {
        return __HardLinkCount;
    }

    public void setHardLinkCount(short value) {
        __HardLinkCount = value;
    }

    public boolean getIsMftRecord() {
        return getMasterFileTableIndex() == MasterFileTable.MftIndex ||
               (getBaseFile().getMftIndex() == MasterFileTable.MftIndex && getBaseFile().getSequenceNumber() != 0);
    }

    private int __LoadedIndex;

    public int getLoadedIndex() {
        return __LoadedIndex;
    }

    public void setLoadedIndex(int value) {
        __LoadedIndex = value;
    }

    private long __LogFileSequenceNumber;

    public long getLogFileSequenceNumber() {
        return __LogFileSequenceNumber;
    }

    public void setLogFileSequenceNumber(long value) {
        __LogFileSequenceNumber = value;
    }

    public int getMasterFileTableIndex() {
        return _haveIndex ? _index : getLoadedIndex();
    }

    private short __NextAttributeId;

    public short getNextAttributeId() {
        return __NextAttributeId;
    }

    public void setNextAttributeId(short value) {
        __NextAttributeId = value;
    }

    private int __RealSize;

    public int getRealSize() {
        return __RealSize;
    }

    public void setRealSize(int value) {
        __RealSize = value;
    }

    public FileRecordReference getReference() {
        return new FileRecordReference(getMasterFileTableIndex(), getSequenceNumber());
    }

    private short __SequenceNumber;

    public short getSequenceNumber() {
        return __SequenceNumber;
    }

    public void setSequenceNumber(short value) {
        __SequenceNumber = value;
    }

    public static EnumSet<FileAttributeFlags> convertFlags(EnumSet<FileRecordFlags> source) {
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
        setSequenceNumber((short) (getSequenceNumber() + 1));
        setFlags(EnumSet.noneOf(FileRecordFlags.class));
        setAllocatedSize(recordLength);
        setNextAttributeId((short) 0);
        _index = index;
        setHardLinkCount((short) 0);
        setBaseFile(new FileRecordReference(0));
        setAttributes(new ArrayList<>());
        _haveIndex = true;
    }

    /**
     * Gets an attribute by it's id.
     *
     * @param id The attribute's id.
     * @return The attribute, or
     *         {@code null}
     *         .
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
     * @return The attribute, or
     *         {@code null}
     *         .
     */
    public AttributeRecord getAttribute(AttributeType type) {
        return getAttribute(type, null);
    }

    /**
     * Gets an named attribute.
     *
     * @param type The attribute type.
     * @param name The name of the attribute.
     * @return The attribute, or
     *         {@code null}
     *         .
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
                return fnAttr.getContent().FileName;
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
        setNextAttributeId((short) (getNextAttributeId() + 1));
        short id = getNextAttributeId();
        getAttributes().add(new ResidentAttributeRecord(type, name, id, indexed, flags));
        Collections.sort(getAttributes());
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
        setNextAttributeId((short) (getNextAttributeId() + 1));
        short id = getNextAttributeId();
        getAttributes().add(new NonResidentAttributeRecord(type, name, id, flags, 0, new ArrayList<>()));
        Collections.sort(getAttributes());
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
        setNextAttributeId((short) (getNextAttributeId() + 1));
        short id = getNextAttributeId();
        getAttributes().add(new NonResidentAttributeRecord(type, name, id, flags, firstCluster, numClusters, bytesPerCluster));
        Collections.sort(getAttributes());
        return id;
    }

    /**
     * Adds an existing attribute.
     *
     * @param attrRec The attribute to add.
     * @return The new Id of the attribute.This method is used to move an
     *         attribute between different MFT records.
     */
    public short addAttribute(AttributeRecord attrRec) {
        setNextAttributeId((short) (getNextAttributeId() + 1));
        attrRec.setAttributeId(getNextAttributeId());
        getAttributes().add(attrRec);
        Collections.sort(getAttributes());
        return attrRec.getAttributeId();
    }

    /**
     * Removes an attribute by it's id.
     *
     * @param id The attribute's id.
     */
    public void removeAttribute(short id) {
        for (int i = 0; i < getAttributes().size(); ++i) {
            if (getAttributes().get(i).getAttributeId() == id) {
                getAttributes().remove(i);
                break;
            }
        }
    }

    public void reset() {
        getAttributes().clear();
        setFlags(EnumSet.noneOf(FileRecordFlags.class));
        setHardLinkCount((short) 0);
        setNextAttributeId((short) 0);
        setRealSize(0);
    }

    public long getAttributeOffset(short id) {
        int firstAttrPos = (short) MathUtilities.roundUp((_haveIndex ? 0x30 : 0x2A) + getUpdateSequenceSize(), 8);
        int offset = firstAttrPos;
        for (AttributeRecord attr : getAttributes()) {
            if (attr.getAttributeId() == id) {
                return offset;
            }

            offset += attr.getSize();
        }
        return -1;
    }

    public void dump(PrintWriter writer, String indent) {
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
        writer.println(indent + "          Base File: " + getBaseFile());
        writer.println(indent + "  Next Attribute Id: " + getNextAttributeId());
        writer.println(indent + "    Attribute Count: " + getAttributes().size());
        writer.println(indent + "   Index (Self Ref): " + _index);
    }

    protected void read(byte[] buffer, int offset) {
        setLogFileSequenceNumber(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x08));
        setSequenceNumber((short) EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x10));
        setHardLinkCount((short) EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x12));
        _firstAttributeOffset = (short) EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x14);
        setFlags(FileRecordFlags.valueOf(EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x16)));
        setRealSize(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x18));
        setAllocatedSize(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x1C));
        setBaseFile(new FileRecordReference(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x20)));
        setNextAttributeId((short) EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x28));
        if (getUpdateSequenceOffset() >= 0x30) {
            _index = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x2C);
            _haveIndex = true;
        }

        setAttributes(new ArrayList<>());
        int focus = _firstAttributeOffset;
        while (true) {
            int[] length = new int[1];
            AttributeRecord attr = AttributeRecord.fromBytes(buffer, focus, length);
            if (attr == null) {
                break;
            }

            getAttributes().add(attr);
            focus += length[0];
        }
    }

    protected short write(byte[] buffer, int offset) {
        short headerEnd = (short) (_haveIndex ? 0x30 : 0x2A);
        _firstAttributeOffset = (short) MathUtilities.roundUp(headerEnd + getUpdateSequenceSize(), 0x08);
        setRealSize(calcSize());
        EndianUtilities.writeBytesLittleEndian(getLogFileSequenceNumber(), buffer, offset + 0x08);
        EndianUtilities.writeBytesLittleEndian(getSequenceNumber(), buffer, offset + 0x10);
        EndianUtilities.writeBytesLittleEndian(getHardLinkCount(), buffer, offset + 0x12);
        EndianUtilities.writeBytesLittleEndian(_firstAttributeOffset, buffer, offset + 0x14);
        EndianUtilities.writeBytesLittleEndian((short) FileRecordFlags.valueOf(getFlags()), buffer, offset + 0x16);
        EndianUtilities.writeBytesLittleEndian(getRealSize(), buffer, offset + 0x18);
        EndianUtilities.writeBytesLittleEndian(getAllocatedSize(), buffer, offset + 0x1C);
        EndianUtilities.writeBytesLittleEndian(getBaseFile().getValue(), buffer, offset + 0x20);
        EndianUtilities.writeBytesLittleEndian(getNextAttributeId(), buffer, offset + 0x28);
        if (_haveIndex) {
            EndianUtilities.writeBytesLittleEndian((short) 0, buffer, offset + 0x2A);
            // Alignment field
            EndianUtilities.writeBytesLittleEndian(_index, buffer, offset + 0x2C);
        }

        int pos = _firstAttributeOffset;
        for (AttributeRecord attr : getAttributes()) {
            pos += attr.write(buffer, offset + pos);
        }
        EndianUtilities.writeBytesLittleEndian(Integer.MAX_VALUE, buffer, offset + pos);
        return headerEnd;
    }

    protected int calcSize() {
        int firstAttrPos = (short) MathUtilities.roundUp((_haveIndex ? 0x30 : 0x2A) + getUpdateSequenceSize(), 8);
        int size = firstAttrPos;
        for (AttributeRecord attr : getAttributes()) {
            size += attr.getSize();
        }
        return MathUtilities.roundUp(size + 4, 8);
    }

}

// 0xFFFFFFFF terminator on attributes
