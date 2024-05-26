//
// Aaru Data Preservation Suite
//
// License
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

package aaru.checksum;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.util.ByteUtil;

import static java.lang.System.getLogger;


/** Implements ReedSolomon and CRC32 algorithms as used by CD-ROM */
public class CdChecksums {

    private static final Logger logger = getLogger(CdChecksums.class.getName());

    static byte[] eccFTable;
    static byte[] eccBTable;
    static int[] edcTable;

    /**
     * Checks the EDC and ECC of a CD sector
     *
     * @param buffer CD sector
     * @return <c>true</c> if all checks were correct, <c>false</c> if any of them weren't, and <c>null</c> if none of them
     * are present.
     */
    public static Boolean checkCdSector(byte[] buffer) {
        return checkCdSector(buffer, /*out*/ new Boolean[1], /*out*/ new Boolean[1], /*out*/ new Boolean[1]);
    }

    /**
     * Checks the EDC and ECC of a CD sector
     *
     * @param buffer      CD sector
     * @param correctEccP <c>true</c> if ECC P is correct, <c>false</c> if it isn't, and <c>null</c> if there is no ECC
     *                    P in sector.
     * @param correctEccQ <c>true</c> if ECC Q is correct, <c>false</c> if it isn't, and <c>null</c> if there is no ECC
     *                    Q in sector.
     * @param correctEdc  <c>true</c> if EDC is correct, <c>false</c> if it isn't, and <c>null</c> if there is no EDC in
     *                    sector.
     * @return <c>true</c> if all checks were correct, <c>false</c> if any of them weren't, and <c>null</c> if none of them
     * are present.
     */
    public static Boolean checkCdSector(byte[] buffer, /*out*/ Boolean[] correctEccP, /*out*/ Boolean[] correctEccQ,
            /*out*/ Boolean[] correctEdc) {
        correctEccP[0] = null;
        correctEccQ[0] = null;
        correctEdc[0] = null;

        switch (buffer.length) {
        case 2448: {
            byte[] subchannel = new byte[96];
            byte[] channel = new byte[2352];

            System.arraycopy(buffer, 0, channel, 0, 2352);
            System.arraycopy(buffer, 2352, subchannel, 0, 96);

            Boolean channelStatus =
                    checkCdSectorChannel(channel, /*out*/ correctEccP, /*out*/ correctEccQ, /*out*/ correctEdc);

            Boolean subchannelStatus = checkCdSectorSubChannel(subchannel);
            Boolean status = null;

            if (!channelStatus || !subchannelStatus)
                status = false;
            if (channelStatus == null && subchannelStatus) {
                status = true;
            } else if (channelStatus && subchannelStatus == null) {
                status = true;
            }

            return status;
        }

        case 2352:
            return checkCdSectorChannel(buffer, /*out*/ correctEccP, /*out*/ correctEccQ, /*out*/ correctEdc);
        default:
            return null;
        }
    }

