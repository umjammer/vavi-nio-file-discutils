/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.streams.util;

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
        v = EndianUtilities.toUInt16LittleEndian(buf, 0);
        assertEquals((short) 0xfedc, v);
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

        EndianUtilities.writeBytesBigEndian(0xfedcba9876543210L, buf, 0);
        long v = EndianUtilities.toInt64BigEndian(buf, 0);
//System.err.printf("%x\n", v);
        assertEquals(0xfedcba9876543210L, v);
    }

    @Test
    void test64UBE() throws Exception {
        byte[] buf = new byte[8];

        EndianUtilities.writeBytesBigEndian(0xfedcba9876543210L, buf, 0);
        long v = EndianUtilities.toUInt64BigEndian(buf, 0);
//System.err.printf("%x\n", v);
        assertEquals(0xfedcba9876543210L, v);
    }

    @Test
    void test64LE() throws Exception {
        byte[] buf = new byte[8];
        EndianUtilities.writeBytesLittleEndian(0xfedcba9876543210L, buf, 0);
        long v = EndianUtilities.toInt64LittleEndian(buf, 0);
//System.err.printf("%x\n", v);
        assertEquals(0xfedcba9876543210L, v);
    }

    @Test
    void test64ULE() throws Exception {
        byte[] buf = new byte[8];
        EndianUtilities.writeBytesLittleEndian(0xfedcba9876543210L, buf, 0);
        long v = EndianUtilities.toUInt64LittleEndian(buf, 0);
//System.err.printf("%x\n", v);
        assertEquals(0xfedcba9876543210L, v);
    }

    @Test
    void testGuidBE() throws Exception {
        byte[] buf = new byte[16];
        UUID uuid = UUID.randomUUID();

        EndianUtilities.writeBytesBigEndian(uuid, buf, 0);
        UUID v = EndianUtilities.toGuidBigEndian(buf, 0);
//System.err.println(uuid + ", " + v);
        assertEquals(uuid, v);

        UUID uuid2 = UUID.fromString("6d7ad8eb-f061-4aac-8b7e-4da04d959d77");
        EndianUtilities.writeBytesBigEndian(uuid2, buf, 0);
        UUID v2 = EndianUtilities.toGuidBigEndian(buf, 0);
        assertEquals(uuid2, v2);
    }

    @Test
    void testGuidLE() throws Exception {
        byte[] buf = new byte[16];
        UUID uuid = UUID.randomUUID();

        EndianUtilities.writeBytesLittleEndian(uuid, buf, 0);
        UUID v = EndianUtilities.toGuidLittleEndian(buf, 0);
        assertEquals(uuid, v);

        UUID uuid2 = UUID.fromString("6d7ad8eb-f061-4aac-8b7e-4da04d959d77");
        EndianUtilities.writeBytesLittleEndian(uuid2, buf, 0);
        UUID v2 = EndianUtilities.toGuidLittleEndian(buf, 0);
        assertEquals(uuid2, v2);
    }

    @Test
    void textXXX() {
        byte[] bytes = new byte[] {
            0x66, 0x77, (byte) 0xC2, 0x2D, 0x23, (byte) 0xF6, 0x00, 0x42, (byte) 0x9D, 0x64, 0x11, 0x5E, (byte) 0x9B,
            (byte) 0xFD, 0x4A, 0x08
        };
        UUID v = EndianUtilities.toGuidLittleEndian(bytes, 0);
        UUID expected = UUID.fromString("2dc27766-f623-4200-9d64-115e9bfd4a08");
//System.err.println(expected + ", " + v);
        assertEquals(expected, v);

        byte[] buf = new byte[16];
        EndianUtilities.writeBytesLittleEndian(expected, buf, 0);
        assertArrayEquals(bytes, buf);
    }

    @Test
    void testString() {
        String s = new String(new char[] { 'á', 'â', 'ã' });
        byte[] bytes = new byte[s.length()];
        EndianUtilities.stringToBytes(s, bytes, 0, bytes.length);
        assertEquals(s, EndianUtilities.bytesToString(bytes, 0, bytes.length));
    }
}

/* */
