//
// Aaru Data Preservation Suite
//
//
// Filename       : APFS.cs
// Author(s)      : Natalia Portillo <claunia@claunia.com>
//
// Component      : Apple filesystem plugin.
//
// Description ] ----------------------------------------------------------
//
//     Identifies the Apple filesystem and shows information.
//
// License ] --------------------------------------------------------------
//
//     This library is free software; you can redistribute it and/or modify
//     it under the terms of the GNU Lesser General Public License as
//     published by the Free Software Foundation; either version 2.1 of the
//     License, or (at your option) any later version.
//
//     This library is distributed in the hope that it will be useful, but
//     WITHOUT ANY WARRANTY; without even the implied warranty of
//     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//     Lesser General Public License for more details.
//
//     You should have received a copy of the GNU Lesser General Public
//     License along with this library; if not, see <http://www.gnu.org/licenses/>.
//
//
// Copyright © 2011-2021 Natalia Portillo
//


package aaru.filesystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import aaru.commonType.Partition;
import dotnet4j.io.Stream;
import vavi.util.serdes.Serdes;


/// <summary>Implements detection of the Apple File System (APFS)</summary>
public class APFS //: IFilesystem
{
    static final int APFS_CONTAINER_MAGIC = 0x4253584E; // "NXSB"
    static final int APFS_VOLUME_MAGIC = 0x42535041; // "APSB"

    private Charset charset;

    public Charset getCharset() {
        return null;
    }

    public String getName() { return "Apple File System"; }

    public UUID getId() { return UUID.fromString("A4060F9D-2909-42E2-9D95-DB31FA7EA797"); }

    public String getAuthor () { return "Natalia Portillo"; }

    public boolean Identify(Stream imagePlugin, Partition partition) {
        if (partition.start >= partition.end())
            return false;

        byte[][] sector = new byte[1][];
//        int errno = imagePlugin.readSector(partition.start, /*out byte[]*/ sector);

//        if (errno != 0)
//            return false;

        ContainerSuperBlock nxSb = new ContainerSuperBlock();

        try {
            Serdes.Util.deserialize(new ByteArrayInputStream(sector[0]), nxSb);
        } catch(IOException e) {
            return false;
        }

        return nxSb.magic == APFS_CONTAINER_MAGIC;
    }

    /// <inheritdoc />
    public void GetInformation(Stream imagePlugin, Partition partition, /*out*/ String information,
                               Charset encoding) {
        charset = StandardCharsets.UTF_8;
        StringBuilder sbInformation = new StringBuilder();
        information = "";

        if (partition.start >= partition.end())
            return;

        byte[][] sector = new byte[1][];
//        int errno = imagePlugin.readSector(partition.start, /*out byte[]*/ sector);

//        if (errno != 0)
//            return;

        ContainerSuperBlock nxSb = new ContainerSuperBlock();

        try {
            Serdes.Util.deserialize(new ByteArrayInputStream(sector[0]), nxSb);
        } catch(IOException e) {
            return;
        }

        if (nxSb.magic != APFS_CONTAINER_MAGIC)
            return;

        sbInformation.append("Apple File System").append("\n");
        sbInformation.append("\n");
        sbInformation.append(String.format("%d bytes per block", nxSb.blockSize)).append("\n");

        sbInformation.append(String.format("Container has %d bytes in %d blocks", nxSb.containerBlocks * nxSb.blockSize,
                nxSb.containerBlocks)).append("\n");

        information = sbInformation.toString();
    }

    @Serdes
    static class ContainerSuperBlock {
        public long unknown1; // Varies between copies of the superblock
        public long unknown2;
        public long unknown3; // Varies by 1 between copies of the superblock
        public long unknown4;
        public int magic;
        public int blockSize;
        public long containerBlocks;
    }
}
