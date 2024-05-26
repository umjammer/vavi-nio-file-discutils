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

package discUtils.vmdk;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import discUtils.core.Geometry;
import discUtils.streams.util.Sizes;
import dotnet4j.io.Stream;
import dotnet4j.io.StreamReader;
import vavi.util.ByteUtil;


public class DescriptorFile {

    private static final String HeaderVersion = "version";

    private static final String HeaderContentId = "CID";

    private static final String HeaderParentContentId = "parentCID";

    private static final String HeaderCreateType = "createType";

    private static final String HeaderParentFileNameHint = "parentFileNameHint";

    private static final String DiskDbAdapterType = "ddb.adapterType";

    private static final String DiskDbSectors = "ddb.geometry.sectors";

    private static final String DiskDbHeads = "ddb.geometry.heads";

    private static final String DiskDbCylinders = "ddb.geometry.cylinders";

    private static final String DiskDbBiosSectors = "ddb.geometry.biosSectors";

    private static final String DiskDbBiosHeads = "ddb.geometry.biosHeads";

    private static final String DiskDbBiosCylinders = "ddb.geometry.biosCylinders";

    private static final String DiskDbHardwareVersion = "ddb.virtualHWVersion";

    private static final String DiskDbUuid = "ddb.uuid";

    private static final long MaxSize = 20 * Sizes.OneKiB;

    private final List<DescriptorFileEntry> diskDataBase;

    private final List<DescriptorFileEntry> header;

    public DescriptorFile() {
        header = new ArrayList<>();
        extents = new ArrayList<>();
        diskDataBase = new ArrayList<>();
        header.add(new DescriptorFileEntry(HeaderVersion, "1", DescriptorFileEntryType.Plain));
        header.add(new DescriptorFileEntry(HeaderContentId, "ffffffff", DescriptorFileEntryType.Plain));
        header.add(new DescriptorFileEntry(HeaderParentContentId, "ffffffff", DescriptorFileEntryType.Plain));
        header.add(new DescriptorFileEntry(HeaderCreateType, "", DescriptorFileEntryType.Quoted));
    }

    public DescriptorFile(Stream source) {
        header = new ArrayList<>();
        extents = new ArrayList<>();
        diskDataBase = new ArrayList<>();
        load(source);
    }

    public DiskAdapterType getAdapterType() {
        return parseAdapterType(getDiskDatabase(DiskDbAdapterType));
    }

    public void setAdapterType(DiskAdapterType value) {
        setDiskDatabase(DiskDbAdapterType, formatAdapterType(value));
    }

    public Geometry getBiosGeometry() {
        String cylStr = getDiskDatabase(DiskDbBiosCylinders);
        String headsStr = getDiskDatabase(DiskDbBiosHeads);
        String sectorsStr = getDiskDatabase(DiskDbBiosSectors);
        if (cylStr != null && !cylStr.isEmpty() && headsStr != null && !headsStr.isEmpty() && sectorsStr != null
                && !sectorsStr.isEmpty()) {
            return new Geometry(Integer.parseInt(cylStr), Integer.parseInt(headsStr), Integer.parseInt(sectorsStr));
        }

        return null;
    }

    public void setBiosGeometry(Geometry value) {
        setDiskDatabase(DiskDbBiosCylinders, String.valueOf(value.getCylinders()));
        setDiskDatabase(DiskDbBiosHeads, String.valueOf(value.getHeadsPerCylinder()));
        setDiskDatabase(DiskDbBiosSectors, String.valueOf(value.getSectorsPerTrack()));
    }

    public int getContentId() {
        return (int) Long.parseLong(getHeader(HeaderContentId), 16);
    }

    public void setContentId(int value) {
        setHeader(HeaderContentId, String.format("%8x", value), DescriptorFileEntryType.Plain);
    }

    public DiskCreateType getCreateType() {
        return parseCreateType(getHeader(HeaderCreateType));
    }

    public void setCreateType(DiskCreateType value) {
        setHeader(HeaderCreateType, formatCreateType(value), DescriptorFileEntryType.Plain);
    }

    public Geometry getDiskGeometry() {
        String cylStr = getDiskDatabase(DiskDbCylinders);
        String headsStr = getDiskDatabase(DiskDbHeads);
        String sectorsStr = getDiskDatabase(DiskDbSectors);
        if (cylStr != null && !cylStr.isEmpty() && headsStr != null && !headsStr.isEmpty() && sectorsStr != null
                && !sectorsStr.isEmpty()) {
            return new Geometry(Integer.parseInt(cylStr), Integer.parseInt(headsStr), Integer.parseInt(sectorsStr));
        }

        return null;
    }

