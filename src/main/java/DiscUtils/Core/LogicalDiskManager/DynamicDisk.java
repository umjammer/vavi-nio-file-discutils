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

package DiscUtils.Core.LogicalDiskManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.Partitions.BiosPartitionTable;
import DiscUtils.Core.Partitions.GuidPartitionTypes;
import DiscUtils.Core.Partitions.PartitionInfo;
import DiscUtils.Core.Partitions.PartitionTable;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Util.Sizes;


public class DynamicDisk implements IDiagnosticTraceable {
    private final VirtualDisk _disk;

    private final PrivateHeader _header;

    public DynamicDisk(VirtualDisk disk) throws IOException {
        _disk = disk;
        _header = getPrivateHeader(_disk);
        TocBlock toc = getTableOfContents();
        long dbStart = _header.ConfigurationStartLba * 512 + toc.Item1Start * 512;
        _disk.getContent().setPosition(dbStart);
        __Database = new Database(_disk.getContent());
    }

    public SparseStream getContent() throws IOException {
        return _disk.getContent();
    }

    private Database __Database;

    public Database getDatabase() {
        return __Database;
    }

    public long getDataOffset() {
        return _header.DataStartLba;
    }

    public UUID getGroupId() {
        return _header.DiskGroupId == null || _header.DiskGroupId.isEmpty() ? new UUID(0L, 0L) : UUID.fromString(_header.DiskGroupId);
    }

    public UUID getId() {
        return UUID.fromString(_header.DiskId);
    }

    public void dump(PrintWriter writer, String linePrefix) {
        writer.println(linePrefix + "DISK (" + _header.DiskId + ")");
        writer.println(linePrefix + "      Metadata Version: " + ((_header.Version >>> 16) & 0xFFFF) + "." +
                       (_header.Version & 0xFFFF));
        writer.println(linePrefix + "             Timestamp: " + _header.Timestamp);
        writer.println(linePrefix + "               Disk Id: " + _header.DiskId);
        writer.println(linePrefix + "               Host Id: " + _header.HostId);
        writer.println(linePrefix + "         Disk Group Id: " + _header.DiskGroupId);
        writer.println(linePrefix + "       Disk Group Name: " + _header.DiskGroupName);
        writer.println(linePrefix + "            Data Start: " + _header.DataStartLba + " (Sectors)");
        writer.println(linePrefix + "             Data Size: " + _header.DataSizeLba + " (Sectors)");
        writer.println(linePrefix + "   Configuration Start: " + _header.ConfigurationStartLba + " (Sectors)");
        writer.println(linePrefix + "    Configuration Size: " + _header.ConfigurationSizeLba + " (Sectors)");
        writer.println(linePrefix + "              TOC Size: " + _header.TocSizeLba + " (Sectors)");
        writer.println(linePrefix + "              Next TOC: " + _header.NextTocLba + " (Sectors)");
        writer.println(linePrefix + "     Number of Configs: " + _header.NumberOfConfigs);
        writer.println(linePrefix + "           Config Size: " + _header.ConfigurationSizeLba + " (Sectors)");
        writer.println(linePrefix + "        Number of Logs: " + _header.NumberOfLogs);
        writer.println(linePrefix + "              Log Size: " + _header.LogSizeLba + " (Sectors)");
    }

    public static PrivateHeader getPrivateHeader(VirtualDisk disk) throws IOException {
        if (disk.isPartitioned()) {
            long headerPos = 0;
            PartitionTable pt = disk.getPartitions();
            if (pt instanceof BiosPartitionTable) {
                headerPos = 0xc00;
            } else {
                for (PartitionInfo part : pt.getPartitions()) {
                    if (part.getGuidType() == GuidPartitionTypes.WindowsLdmMetadata) {
                        headerPos = part.getLastSector() * Sizes.Sector;
                    }

                }
            }
            if (headerPos != 0) {
                disk.getContent().setPosition(headerPos);
                byte[] buffer = new byte[Sizes.Sector];
                disk.getContent().read(buffer, 0, buffer.length);
                PrivateHeader hdr = new PrivateHeader();
                hdr.readFrom(buffer, 0);
                return hdr;
            }

        }

        return null;
    }

    private TocBlock getTableOfContents() throws IOException {
        byte[] buffer = new byte[(int) _header.TocSizeLba * 512];
        _disk.getContent().setPosition(_header.ConfigurationStartLba * 512 + 1 * _header.TocSizeLba * 512);
        _disk.getContent().read(buffer, 0, buffer.length);
        TocBlock tocBlock = new TocBlock();
        tocBlock.readFrom(buffer, 0);
        if ("TOCBLOCK".equals(tocBlock.Signature)) {
            return tocBlock;
        }

        return null;
    }

}
