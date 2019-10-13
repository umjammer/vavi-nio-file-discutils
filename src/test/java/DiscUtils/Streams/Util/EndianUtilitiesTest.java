/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package DiscUtils.Streams.Util;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/**
 * EndianUtilitiesTest.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2019/10/13 nsano initial version <br>
 */
class EndianUtilitiesTest {

    @Test
    void test16() throws Exception {
        byte[] buf = new byte[2];

        EndianUtilities.writeBytesBigEndian((short) 0xfedc, buf, 0);
        short v = EndianUtilities.toInt16BigEndian(buf, 0);
        assertEquals((short) 0xfedc, v);

        EndianUtilities.writeBytesLittleEndian((short) 0xfedc, buf, 0);
        v = EndianUtilities.toInt16LittleEndian(buf, 0);
        assertEquals((short) 0xfedc, v);

        EndianUtilities.writeBytesBigEndian((short) 0xfedc, buf, 0);
        v = EndianUtilities.toUInt16BigEndian(buf, 0);
        assertEquals((short) 0xfedc, v);

        EndianUtilities.writeBytesLittleEndian((short) 0xfedc, buf, 0);
        int i = EndianUtilities.toUInt16LittleEndian(buf, 0);
        assertEquals(0xfedc, i);
    }

    @Test
    void test32() throws Exception {
        byte[] buf = new byte[4];

        EndianUtilities.writeBytesBigEndian(0xfedcba98, buf, 0);
        int v = EndianUtilities.toInt32BigEndian(buf, 0);
        assertEquals(0xfedcba98, v);

        EndianUtilities.writeBytesLittleEndian(0xfedcba98, buf, 0);
        v = EndianUtilities.toInt32LittleEndian(buf, 0);
        assertEquals(0xfedcba98, v);

        EndianUtilities.writeBytesBigEndian(0xfedcba98, buf, 0);
        v = EndianUtilities.toUInt32BigEndian(buf, 0);
        assertEquals(0xfedcba98, v);

        EndianUtilities.writeBytesLittleEndian(0xfedcba98, buf, 0);
        v = EndianUtilities.toUInt32LittleEndian(buf, 0);
        assertEquals(0xfedcba98, v);
    }

    @Test
    void test64BE() throws Exception {
        byte[] buf = new byte[8];

        EndianUtilities.writeBytesBigEndian(0xfedcba9876543210l, buf, 0);
        long v = EndianUtilities.toInt64BigEndian(buf, 0);
//System.err.printf("%x\n", v);
        assertEquals(0xfedcba9876543210l, v);
    }

    @Test
    void test64UBE() throws Exception {
        byte[] buf = new byte[8];

        EndianUtilities.writeBytesBigEndian(0xfedcba9876543210l, buf, 0);
        long v = EndianUtilities.toUInt64BigEndian(buf, 0);
//System.err.printf("%x\n", v);
        assertEquals(0xfedcba9876543210l, v);
    }

    @Test
    void test64LE() throws Exception {
        byte[] buf = new byte[8];
        EndianUtilities.writeBytesLittleEndian(0xfedcba9876543210l, buf, 0);
        long v = EndianUtilities.toInt64LittleEndian(buf, 0);
//System.err.printf("%x\n", v);
        assertEquals(0xfedcba9876543210l, v);
    }

    @Test
    void test64ULE() throws Exception {
        byte[] buf = new byte[8];
        EndianUtilities.writeBytesLittleEndian(0xfedcba9876543210l, buf, 0);
        long v = EndianUtilities.toUInt64LittleEndian(buf, 0);
//System.err.printf("%x\n", v);
        assertEquals(0xfedcba9876543210l, v);
    }

    @Test
    void testGuidBE() throws Exception {
        byte[] buf = new byte[16];
        UUID uuid = UUID.randomUUID();

        EndianUtilities.writeBytesBigEndian(uuid, buf, 0);
        UUID v = EndianUtilities.toGuidBigEndian(buf, 0);
        assertEquals(uuid, v);
    }

    @Test
    void testGuidLE() throws Exception {
        byte[] buf = new byte[16];
        UUID uuid = UUID.randomUUID();

        EndianUtilities.writeBytesLittleEndian(uuid, buf, 0);
        UUID v = EndianUtilities.toGuidLittleEndian(buf, 0);
        assertEquals(uuid, v);
    }
}

/* */