    public void setDiskGeometry(Geometry value) {
        setDiskDatabase(DiskDbCylinders, String.valueOf(value.getCylinders()));
        setDiskDatabase(DiskDbHeads, String.valueOf(value.getHeadsPerCylinder()));
        setDiskDatabase(DiskDbSectors, String.valueOf(value.getSectorsPerTrack()));
    }

    private List<ExtentDescriptor> extents;

    public List<ExtentDescriptor> getExtents() {
        return extents;
    }

    public String getHardwareVersion() {
        return getDiskDatabase(DiskDbHardwareVersion);
    }

    public void setHardwareVersion(String value) {
        setDiskDatabase(DiskDbHardwareVersion, value);
    }

    public int getParentContentId() {
        return (int) Long.parseLong(getHeader(HeaderParentContentId), 16);
    }

    public void setParentContentId(int value) {
        setHeader(HeaderParentContentId, String.format("%8x", value), DescriptorFileEntryType.Plain);
    }

    public String getParentFileNameHint() {
        return getHeader(HeaderParentFileNameHint);
    }

    public void setParentFileNameHint(String value) {
        setHeader(HeaderParentFileNameHint, value, DescriptorFileEntryType.Quoted);
    }

    public UUID getUniqueId() {
        return parseUuid(getDiskDatabase(DiskDbUuid));
    }

    public void setUniqueId(UUID value) {
        setDiskDatabase(DiskDbUuid, formatUuid(value));
    }

    public void write(Stream stream) {
        StringBuilder content = new StringBuilder();
        content.append("# Disk DescriptorFile\n");
        for (DescriptorFileEntry descriptorFileEntry : header) {
            content.append(descriptorFileEntry.toString()).append("\n");
        }
        content.append("\n");
        content.append("# Extent description\n");
        for (int i = 0; i < getExtents().size(); ++i) {
            content.append(getExtents().get(i)).append("\n");
        }
        content.append("\n");
        content.append("# The Disk Data base\n");
        content.append("#DDB\n");
        for (DescriptorFileEntry descriptorFileEntry : diskDataBase) {
            content.append(descriptorFileEntry.toString()).append("\n");
        }
        byte[] contentBytes = content.toString().getBytes(StandardCharsets.US_ASCII);
        stream.write(contentBytes, 0, contentBytes.length);
    }

    private static DiskAdapterType parseAdapterType(String value) {
        switch (value) {
        case "ide":
            return DiskAdapterType.Ide;
        case "buslogic":
            return DiskAdapterType.BusLogicScsi;
        case "lsilogic":
            return DiskAdapterType.LsiLogicScsi;
        case "legacyESX":
            return DiskAdapterType.LegacyEsx;
        default:
            throw new IllegalArgumentException(String.format("Unknown type: %s", value));
        }
    }

    private static String formatAdapterType(DiskAdapterType value) {
        switch (value) {
        case Ide:
            return "ide";
        case BusLogicScsi:
            return "buslogic";
        case LsiLogicScsi:
            return "lsilogic";
        case LegacyEsx:
            return "legacyESX";
        default:
            throw new IllegalArgumentException(String.format("Unknown type: %s", value));
        }
    }

    private static DiskCreateType parseCreateType(String value) {
        switch (value) {
        case "monolithicSparse":
            return DiskCreateType.MonolithicSparse;
        case "vmfsSparse":
            return DiskCreateType.VmfsSparse;
        case "monolithicFlat":
            return DiskCreateType.MonolithicFlat;
        case "vmfs":
            return DiskCreateType.Vmfs;
        case "twoGbMaxExtentSparse":
            return DiskCreateType.TwoGbMaxExtentSparse;
        case "twoGbMaxExtentFlat":
            return DiskCreateType.TwoGbMaxExtentFlat;
        case "fullDevice":
            return DiskCreateType.FullDevice;
        case "vmfsRaw":
            return DiskCreateType.VmfsRaw;
        case "partitionedDevice":
            return DiskCreateType.PartitionedDevice;
        case "vmfsRawDeviceMap":
            return DiskCreateType.VmfsRawDeviceMap;
        case "vmfsPassthroughRawDeviceMap":
            return DiskCreateType.VmfsPassthroughRawDeviceMap;
        case "streamOptimized":
            return DiskCreateType.StreamOptimized;
        default:
            throw new IllegalArgumentException(String.format("Unknown type: %s", value));
        }
    }

