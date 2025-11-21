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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import discUtils.core.internal.ObjectCache;
import discUtils.core.internal.Utilities;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.buffer.IBuffer;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileNotFoundException;
import dotnet4j.io.SeekOrigin;


class File {

    private final List<NtfsAttribute> attributes;

    protected final INtfsContext context;

    private final ObjectCache<String, Index> indexCache;

    private final MasterFileTable mft;

    private final List<FileRecord> records;

    public File(INtfsContext context, FileRecord baseRecord) {
        this.context = context;
        mft = this.context.getMft();
        records = new ArrayList<>();
        records.add(baseRecord);
        indexCache = new ObjectCache<>();
        attributes = new ArrayList<>();
        loadAttributes();
    }

    /**
     * Gets an enumeration of all the attributes.
     */
    List<NtfsAttribute> getAllAttributes() {
        return attributes;
    }

    public List<NtfsStream> getAllStreams() {
        List<NtfsStream> result = new ArrayList<>();
        for (NtfsAttribute attr : attributes) {
            result.add(new NtfsStream(this, attr));
        }
        return result;
    }

    public String getBestName() {
        List<NtfsAttribute> attrs = getAttributes(AttributeType.FileName);
        String bestName = null;
        if (attrs != null && !attrs.isEmpty()) {
            bestName = attrs.get(0).toString();
            for (int i = 1; i < attrs.size(); ++i) {
                String name = attrs.get(i).toString();
                if (Utilities.is8Dot3(bestName)) {
                    bestName = name;
                }
            }
        }

        return bestName;
    }

    INtfsContext getContext() {
        return context;
    }

    public DirectoryEntry getDirectoryEntry() {
        if (context.getGetDirectoryByRef() == null) {
            return null;
        }

        NtfsStream stream = getStream(AttributeType.FileName, null);
        if (stream == null) {
            return null;
        }

        FileNameRecord record = stream.getContent(FileNameRecord.class);
        // Root dir is stored without root directory flag set in FileNameRecord,
        // simulate it.
        if (records.get(0).getMasterFileTableIndex() == MasterFileTable.RootDirIndex) {
            record.flags.add(FileAttributeFlags.Directory);
        }

        return new DirectoryEntry(context.getGetDirectoryByRef().invoke(record.parentDirectory), getMftReference(), record);
    }

    public int getHardLinkCount() {
        return records.get(0).getHardLinkCount();
    }

    public void setHardLinkCount(short value) {
        records.get(0).setHardLinkCount(value);
    }

    public boolean getHasWin32OrDosName() {
        for (Object _attr : getAttributes(AttributeType.FileName)) { // need cast
            @SuppressWarnings({"unchecked"})
            StructuredNtfsAttribute<FileNameRecord> attr = (StructuredNtfsAttribute<FileNameRecord>) _attr;
            FileNameRecord fnr = attr.getContent();
            if (fnr.fileNameNamespace != FileNameNamespace.Posix) {
                return true;
            }
        }
        return false;
    }

    public int getIndexInMft() {
        return records.get(0).getMasterFileTableIndex();
    }

    public boolean isDirectory() {
        return records.get(0).getFlags().contains(FileRecordFlags.IsDirectory);
    }

    public int getMaxMftRecordSize() {
        return records.get(0).getAllocatedSize();
    }

    private boolean mftRecordIsDirty;

    public boolean getMftRecordIsDirty() {
        return mftRecordIsDirty;
    }

    public void setMftRecordIsDirty(boolean value) {
        mftRecordIsDirty = value;
    }

    public FileRecordReference getMftReference() {
        return records.get(0).getReference();
    }

    public List<String> getNames() {
        List<String> result = new ArrayList<>();
        if (getIndexInMft() == MasterFileTable.RootDirIndex) {
            result.add("");
        } else {
            for (Object _attr : getAttributes(AttributeType.FileName)) { // need cast
                @SuppressWarnings({"unchecked"})
                StructuredNtfsAttribute<FileNameRecord> attr = (StructuredNtfsAttribute<FileNameRecord>) _attr;
                String name = attr.getContent().fileName;
                Directory parentDir = context.getGetDirectoryByRef().invoke(attr.getContent().parentDirectory);
                if (parentDir != null) {
                    for (String dirName : parentDir.getNames()) {
                        result.add(Utilities.combinePaths(dirName, name));
                    }
                }
            }
        }
        return result;
    }

