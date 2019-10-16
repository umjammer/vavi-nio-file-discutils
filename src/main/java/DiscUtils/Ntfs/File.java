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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import DiscUtils.Core.Internal.ObjectCache;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.FileNotFoundException;
import moe.yo3explorer.dotnetio4j.SeekOrigin;


public class File {
    private final List<NtfsAttribute> _attributes;

    protected INtfsContext _context;

    private final ObjectCache<String, Index> _indexCache;

    private final MasterFileTable _mft;

    private final List<FileRecord> _records;

    public File(INtfsContext context, FileRecord baseRecord) {
        _context = context;
        _mft = _context.getMft();
        _records = new ArrayList<>();
        _records.add(baseRecord);
        _indexCache = new ObjectCache<>();
        _attributes = new ArrayList<>();
        loadAttributes();
    }

    /**
     * Gets an enumeration of all the attributes.
     */
    public List<NtfsAttribute> getAllAttributes() {
        return _attributes;
    }

    public List<NtfsStream> getAllStreams() {
        List<NtfsStream> result = new ArrayList<>();
        for (NtfsAttribute attr : _attributes) {
            result.add(new NtfsStream(this, attr));
        }
        return result;
    }

    public String getBestName() {
        List<NtfsAttribute> attrs = getAttributes(AttributeType.FileName);
        String bestName = null;
        if (attrs != null && attrs.size() != 0) {
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

    public INtfsContext getContext() {
        return _context;
    }

    public DirectoryEntry getDirectoryEntry() {
        if (_context.getGetDirectoryByRef() == null) {
            return null;
        }

        NtfsStream stream = getStream(AttributeType.FileName, null);
        if (stream == null) {
            return null;
        }

        FileNameRecord record = stream.getContent(FileNameRecord.class);
        // Root dir is stored without root directory flag set in FileNameRecord, simulate it.
        if (_records.get(0).getMasterFileTableIndex() == MasterFileTable.RootDirIndex) {
            record.Flags.add(FileAttributeFlags.Directory);
        }

        return new DirectoryEntry(_context.getGetDirectoryByRef().invoke(record.ParentDirectory), getMftReference(), record);
    }

    public short getHardLinkCount() {
        return _records.get(0).getHardLinkCount();
    }

    public void setHardLinkCount(short value) {
        _records.get(0).setHardLinkCount(value);
    }

    public boolean getHasWin32OrDosName() {
        //StructuredNtfsAttribute<FileNameRecord>
        for (Object _attr : getAttributes(AttributeType.FileName)) {
            StructuredNtfsAttribute<FileNameRecord> attr = (StructuredNtfsAttribute<FileNameRecord>) _attr;
            FileNameRecord fnr = attr.getContent();
            if (fnr._FileNameNamespace != FileNameNamespace.Posix) {
                return true;
            }

        }
        return false;
    }

    public int getIndexInMft() {
        return _records.get(0).getMasterFileTableIndex();
    }

    public boolean getIsDirectory() {
        return _records.get(0).getFlags().contains(FileRecordFlags.IsDirectory);
    }

    public int getMaxMftRecordSize() {
        return _records.get(0).getAllocatedSize();
    }

    private boolean __MftRecordIsDirty;

    public boolean getMftRecordIsDirty() {
        return __MftRecordIsDirty;
    }

    public void setMftRecordIsDirty(boolean value) {
        __MftRecordIsDirty = value;
    }

    public FileRecordReference getMftReference() {
        return _records.get(0).getReference();
    }

    public List<String> getNames() {
        List<String> result = new ArrayList<>();
        if (getIndexInMft() == MasterFileTable.RootDirIndex) {
            result.add("");
        } else {
            for (Object _attr : getAttributes(AttributeType.FileName)) {
                StructuredNtfsAttribute<FileNameRecord> attr = (StructuredNtfsAttribute<FileNameRecord>) _attr;
                String name = attr.getContent().FileName;
                Directory parentDir = _context.getGetDirectoryByRef().invoke(attr.getContent().ParentDirectory);
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
        return createNew(context, EnumSet.of(FileRecordFlags.None), dirFlags);
    }

    public static File createNew(INtfsContext context, EnumSet<FileRecordFlags> flags, EnumSet<FileAttributeFlags> dirFlags) {
        File newFile = context.getAllocateFile().invoke(flags);
        EnumSet<FileAttributeFlags> fileFlags = FileRecord.convertFlags(flags);
        fileFlags.add(FileAttributeFlags.Archive);
        fileFlags.add(dirFlags.contains(FileAttributeFlags.Compressed) ? FileAttributeFlags.Compressed : FileAttributeFlags.None);
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
        for (FileRecord record : _records) {
            if (record.getAttribute(attrType, attrName) != null) {
                return _mft.getRecordSize() - (int) record.getSize();
            }

        }
        throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to determine free space for non-existent attribute");
    }

    public void modified() {
        long now = System.currentTimeMillis();
        NtfsStream siStream = getStream(AttributeType.StandardInformation, null);
        StandardInformation si = siStream.getContent(StandardInformation.class);
        si.LastAccessTime = now;
        si.ModificationTime = now;
        siStream.setContent(si);
        markMftRecordDirty();
    }

    public void accessed() {
        long now = System.currentTimeMillis();
        NtfsStream siStream = getStream(AttributeType.StandardInformation, null);
        StandardInformation si = siStream.getContent(StandardInformation.class);
        si.LastAccessTime = now;
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
                si.MftChangedTime = NtfsTransaction.getCurrent().getTimestamp();
                stream.setContent(si);
            }

            boolean fixesApplied = true;
            while (fixesApplied) {
                fixesApplied = false;
                for (int i = 0; i < _records.size(); ++i) {
                    FileRecord record = _records.get(i);
                    boolean fixedAttribute = true;
                    while (record.getSize() > _mft.getRecordSize() && fixedAttribute) {
                        fixedAttribute = false;
                        if (!fixedAttribute && !record.getIsMftRecord()) {
                            for (AttributeRecord attr : record.getAttributes()) {
                                if (!attr.getIsNonResident() &&
                                    !_context.getAttributeDefinitions().mustBeResident(attr.getAttributeType())) {
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
                                if (_records.size() == 1) {
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
            for (FileRecord record : _records) {
                _mft.writeRecord(record);
            }
        }

    }

    public Index createIndex(String name, AttributeType attrType, AttributeCollationRule collRule) {
        Index.create(attrType, collRule, this, name);
        return getIndex(name);
    }

    public Index getIndex(String name) {
        Index idx = _indexCache.get___idx(name);
        if (idx == null) {
            idx = new Index(this, name, _context.getBiosParameterBlock(), _context.getUpperCase());
            _indexCache.set___idx(name, idx);
        }

        return idx;
    }

    public void delete() {
        if (_records.get(0).getHardLinkCount() != 0) {
            throw new UnsupportedOperationException("Attempt to delete in-use file: " + toString());
        }

        _context.getForgetFile().invoke(this);
        NtfsStream objIdStream = getStream(AttributeType.ObjectId, null);
        if (objIdStream != null) {
            ObjectId objId = objIdStream.getContent(ObjectId.class);
            getContext().getObjectIds().remove(objId.Id);
        }

        // Truncate attributes, allowing for truncation silently removing the AttributeList attribute
        // in some cases (large file with all attributes first extent in the first MFT record).  This
        // releases all allocated clusters in most cases.
        List<NtfsAttribute> truncateAttrs = new ArrayList<>(_attributes.size());
        for (NtfsAttribute attr : _attributes) {
            if (attr.getType() != AttributeType.AttributeList) {
                truncateAttrs.add(attr);
            }

        }
        for (NtfsAttribute attr : truncateAttrs) {
            attr.getDataBuffer().setCapacity(0);
        }
        // If the attribute list record remains, free any possible clusters it owns.  We've now freed
        // all clusters.
        NtfsAttribute attrList = getAttribute(AttributeType.AttributeList, null);
        if (attrList != null) {
            attrList.getDataBuffer().setCapacity(0);
        }

        for (FileRecord mftRecord : _records) {
            // Now go through the MFT records, freeing them up
            _context.getMft().removeRecord(mftRecord.getReference());
        }
        _attributes.clear();
        _records.clear();
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
        for (NtfsAttribute attr : _attributes) {
            if (attr.getType() == attrType && Utilities.equals(attr.getName(), name)) {
                result.add(new NtfsStream(this, attr));
            }
        }
        return result;
    }

    public NtfsStream createStream(AttributeType attrType, String name) {
        return new NtfsStream(this, createAttribute(attrType, name, EnumSet.of(AttributeFlags.None)));
    }

    public NtfsStream createStream(AttributeType attrType,
                                   String name,
                                   long firstCluster,
                                   long numClusters,
                                   int bytesPerCluster) {
        return new NtfsStream(this,
                              createAttribute(attrType,
                                              name,
                                              EnumSet.of(AttributeFlags.None),
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

    public FileNameRecord getFileNameRecord(String name, boolean freshened) {
        List<NtfsAttribute> attrs = getAttributes(AttributeType.FileName);
        StructuredNtfsAttribute<FileNameRecord> attr = null;
        if (name == null || name.isEmpty()) {
            if (attrs.size() != 0) {
                attr = (StructuredNtfsAttribute<FileNameRecord>) attrs.get(0);
            }
        } else {
            for (Object _a : attrs) {
                StructuredNtfsAttribute<FileNameRecord> a = (StructuredNtfsAttribute<FileNameRecord>) _a;
                if (_context.getUpperCase().compare(a.getContent().FileName, name) == 0) {
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
        writer.println(indent + "FILE (" + toString() + ")");
        writer.println(indent + "  File Number: " + _records.get(0).getMasterFileTableIndex());
        _records.get(0).dump(writer, indent + "  ");
        for (AttributeRecord attrRec : _records.get(0).getAttributes()) {
            NtfsAttribute.fromRecord(this, getMftReference(), attrRec).dump(writer, indent + "  ");
        }
    }

    public String toString() {
        try {
            String bestName = getBestName();
            if (bestName == null) {
                return "?????";
            }

            return bestName;
        } catch (RuntimeException __dummyCatchVar0) {
            throw __dummyCatchVar0;
        } catch (Exception __dummyCatchVar0) {
            throw new RuntimeException(__dummyCatchVar0);
        }

    }

    public void removeAttributeExtents(NtfsAttribute attr) {
        attr.getDataBuffer().setCapacity(0);
        for (AttributeReference extentRef : attr.getExtents().keySet()) {
            removeAttributeExtent(extentRef);
        }
    }

    public void removeAttributeExtent(AttributeReference extentRef) {
        FileRecord fileRec = getFileRecord(extentRef.getFile());
        if (fileRec != null) {
            fileRec.removeAttribute(extentRef.getAttributeId());
            // Remove empty non-primary MFT records
            if (fileRec.getAttributes().size() == 0 && fileRec.getBaseFile().getValue() != 0) {
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
    public NtfsAttribute getAttribute(AttributeReference attrRef) {
        for (NtfsAttribute attr : _attributes) {
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
     * @return The attribute of
     *         {@code null}
     *         .
     */
    public NtfsAttribute getAttribute(AttributeType type, String name) {
        for (NtfsAttribute attr : _attributes) {
            if (attr.getPrimaryRecord().getAttributeType() == type && Utilities.equals(attr.getName(), name)) {
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
    public List<NtfsAttribute> getAttributes(AttributeType type) {
        List<NtfsAttribute> matches = new ArrayList<>();
        for (NtfsAttribute attr : _attributes) {
            if (attr.getPrimaryRecord().getAttributeType() == type && (attr.getName() == null || attr.getName().isEmpty())) {
                matches.add(attr);
            }

        }
        return matches;
    }

    public void makeAttributeNonResident(AttributeReference attrRef, int maxData) {
        NtfsAttribute attr = getAttribute(attrRef);
        if (attr.getIsNonResident()) {
            throw new UnsupportedOperationException("Attribute is already non-resident");
        }

        short id = _records.get(0).createNonResidentAttribute(attr.getType(), attr.getName(), attr.getFlags());
        AttributeRecord newAttrRecord = _records.get(0).getAttribute(id);
        IBuffer attrBuffer = attr.getDataBuffer();
        byte[] tempData = StreamUtilities.readExact(attrBuffer, 0, (int) Math.min(maxData, attrBuffer.getCapacity()));
        removeAttributeExtents(attr);
        attr.setExtent(_records.get(0).getReference(), newAttrRecord);
        attr.getDataBuffer().write(0, tempData, 0, tempData.length);
        updateAttributeList();
    }

    public void freshenFileName(FileNameRecord fileName, boolean updateMftRecord) {
        //
        // Freshen the record from the definitive info in the other attributes
        //
        StandardInformation si = getStandardInformation();
        NtfsAttribute anonDataAttr = getAttribute(AttributeType.Data, null);
        fileName.CreationTime = si.CreationTime;
        fileName.ModificationTime = si.ModificationTime;
        fileName.MftChangedTime = si.MftChangedTime;
        fileName.LastAccessTime = si.LastAccessTime;
        fileName.Flags = si._FileAttributes;
        if (getMftRecordIsDirty() && NtfsTransaction.getCurrent() != null) {
            fileName.MftChangedTime = NtfsTransaction.getCurrent().getTimestamp();
        }

        // Directories don't have directory flag set in StandardInformation, so set from MFT record
        if (_records.get(0).getFlags().contains(FileRecordFlags.IsDirectory)) {
            fileName.Flags.add(FileAttributeFlags.Directory);
        }

        if (anonDataAttr != null) {
            fileName.RealSize = anonDataAttr.getPrimaryRecord().getDataLength();
            fileName.AllocatedSize = anonDataAttr.getPrimaryRecord().getAllocatedLength();
        }

        if (updateMftRecord) {
            for (NtfsStream stream : getStreams(AttributeType.FileName, null)) {
                FileNameRecord fnr = stream.getContent(FileNameRecord.class);
                if (fnr.equals(fileName)) {
                    fnr = new FileNameRecord(fileName);
                    fnr.Flags.remove(FileAttributeFlags.ReparsePoint);
                    stream.setContent(fnr);
                }
            }
        }
    }

    public long getAttributeOffset(AttributeReference attrRef) {
        long recordOffset = _mft.getRecordOffset(attrRef.getFile());
        FileRecord frs = getFileRecord(attrRef.getFile());
        return recordOffset + frs.getAttributeOffset(attrRef.getAttributeId());
    }

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
        AttributeRecord attrListRec = _records.get(0).getAttribute(AttributeType.AttributeList);
        if (attrListRec != null) {
            NtfsAttribute lastAttr = null;
            StructuredNtfsAttribute<AttributeList> attrListAttr = (StructuredNtfsAttribute<AttributeList>) NtfsAttribute
                    .fromRecord(this, getMftReference(), attrListRec);
            AttributeList attrList = attrListAttr.getContent();
            _attributes.add(attrListAttr);
            for (AttributeListRecord record : attrList) {
                FileRecord attrFileRecord = _records.get(0);
                if (record.BaseFileReference.getMftIndex() != _records.get(0).getMasterFileTableIndex()) {
                    if (!extraFileRecords.containsKey(record.BaseFileReference.getMftIndex())) {
                        attrFileRecord = _context.getMft().getRecord(record.BaseFileReference);
                        if (attrFileRecord != null) {
                            extraFileRecords.put((long) attrFileRecord.getMasterFileTableIndex(), attrFileRecord);
                        }
                    }
                    attrFileRecord = extraFileRecords.get(record.BaseFileReference.getMftIndex());
                }

                if (attrFileRecord != null) {
                    AttributeRecord attrRec = attrFileRecord.getAttribute(record.AttributeId);
                    if (attrRec != null) {
                        if (record.StartVcn == 0) {
                            lastAttr = NtfsAttribute.fromRecord(this, record.BaseFileReference, attrRec);
                            _attributes.add(lastAttr);
                        } else {
                            lastAttr.addExtent(record.BaseFileReference, attrRec);
                        }
                    }

                }

            }
            for (Map.Entry<Long, FileRecord> extraFileRecord : extraFileRecords.entrySet()) {
                _records.add(extraFileRecord.getValue());
            }
        } else {
            for (AttributeRecord record : _records.get(0).getAttributes()) {
                _attributes.add(NtfsAttribute.fromRecord(this, getMftReference(), record));
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
                saved += runs.get(splitIndex).getSize();
            }
        }

        AttributeRecord newAttr = targetAttr.split(splitIndex);
        // Find a home for the new attribute record
        FileRecord newAttrHome = null;
        for (FileRecord targetRecord : _records) {
            if (!targetRecord.getIsMftRecord() && _mft.getRecordSize() - targetRecord.getSize() >= newAttr.getSize()) {
                targetRecord.addAttribute(newAttr);
                newAttrHome = targetRecord;
            }

        }
        if (newAttrHome == null) {
            _records.get(0).getFlags().remove(FileRecordFlags.InUse);
            newAttrHome = _mft.allocateRecord(_records.get(0).getFlags(), record.getIsMftRecord());
            newAttrHome.setBaseFile(record.getBaseFile().getIsNull() ? record.getReference() : record.getBaseFile());
            _records.add(newAttrHome);
            newAttrHome.addAttribute(newAttr);
        }

        // Add the new attribute record as an extent on the attribute it split from
        boolean added = false;
        for (NtfsAttribute attr : _attributes) {
            for (Map.Entry<AttributeReference, AttributeRecord> existingRecord : attr.getExtents().entrySet()) {
                if (existingRecord.getKey().getFile() == record.getReference() &&
                    existingRecord.getKey().getAttributeId() == targetAttr.getAttributeId()) {
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
            // Special case for MFT - can't fully expel attributes, instead split most of the data runs off.
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
                    for (FileRecord targetRecord : _records) {
                        if (_mft.getRecordSize() - targetRecord.getSize() >= attr.getSize()) {
                            moveAttribute(record, attr, targetRecord);
                            return true;
                        }

                    }
                    FileRecord newFileRecord = _mft.allocateRecord(EnumSet.of(FileRecordFlags.None), record.getIsMftRecord());
                    newFileRecord.setBaseFile(record.getReference());
                    _records.add(newFileRecord);
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
        for (NtfsAttribute attr : _attributes) {
            attr.replaceExtent(oldRef, newRef, attrRec);
        }
        updateAttributeList();
    }

    private void createAttributeList() {
        short id = _records.get(0).createAttribute(AttributeType.AttributeList, null, false, EnumSet.of(AttributeFlags.None));
        StructuredNtfsAttribute<AttributeList> newAttr = (StructuredNtfsAttribute<AttributeList>) NtfsAttribute
                .fromRecord(this, getMftReference(), _records.get(0).getAttribute(id));
        _attributes.add(newAttr);
        updateAttributeList();
    }

    private void updateAttributeList() {
        if (_records.size() > 1) {
            AttributeList attrList = new AttributeList();
            for (NtfsAttribute attr : _attributes) {
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
     * @param type The type of the new attribute.
     * @param flags The flags of the new attribute.
     * @return The new attribute.
     */
    private NtfsAttribute createAttribute(AttributeType type, EnumSet<AttributeFlags> flags) {
        return createAttribute(type, null, flags);
    }

    /**
     * Creates a new attribute.
     *
     * @param type The type of the new attribute.
     * @param name The name of the new attribute.
     * @param flags The flags of the new attribute.
     * @return The new attribute.
     */
    private NtfsAttribute createAttribute(AttributeType type, String name, EnumSet<AttributeFlags> flags) {
        boolean indexed = _context.getAttributeDefinitions().isIndexed(type);
        short id = _records.get(0).createAttribute(type, name, indexed, flags);
        AttributeRecord newAttrRecord = _records.get(0).getAttribute(id);
        NtfsAttribute newAttr = NtfsAttribute.fromRecord(this, getMftReference(), newAttrRecord);
        _attributes.add(newAttr);
        updateAttributeList();
        markMftRecordDirty();
        return newAttr;
    }

    /**
     * Creates a new attribute at a fixed cluster.
     *
     * @param type The type of the new attribute.
     * @param name The name of the new attribute.
     * @param flags The flags of the new attribute.
     * @param firstCluster The first cluster to assign to the attribute.
     * @param numClusters The number of sequential clusters to assign to the
     *            attribute.
     * @param bytesPerCluster The number of bytes in each cluster.
     * @return The new attribute.
     */
    private NtfsAttribute createAttribute(AttributeType type,
                                          String name,
                                          EnumSet<AttributeFlags> flags,
                                          long firstCluster,
                                          long numClusters,
                                          int bytesPerCluster) {
        boolean indexed = _context.getAttributeDefinitions().isIndexed(type);
        short id = _records.get(0).createNonResidentAttribute(type, name, flags, firstCluster, numClusters, bytesPerCluster);
        AttributeRecord newAttrRecord = _records.get(0).getAttribute(id);
        NtfsAttribute newAttr = NtfsAttribute.fromRecord(this, getMftReference(), newAttrRecord);
        _attributes.add(newAttr);
        updateAttributeList();
        markMftRecordDirty();
        return newAttr;
    }

    private void removeAttribute(NtfsAttribute attr) {
        if (attr != null) {
            if (attr.getPrimaryRecord().getAttributeType() == AttributeType.IndexRoot) {
                _indexCache.remove(attr.getPrimaryRecord().getName());
            }

            removeAttributeExtents(attr);
            _attributes.remove(attr);
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
        if (!attr.getIsNonResident()) {
            throw new UnsupportedOperationException("Attribute is already resident");
        }

        short id = _records.get(0)
                .createAttribute(attr.getType(),
                                 attr.getName(),
                                 _context.getAttributeDefinitions().isIndexed(attr.getType()),
                                 attr.getFlags());
        AttributeRecord newAttrRecord = _records.get(0).getAttribute(id);
        IBuffer attrBuffer = attr.getDataBuffer();
        byte[] tempData = StreamUtilities.readExact(attrBuffer, 0, (int) Math.min(maxData, attrBuffer.getCapacity()));
        removeAttributeExtents(attr);
        attr.setExtent(_records.get(0).getReference(), newAttrRecord);
        attr.getDataBuffer().write(0, tempData, 0, tempData.length);
        updateAttributeList();
    }

    private FileRecord getFileRecord(FileRecordReference fileReference) {
        for (FileRecord record : _records) {
            if (record.getMasterFileTableIndex() == fileReference.getMftIndex()) {
                return record;
            }

        }
        return null;
    }

    private void removeFileRecord(FileRecordReference fileReference) {
        for (int i = 0; i < _records.size(); ++i) {
            if (_records.get(i).getMasterFileTableIndex() == fileReference.getMftIndex()) {
                FileRecord record = _records.get(i);
                if (record.getAttributes().size() > 0) {
                    throw new moe.yo3explorer.dotnetio4j.IOException("Attempting to remove non-empty MFT record");
                }

                _context.getMft().removeRecord(fileReference);
                _records.remove(record);
                if (_records.size() == 1) {
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
        private final NtfsAttribute _attr;

        private final File _file;

        private final SparseStream _wrapped;

        public FileStream(File file, NtfsAttribute attr, FileAccess access) {
            _file = file;
            _attr = attr;
            _wrapped = attr.open(access);
        }

        public boolean canRead() {
            return _wrapped.canRead();
        }

        public boolean canSeek() {
            return _wrapped.canSeek();
        }

        public boolean canWrite() {
            return _wrapped.canWrite();
        }

        public List<StreamExtent> getExtents() {
            return _wrapped.getExtents();
        }

        public long getLength() {
            return _wrapped.getLength();
        }

        public long getPosition() {
            return _wrapped.getPosition();
        }

        public void setPosition(long value) {
            _wrapped.setPosition(value);
        }

        public void close() throws IOException {
            _wrapped.close();
        }

        public void flush() {
            _wrapped.flush();
        }

        public int read(byte[] buffer, int offset, int count) {
            return _wrapped.read(buffer, offset, count);
        }

        public long seek(long offset, SeekOrigin origin) {
            return _wrapped.seek(offset, origin);
        }

        public void setLength(long value) {
            changeAttributeResidencyByLength(value);
            _wrapped.setLength(value);
        }

        public void write(byte[] buffer, int offset, int count) {
            if (_wrapped.getPosition() + count > getLength()) {
                changeAttributeResidencyByLength(_wrapped.getPosition() + count);
            }

            _wrapped.write(buffer, offset, count);
        }

        public void clear(int count) {
            if (_wrapped.getPosition() + count > getLength()) {
                changeAttributeResidencyByLength(_wrapped.getPosition() + count);
            }

            _wrapped.clear(count);
        }

        public String toString() {
            try {
                return _file + ".attr[" + _attr.getId() + "]";
            } catch (RuntimeException __dummyCatchVar1) {
                throw __dummyCatchVar1;
            } catch (Exception __dummyCatchVar1) {
                throw new RuntimeException(__dummyCatchVar1);
            }

        }

        /**
         * Change attribute residency if it gets too big (or small).
         *
         * @param value The new (anticipated) length of the stream.Has
         *            hysteresis - the decision is based on the input and the
         *            current
         *            state, not the current state alone.
         */
        private void changeAttributeResidencyByLength(long value) {
            // This is a bit of a hack - but it's really important the bitmap file remains non-resident
            if (_file._records.get(0).getMasterFileTableIndex() == MasterFileTable.BitmapIndex) {
                return;
            }

            if (!_attr.getIsNonResident() && value >= _file.getMaxMftRecordSize()) {
                _file.makeAttributeNonResident(_attr.getReference(), (int) Math.min(value, _wrapped.getLength()));
            } else if (_attr.getIsNonResident() && value <= _file.getMaxMftRecordSize() / 4) {
                // Use of 1/4 of record size here is just a heuristic - the important thing is not to end up with
                // zero-length non-resident attributes
                _file.makeAttributeResident(_attr.getReference(), (int) value);
            }
        }
    }
}
