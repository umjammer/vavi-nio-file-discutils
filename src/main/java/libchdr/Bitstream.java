/*
 * license:BSD-3-Clause
 * copyright-holders:Aaron Giles
 *
 * ported from bitstream.c
 */

package libchdr;


/**
 * Helper classes for reading/writing at the bit level.
 */
public class Bitstream {

    /** current bit accumulator */
    public int buffer;
    /** number of bits in the accumulator */
    public int bits;
    /** read pointer */
    public final byte[] read;
    public final int readOffset;
    /** byte offset within the data */
    public int doffset;
    /** length of the data */
    public final int dlength;

    public Bitstream(byte[] src, int srcOffset, int srcLength) {
        read = src;
        readOffset = srcOffset;
        dlength = srcLength;
    }

    public boolean overflow() {
        return ((doffset - bits / 8) > dlength);
    }

    /**
     * fetch the requested number of bits
     * but don't advance the input pointer
     */
    public int peek(int numBits) {
        if (numBits == 0) {
            return 0;
        }

        // fetch data if we need more
        if (numBits > bits) {
            while (bits <= 24) {
                if (doffset < dlength) {
                    buffer |= (read[readOffset + doffset] & 0xff) << (24 - bits);
                }
                doffset++;
                bits += 8;
            }
        }

        // return the data
        return buffer >>> (32 - numBits);
    }

    /**
     * advance the input pointer by the
     * specified number of bits
     */
    public void remove(int numBits) {
        buffer <<= numBits;
        bits -= numBits;
    }

    /**
     * fetch the requested number of bits
     */
    public int read(int numBits) {
        int result = peek(numBits);
        remove(numBits);
        return result;
    }

    /**
     * return the current read offset
     */
    public int read_offset() {
        int result = doffset;
        int nbits = bits;
        while (nbits >= 8) {
            result--;
            nbits -= 8;
        }
        return result;
    }

    /**
     * flush to the nearest byte
     */
    public int flush() {
        while (bits >= 8) {
            doffset--;
            bits -= 8;
        }
        bits = buffer = 0;
        return doffset;
    }
}