    static void initEcc() {
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

    static boolean checkEcc(byte[] address, byte[] data, int majorCount, int minorCount, int majorMult,
                            int minorInc, byte[] ecc) {
        int size = majorCount * minorCount;
        int major;

        for (major = 0; major < majorCount; major++) {
            int index = ((major >> 1) * majorMult) + (major & 1);
            byte eccA = 0;
            byte eccB = 0;
            int minor;

            for (minor = 0; minor < minorCount; minor++) {
                byte temp = index < 4 ? address[index] : data[index - 4];
                index += minorInc;

                if (index >= size)
                    index -= size;

                eccA ^= temp;
                eccB ^= temp;
                eccA = eccFTable[eccA];
            }

            eccA = eccBTable[eccFTable[eccA] ^ eccB];

            if (ecc[major] != eccA ||
                    ecc[major + majorCount] != (eccA ^ eccB))
                return false;
        }

        return true;
    }

    static Boolean checkCdSectorChannel(byte[] channel, /*out*/ Boolean[] correctEccP, /*out*/ Boolean[] correctEccQ,
            /*out*/ Boolean[] correctEdc) {
        initEcc();

        correctEccP[0] = null;
        correctEccQ[0] = null;
        correctEdc[0] = null;

        if (channel[0x000] != 0x00 ||
            (channel[0x001] & 0xff) != 0xFF ||
            (channel[0x002] & 0xff) != 0xFF ||
            (channel[0x003] & 0xff) != 0xFF ||
            (channel[0x004] & 0xff) != 0xFF ||
            (channel[0x005] & 0xff) != 0xFF ||
            (channel[0x006] & 0xff) != 0xFF ||
            (channel[0x007] & 0xff) != 0xFF ||
            (channel[0x008] & 0xff) != 0xFF ||
            (channel[0x009] & 0xff) != 0xFF ||
            (channel[0x00A] & 0xff) != 0xFF ||
            channel[0x00B] != 0x00)
            return null;

//logger.log(Level.TRACE, String.format("CD checksum: Data sector, address %2x:%2x:%2x", channel[0x00C], channel[0x00D], channel[0x00E]));

        if ((channel[0x00F] & 0x03) == 0x00) { // mode (1 byte)
//logger.log(Level.TRACE, String.format("CD checksum: Mode 0 sector at address %2x:%2x:%2x", channel[0x00C], channel[0x00D], channel[0x00E]));
            for (int i = 0x010; i < 0x930; i++)
                if (channel[i] != 0x00) {
logger.log(Level.DEBUG, String.format("CD checksum: Mode 0 sector with error at address: %2x:%2x:%2x", channel[0x00C], channel[0x00D], channel[0x00E]));

                    return false;
                }

            return true;
        }

        if ((channel[0x00F] & 0x03) == 0x01) { // mode (1 byte)
//logger.log(Level.TRACE, String.format("CD checksum: Mode 1 sector at address %2x:%2x:%2x", channel[0x00C], channel[0x00D], channel[0x00E]));

            if (channel[0x814] != 0x00 || // reserved (8 bytes)
                    channel[0x815] != 0x00 ||
                    channel[0x816] != 0x00 ||
                    channel[0x817] != 0x00 ||
                    channel[0x818] != 0x00 ||
                    channel[0x819] != 0x00 ||
                    channel[0x81A] != 0x00 ||
                    channel[0x81B] != 0x00) {
logger.log(Level.DEBUG, String.format("CD checksum: Mode 1 sector with data in reserved bytes at address: %2x:%2x:%2x", channel[0x00C], channel[0x00D], channel[0x00E]));

                return false;
            }

            byte[] address = new byte[4];
            byte[] data = new byte[2060];
            byte[] data2 = new byte[2232];
            byte[] eccP = new byte[172];
            byte[] eccQ = new byte[104];

            System.arraycopy(channel, 0x0C, address, 0, 4);
            System.arraycopy(channel, 0x10, data, 0, 2060);
            System.arraycopy(channel, 0x10, data2, 0, 2232);
            System.arraycopy(channel, 0x81C, eccP, 0, 172);
            System.arraycopy(channel, 0x8C8, eccQ, 0, 104);

            boolean failedEccP = !checkEcc(address, data, 86, 24, 2, 86, eccP);
            boolean failedEccQ = !checkEcc(address, data2, 52, 43, 86, 88, eccQ);

            correctEccP[0] = !failedEccP;
            correctEccQ[0] = !failedEccQ;

            if (failedEccP) {
logger.log(Level.DEBUG, String.format("CD checksum: Mode 1 sector at address: %2x:%2x:%2x, fails ECC P check", channel[0x00C], channel[0x00D], channel[0x00E]));
            }

            if (failedEccQ) {
logger.log(Level.DEBUG, String.format("CD checksum: Mode 1 sector at address: %2x:%2x:%2x, fails ECC Q check", channel[0x00C], channel[0x00D], channel[0x00E]));
            }

            int storedEdc = ByteUtil.readLeInt(channel, 0x810);
            int calculatedEdc = computeEdc(0, channel, 0x810);

            correctEdc[0] = calculatedEdc == storedEdc;

            if (calculatedEdc == storedEdc) {
                return !failedEccP && !failedEccQ;
            }

logger.log(Level.DEBUG, String.format("CD checksum: Mode 1 sector at address: %2x:%2x:%2x, got CRC 0x%08x expected 0x%08x", channel[0x00C], channel[0x00D], channel[0x00E], calculatedEdc, storedEdc));

            return false;
        }

        if ((channel[0x00F] & 0x03) == 0x02) { // mode (1 byte)
//logger.log(Level.TRACE, String.format("CD checksum: Mode 2 sector at address %2x:%2x:%2x", channel[0x00C], channel[0x00D], channel[0x00E]));
            byte[] mode2Sector = new byte[channel.length - 0x10];
            System.arraycopy(channel, 0x10, mode2Sector, 0, mode2Sector.length);

            if ((channel[0x012] & 0x20) == 0x20) { // mode 2 form 2
                if (channel[0x010] != channel[0x014] ||
                        channel[0x011] != channel[0x015] ||
                        channel[0x012] != channel[0x016] ||
                        channel[0x013] != channel[0x017]) {
logger.log(Level.DEBUG, String.format("CD checksum: Subheader copies differ in mode 2 form 2 sector at address: %2x:%2x:%2x",
                            channel[0x00C], channel[0x00D], channel[0x00E]));
                }

                int storedEdc = ByteUtil.readLeInt(mode2Sector, 0x91c);

                // No CRC stored!
                if (storedEdc == 0x00000000)
                    return true;

                int calculatedEdc = computeEdc(0, mode2Sector, 0x91c);

                correctEdc[0] = calculatedEdc == storedEdc || storedEdc == 0;

                if (calculatedEdc == storedEdc || storedEdc == 0x0000_0000)
                    return true;

logger.log(Level.DEBUG, String.format("CD checksum: Mode 2 form 2 sector at address: %2x:%2x:%2x, got CRC 0x%08x expected 0x%08x",
                        channel[0x00C], channel[0x00D], channel[0x00E], calculatedEdc, storedEdc));

                return false;
            } else {
                if (channel[0x010] != channel[0x014] ||
                        channel[0x011] != channel[0x015] ||
                        channel[0x012] != channel[0x016] ||
                        channel[0x013] != channel[0x017]) {
logger.log(Level.DEBUG, String.format("CD checksum: Subheader copies differ in mode 2 form 1 sector at address: %2x:%2x:%2x", channel[0x00C], channel[0x00D], channel[0x00E]));
                }

                byte[] address = new byte[4];
                byte[] eccP = new byte[172];
                byte[] eccQ = new byte[104];

                System.arraycopy(mode2Sector, 0x80C, eccP, 0, 172);
                System.arraycopy(mode2Sector, 0x8B8, eccQ, 0, 104);

                boolean failedEccP = !checkEcc(address, mode2Sector, 86, 24, 2, 86, eccP);
                boolean failedEccQ = !checkEcc(address, mode2Sector, 52, 43, 86, 88, eccQ);

                correctEccP[0] = !failedEccP;
                correctEccQ[0] = !failedEccQ;

                if (failedEccP) {
logger.log(Level.DEBUG, String.format("CD checksum: Mode 2 form 1 sector at address: %2x:%2x:%2x, fails ECC P check", channel[0x00C], channel[0x00D], channel[0x00E]));
                }

                if (failedEccQ) {
logger.log(Level.DEBUG, String.format("CD checksum: Mode 2 form 1 sector at address: %2x:%2x:%2x, fails ECC Q check", channel[0x00C], channel[0x00D], channel[0x00E]));
                }

                int storedEdc = ByteUtil.readLeInt(mode2Sector, 0x808);
                int calculatedEdc = computeEdc(0, mode2Sector, 0x808);

                correctEdc[0] = calculatedEdc == storedEdc;

                if (calculatedEdc == storedEdc)
                    return !failedEccP && !failedEccQ;

logger.log(Level.DEBUG, String.format("CD checksum: Mode 2 sector at address: %2x:%2x:%2x, got CRC 0x%4$08x expected 0x%5$08x", channel[0x00C], channel[0x00D], channel[0x00E], calculatedEdc, storedEdc));

                return false;
            }
        }

logger.log(Level.DEBUG, String.format("CD checksum: Unknown mode %d sector at address: %2x:%2x:%2x", channel[0x00F], channel[0x00C], channel[0x00D], channel[0x00E]));

        return null;
    }

    static int computeEdc(int edc, byte[] src, int size) {
        int pos = 0;

        for (; size > 0; size--)
            edc = (edc >> 8) ^ edcTable[(edc ^ src[pos++]) & 0xFF];

        return edc;
    }

    static Boolean checkCdSectorSubChannel(byte[] subchannel) {
        boolean status = true;
        byte[] qSubChannel = new byte[12];
        byte[] cdTextPack1 = new byte[18];
        byte[] cdTextPack2 = new byte[18];
        byte[] cdTextPack3 = new byte[18];
        byte[] cdTextPack4 = new byte[18];
        byte[] cdSubRwPack1 = new byte[24];
        byte[] cdSubRwPack2 = new byte[24];
        byte[] cdSubRwPack3 = new byte[24];
        byte[] cdSubRwPack4 = new byte[24];

        int i = 0;

        for (int j = 0; j < 12; j++) {
            qSubChannel[j] = 0;
        }

        for (int j = 0; j < 18; j++) {
            cdTextPack1[j] = 0;
            cdTextPack2[j] = 0;
            cdTextPack3[j] = 0;
            cdTextPack4[j] = 0;
        }

        for (int j = 0; j < 24; j++) {
            cdSubRwPack1[j] = 0;
            cdSubRwPack2[j] = 0;
            cdSubRwPack3[j] = 0;
            cdSubRwPack4[j] = 0;
        }

        for (int j = 0; j < 12; j++) {
            qSubChannel[j] = (byte) (qSubChannel[j] | ((subchannel[i++] & 0x40) << 1));
            qSubChannel[j] = (byte) (qSubChannel[j] | (subchannel[i++] & 0x40));
            qSubChannel[j] = (byte) (qSubChannel[j] | ((subchannel[i++] & 0x40) >> 1));
            qSubChannel[j] = (byte) (qSubChannel[j] | ((subchannel[i++] & 0x40) >> 2));
            qSubChannel[j] = (byte) (qSubChannel[j] | ((subchannel[i++] & 0x40) >> 3));
            qSubChannel[j] = (byte) (qSubChannel[j] | ((subchannel[i++] & 0x40) >> 4));
            qSubChannel[j] = (byte) (qSubChannel[j] | ((subchannel[i++] & 0x40) >> 5));
            qSubChannel[j] = (byte) (qSubChannel[j] | ((subchannel[i++] & 0x40) >> 6));
        }

        i = 0;

        for (int j = 0; j < 18; j++) {
            cdTextPack1[j] = (byte) (cdTextPack1[j] | ((subchannel[i++] & 0x3F) << 2));

            if (j < 17)
                cdTextPack1[j] = (byte) (cdTextPack1[j++] | ((subchannel[i] & 0xC0) >> 4));

            cdTextPack1[j] = (byte) (cdTextPack1[j] | ((subchannel[i++] & 0x0F) << 4));

            if (j < 17)
                cdTextPack1[j] = (byte) (cdTextPack1[j++] | ((subchannel[i] & 0x3C) >> 2));

            cdTextPack1[j] = (byte) (cdTextPack1[j] | ((subchannel[i++] & 0x03) << 6));

            cdTextPack1[j] = (byte) (cdTextPack1[j] | (subchannel[i++] & 0x3F));
        }

        for (int j = 0; j < 18; j++) {
            cdTextPack2[j] = (byte) (cdTextPack2[j] | ((subchannel[i++] & 0x3F) << 2));

            if (j < 17)
                cdTextPack2[j] = (byte) (cdTextPack2[j++] | ((subchannel[i] & 0xC0) >> 4));

            cdTextPack2[j] = (byte) (cdTextPack2[j] | ((subchannel[i++] & 0x0F) << 4));

            if (j < 17)
                cdTextPack2[j] = (byte) (cdTextPack2[j++] | ((subchannel[i] & 0x3C) >> 2));

            cdTextPack2[j] = (byte) (cdTextPack2[j] | ((subchannel[i++] & 0x03) << 6));

            cdTextPack2[j] = (byte) (cdTextPack2[j] | (subchannel[i++] & 0x3F));
        }

        for (int j = 0; j < 18; j++) {
            cdTextPack3[j] = (byte) (cdTextPack3[j] | ((subchannel[i++] & 0x3F) << 2));

            if (j < 17)
                cdTextPack3[j] = (byte) (cdTextPack3[j++] | ((subchannel[i] & 0xC0) >> 4));

            cdTextPack3[j] = (byte) (cdTextPack3[j] | ((subchannel[i++] & 0x0F) << 4));

            if (j < 17)
                cdTextPack3[j] = (byte) (cdTextPack3[j++] | ((subchannel[i] & 0x3C) >> 2));

            cdTextPack3[j] = (byte) (cdTextPack3[j] | ((subchannel[i++] & 0x03) << 6));

            cdTextPack3[j] = (byte) (cdTextPack3[j] | (subchannel[i++] & 0x3F));
        }

        for (int j = 0; j < 18; j++) {
            cdTextPack4[j] = (byte) (cdTextPack4[j] | ((subchannel[i++] & 0x3F) << 2));

            if (j < 17)
                cdTextPack4[j] = (byte) (cdTextPack4[j++] | ((subchannel[i] & 0xC0) >> 4));

            cdTextPack4[j] = (byte) (cdTextPack4[j] | ((subchannel[i++] & 0x0F) << 4));

            if (j < 17)
                cdTextPack4[j] = (byte) (cdTextPack4[j++] | ((subchannel[i] & 0x3C) >> 2));

            cdTextPack4[j] = (byte) (cdTextPack4[j] | ((subchannel[i++] & 0x03) << 6));

            cdTextPack4[j] = (byte) (cdTextPack4[j] | (subchannel[i++] & 0x3F));
        }

        i = 0;

        for (int j = 0; j < 24; j++)
            cdSubRwPack1[j] = (byte) (subchannel[i++] & 0x3F);

        for (int j = 0; j < 24; j++)
            cdSubRwPack2[j] = (byte) (subchannel[i++] & 0x3F);

        for (int j = 0; j < 24; j++)
            cdSubRwPack3[j] = (byte) (subchannel[i++] & 0x3F);

        for (int j = 0; j < 24; j++)
            cdSubRwPack4[j] = (byte) (subchannel[i++] & 0x3F);

        switch (cdSubRwPack1[0]) {
        case 0x00:
logger.log(Level.DEBUG, "CD checksum: Detected Zero Pack in subchannel");

            break;
        case 0x08:
logger.log(Level.DEBUG, "CD checksum: Detected Line Graphics Pack in subchannel");

            break;
        case 0x09:
logger.log(Level.DEBUG, "CD checksum: Detected CD+G Pack in subchannel");

            break;
        case 0x0A:
logger.log(Level.DEBUG, "CD checksum: Detected CD+EG Pack in subchannel");

            break;
        case 0x14:
logger.log(Level.DEBUG, "CD checksum: Detected CD-TEXT Pack in subchannel");

            break;
        case 0x18:
logger.log(Level.DEBUG, "CD checksum: Detected CD+MIDI Pack in subchannel");

            break;
        case 0x38:
logger.log(Level.DEBUG, "CD checksum: Detected User Pack in subchannel");

            break;
        default:
logger.log(Level.DEBUG, String.format("CD checksum: Detected unknown Pack type in subchannel: mode %02x, item %02x", cdSubRwPack1[0] & 0x38, cdSubRwPack1[0] & 0x07));

            break;
        }

        short qSubChannelCrc = ByteUtil.readBeShort(qSubChannel, 10);
        byte[] qSubChannelForCrc = new byte[10];
        System.arraycopy(qSubChannel, 0, qSubChannelForCrc, 0, 10);
        short calculatedQcrc = CRC16CCITTContext.calculate(qSubChannelForCrc);

        if (qSubChannelCrc != calculatedQcrc) {
logger.log(Level.DEBUG, String.format("CD checksum: Q subchannel CRC 0x%04x, expected 0x%04x", calculatedQcrc, qSubChannelCrc));

            status = false;
        }

        if ((cdTextPack1[0] & 0x80) == 0x80) {
            short cdTextPack1Crc = ByteUtil.readBeShort(cdTextPack1, 16);
            byte[] cdTextPack1ForCrc = new byte[16];
            System.arraycopy(cdTextPack1, 0, cdTextPack1ForCrc, 0, 16);
            short calculatedCdtp1Crc = CRC16CCITTContext.calculate(cdTextPack1ForCrc);

            if (cdTextPack1Crc != calculatedCdtp1Crc && cdTextPack1Crc != 0) {
logger.log(Level.DEBUG, String.format("CD checksum: CD-Text Pack 1 CRC 0x%04x, expected 0x%04x", cdTextPack1Crc, calculatedCdtp1Crc));

                status = false;
            }
        }

        if ((cdTextPack2[0] & 0x80) == 0x80) {
            short cdTextPack2Crc = ByteUtil.readBeShort(cdTextPack2, 16);
            byte[] cdTextPack2ForCrc = new byte[16];
            System.arraycopy(cdTextPack2, 0, cdTextPack2ForCrc, 0, 16);
            short calculatedCdtp2Crc = CRC16CCITTContext.calculate(cdTextPack2ForCrc);

logger.log(Level.DEBUG, String.format("CD checksum: Cyclic CDTP2 0x%04x, Calc CDTP2 0x%04x", cdTextPack2Crc, calculatedCdtp2Crc));

            if (cdTextPack2Crc != calculatedCdtp2Crc && cdTextPack2Crc != 0) {
logger.log(Level.DEBUG, String.format("CD checksum: CD-Text Pack 2 CRC 0x%04x, expected 0x%04x", cdTextPack2Crc, calculatedCdtp2Crc));

                status = false;
            }
        }

        if ((cdTextPack3[0] & 0x80) == 0x80) {
            short cdTextPack3Crc = ByteUtil.readBeShort(cdTextPack3, 16);
            byte[] cdTextPack3ForCrc = new byte[16];
            System.arraycopy(cdTextPack3, 0, cdTextPack3ForCrc, 0, 16);
            short calculatedCdtp3Crc = CRC16CCITTContext.calculate(cdTextPack3ForCrc);

logger.log(Level.DEBUG, String.format("CD checksum: Cyclic CDTP3 0x%04x, Calc CDTP3 0x%04x", cdTextPack3Crc, calculatedCdtp3Crc));

            if (cdTextPack3Crc != calculatedCdtp3Crc && cdTextPack3Crc != 0) {
logger.log(Level.DEBUG, String.format("CD checksum: CD-Text Pack 3 CRC 0x%04x, expected 0x%04x", cdTextPack3Crc, calculatedCdtp3Crc));

                status = false;
            }
        }

        if ((cdTextPack4[0] & 0x80) != 0x80)
            return status;

        short cdTextPack4Crc = ByteUtil.readBeShort(cdTextPack4, 16);
        byte[] cdTextPack4ForCrc = new byte[16];
        System.arraycopy(cdTextPack4, 0, cdTextPack4ForCrc, 0, 16);
        short calculatedCdtp4Crc = CRC16CCITTContext.calculate(cdTextPack4ForCrc);

logger.log(Level.DEBUG, String.format("CD checksum: Cyclic CDTP4 0x%04x, Calc CDTP4 0x%04x", cdTextPack4Crc, calculatedCdtp4Crc));

        if (cdTextPack4Crc == calculatedCdtp4Crc || cdTextPack4Crc == 0)
            return status;

logger.log(Level.DEBUG, String.format("CD checksum: CD-Text Pack 4 CRC 0x%04x, expected 0x%04x", cdTextPack4Crc, calculatedCdtp4Crc));

        return false;
    }
}
