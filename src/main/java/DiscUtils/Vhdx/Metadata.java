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

package DiscUtils.Vhdx;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.UUID;

import DiscUtils.Core.CoreCompat.ReflectionHelper;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.Stream;


public final class Metadata {
    private final Stream _regionStream;

    @SuppressWarnings("unused")
    private UUID _page83Data;

    public Metadata(Stream regionStream) {
        _regionStream = regionStream;
        _regionStream.setPosition(0);
        _table = StreamUtilities.readStruct(MetadataTable.class, _regionStream);
        _fileParameters = readStruct(FileParameters.class, MetadataTable.FileParametersGuid, false);
        _diskSize = readValue(MetadataTable.VirtualDiskSizeGuid, false, Long.TYPE, EndianUtilities::toUInt64LittleEndian);
        _page83Data = readValue(MetadataTable.Page83DataGuid, false, UUID.class, EndianUtilities::toGuidLittleEndian);
        _logicalSectorSize = readValue(MetadataTable.LogicalSectorSizeGuid,
                                       false,
                                       Integer.TYPE,
                                       EndianUtilities::toUInt32LittleEndian);
        _physicalSectorSize = readValue(MetadataTable.PhysicalSectorSizeGuid,
                                        false,
                                        Integer.TYPE,
                                        EndianUtilities::toUInt32LittleEndian);
        _parentLocator = readStruct(ParentLocator.class, MetadataTable.ParentLocatorGuid, false);
    }

    @FunctionalInterface
    private static interface Reader<T> {

        T invoke(byte[] buffer, int offset);
    }

    @FunctionalInterface
    private static interface Writer<T> {

        void invoke(T val, byte[] buffer, int offset);
    }

    private MetadataTable _table;

    public MetadataTable getTable() {
        return _table;
    }

    private FileParameters _fileParameters;

    public FileParameters getFileParameters() {
        return _fileParameters;
    }

    private long _diskSize;

    public long getDiskSize() {
        return _diskSize;
    }

    private int _logicalSectorSize;

    public int getLogicalSectorSize() {
        return _logicalSectorSize;
    }

    private int _physicalSectorSize;

    public int getPhysicalSectorSize() {
        return _physicalSectorSize;
    }

    private ParentLocator _parentLocator;

    public ParentLocator getParentLocator() {
        return _parentLocator;
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
        entry.ItemId = id;
        entry.Offset = dataOffset;
        entry.Length = data.size();
        entry.Flags = flags;
        header.Entries.put(key, entry);

        stream.setPosition(dataOffset);
        StreamUtilities.writeStruct(stream, data);

        return entry.Length;
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
        entry.ItemId = id;
        entry.Offset = dataOffset;
        entry.Length = ReflectionHelper.sizeOf(data.getClass());
        entry.Flags = flags;

        header.Entries.put(key, entry);

        stream.setPosition(dataOffset);

        byte[] buffer = new byte[entry.Length];
        writer.invoke(data, buffer, 0);
        stream.write(buffer, 0, buffer.length);

        return entry.Length;
    }

    private <T extends IByteArraySerializable> T readStruct(Class<T> c, UUID itemId, boolean isUser) {
        MetadataEntryKey key = new MetadataEntryKey(itemId, isUser);
        if (getTable().Entries.containsKey(key)) {
            MetadataEntry entry = getTable().Entries.get(key);
            _regionStream.setPosition(entry.Offset);
            return StreamUtilities.readStruct(c, _regionStream, entry.Length);
        }

        return null;
    }

    private <T extends Serializable> T readValue(UUID itemId, boolean isUser, Class<T> c, Reader<T> reader) {
        MetadataEntryKey key = new MetadataEntryKey(itemId, isUser);
        if (getTable().Entries.containsKey(key)) {
            _regionStream.setPosition(getTable().Entries.get(key).Offset);
            byte[] data = StreamUtilities.readExact(_regionStream, ReflectionHelper.sizeOf(c));
            return reader.invoke(data, 0);
        }

        return null;
    }
}
