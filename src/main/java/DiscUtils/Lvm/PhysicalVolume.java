//
// Copyright (c) 2016, Bianco Veigel
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

package DiscUtils.Lvm;

import DiscUtils.Core.Partitions.PartitionInfo;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.Stream;


public class PhysicalVolume {
    public static final short SECTOR_SIZE = 512;

    private static final int INITIAL_CRC = 0xf597a6cf;

    public final PhysicalVolumeLabel _PhysicalVolumeLabel;

    public final PvHeader PvHeader;

    public VolumeGroupMetadata VgMetadata;

    private Stream __Content;

    public Stream getContent() {
        return __Content;
    }

    public void setContent(Stream value) {
        __Content = value;
    }

    public PhysicalVolume(PhysicalVolumeLabel physicalVolumeLabel, Stream content) {
        _PhysicalVolumeLabel = physicalVolumeLabel;
        PvHeader = new PvHeader();
        content.setPosition(physicalVolumeLabel.Sector * SECTOR_SIZE);
        byte[] buffer = StreamUtilities.readExact(content, SECTOR_SIZE);
        PvHeader.readFrom(buffer, (int) physicalVolumeLabel.Offset);
        if (PvHeader.MetadataDiskAreas.size() > 0) {
            DiskArea area = PvHeader.MetadataDiskAreas.get(0);
            VolumeGroupMetadata metadata = new VolumeGroupMetadata();
            content.setPosition(area.Offset);
            buffer = StreamUtilities.readExact(content, (int) area.Length);
            metadata.readFrom(buffer, 0x0);
            VgMetadata = metadata;
        }

        setContent(content);
    }

    public static boolean tryOpen(PartitionInfo volumeInfo, PhysicalVolume[] pv) {
        SparseStream content = volumeInfo.open();
        return tryOpen(content, pv);
    }

    public static boolean tryOpen(Stream content, PhysicalVolume[] pv) {
        PhysicalVolumeLabel[] label = new PhysicalVolumeLabel[1];
        pv[0] = null;
        boolean r = !searchLabel(content, label);
        if (!r) {
            return false;
        }

        pv[0] = new PhysicalVolume(label[0], content);
        return true;
    }

    private static boolean searchLabel(Stream content, PhysicalVolumeLabel[] pvLabel) {
        pvLabel[0] = null;
        content.setPosition(0);
        byte[] buffer = new byte[SECTOR_SIZE];
        for (int i = 0; i < 4; i++) {
            if (StreamUtilities.readMaximum(content, buffer, 0, SECTOR_SIZE) != SECTOR_SIZE) {
                return false;
            }

            String label = EndianUtilities.bytesToString(buffer, 0x0, 0x8);
            if (label.equals(PhysicalVolumeLabel.LABEL_ID)) {
                pvLabel[0] = new PhysicalVolumeLabel();
                pvLabel[0].readFrom(buffer, 0x0);
                if (pvLabel[0].Sector != i) {
                    return false;
                }

                //Invalid PV Sector;
                if (pvLabel[0].Crc != pvLabel[0].CalculatedCrc) {
                    return false;
                }

                //Invalid PV CRC
                if (!pvLabel[0].Label2.equals(PhysicalVolumeLabel.LVM2_LABEL)) {
                    return false;
                }

                return true;
            }

        }
        return false;
    }

    // Invalid LVM2 Label

    /**
     * LVM2.2.02.79:lib/misc/crc.c:_calc_crc_old()
     */
    public static int calcCrc(byte[] buffer, int offset, int length) {
        int crc = INITIAL_CRC;
        final int[] crctab = new int[] {
            0x00000000, 0x1db71064, 0x3b6e20c8, 0x26d930ac, 0x76dc4190, 0x6b6b51f4, 0x4db26158, 0x5005713c, 0xedb88320,
            0xf00f9344, 0xd6d6a3e8, 0xcb61b38c, 0x9b64c2b0, 0x86d3d2d4, 0xa00ae278, 0xbdbdf21c
        };
        int i = offset;
        while (i < offset + length) {
            crc ^= buffer[i];
            crc = (crc >>> 4) ^ crctab[crc & 0xf];
            crc = (crc >>> 4) ^ crctab[crc & 0xf];
            i++;
        }
        return crc;
    }
}
