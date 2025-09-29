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

import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import vavi.util.ByteUtil;


/** Implements a CRC16 algorithm */
public class Crc16Context {
    final short finalSeed;
    final boolean inverse;
    short[][] table;
    final boolean useCcitt;
    short[] hashInt = new short[1];

    /** Initializes the CRC16 table with a custom polynomial and seed */
    public Crc16Context(short polynomial, short seed, short[][] table, boolean inverse) {
        hashInt[0] = seed;
        finalSeed = seed;
        this.inverse = inverse;

        useCcitt = polynomial == CRC16CCITTContext.CRC16_CCITT_POLY &&
                seed == CRC16CCITTContext.CRC16_CCITT_SEED && inverse;
    }

    /**
     * Updates the hash with data.
     * @param data Data buffer.
     * @param len Length of buffer to hash.
     */
    public void update(byte[] data, int len) {
        if (inverse)
            stepInverse(/*ref*/ hashInt, table, data, len);
        else
            step(/*ref*/ hashInt, table, data, len);
    }

    /**
     * Updates the hash with data.
     * @param data Data buffer.
     */
    public void update(byte[] data) {
        update(data, data.length);
    }

    /** Returns a byte array of the hash value. */
    public byte[] doFinal() {
        short crc;

        if (inverse)
            crc = (short) ~(hashInt[0] ^ finalSeed);
        else
            crc = (short) (hashInt[0] ^ finalSeed);

        return ByteUtil.getLeBytes(crc);
    }

    /** Returns a hexadecimal representation of the hash value. */
    public String end() {
        StringBuilder crc16Output = new StringBuilder();
        short final_;

        if (inverse)
            final_ =(short) ~(hashInt[0] ^ finalSeed);
        else
            final_ =(short) (hashInt[0] ^ finalSeed);

        byte[] finalBytes = ByteUtil.getLeBytes( final_);

        for (byte finalByte : finalBytes)
            crc16Output.append("%02x".formatted(finalByte));

        return crc16Output.toString();
    }

    static void step(/*ref*/ short[] previousCrc, short[][] table, byte[] data, int len) {
        // Unroll according to Intel slicing by int8_t
        // http://www.intel.com/technology/comms/perfnet/download/CRC_generators.pdf
        // http://sourceforge.net/projects/slicing-by-8/

        short crc;
        int currentPos = 0;
            final int unroll = 4;
            final int bytesAtOnce = 8 * unroll;

        crc = previousCrc[0];

        while (len >= bytesAtOnce) {
            int unrolling;

            for (unrolling = 0; unrolling < unroll; unrolling++) {
                // TODO: What trick is Microsoft doing here that's faster than arithmetic conversion
                int one = ByteUtil.readLeInt(data, currentPos) ^ crc;
                currentPos += 4;
                int two = ByteUtil.readLeInt(data, currentPos);
                currentPos += 4;

                crc = (short) (table[0][(two >> 24) & 0xFF] ^ table[1][(two >> 16) & 0xFF] ^
                        table[2][(two >> 8) & 0xFF] ^ table[3][two & 0xFF] ^ table[4][(one >> 24) & 0xFF] ^
                        table[5][(one >> 16) & 0xFF] ^ table[6][(one >> 8) & 0xFF] ^ table[7][one & 0xFF]);
            }

            len -= bytesAtOnce;
        }

        while (len-- != 0)
            crc = (short) ((crc >> 8) ^ table[0][(crc & 0xFF) ^ data[currentPos++]]);

        previousCrc[0] = crc;
    }

    static void stepInverse(/*ref*/ short[] previousCrc, short[][] table, byte[] data, int len) {
        // Unroll according to Intel slicing by int8_t
        // http://www.intel.com/technology/comms/perfnet/download/CRC_generators.pdf
        // http://sourceforge.net/projects/slicing-by-8/

        short crc;
        int currentPos = 0;
            final int unroll = 4;
            final int bytesAtOnce = 8 * unroll;

        crc = previousCrc[0];

        while (len >= bytesAtOnce) {
            int unrolling;

            for (unrolling = 0; unrolling < unroll; unrolling++) {
                crc = (short) (table[7][data[currentPos + 0] ^ (crc >> 8)] ^
                        table[6][data[currentPos + 1] ^ (crc & 0xFF)] ^ table[5][data[currentPos + 2]] ^
                        table[4][data[currentPos + 3]] ^ table[3][data[currentPos + 4]] ^
                        table[2][data[currentPos + 5]] ^ table[1][data[currentPos + 6]] ^
                        table[0][data[currentPos + 7]]);

                currentPos += 8;
            }

            len -= bytesAtOnce;
        }

        while (len-- != 0)
            crc = (short) ((crc << 8) ^ table[0][(crc >> 8) ^ data[currentPos++]]);

        previousCrc[0] = crc;
    }

