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

package discUtils.lvm;

import java.nio.charset.StandardCharsets;

import discUtils.core.partitions.PartitionInfo;
import discUtils.streams.SparseStream;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


public class PhysicalVolume {

    public static final short SECTOR_SIZE = 512;

    private static final long INITIAL_CRC = 0xf597a6cfL;

    public final PhysicalVolumeLabel physicalVolumeLabel;

    public final PvHeader pvHeader;

    public VolumeGroupMetadata vgMetadata;

    private Stream content;

    public Stream getContent() {
        return content;
    }

    public void setContent(Stream value) {
        content = value;
    }

    public PhysicalVolume(PhysicalVolumeLabel physicalVolumeLabel, Stream content) {
        this.physicalVolumeLabel = physicalVolumeLabel;
        pvHeader = new PvHeader();
        content.position(physicalVolumeLabel.sector * SECTOR_SIZE);
        byte[] buffer = StreamUtilities.readExact(content, SECTOR_SIZE);
        pvHeader.readFrom(buffer, (int) physicalVolumeLabel.offset);
        if (pvHeader.metadataDiskAreas.size() > 0) {
            DiskArea area = pvHeader.metadataDiskAreas.get(0);
            VolumeGroupMetadata metadata = new VolumeGroupMetadata();
            content.position(area.offset);
            buffer = StreamUtilities.readExact(content, (int) area.length);
            metadata.readFrom(buffer, 0x0);
            vgMetadata = metadata;
        }

        this.content = content;
    }

    public static boolean tryOpen(PartitionInfo volumeInfo, PhysicalVolume[] pv) {
        SparseStream content = volumeInfo.open();
        return tryOpen(content, pv);
    }

    /**
     * @param pv {@cs out}
     */
    public static boolean tryOpen(Stream content, PhysicalVolume[] pv) {
        PhysicalVolumeLabel[] label = new PhysicalVolumeLabel[1];
        pv[0] = null;
        if (!searchLabel(content, label)) {
            return false;
        }

        pv[0] = new PhysicalVolume(label[0], content);
        return true;
    }

    /**
     * @param pvLabel {@cs out}
     */
    private static boolean searchLabel(Stream content, PhysicalVolumeLabel[] pvLabel) {
        pvLabel[0] = null;
        content.position(0);
        byte[] buffer = new byte[SECTOR_SIZE];
        for (int i = 0; i < 4; i++) {
            if (StreamUtilities.readMaximum(content, buffer, 0, SECTOR_SIZE) != SECTOR_SIZE) {
                return false;
            }

            String label = new String(buffer, 0x0, 0x8, StandardCharsets.US_ASCII);
            if (label.equals(PhysicalVolumeLabel.LABEL_ID)) {
                pvLabel[0] = new PhysicalVolumeLabel();
                pvLabel[0].readFrom(buffer, 0x0);
                if (pvLabel[0].sector != i) {
                    //Invalid PV Sector;
                    return false;
                }
                if (pvLabel[0].crc != pvLabel[0].calculatedCrc) {
                    //Invalid PV CRC
                    return false;
                }
                if (!pvLabel[0].label2.equals(PhysicalVolumeLabel.LVM2_LABEL)) {
                    // Invalid LVM2 Label
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * LVM2.2.02.79:lib/misc/crc.c:_calc_crc_old()
     */
    static int calcCrc(byte[] buffer, int offset, int length) {
        long crc = INITIAL_CRC;
        long[] crcTab = new long[] {
            0x00000000, 0x1db71064, 0x3b6e20c8, 0x26d930ac, 0x76dc4190, 0x6b6b51f4, 0x4db26158, 0x5005713c, 0xedb88320L,
            0xf00f9344L, 0xd6d6a3e8L, 0xcb61b38cL, 0x9b64c2b0L, 0x86d3d2d4L, 0xa00ae278L, 0xbdbdf21cL
        };
        int i = offset;
        while (i < offset + length) {
            crc ^= (buffer[i] & 0xff);
            crc = ((crc & 0xffff_ffffL) >>> 4) ^ crcTab[(int) crc & 0xf];
            crc = ((crc & 0xffff_ffffL) >>> 4) ^ crcTab[(int) crc & 0xf];
            i++;
        }
        return (int) crc;
    }
}