    public StandardInformation getStandardInformation() {
        return getStream(AttributeType.StandardInformation, null).getContent(StandardInformation.class);
    }

    public static File createNew(INtfsContext context, EnumSet<FileAttributeFlags> dirFlags) {
        return createNew(context, EnumSet.noneOf(FileRecordFlags.class), dirFlags);
    }

    public static File createNew(INtfsContext context, EnumSet<FileRecordFlags> flags, EnumSet<FileAttributeFlags> dirFlags) {
        File newFile = context.getAllocateFile().invoke(flags);

        EnumSet<FileAttributeFlags> fileFlags = EnumSet.of(FileAttributeFlags.Archive);
        fileFlags.addAll(FileRecord.convertFlags(flags));
        if (dirFlags.contains(FileAttributeFlags.Compressed)) {
            fileFlags.add(FileAttributeFlags.Compressed);
        }

        EnumSet<AttributeFlags> dataAttrFlags = EnumSet.noneOf(AttributeFlags.class);
        if (dirFlags.contains(FileAttributeFlags.Compressed)) {
            dataAttrFlags.add(AttributeFlags.Compressed);
        }

        StandardInformation.initializeNewFile(newFile, fileFlags);

        if (context.getObjectIds() != null) {
            UUID newId = createNewGuid(context);
            NtfsStream stream = newFile.createStream(AttributeType.ObjectId, null);
            ObjectId objId = new ObjectId();
            objId.Id = newId;
            stream.setContent(objId);
            context.getObjectIds().add(newId, newFile.getMftReference(), newId, new UUID(0L, 0L), new UUID(0L, 0L));
        }

        newFile.createAttribute(AttributeType.Data, dataAttrFlags);

        newFile.updateRecordInMft();

        return newFile;
    }

    public int mftRecordFreeSpace(AttributeType attrType, String attrName) {
        for (FileRecord record : records) {
            if (record.getAttribute(attrType, attrName) != null) {
                return mft.getRecordSize() - (int) record.getSize();
            }
        }
        throw new dotnet4j.io.IOException("Attempt to determine free space for non-existent attribute");
    }

    public void modified() {
        long now = System.currentTimeMillis();
        NtfsStream siStream = getStream(AttributeType.StandardInformation, null);
        StandardInformation si = siStream.getContent(StandardInformation.class);
        si.lastAccessTime = now;
        si.modificationTime = now;
        siStream.setContent(si);
        markMftRecordDirty();
    }

    public void accessed() {
        long now = System.currentTimeMillis();
        NtfsStream siStream = getStream(AttributeType.StandardInformation, null);
        StandardInformation si = siStream.getContent(StandardInformation.class);
        si.lastAccessTime = now;
        siStream.setContent(si);
        markMftRecordDirty();
    }

    public void markMftRecordDirty() {
        setMftRecordIsDirty(true);
    }

    public void updateRecordInMft() {
        if (getMftRecordIsDirty()) {
            if (NtfsTransaction.getCurrent() != null) {
                NtfsStream stream = getStream(AttributeType.StandardInformation, null);
                StandardInformation si = stream.getContent(StandardInformation.class);
                si.mftChangedTime = NtfsTransaction.getCurrent().getTimestamp();
                stream.setContent(si);
            }

            boolean fixesApplied = true;
            while (fixesApplied) {
                fixesApplied = false;

                for (int i = 0; i < records.size(); ++i) {
                    FileRecord record = records.get(i);

                    boolean fixedAttribute = true;
                    while (record.getSize() > mft.getRecordSize() && fixedAttribute) {
                        fixedAttribute = false;

                        if (!fixedAttribute && !record.getIsMftRecord()) {
                            for (AttributeRecord attr : record.getAttributes()) {
                                if (!attr.isNonResident()
                                        && !context.getAttributeDefinitions().mustBeResident(attr.getAttributeType())) {
                                    makeAttributeNonResident(new AttributeReference(record.getReference(),
                                                                                    attr.getAttributeId()),
                                                             (int) attr.getDataLength());
                                    fixedAttribute = true;
                                    break;
                                }
                            }
                        }

                        if (!fixedAttribute) {
                            for (AttributeRecord attr : record.getAttributes()) {
                                if (attr.getAttributeType() == AttributeType.IndexRoot && shrinkIndexRoot(attr.getName())) {
                                    fixedAttribute = true;
                                    break;
                                }
                            }
                        }

                        if (!fixedAttribute) {
                            if (record.getAttributes().size() == 1) {
                                fixedAttribute = splitAttribute(record);
                            } else {
                                if (records.size() == 1) {
                                    createAttributeList();
                                }

                                fixedAttribute = expelAttribute(record);
                            }
                        }

                        fixesApplied |= fixedAttribute;
                    }
                }
            }

            setMftRecordIsDirty(false);
            for (FileRecord record : records) {
                mft.writeRecord(record);
            }
        }
    }

