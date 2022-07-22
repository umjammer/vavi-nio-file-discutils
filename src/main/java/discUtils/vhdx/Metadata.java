//
// Copyright (c) 2008-2012, Kenneth Bell
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

package discUtils.vhdx;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.UUID;

import discUtils.core.coreCompat.ReflectionHelper;
import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


public final class Metadata {

    private final Stream regionStream;

    @SuppressWarnings("unused")
    private UUID page83Data;

    public Metadata(Stream regionStream) {
        this.regionStream = regionStream;
        this.regionStream.setPosition(0);
        table = StreamUtilities.readStruct(MetadataTable.class, this.regionStream);
        fileParameters = readStruct(FileParameters.class, MetadataTable.FileParametersGuid, false);
        diskSize = readValue(MetadataTable.VirtualDiskSizeGuid, false, Long.TYPE, EndianUtilities::toUInt64LittleEndian);
        page83Data = readValue(MetadataTable.Page83DataGuid, false, UUID.class, EndianUtilities::toGuidLittleEndian);
        logicalSectorSize = readValue(MetadataTable.LogicalSectorSizeGuid,
                                       false,
                                       Integer.TYPE,
                                       EndianUtilities::toUInt32LittleEndian);
        physicalSectorSize = readValue(MetadataTable.PhysicalSectorSizeGuid,
                                        false,
                                        Integer.TYPE,
                                        EndianUtilities::toUInt32LittleEndian);
        parentLocator = readStruct(ParentLocator.class, MetadataTable.ParentLocatorGuid, false);
    }

    @FunctionalInterface
    private interface Reader<T> {

        T invoke(byte[] buffer, int offset);
    }

    @FunctionalInterface
    private interface Writer<T> {

        void invoke(T val, byte[] buffer, int offset);
    }

    private MetadataTable table;

    public MetadataTable getTable() {
        return table;
    }

    private FileParameters fileParameters;

    public FileParameters getFileParameters() {
        return fileParameters;
    }

    private long diskSize;

    public long getDiskSize() {
        return diskSize;
    }

    private int logicalSectorSize;

    public int getLogicalSectorSize() {
        return logicalSectorSize;
    }

    private int physicalSectorSize;

    public int getPhysicalSectorSize() {
        return physicalSectorSize;
    }

    private ParentLocator parentLocator;

    public ParentLocator getParentLocator() {
        return parentLocator;
    }

    static Metadata initialize(Stream metadataStream,
                                      FileParameters fileParameters,
                                      long diskSize,
                                      int logicalSectorSize,
                                      int physicalSectorSize,
                                      ParentLocator parentLocator) {
        MetadataTable header = new MetadataTable();
        int dataOffset = (int) (64 * Sizes.OneKiB);
        dataOffset += addEntryStruct(fileParameters,
                                     MetadataTable.FileParametersGuid,
                                     EnumSet.of(MetadataEntryFlags.IsRequired),
                                     header,
                                     dataOffset,
                                     metadataStream);
        dataOffset += addEntryValue(diskSize,
                                    EndianUtilities::writeBytesLittleEndian,
                                    MetadataTable.VirtualDiskSizeGuid,
                                    EnumSet.of(MetadataEntryFlags.IsRequired, MetadataEntryFlags.IsVirtualDisk),
                                    header,
                                    dataOffset,
                                    metadataStream);
        dataOffset += addEntryValue(UUID.randomUUID(),
                                    EndianUtilities::writeBytesLittleEndian,
                                    MetadataTable.Page83DataGuid,
                                    EnumSet.of(MetadataEntryFlags.IsRequired, MetadataEntryFlags.IsVirtualDisk),
                                    header,
                                    dataOffset,
                                    metadataStream);
        dataOffset += addEntryValue(logicalSectorSize,
                                    EndianUtilities::writeBytesLittleEndian,
                                    MetadataTable.LogicalSectorSizeGuid,
                                    EnumSet.of(MetadataEntryFlags.IsRequired, MetadataEntryFlags.IsVirtualDisk),
                                    header,
                                    dataOffset,
                                    metadataStream);
        dataOffset += addEntryValue(physicalSectorSize,
                                    EndianUtilities::writeBytesLittleEndian,
                                    MetadataTable.PhysicalSectorSizeGuid,
                                    EnumSet.of(MetadataEntryFlags.IsRequired, MetadataEntryFlags.IsVirtualDisk),
                                    header,
                                    dataOffset,
                                    metadataStream);
        if (parentLocator != null) {
            dataOffset += addEntryStruct(parentLocator,
                                         MetadataTable.ParentLocatorGuid,
                                         EnumSet.of(MetadataEntryFlags.IsRequired),
                                         header,
                                         dataOffset,
                                         metadataStream);
        }

        metadataStream.setPosition(0);
        StreamUtilities.writeStruct(metadataStream, header);
        return new Metadata(metadataStream);
    }

    private static <T extends IByteArraySerializable> int addEntryStruct(T data,
                                                                         UUID id,
                                                                         EnumSet<MetadataEntryFlags> flags,
                                                                         MetadataTable header,
                                                                         int dataOffset,
                                                                         Stream stream) {
        MetadataEntryKey key = new MetadataEntryKey(id, flags.contains(MetadataEntryFlags.IsUser));
        MetadataEntry entry = new MetadataEntry();
        entry.itemId = id;
        entry.offset = dataOffset;
        entry.length = data.size();
        entry.flags = flags;
        header.entries.put(key, entry);

        stream.setPosition(dataOffset);
        StreamUtilities.writeStruct(stream, data);

        return entry.length;
    }

    private static <T extends Serializable> int addEntryValue(T data,
                                                              Writer<T> writer,
                                                              UUID id,
                                                              EnumSet<MetadataEntryFlags> flags,
                                                              MetadataTable header,
                                                              int dataOffset,
                                                              Stream stream) {
        MetadataEntryKey key = new MetadataEntryKey(id, flags.contains(MetadataEntryFlags.IsUser));
        MetadataEntry entry = new MetadataEntry();
        entry.itemId = id;
        entry.offset = dataOffset;
        entry.length = ReflectionHelper.sizeOf(data.getClass());
        entry.flags = flags;

        header.entries.put(key, entry);

        stream.setPosition(dataOffset);

        byte[] buffer = new byte[entry.length];
        writer.invoke(data, buffer, 0);
        stream.write(buffer, 0, buffer.length);

        return entry.length;
    }

    private <T extends IByteArraySerializable> T readStruct(Class<T> c, UUID itemId, boolean isUser) {
        MetadataEntryKey key = new MetadataEntryKey(itemId, isUser);
        if (getTable().entries.containsKey(key)) {
            MetadataEntry entry = getTable().entries.get(key);
            regionStream.setPosition(entry.offset);
            return StreamUtilities.readStruct(c, regionStream, entry.length);
        }

        return null;
    }

    private <T extends Serializable> T readValue(UUID itemId, boolean isUser, Class<T> c, Reader<T> reader) {
        MetadataEntryKey key = new MetadataEntryKey(itemId, isUser);
        if (getTable().entries.containsKey(key)) {
            regionStream.setPosition(getTable().entries.get(key).offset);
            byte[] data = StreamUtilities.readExact(regionStream, ReflectionHelper.sizeOf(c));
            return reader.invoke(data, 0);
        }

        return null;
    }
}
