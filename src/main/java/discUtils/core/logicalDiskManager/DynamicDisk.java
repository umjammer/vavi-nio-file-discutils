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

package discUtils.core.logicalDiskManager;

import java.io.PrintWriter;
import java.util.UUID;

import discUtils.core.IDiagnosticTraceable;
import discUtils.core.VirtualDisk;
import discUtils.core.partitions.BiosPartitionTable;
import discUtils.core.partitions.GuidPartitionTypes;
import discUtils.core.partitions.PartitionInfo;
import discUtils.core.partitions.PartitionTable;
import discUtils.streams.SparseStream;
import discUtils.streams.util.Sizes;


class DynamicDisk implements IDiagnosticTraceable {

    private static final UUID EMPTY = new UUID(0L, 0L);

    private final VirtualDisk disk;

    private final PrivateHeader header;

    public DynamicDisk(VirtualDisk disk) {
        this.disk = disk;
        header = getPrivateHeader(this.disk);
        TocBlock toc = getTableOfContents();
        long dbStart = header.configurationStartLba * 512 + toc.item1Start * 512;
        this.disk.getContent().position(dbStart);
        database = new Database(this.disk.getContent());
    }

    public SparseStream getContent() {
        return disk.getContent();
    }

    private final Database database;

    public Database getDatabase() {
        return database;
    }

    public long getDataOffset() {
        return header.dataStartLba;
    }

    public UUID getGroupId() {
        return header.diskGroupId == null || header.diskGroupId.isEmpty() ? EMPTY : UUID.fromString(header.diskGroupId);
    }

    public UUID getId() {
        return UUID.fromString(header.diskId);
    }

    @Override public void dump(PrintWriter writer, String linePrefix) {
        writer.println(linePrefix + "DISK (" + header.diskId + ")");
        writer.println(linePrefix + "      Metadata version: " + ((header.version >>> 16) & 0xFFFF) + "." +
                       (header.version & 0xFFFF));
        writer.println(linePrefix + "             Timestamp: " + header.timestamp);
        writer.println(linePrefix + "               Disk Id: " + header.diskId);
        writer.println(linePrefix + "               Host Id: " + header.hostId);
        writer.println(linePrefix + "         Disk Group Id: " + header.diskGroupId);
        writer.println(linePrefix + "       Disk Group Name: " + header.diskGroupName);
        writer.println(linePrefix + "            Data Start: " + header.dataStartLba + " (Sectors)");
        writer.println(linePrefix + "             Data Size: " + header.dataSizeLba + " (Sectors)");
        writer.println(linePrefix + "   Configuration Start: " + header.configurationStartLba + " (Sectors)");
        writer.println(linePrefix + "    Configuration Size: " + header.configurationSizeLba + " (Sectors)");
        writer.println(linePrefix + "              TOC Size: " + header.tocSizeLba + " (Sectors)");
        writer.println(linePrefix + "              Next TOC: " + header.nextTocLba + " (Sectors)");
        writer.println(linePrefix + "     Number of Configs: " + header.numberOfConfigs);
        writer.println(linePrefix + "           Config Size: " + header.configurationSizeLba + " (Sectors)");
        writer.println(linePrefix + "        Number of Logs: " + header.numberOfLogs);
        writer.println(linePrefix + "              Log Size: " + header.logSizeLba + " (Sectors)");
    }

    static PrivateHeader getPrivateHeader(VirtualDisk disk) {
        if (disk.isPartitioned()) {
            long headerPos = 0;
            PartitionTable pt = disk.getPartitions();
            if (pt instanceof BiosPartitionTable) {
                headerPos = 0xc00;
            } else {
                for (PartitionInfo part : pt.getPartitions()) {
                    if (part.getGuidType().equals(GuidPartitionTypes.WindowsLdmMetadata)) {
                        headerPos = part.getLastSector() * Sizes.Sector;
                        break;
                    }
                }
            }

            if (headerPos != 0) {
                disk.getContent().position(headerPos);
                byte[] buffer = new byte[Sizes.Sector];
                disk.getContent().read(buffer, 0, buffer.length);
                PrivateHeader hdr = new PrivateHeader();
                hdr.readFrom(buffer, 0);
                return hdr;
            }
        }

        return null;
    }

    private TocBlock getTableOfContents() {
        byte[] buffer = new byte[(int) header.tocSizeLba * 512];
        disk.getContent().position(header.configurationStartLba * 512 + 1 * header.tocSizeLba * 512);

        disk.getContent().read(buffer, 0, buffer.length);
        TocBlock tocBlock = new TocBlock();
        tocBlock.readFrom(buffer, 0);

        if ("TOCBLOCK".equals(tocBlock.signature)) {
            return tocBlock;
        }

        return null;
    }
}
