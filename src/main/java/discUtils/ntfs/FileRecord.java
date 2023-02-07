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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import discUtils.streams.util.MathUtilities;
import dotnet4j.util.compat.Utilities;
import vavi.util.ByteUtil;


public class FileRecord extends FixupRecordBase {

    private short firstAttributeOffset;

    private boolean haveIndex;

    // Self-reference (on XP+)
    private int index;

    public FileRecord(int sectorSize) {
        super("FILE", sectorSize);
    }

    public FileRecord(int sectorSize, int recordLength, int index) {
        super("FILE", sectorSize, recordLength);
        reInitialize(sectorSize, recordLength, index);
    }

    private int allocatedSize;

    public int getAllocatedSize() {
        return allocatedSize;
    }

    public void setAllocatedSize(int value) {
        allocatedSize = value;
    }

    private List<AttributeRecord> attributes;

    public List<AttributeRecord> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<AttributeRecord> value) {
        attributes = value;
    }

    private FileRecordReference baseFile;

    public FileRecordReference getBaseFile() {
        return baseFile;
    }

    public void setBaseFile(FileRecordReference value) {
        baseFile = value;
    }

    public AttributeRecord getFirstAttribute() {
        return attributes.size() > 0 ? attributes.get(0) : null;
    }

    private EnumSet<FileRecordFlags> flags;

    public EnumSet<FileRecordFlags> getFlags() {
        return flags;
    }

    public void setFlags(EnumSet<FileRecordFlags> value) {
        flags = value;
    }

    private short hardLinkCount;

    public int getHardLinkCount() {
        return hardLinkCount & 0xffff;
    }

    public void setHardLinkCount(short value) {
        hardLinkCount = value;
    }

    public boolean getIsMftRecord() {
        return getMasterFileTableIndex() == MasterFileTable.MftIndex ||
               (baseFile.getMftIndex() == MasterFileTable.MftIndex && baseFile.getSequenceNumber() != 0);
    }

    private int loadedIndex;

    public int getLoadedIndex() {
        return loadedIndex;
    }

    public void setLoadedIndex(int value) {
        loadedIndex = value;
    }

    private long logFileSequenceNumber;

    public long getLogFileSequenceNumber() {
        return logFileSequenceNumber;
    }

    public void setLogFileSequenceNumber(long value) {
        logFileSequenceNumber = value;
    }

    public int getMasterFileTableIndex() {
        return haveIndex ? index : loadedIndex;
    }

    private short nextAttributeId;

    public int getNextAttributeId() {
        return nextAttributeId & 0xffff;
    }

    public void setNextAttributeId(short value) {
        nextAttributeId = value;
    }

    private int realSize;

    public int getRealSize() {
        return realSize;
    }

    public void setRealSize(int value) {
        realSize = value;
    }

    public FileRecordReference getReference() {
        return new FileRecordReference(getMasterFileTableIndex(), sequenceNumber);
    }

    private short sequenceNumber;

    public int getSequenceNumber() {
        return sequenceNumber & 0xffff;
    }

    public void setSequenceNumber(short value) {
        sequenceNumber = value;
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
        sequenceNumber++; // TODO
        assert sequenceNumber > 0;
        flags = EnumSet.noneOf(FileRecordFlags.class);
        allocatedSize = recordLength;
        nextAttributeId = 0;
        this.index = index;
        hardLinkCount = 0;
        baseFile = new FileRecordReference(0);

        attributes = new ArrayList<>();
        haveIndex = true;
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
            if (attrRec.getAttributeType() == type && Utilities.equals(attrRec.getName(), name)) {
                return attrRec;
            }
        }

        return null;
    }

    @Override public String toString() {
        for (AttributeRecord attr : getAttributes()) {
            if (attr.getAttributeType() == AttributeType.FileName) {
                @SuppressWarnings("unchecked")
                StructuredNtfsAttribute<FileNameRecord> fnAttr = (StructuredNtfsAttribute<FileNameRecord>) NtfsAttribute.fromRecord(null, new FileRecordReference(0), attr);
                return fnAttr.getContent().fileName;
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
     * @param flags flags for the new attribute.
     * @return The id of the new attribute.
     */
    public short createAttribute(AttributeType type, String name, boolean indexed, EnumSet<AttributeFlags> flags) {
        short id = nextAttributeId++;
        attributes.add(new ResidentAttributeRecord(type, name, id, indexed, flags));
        Collections.sort(attributes);
        return id;
    }

    /**
     * Creates a new non-resident attribute.
     *
     * @param type The type of the new attribute.
     * @param name The name of the new attribute.
     * @param flags flags for the new attribute.
     * @return The id of the new attribute.
     */
    public short createNonResidentAttribute(AttributeType type, String name, EnumSet<AttributeFlags> flags) {
        short id = nextAttributeId++;
        attributes.add(new NonResidentAttributeRecord(type, name, id, flags, 0, new ArrayList<>()));
        Collections.sort(attributes);
        return id;
    }

    /**
     * Creates a new attribute.
     *
     * @param type The type of the new attribute.
     * @param name The name of the new attribute.
     * @param flags flags for the new attribute.
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
        short id = nextAttributeId++;
        attributes.add(new NonResidentAttributeRecord(type, name, id, flags, firstCluster, numClusters, bytesPerCluster));
        Collections.sort(attributes);
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
        attrRec.setAttributeId(nextAttributeId++);
        attributes.add(attrRec);
        Collections.sort(attributes);
        return attrRec.getAttributeId();
    }

    /**
     * Removes an attribute by it's id.
     *
     * @param id The attribute's id.
     */
    public void removeAttribute(short id) {
        for (int i = 0; i < attributes.size(); ++i) {
            if (attributes.get(i).getAttributeId() == id) {
                attributes.remove(i);
                break;
            }
        }
    }

    public void reset() {
        attributes.clear();
        flags = EnumSet.noneOf(FileRecordFlags.class);
        hardLinkCount = 0;
        nextAttributeId = 0;
        realSize = 0;
    }

    long getAttributeOffset(short id) {
        int firstAttrPos = (short) MathUtilities.roundUp((haveIndex ? 0x30 : 0x2A) + getUpdateSequenceSize(), 8);

        int offset = firstAttrPos;
        for (AttributeRecord attr : attributes) {
            if (attr.getAttributeId() == id) {
                return offset;
            }

            offset += attr.getSize();
        }

        return -1;
    }

    void dump(PrintWriter writer, String indent) {
        writer.println(indent + "FILE RECORD (" + this + ")");
        writer.println(indent + "              Magic: " + getMagic());
        writer.println(indent + "  Update Seq Offset: " + getUpdateSequenceOffset());
        writer.println(indent + "   Update Seq Count: " + getUpdateSequenceCount());
        writer.println(indent + "  Update Seq Number: " + getUpdateSequenceNumber());
        writer.println(indent + "   Log File Seq Num: " + getLogFileSequenceNumber());
        writer.println(indent + "    Sequence Number: " + getSequenceNumber());
        writer.println(indent + "    Hard Link Count: " + getHardLinkCount());
        writer.println(indent + "              flags: " + getFlags());
        writer.println(indent + "   Record Real Size: " + getRealSize());
        writer.println(indent + "  Record Alloc Size: " + getAllocatedSize());
        writer.println(indent + "          base File: " + baseFile);
        writer.println(indent + "  Next Attribute Id: " + nextAttributeId);
        writer.println(indent + "    Attribute Count: " + attributes.size());
        writer.println(indent + "   Index (Self Ref): " + index);
    }

    @Override protected void read(byte[] buffer, int offset) {
        logFileSequenceNumber = ByteUtil.readLeLong(buffer, offset + 0x08);
        sequenceNumber = ByteUtil.readLeShort(buffer, offset + 0x10);
        hardLinkCount = ByteUtil.readLeShort(buffer, offset + 0x12);
        firstAttributeOffset = ByteUtil.readLeShort(buffer, offset + 0x14);
        flags = FileRecordFlags.valueOf(ByteUtil.readLeShort(buffer, offset + 0x16));
        realSize = ByteUtil.readLeInt(buffer, offset + 0x18);
        allocatedSize = ByteUtil.readLeInt(buffer, offset + 0x1C);
        baseFile = new FileRecordReference(ByteUtil.readLeLong(buffer, offset + 0x20));
        nextAttributeId = ByteUtil.readLeShort(buffer, offset + 0x28);

        if (getUpdateSequenceOffset() >= 0x30) {
            index = ByteUtil.readLeInt(buffer, offset + 0x2C);
            haveIndex = true;
        }

        attributes = new ArrayList<>();
        int focus = firstAttributeOffset & 0xffff;
        while (true) {
            int[] length = new int[1];
            AttributeRecord attr = AttributeRecord.fromBytes(buffer, focus, length);
            if (attr == null) {
                break;
            }

            attributes.add(attr);
            focus += length[0];
        }
    }

    @Override protected short write(byte[] buffer, int offset) {
        short headerEnd = (short) (haveIndex ? 0x30 : 0x2A);

        firstAttributeOffset = (short) MathUtilities.roundUp(headerEnd + getUpdateSequenceSize(), 0x08);
        realSize = calcSize();

        ByteUtil.writeLeLong(logFileSequenceNumber, buffer, offset + 0x08);
        ByteUtil.writeLeShort(sequenceNumber, buffer, offset + 0x10);
        ByteUtil.writeLeShort(hardLinkCount, buffer, offset + 0x12);
        ByteUtil.writeLeShort(firstAttributeOffset, buffer, offset + 0x14);
        ByteUtil.writeLeShort((short) FileRecordFlags.valueOf(flags), buffer, offset + 0x16);
        ByteUtil.writeLeInt(realSize, buffer, offset + 0x18);
        ByteUtil.writeLeInt(allocatedSize, buffer, offset + 0x1C);
        ByteUtil.writeLeLong(baseFile.getValue(), buffer, offset + 0x20);
        ByteUtil.writeLeShort(nextAttributeId, buffer, offset + 0x28);

        if (haveIndex) {
            ByteUtil.writeLeShort((short) 0, buffer, offset + 0x2A); // Alignment field
            ByteUtil.writeLeShort((short) index, buffer, offset + 0x2C);
        }

        int pos = firstAttributeOffset & 0xffff;
        for (AttributeRecord attr : getAttributes()) {
            pos += attr.write(buffer, offset + pos);
        }

        ByteUtil.writeLeInt(0xffff_ffff, buffer, offset + pos);

        return headerEnd;
    }

    @Override protected int calcSize() {
        int firstAttrPos = (short) MathUtilities.roundUp((haveIndex ? 0x30 : 0x2A) + getUpdateSequenceSize(), 8);

        int size = firstAttrPos;
        for (AttributeRecord attr : getAttributes()) {
            size += attr.getSize();
        }

        return MathUtilities.roundUp(size + 4, 8); // 0xFFFFFFFF terminator on attributes
    }
}