    static short[][] generateTable(short polynomial, boolean inverseTable) {
        short[][] table = new short[8][];

        for (int i = 0; i < 8; i++)
            table[i] = new short[256];

        if (!inverseTable)
            for (int i = 0; i < 256; i++) {
                int entry = i;

                for (int j = 0; j < 8; j++)
                    if ((entry & 1) == 1)
                        entry = (entry >> 1) ^ polynomial;
                    else
                        entry >>= 1;

                table[0][i] = (short) entry;
            }
        else {
            for (int i = 0; i < 256; i++) {
                int entry = i << 8;

                for (int j = 0; j < 8; j++) {
                    if ((entry & 0x8000) > 0)
                        entry = (entry << 1) ^ polynomial;
                    else
                        entry <<= 1;

                    table[0][i] = (short) entry;
                }
            }
        }

        for (int slice = 1; slice < 8; slice++)
            for (int i = 0; i < 256; i++) {
                if (inverseTable)
                    table[slice][i] = (short) ((table[slice - 1][i] << 8) ^ table[0][table[slice - 1][i] >> 8]);
                else
                    table[slice][i] = (short) ((table[slice - 1][i] >> 8) ^ table[0][table[slice - 1][i] & 0xFF]);
            }

        return table;
    }

    /**
     * Gets the hash of a file in hexadecimal and as a byte array.
     * @param filename File path. 
     * @param hash Byte array of the hash value. 
     * @param polynomial CRC polynomial 
     * @param seed CRC seed 
     * @param table CRC lookup table 
     * @param inverse Is CRC inverted?
     */
    public static String file(String filename, /*out*/ byte[][] hash, short polynomial, short seed, short[][] table,
                              boolean inverse) {

        boolean useCcitt = polynomial == CRC16CCITTContext.CRC16_CCITT_POLY &&
                seed == CRC16CCITTContext.CRC16_CCITT_SEED && inverse;

        FileStream fileStream = new FileStream(filename, FileMode.Open);

        short[] localHashInt = new short[] {seed};

        short[][] localTable = table != null ? generateTable(polynomial, inverse) : null;

        byte[] buffer = new byte[65536];
        int read = fileStream.read(buffer, 0, 65536);

        while (read > 0) {
            if (inverse)
                stepInverse(/*ref*/ localHashInt, localTable, buffer, read);
            else
                step(/*ref*/ localHashInt, localTable, buffer, read);

            read = fileStream.read(buffer, 0, 65536);
        }

        localHashInt[0] = (short) (localHashInt[0] ^ seed);

        if (inverse)
            localHashInt[0] = (short) ~localHashInt[0];

        hash[0] = ByteUtil.getLeBytes(localHashInt[0]);

        StringBuilder crc16Output = new StringBuilder();

        for (byte h : hash[0])
            crc16Output.append(String.format(("%02x"), h));

        fileStream.close();

        return crc16Output.toString();
    }

    /**
     * Gets the hash of the specified data buffer.
     * @param data Data buffer. 
     * @param len Length of the data buffer to hash. 
     * @param hash Byte array of the hash value. 
     * @param polynomial CRC polynomial 
     * @param seed CRC seed 
     * @param table CRC lookup table 
     * @param inverse Is CRC inverted?
     */
    public static String data(byte[] data, int len, /*out*/ byte[][] hash, short polynomial, short seed,
                              short[][] table, boolean inverse) {

        boolean useCcitt = polynomial == CRC16CCITTContext.CRC16_CCITT_POLY &&
                seed == CRC16CCITTContext.CRC16_CCITT_SEED && inverse;

        short[] localHashInt = new short[] {seed};

        short[][] localTable = table != null ? generateTable(polynomial, inverse) : null;

        if (inverse)
            stepInverse(/*ref*/ localHashInt, localTable, data, len);
        else
            step(/*ref*/ localHashInt, localTable, data, len);

        localHashInt[0] = (short) (localHashInt[0] ^ seed);

        if (inverse)
            localHashInt[0] = (short) ~localHashInt[0];

        hash[0] = ByteUtil.getLeBytes(localHashInt[0]);

        StringBuilder crc16Output = new StringBuilder();

        for ( byte h : hash[0])
            crc16Output.append("%02x".formatted(h));

        return crc16Output.toString();
    }

    /** Calculates the CRC16 of the specified buffer with the specified parameters
     * @param buffer Buffer 
     * @param polynomial Polynomial 
     * @param seed Seed 
     * @param table Pre-generated lookup table 
     * @param inverse Inverse CRC 
     * @return CRC16
     */
    public static short calculate(byte[] buffer, short polynomial, short seed, short[][] table, boolean inverse) {

        boolean useCcitt = polynomial == CRC16CCITTContext.CRC16_CCITT_POLY &&
                seed == CRC16CCITTContext.CRC16_CCITT_SEED && inverse;

        short[] localHashInt = new short[] {seed};

        short[][] localTable = table != null ? generateTable(polynomial, inverse) : null;

        if (inverse)
            stepInverse(/*ref*/ localHashInt, localTable, buffer, buffer.length);
        else
            step(/*ref*/ localHashInt, localTable, buffer, buffer.length);

        localHashInt[0] = (short) (localHashInt[0] ^ seed);

        if (inverse)
            localHashInt[0] = (short) ~localHashInt[0];

        return localHashInt[0];
    }
}