    private static String formatCreateType(DiskCreateType value) {
        switch (value) {
        case MonolithicSparse:
            return "monolithicSparse";
        case VmfsSparse:
            return "vmfsSparse";
        case MonolithicFlat:
            return "monolithicFlat";
        case Vmfs:
            return "vmfs";
        case TwoGbMaxExtentSparse:
            return "twoGbMaxExtentSparse";
        case TwoGbMaxExtentFlat:
            return "twoGbMaxExtentFlat";
        case FullDevice:
            return "fullDevice";
        case VmfsRaw:
            return "vmfsRaw";
        case PartitionedDevice:
            return "partitionedDevice";
        case VmfsRawDeviceMap:
            return "vmfsRawDeviceMap";
        case VmfsPassthroughRawDeviceMap:
            return "vmfsPassthroughRawDeviceMap";
        case StreamOptimized:
            return "streamOptimized";
        default:
            throw new IllegalArgumentException(String.format("Unknown type: %s", value));
        }
    }

    private static UUID parseUuid(String value) {
        byte[] data = new byte[16];
        String[] bytesAsHex = value.split("[ -]");
        if (bytesAsHex.length != 16) {
            throw new IllegalArgumentException("Invalid UUID");
        }

        for (int i = 0; i < 16; ++i) {
            data[i] = Byte.parseByte(bytesAsHex[i], 16);
        }
        return UUID.nameUUIDFromBytes(data);
    }

    private static String formatUuid(UUID value) {
        byte[] data = new byte[16];
        ByteUtil.writeLeUUID(value, data, 0);
        return String.format("%02x %02x %02x %02x %02x %02x %02x %02x-%02x %02x %02x %02x %02x %02x %02x %02x",
                             data[0],
                             data[1],
                             data[2],
                             data[3],
                             data[4],
                             data[5],
                             data[6],
                             data[7],
                             data[8],
                             data[9],
                             data[10],
                             data[11],
                             data[12],
                             data[13],
                             data[14],
                             data[15]);
    }

    private String getHeader(String key) {
        for (DescriptorFileEntry entry : header) {
            if (entry.getKey().equals(key)) {
//logger.log(Level.DEBUG, entry.getKey() + ", " + key);
                return entry.getValue();
            }
        }
        return null;
    }

    private void setHeader(String key, String newValue, DescriptorFileEntryType type) {
        for (DescriptorFileEntry entry : header) {
            if (entry.getKey().equals(key)) {
                entry.setValue(newValue);
                return;
            }
        }
        header.add(new DescriptorFileEntry(key, newValue, type));
    }

    private String getDiskDatabase(String key) {
        for (DescriptorFileEntry entry : diskDataBase) {
            if (entry.getKey().equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void setDiskDatabase(String key, String value) {
        for (DescriptorFileEntry entry : diskDataBase) {
            if (entry.getKey().equals(key)) {
                entry.setValue(value);
                return;
            }
        }
        diskDataBase.add(new DescriptorFileEntry(key, value, DescriptorFileEntryType.Quoted));
    }

    private void load(Stream source) {
        if (source.getLength() - source.position() > MaxSize) {
            throw new dotnet4j.io.IOException(String
                    .format("Invalid VMDK descriptor file, more than %s bytes in length", MaxSize));
        }

        StreamReader reader = new StreamReader(source);
        String line = reader.readLine();
        while (line != null) {
            line = line.replaceFirst("\0*$", "");
            int commentPos = line.indexOf('#');
            if (commentPos >= 0) {
                line = line.substring(0, commentPos);
            }

            if (line.length() > 0) {
                if (line.startsWith("RW") || line.startsWith("RDONLY") || line.startsWith("NOACCESS")) {
                    getExtents().add(ExtentDescriptor.parse(line));
                } else {
                    DescriptorFileEntry entry = DescriptorFileEntry.parse(line);
                    if (entry.getKey().startsWith("ddb.")) {
                        diskDataBase.add(entry);
                    } else {
                        header.add(entry);
                    }
                }
            }

            line = reader.readLine();
        }
    }
}
