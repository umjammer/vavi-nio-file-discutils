//
// Aaru Data Preservation Suite
//
//
// Filename       : SectorBuilder.cs
// Author(s)      : Natalia Portillo <claunia@claunia.com>
//
// Component      : Device structures decoders.
//
// License
//
//     This program is free software: you can redistribute it and/or modify
//     it under the terms of the GNU General Public License as
//     published by the Free Software Foundation, either version 3 of the
//     License, or (at your option) any later version.
//
//     This program is distributed in the hope that it will be useful,
//     but WITHOUT ANY WARRANTY; without even the implied warranty of
//     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//     GNU General Public License for more details.
//
//     You should have received a copy of the GNU General Public License
//     along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//
// Copyright © 2011-2021 Natalia Portillo
//

package aaru.decoder;

import aaru.commonType.TrackType;
import dotnet4j.util.compat.Tuple3;
import vavi.util.ByteUtil;


public class SectorBuilder {

    final byte[] eccBTable;
    final byte[] eccFTable;
    final int[] edcTable;

    public SectorBuilder() {
        eccFTable = new byte[256];
        eccBTable = new byte[256];
        edcTable = new int[256];

        for (int i = 0; i < 256; i++) {
            int edc = i;
            int j = (i << 1) ^ ((i & 0x80) == 0x80 ? 0x11D : 0);
            eccFTable[i] = (byte) j;
            eccBTable[i ^ j] = (byte) i;

            for (j = 0; j < 8; j++)
                edc = (edc >> 1) ^ ((edc & 1) > 0 ? 0xD8018001 : 0);

            edcTable[i] = edc;
        }
    }

    static Tuple3<Byte, Byte, Byte> lbaToMsf(long pos) {
        return new Tuple3<>((byte) ((pos + 150) / 75 / 60), (byte) ((pos + 150) / 75 % 60), (byte) ((pos + 150) % 75));
    }

    /**
     * @param sector must point to a full 2352-byte sector
     */
    public void reconstructPrefix(/*ref*/ byte[] sector, TrackType type, long lba) {
        //
        // Sync
        //
        sector[0x000] = 0x00;
        sector[0x001] = (byte) 0xFF;
        sector[0x002] = (byte) 0xFF;
        sector[0x003] = (byte) 0xFF;
        sector[0x004] = (byte) 0xFF;
        sector[0x005] = (byte) 0xFF;
        sector[0x006] = (byte) 0xFF;
        sector[0x007] = (byte) 0xFF;
        sector[0x008] = (byte) 0xFF;
        sector[0x009] = (byte) 0xFF;
        sector[0x00A] = (byte) 0xFF;
        sector[0x00B] = 0x00;

        Tuple3<Byte, Byte, Byte> msf = lbaToMsf(lba);

        sector[0x00C] = (byte) (((msf.getItem1() / 10) << 4) + (msf.getItem1() % 10));
        sector[0x00D] = (byte) (((msf.getItem2() / 10) << 4) + (msf.getItem2() % 10));
        sector[0x00E] = (byte) (((msf.getItem3() / 10) << 4) + (msf.getItem3() % 10));

        switch (type) {
        case CdMode1:
            //
            // Mode
            //
            sector[0x00F] = 0x01;

            break;
        case CdMode2Form1:
        case CdMode2Form2:
        case CdMode2Formless:
            //
            // Mode
            //
            sector[0x00F] = 0x02;

            //
            // flags
            //
            sector[0x010] = sector[0x014];
            sector[0x011] = sector[0x015];
            sector[0x012] = sector[0x016];
            sector[0x013] = sector[0x017];

            break;
        default:
            break;
        }
    }

    int computeEdc(int edc, byte[] src, int size, int srcOffset/* =0*/) {
        int pos = srcOffset;

        for (; size > 0; size--)
            edc = (edc >> 8) ^ edcTable[(edc ^ src[pos++]) & 0xFF];

        return edc;
    }

    /**
     * @param sector must point to a full 2352-byte sector
     */
    public void reconstructEcc(/*ref*/ byte[] sector, TrackType type) {
        byte[] computedEdc;

        switch (type) {
        //
        // Compute EDC
        //
        case CdMode1:
            computedEdc = ByteUtil.getLeBytes(computeEdc(0, sector, 0x810, 0));
            sector[0x810] = computedEdc[0];
            sector[0x811] = computedEdc[1];
            sector[0x812] = computedEdc[2];
            sector[0x813] = computedEdc[3];

            break;
        case CdMode2Form1:
            computedEdc = ByteUtil.getLeBytes(computeEdc(0, sector, 0x808, 0x10));
            sector[0x818] = computedEdc[0];
            sector[0x819] = computedEdc[1];
            sector[0x81A] = computedEdc[2];
            sector[0x81B] = computedEdc[3];

            break;
        case CdMode2Form2:
            computedEdc = ByteUtil.getLeBytes(computeEdc(0, sector, 0x91C, 0x10));
            sector[0x92C] = computedEdc[0];
            sector[0x92D] = computedEdc[1];
            sector[0x92E] = computedEdc[2];
            sector[0x92F] = computedEdc[3];

            break;
        default:
            return;
        }

        byte[] zeroaddress = new byte[4];

        switch (type) {
        //
        // Compute ECC
        //
        case CdMode1:
            //
            // Reserved
            //
            sector[0x814] = 0x00;
            sector[0x815] = 0x00;
            sector[0x816] = 0x00;
            sector[0x817] = 0x00;
            sector[0x818] = 0x00;
            sector[0x819] = 0x00;
            sector[0x81A] = 0x00;
            sector[0x81B] = 0x00;
            writeEccSector(sector, sector, /*ref*/ sector, 0xC, 0x10, 0x81C);

            break;
        case CdMode2Form1:
            writeEccSector(zeroaddress, sector, /*ref*/ sector, 0, 0x10, 0x81C);

            break;
        default:
        }

        //
        // Done
        //
    }

    void writeEccSector(byte[] address, byte[] data, /*ref*/ byte[] ecc, int addressOffset, int dataOffset,
                        int eccOffset) {
        writeEcc(address, data, 86, 24, 2, 86, /*ref*/ ecc, addressOffset, dataOffset, eccOffset);         // P
        writeEcc(address, data, 52, 43, 86, 88, /*ref*/ ecc, addressOffset, dataOffset, eccOffset + 0xAC); // Q
    }

    void writeEcc(byte[] address, byte[] data, int majorCount, int minorCount, int majorMult, int minorInc,
            /*ref*/ byte[] ecc, int addressOffset, int dataOffset, int eccOffset) {
        int size = majorCount * minorCount;
        int major;

        for (major = 0; major < majorCount; major++) {
            int idx = ((major >> 1) * majorMult) + (major & 1);
            byte eccA = 0;
            byte eccB = 0;
            int minor;

            for (minor = 0; minor < minorCount; minor++) {
                byte temp = idx < 4 ? address[idx + addressOffset] : data[idx + dataOffset - 4];
                idx += minorInc;

                if (idx >= size)
                    idx -= size;

                eccA ^= temp;
                eccB ^= temp;
                eccA = eccFTable[eccA];
            }

            eccA = eccBTable[eccFTable[eccA] ^ eccB];
            ecc[major + eccOffset] = eccA;
            ecc[major + majorCount + eccOffset] = (byte) (eccA ^ eccB);
        }
    }
}
