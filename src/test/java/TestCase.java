/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import discUtils.fat.FatAttributes;
import discUtils.ntfs.FileAttributeFlags;
import discUtils.ntfs.internals.NtfsFileAttributes;
import discUtils.vhd.Footer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import vavi.util.ByteUtil;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/10/07 umjammer initial version <br>
 */
class TestCase {

    @Test
    void test1() throws Exception {
        EnumSet<FileAttributeFlags> set = FileAttributeFlags.valueOf(7);
        assertTrue(set.contains(FileAttributeFlags.ReadOnly));
        assertTrue(set.contains(FileAttributeFlags.Hidden));
        assertTrue(set.contains(FileAttributeFlags.System));
        assertFalse(set.contains(FileAttributeFlags.Archive));
        assertFalse(set.contains(FileAttributeFlags.Device));
        assertFalse(set.contains(FileAttributeFlags.Normal));
        assertFalse(set.contains(FileAttributeFlags.Temporary));
    }

    @Test
    void test2() throws Exception {
        EnumSet<NtfsFileAttributes> set = FileAttributeFlags.cast(NtfsFileAttributes.class, FileAttributeFlags.valueOf(7));
//System.err.println(set);
        assertEquals(7, NtfsFileAttributes.valueOf(set));
    }

    @Test
    void test11() throws Exception {
        EnumSet<FatAttributes> set = FatAttributes.valueOf(16);
        assertTrue(set.contains(FatAttributes.Directory));
        assertFalse(set.contains(FatAttributes.Archive));
    }

    // @see
    // "https://stackoverflow.com/questions/2685537/how-can-i-implement-comparable-more-than-once"

    interface FooComparable<T extends FooComparable<?>> extends Comparable<T> {
    }

    interface BarComparable<T extends BarComparable<?>> extends Comparable<T> {
    }

    abstract static class BarDescription<T extends BarComparable<?>> implements BarComparable<T> {
    }

    static class FooBar extends BarDescription<FooBar> implements FooComparable<FooBar> {
        @Override
        public int compareTo(FooBar o) {
            return 0;
        }
    }

    @Test
    void test3() {
        try {
            Instant i = Instant.parse("9999-12-31T23:59:59.999999900Z");
//System.err.println(i);
            assertEquals("9999-12-31T23:59:59.999999900Z", i.toString());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    void test4() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(0xcafebabe);
        byte[] b = buffer.array();
        String hex = IntStream.range(0, b.length).mapToObj(i -> "%02x".formatted(b[i])).collect(Collectors.joining());
        assertEquals("cafebabe", hex);
    }

    @Test
    void test5() throws Exception {
        byte b = (byte) 0xaa;
        assertEquals(0xaa, (b & 0xff));
    }

    @Test
    void test5_1() throws Exception {
        UUID uuid1 = new UUID(0L, 0L);
        UUID uuid2 = new UUID(0L, 0L);
        assertEquals(uuid1, uuid2);

//System.err.printf("%s:%s\n", uuid1, uuid2);
    }

    @Test
    void test5_2() throws Exception {
        assertEquals("doubleQuoteStriped", "\"doubleQuoteStriped\"".replaceAll("(^\"*|\"*$)", ""));
        assertEquals("parenthesesStriped", "[parenthesesStriped]".replaceAll("(^\\[*|\\]*$)", ""));
    }

    @Test
    void test5_3() throws Exception {
        int n = 12345;
        BitSet bs = BitSet.valueOf(new long[] {
            n
        });
//System.err.printf("%s, %x\n", bs, n);
        assertEquals(n, bs.toLongArray()[0]);
    }

    @Test
    void test6() {
        long t = Footer.EpochUtc.plusSeconds(123456789).toEpochMilli();
//Debug.println(t);
        byte[] bytes = new byte[4];
        ByteUtil.writeBeInt((int) Duration.between(Footer.EpochUtc, Instant.ofEpochMilli(t)).getSeconds(), bytes, 0);
        assertEquals(t, Footer.EpochUtc.plusSeconds(ByteUtil.readBeInt(bytes, 0)).toEpochMilli());
    }

    @Test
    void test7() {
        assertEquals(Instant.now().toEpochMilli(), Instant.now().atZone(ZoneId.of("UTC")).toInstant().toEpochMilli());
    }

    @Test
    public void test8() {
        // formatter:off
        String[][] aa = {
            { "a1", "a2", "a3", "a4" },
            { "b1", "b2", "b3" },
            { "c1", "c2" },
        };
        String[] x = { "a1", "a2", "a3", "a4", "b1", "b2", "b3", "c1", "c2" };
        String[] r = Arrays.stream(aa).flatMap(Arrays::stream).toArray(String[]::new);
        assertArrayEquals(x, r);
        // formatter:on
    }

    @Test
    public void test9() {
        byte b = (byte) 0x80;
        char c = (char) (b & 0xff);
        assertEquals((char) 0x80, c);

        assertEquals(1, "\01".length());
    }

    @Test
    @DisabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test10() {
        byte[] b = new byte[4];
        ByteUtil.writeLeInt(0x64616568, b);
Debug.println(new String(b));
    }
}