    public Index createIndex(String name, AttributeType attrType, AttributeCollationRule collRule) {
        Index.create(attrType, collRule, this, name);
        return getIndex(name);
    }

    public Index getIndex(String name) {
        Index idx = indexCache.get(name);
        if (idx == null) {
            idx = new Index(this, name, context.getBiosParameterBlock(), context.getUpperCase());
            indexCache.put(name, idx);
        }

        return idx;
    }

    public void delete() {
        if (records.get(0).getHardLinkCount() != 0) {
            throw new UnsupportedOperationException("Attempt to delete in-use file: " + this);
        }

        context.getForgetFile().invoke(this);
        NtfsStream objIdStream = getStream(AttributeType.ObjectId, null);
        if (objIdStream != null) {
            ObjectId objId = objIdStream.getContent(ObjectId.class);
            getContext().getObjectIds().remove(objId.Id);
        }

        // Truncate attributes, allowing for truncation silently removing the
        // AttributeList attribute
        // in some cases (large file with all attributes first extent in the first MFT
        // record). This
        // releases all allocated clusters in most cases.
        List<NtfsAttribute> truncateAttrs = new ArrayList<>(attributes.size());
        for (NtfsAttribute attr : attributes) {
            if (attr.getType() != AttributeType.AttributeList) {
                truncateAttrs.add(attr);
            }

        }
        for (NtfsAttribute attr : truncateAttrs) {
            attr.getDataBuffer().setCapacity(0);
        }
        // If the attribute list record remains, free any possible clusters it owns.
        // We've now freed
        // all clusters.
        NtfsAttribute attrList = getAttribute(AttributeType.AttributeList, null);
        if (attrList != null) {
            attrList.getDataBuffer().setCapacity(0);
        }

        for (FileRecord mftRecord : records) {
            // Now go through the MFT records, freeing them up
            context.getMft().removeRecord(mftRecord.getReference());
        }
        attributes.clear();
        records.clear();
    }

    public boolean streamExists(AttributeType attrType, String name) {
        return getStream(attrType, name) != null;
    }

    public NtfsStream getStream(AttributeType attrType, String name) {
        for (NtfsStream stream : getStreams(attrType, name)) {
            return stream;
        }
        return null;
    }

    public List<NtfsStream> getStreams(AttributeType attrType, String name) {
        List<NtfsStream> result = new ArrayList<>();
        for (NtfsAttribute attr : attributes) {
            if (attr.getType() == attrType && dotnet4j.util.compat.Utilities.equals(attr.getName(), name)) {
                result.add(new NtfsStream(this, attr));
            }
        }
        return result;
    }

    public NtfsStream createStream(AttributeType attrType, String name) {
        return new NtfsStream(this, createAttribute(attrType, name, EnumSet.noneOf(AttributeFlags.class)));
    }

    public NtfsStream createStream(AttributeType attrType,
                                   String name,
                                   long firstCluster,
                                   long numClusters,
                                   int bytesPerCluster) {
        return new NtfsStream(this,
                              createAttribute(attrType,
                                              name,
                                              EnumSet.noneOf(AttributeFlags.class),
                                              firstCluster,
                                              numClusters,
                                              bytesPerCluster));
    }

    public SparseStream openStream(AttributeType attrType, String name, FileAccess access) {
        NtfsAttribute attr = getAttribute(attrType, name);
        if (attr != null) {
            return new FileStream(this, attr, access);
        }

        return null;
    }

    public void removeStream(NtfsStream stream) {
        removeAttribute(stream.getAttribute());
    }

    @SuppressWarnings("unchecked")
    public FileNameRecord getFileNameRecord(String name, boolean freshened) {
        List<NtfsAttribute> attrs = getAttributes(AttributeType.FileName);
        StructuredNtfsAttribute<FileNameRecord> attr = null;
        if (name == null || name.isEmpty()) {
            if (!attrs.isEmpty()) {
                attr = (StructuredNtfsAttribute<FileNameRecord>) attrs.get(0);
            }
        } else {
            for (Object _a : attrs) { // need cast
                StructuredNtfsAttribute<FileNameRecord> a = (StructuredNtfsAttribute<FileNameRecord>) _a;
                if (context.getUpperCase().compare(a.getContent().fileName, name) == 0) {
                    attr = a;
                }
            }
            if (attr == null) {
                throw new FileNotFoundException("File name not found on file: " + name);
            }
        }
        FileNameRecord fnr = attr == null ? new FileNameRecord() : new FileNameRecord(attr.getContent());
        if (freshened) {
            freshenFileName(fnr, false);
        }

        return fnr;
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "FILE (" + this + ")");
        writer.println(indent + "  File Number: " + records.get(0).getMasterFileTableIndex());
        records.get(0).dump(writer, indent + "  ");
        for (AttributeRecord attrRec : records.get(0).getAttributes()) {
            NtfsAttribute.fromRecord(this, getMftReference(), attrRec).dump(writer, indent + "  ");
        }
    }

    public String toString() {
        String bestName = getBestName();
        if (bestName == null) {
            return "?????";
        }
        return bestName;
    }

    void removeAttributeExtents(NtfsAttribute attr) {
        attr.getDataBuffer().setCapacity(0);
        for (AttributeReference extentRef : attr.getExtents().keySet()) {
            removeAttributeExtent(extentRef);
        }
    }

    void removeAttributeExtent(AttributeReference extentRef) {
        FileRecord fileRec = getFileRecord(extentRef.getFile());
        if (fileRec != null) {
            fileRec.removeAttribute(extentRef.getAttributeId());
            // Remove empty non-primary MFT records
            if (fileRec.getAttributes().isEmpty() && fileRec.getBaseFile().getValue() != 0) {
                removeFileRecord(extentRef.getFile());
            }
        }
    }

    /**
     * Gets an attribute by reference.
     *
     * @param attrRef Reference to the attribute.
     * @return The attribute.
     */
    NtfsAttribute getAttribute(AttributeReference attrRef) {
        for (NtfsAttribute attr : attributes) {
            if (attr.getReference().equals(attrRef)) {
                return attr;
            }
        }
        return null;
    }

    /**
     * Gets the first (if more than one) instance of a named attribute.
     *
     * @param type The attribute type.
     * @param name The attribute's name.
     * @return The attribute of {@code null} .
     */
    NtfsAttribute getAttribute(AttributeType type, String name) {
        for (NtfsAttribute attr : attributes) {
            if (attr.getPrimaryRecord().getAttributeType() == type && dotnet4j.util.compat.Utilities.equals(attr.getName(), name)) {
                return attr;
            }
        }
        return null;
    }

    /**
     * Gets all instances of an unnamed attribute.
     *
     * @param type The attribute type.
     * @return The attributes.
     */
    List<NtfsAttribute> getAttributes(AttributeType type) {
        List<NtfsAttribute> matches = new ArrayList<>();
        for (NtfsAttribute attr : attributes) {
            if (attr.getPrimaryRecord().getAttributeType() == type && (attr.getName() == null || attr.getName().isEmpty())) {
                matches.add(attr);
            }

        }
        return matches;
    }

    void makeAttributeNonResident(AttributeReference attrRef, int maxData) {
        NtfsAttribute attr = getAttribute(attrRef);
        if (attr.isNonResident()) {
            throw new UnsupportedOperationException("Attribute is already non-resident");
        }

        short id = records.get(0).createNonResidentAttribute(attr.getType(), attr.getName(), attr.getFlags());
        AttributeRecord newAttrRecord = records.get(0).getAttribute(id);
        IBuffer attrBuffer = attr.getDataBuffer();
        byte[] tempData = StreamUtilities.readExact(attrBuffer, 0, (int) Math.min(maxData, attrBuffer.getCapacity()));
        removeAttributeExtents(attr);
        attr.setExtent(records.get(0).getReference(), newAttrRecord);
        attr.getDataBuffer().write(0, tempData, 0, tempData.length);
        updateAttributeList();
    }

    void freshenFileName(FileNameRecord fileName, boolean updateMftRecord) {
        //
        // Freshen the record from the definitive info in the other attributes
        //
        StandardInformation si = getStandardInformation();
        NtfsAttribute anonDataAttr = getAttribute(AttributeType.Data, null);

        fileName.creationTime = si.creationTime;
        fileName.modificationTime = si.modificationTime;
        fileName.mftChangedTime = si.mftChangedTime;
        fileName.lastAccessTime = si.lastAccessTime;
        fileName.flags = si.fileAttributeFlags;

        if (getMftRecordIsDirty() && NtfsTransaction.getCurrent() != null) {
            fileName.mftChangedTime = NtfsTransaction.getCurrent().getTimestamp();
        }

        // Directories don't have directory flag set in StandardInformation, so set from
        // MFT record
        if (records.get(0).getFlags().contains(FileRecordFlags.IsDirectory)) {
            fileName.flags.add(FileAttributeFlags.Directory);
        }

        if (anonDataAttr != null) {
            fileName.realSize = anonDataAttr.getPrimaryRecord().getDataLength();
            fileName.allocatedSize = anonDataAttr.getPrimaryRecord().getAllocatedLength();
        }

        if (updateMftRecord) {
            for (NtfsStream stream : getStreams(AttributeType.FileName, null)) {
                FileNameRecord fnr = stream.getContent(FileNameRecord.class);
                if (fnr.equals(fileName)) {
                    fnr = new FileNameRecord(fileName);
                    fnr.flags.remove(FileAttributeFlags.ReparsePoint);
                    stream.setContent(fnr);
                }
            }
        }
    }

    long getAttributeOffset(AttributeReference attrRef) {
        long recordOffset = mft.getRecordOffset(attrRef.getFile());
        FileRecord frs = getFileRecord(attrRef.getFile());
        return recordOffset + frs.getAttributeOffset(attrRef.getAttributeId());
    }

    // TODO to be deprecated, use UUID.randomUUID()
    private static UUID createNewGuid(INtfsContext context) {
        Random rng = context.getOptions().getRandomNumberGenerator();
        if (rng != null) {
            byte[] buffer = new byte[16];
            rng.nextBytes(buffer);
            return UUID.nameUUIDFromBytes(buffer);
        }

        return UUID.randomUUID();
    }

    private void loadAttributes() {
        Map<Long, FileRecord> extraFileRecords = new HashMap<>();

        AttributeRecord attrListRec = records.get(0).getAttribute(AttributeType.AttributeList);
        if (attrListRec != null) {
            NtfsAttribute lastAttr = null;

            @SuppressWarnings("unchecked")
            StructuredNtfsAttribute<AttributeList> attrListAttr = (StructuredNtfsAttribute<AttributeList>) NtfsAttribute.fromRecord(this, getMftReference(), attrListRec);
            AttributeList attrList = attrListAttr.getContent();
            attributes.add(attrListAttr);

            for (AttributeListRecord record : attrList) {
                FileRecord attrFileRecord = records.get(0);
                if (record.baseFileReference.getMftIndex() != records.get(0).getMasterFileTableIndex()) {
                    if (!extraFileRecords.containsKey(record.baseFileReference.getMftIndex())) {
                        attrFileRecord = context.getMft().getRecord(record.baseFileReference);
                        if (attrFileRecord != null) {
                            extraFileRecords.put((long) attrFileRecord.getMasterFileTableIndex(), attrFileRecord);
                        }
                    } else
                        attrFileRecord = extraFileRecords.get(record.baseFileReference.getMftIndex());
                }

                if (attrFileRecord != null) {
                    AttributeRecord attrRec = attrFileRecord.getAttribute(record.attributeId);

                    if (attrRec != null) {
                        if (record.startVcn == 0) {
                            lastAttr = NtfsAttribute.fromRecord(this, record.baseFileReference, attrRec);
                            attributes.add(lastAttr);
                        } else {
                            lastAttr.addExtent(record.baseFileReference, attrRec);
                        }
                    }
                }
            }

            for (Map.Entry<Long, FileRecord> extraFileRecord : extraFileRecords.entrySet()) {
                records.add(extraFileRecord.getValue());
            }
        } else {
            for (AttributeRecord record : records.get(0).getAttributes()) {
                attributes.add(NtfsAttribute.fromRecord(this, getMftReference(), record));
            }
        }
    }

    private boolean splitAttribute(FileRecord record) {
        if (record.getAttributes().size() != 1) {
            throw new UnsupportedOperationException("Attempting to split attribute in MFT record containing multiple attributes");
        }

        return splitAttribute(record, (NonResidentAttributeRecord) record.getFirstAttribute(), false);
    }

    private boolean splitAttribute(FileRecord record, NonResidentAttributeRecord targetAttr, boolean atStart) {
        if (targetAttr.getDataRuns().size() <= 1) {
            return false;
        }

        int splitIndex = 1;
        if (!atStart) {
            List<DataRun> runs = targetAttr.getDataRuns();
            splitIndex = runs.size() - 1;
            int saved = (int) runs.get(splitIndex).getSize();
            while (splitIndex > 1 && record.getSize() - saved > record.getAllocatedSize()) {
                --splitIndex;
                saved += (int) runs.get(splitIndex).getSize();
            }
        }

        AttributeRecord newAttr = targetAttr.split(splitIndex);
        // Find a home for the new attribute record
        FileRecord newAttrHome = null;
        for (FileRecord targetRecord : records) {
            if (!targetRecord.getIsMftRecord() && mft.getRecordSize() - targetRecord.getSize() >= newAttr.getSize()) {
                targetRecord.addAttribute(newAttr);
                newAttrHome = targetRecord;
            }
        }
        if (newAttrHome == null) {
            records.get(0).getFlags().remove(FileRecordFlags.InUse);
            newAttrHome = mft.allocateRecord(records.get(0).getFlags(), record.getIsMftRecord());
            newAttrHome.setBaseFile(record.getBaseFile().isNull() ? record.getReference() : record.getBaseFile());
            records.add(newAttrHome);
            newAttrHome.addAttribute(newAttr);
        }

        // Add the new attribute record as an extent on the attribute it split from
        boolean added = false;
        for (NtfsAttribute attr : attributes) {
            for (Map.Entry<AttributeReference, AttributeRecord> existingRecord : attr.getExtents().entrySet()) {
                if (existingRecord.getKey().getFile().equals(record.getReference())
                        && existingRecord.getKey().getAttributeId() == targetAttr.getAttributeId()) {
                    attr.addExtent(newAttrHome.getReference(), newAttr);
                    added = true;
                    break;
                }
            }
            if (added) {
                break;
            }
        }
        updateAttributeList();
        return true;
    }

    private boolean expelAttribute(FileRecord record) {
        if (record.getMasterFileTableIndex() == MasterFileTable.MftIndex) {
            // Special case for MFT - can't fully expel attributes, instead split most of
            // the data runs off.
            List<AttributeRecord> attrs = record.getAttributes();
            for (int i = attrs.size() - 1; i >= 0; --i) {
                AttributeRecord attr = attrs.get(i);
                if (attr.getAttributeType() == AttributeType.Data) {
                    if (splitAttribute(record, (NonResidentAttributeRecord) attr, true)) {
                        return true;
                    }
                }
            }
        } else {
            List<AttributeRecord> attrs = record.getAttributes();
            for (int i = attrs.size() - 1; i >= 0; --i) {
                AttributeRecord attr = attrs.get(i);
                if (attr.getAttributeType().ordinal() > AttributeType.AttributeList.ordinal()) {
                    for (FileRecord targetRecord : records) {
                        if (mft.getRecordSize() - targetRecord.getSize() >= attr.getSize()) {
                            moveAttribute(record, attr, targetRecord);
                            return true;
                        }
                    }
                    FileRecord newFileRecord = mft.allocateRecord(EnumSet.noneOf(FileRecordFlags.class), record.getIsMftRecord());
                    newFileRecord.setBaseFile(record.getReference());
                    records.add(newFileRecord);
                    moveAttribute(record, attr, newFileRecord);
                    return true;
                }
            }
        }
        return false;
    }

    private void moveAttribute(FileRecord record, AttributeRecord attrRec, FileRecord targetRecord) {
        AttributeReference oldRef = new AttributeReference(record.getReference(), attrRec.getAttributeId());
        record.removeAttribute(attrRec.getAttributeId());
        targetRecord.addAttribute(attrRec);
        AttributeReference newRef = new AttributeReference(targetRecord.getReference(), attrRec.getAttributeId());
        for (NtfsAttribute attr : attributes) {
            attr.replaceExtent(oldRef, newRef, attrRec);
        }
        updateAttributeList();
    }

    private void createAttributeList() {
        short id = records.get(0).createAttribute(AttributeType.AttributeList, null, false, EnumSet.noneOf(AttributeFlags.class));
        @SuppressWarnings("unchecked")
        StructuredNtfsAttribute<AttributeList> newAttr = (StructuredNtfsAttribute<AttributeList>) NtfsAttribute.fromRecord(this, getMftReference(), records.get(0).getAttribute(id));
        attributes.add(newAttr);
        updateAttributeList();
    }

    @SuppressWarnings("unchecked")
    private void updateAttributeList() {
        if (records.size() > 1) {
            AttributeList attrList = new AttributeList();
            for (NtfsAttribute attr : attributes) {
                if (attr.getType() != AttributeType.AttributeList) {
                    for (Map.Entry<AttributeReference, AttributeRecord> extent : attr.getExtents().entrySet()) {
                        attrList.add(AttributeListRecord.fromAttribute(extent.getValue(), extent.getKey().getFile()));
                    }
                }
            }
            StructuredNtfsAttribute<AttributeList> alAttr;
            alAttr = (StructuredNtfsAttribute<AttributeList>) getAttribute(AttributeType.AttributeList, null);
            alAttr.setContent(attrList);
            alAttr.save();
        }
    }

    /**
     * Creates a new unnamed attribute.
     *
     * @param type  The type of the new attribute.
     * @param flags The flags of the new attribute.
     * @return The new attribute.
     */
    private NtfsAttribute createAttribute(AttributeType type, EnumSet<AttributeFlags> flags) {
        return createAttribute(type, null, flags);
    }

    /**
     * Creates a new attribute.
     *
     * @param type  The type of the new attribute.
     * @param name  The name of the new attribute.
     * @param flags The flags of the new attribute.
     * @return The new attribute.
     */
    private NtfsAttribute createAttribute(AttributeType type, String name, EnumSet<AttributeFlags> flags) {
        boolean indexed = context.getAttributeDefinitions().isIndexed(type);
        short id = records.get(0).createAttribute(type, name, indexed, flags);
        AttributeRecord newAttrRecord = records.get(0).getAttribute(id);
        NtfsAttribute newAttr = NtfsAttribute.fromRecord(this, getMftReference(), newAttrRecord);
        attributes.add(newAttr);
        updateAttributeList();
        markMftRecordDirty();
        return newAttr;
    }

    /**
     * Creates a new attribute at a fixed cluster.
     *
     * @param type            The type of the new attribute.
     * @param name            The name of the new attribute.
     * @param flags           The flags of the new attribute.
     * @param firstCluster    The first cluster to assign to the attribute.
     * @param numClusters     The number of sequential clusters to assign to the
     *                            attribute.
     * @param bytesPerCluster The number of bytes in each cluster.
     * @return The new attribute.
     */
    private NtfsAttribute createAttribute(AttributeType type,
                                          String name,
                                          EnumSet<AttributeFlags> flags,
                                          long firstCluster,
                                          long numClusters,
                                          int bytesPerCluster) {
        @SuppressWarnings("unused")
        boolean indexed = context.getAttributeDefinitions().isIndexed(type);
        short id = records.get(0).createNonResidentAttribute(type, name, flags, firstCluster, numClusters, bytesPerCluster);
        AttributeRecord newAttrRecord = records.get(0).getAttribute(id);
        NtfsAttribute newAttr = NtfsAttribute.fromRecord(this, getMftReference(), newAttrRecord);
        attributes.add(newAttr);
        updateAttributeList();
        markMftRecordDirty();
        return newAttr;
    }

    private void removeAttribute(NtfsAttribute attr) {
        if (attr != null) {
            if (attr.getPrimaryRecord().getAttributeType() == AttributeType.IndexRoot) {
                indexCache.remove(attr.getPrimaryRecord().getName());
            }

            removeAttributeExtents(attr);
            attributes.remove(attr);
            updateAttributeList();
        }

    }

    private boolean shrinkIndexRoot(String indexName) {
        NtfsAttribute attr = getAttribute(AttributeType.IndexRoot, indexName);
        // Nothing to win, can't make IndexRoot smaller than this
        // 8 = min size of entry that points to IndexAllocation...
        if (attr.getLength() <= IndexRoot.HeaderOffset + IndexHeader.Size + 8) {
            return false;
        }

        Index idx = getIndex(indexName);
        return idx.shrinkRoot();
    }

    private void makeAttributeResident(AttributeReference attrRef, int maxData) {
        NtfsAttribute attr = getAttribute(attrRef);
        if (!attr.isNonResident()) {
            throw new UnsupportedOperationException("Attribute is already resident");
        }

        short id = records.get(0)
                .createAttribute(attr.getType(),
                                 attr.getName(),
                                 context.getAttributeDefinitions().isIndexed(attr.getType()),
                                 attr.getFlags());
        AttributeRecord newAttrRecord = records.get(0).getAttribute(id);
        IBuffer attrBuffer = attr.getDataBuffer();
        byte[] tempData = StreamUtilities.readExact(attrBuffer, 0, (int) Math.min(maxData, attrBuffer.getCapacity()));
        removeAttributeExtents(attr);
        attr.setExtent(records.get(0).getReference(), newAttrRecord);
        attr.getDataBuffer().write(0, tempData, 0, tempData.length);
        updateAttributeList();
    }

    private FileRecord getFileRecord(FileRecordReference fileReference) {
        for (FileRecord record : records) {
            if (record.getMasterFileTableIndex() == fileReference.getMftIndex()) {
                return record;
            }

        }
        return null;
    }

    private void removeFileRecord(FileRecordReference fileReference) {
        for (int i = 0; i < records.size(); ++i) {
            if (records.get(i).getMasterFileTableIndex() == fileReference.getMftIndex()) {
                FileRecord record = records.get(i);
                if (!record.getAttributes().isEmpty()) {
                    throw new dotnet4j.io.IOException("Attempting to remove non-empty MFT record");
                }

                context.getMft().removeRecord(fileReference);
                records.remove(record);
                if (records.size() == 1) {
                    NtfsAttribute attrListAttr = getAttribute(AttributeType.AttributeList, null);
                    if (attrListAttr != null) {
                        removeAttribute(attrListAttr);
                    }
                }
            }
        }
    }

    /**
     * Wrapper for Resident/Non-Resident attribute streams, that remains valid
     * despite the attribute oscillating between resident and not.
     */
    private static class FileStream extends SparseStream {

        private final NtfsAttribute attr;

        private final File file;

        private final SparseStream wrapped;

        public FileStream(File file, NtfsAttribute attr, FileAccess access) {
            this.file = file;
            this.attr = attr;
            wrapped = attr.open(access);
        }

        @Override public boolean canRead() {
            return wrapped.canRead();
        }

        @Override public boolean canSeek() {
            return wrapped.canSeek();
        }

        @Override public boolean canWrite() {
            return wrapped.canWrite();
        }

        @Override public List<StreamExtent> getExtents() {
            return wrapped.getExtents();
        }

        @Override public long getLength() {
            return wrapped.getLength();
        }

        @Override public long position() {
            return wrapped.position();
        }

        @Override public void position(long value) {
            wrapped.position(value);
        }

        @Override public void close() throws IOException {
            wrapped.close();
        }

        @Override public void flush() {
            wrapped.flush();
        }

        @Override public int read(byte[] buffer, int offset, int count) {
            return wrapped.read(buffer, offset, count);
        }

        @Override public long seek(long offset, SeekOrigin origin) {
            return wrapped.seek(offset, origin);
        }

        @Override public void setLength(long value) {
            changeAttributeResidencyByLength(value);
            wrapped.setLength(value);
        }

        @Override public void write(byte[] buffer, int offset, int count) {
            if (wrapped.position() + count > getLength()) {
                changeAttributeResidencyByLength(wrapped.position() + count);
            }

            wrapped.write(buffer, offset, count);
        }

        @Override public void clear(int count) {
            if (wrapped.position() + count > getLength()) {
                changeAttributeResidencyByLength(wrapped.position() + count);
            }

            wrapped.clear(count);
        }

        @Override public String toString() {
            return file + ".attr[" + attr.getId() + "]";
        }

        /**
         * Change attribute residency if it gets too big (or small).
         *
         * @param value The new (anticipated) length of the stream.Has hysteresis - the
         *                  decision is based on the input and the current state, not
         *                  the current state alone.
         */
        private void changeAttributeResidencyByLength(long value) {
            // This is a bit of a hack - but it's really important the bitmap file remains
            // non-resident
            if (file.records.get(0).getMasterFileTableIndex() == MasterFileTable.BitmapIndex) {
                return;
            }

            if (!attr.isNonResident() && value >= file.getMaxMftRecordSize()) {
                file.makeAttributeNonResident(attr.getReference(), (int) Math.min(value, wrapped.getLength()));
            } else if (attr.isNonResident() && value <= file.getMaxMftRecordSize() / 4) {
                // Use of 1/4 of record size here is just a heuristic - the important thing is
                // not to end up with
                // zero-length non-resident attributes
                file.makeAttributeResident(attr.getReference(), (int) value);
            }
        }
    }
}
