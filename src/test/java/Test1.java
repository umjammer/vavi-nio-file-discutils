/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import DiscUtils.Fat.FatAttributes;
import DiscUtils.Ntfs.FileAttributeFlags;
import DiscUtils.Ntfs.Internals.NtfsFileAttributes;


/**
 * Test1.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/10/07 umjammer initial version <br>
 */
public class Test1 {

    @Test
    public void test1() throws Exception {
        EnumSet<FileAttributeFlags> set = FileAttributeFlags.valueOf(7);
        assertFalse(set.contains(FileAttributeFlags.None)); // ordinal 0 is not filtered
        assertTrue(set.contains(FileAttributeFlags.ReadOnly));
        assertTrue(set.contains(FileAttributeFlags.Hidden));
        assertTrue(set.contains(FileAttributeFlags.System));
        assertFalse(set.contains(FileAttributeFlags.Archive));
        assertFalse(set.contains(FileAttributeFlags.Device));
        assertFalse(set.contains(FileAttributeFlags.Normal));
        assertFalse(set.contains(FileAttributeFlags.Temporary));
    }

    @Test
    public void test2() throws Exception {
        EnumSet<NtfsFileAttributes> set = FileAttributeFlags.cast(NtfsFileAttributes.class, 7);
//System.err.println(set);
        assertEquals(7, NtfsFileAttributes.valueOf(set));
    }

    @Test
    public void test11() throws Exception {
        EnumSet<FatAttributes> set = FatAttributes.valueOf(16);
        assertTrue(set.contains(FatAttributes.Directory));
        assertFalse(set.contains(FatAttributes.Archive));
    }

    // @see "https://stackoverflow.com/questions/2685537/how-can-i-implement-comparable-more-than-once"

    interface FooComparable<T extends FooComparable<?>> extends Comparable<T> {
    }

    interface BarComparable<T extends BarComparable<?>> extends Comparable<T> {
    }

    abstract class BarDescription<T extends BarComparable<?>> implements BarComparable<T> {
    }

    class FooBar extends BarDescription<FooBar> implements FooComparable<FooBar> {
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
        String hex = IntStream.range(0, b.length)
                .mapToObj(i -> String.format("%02x", b[i]))
                .collect(Collectors.joining());
        assertEquals("cafebabe", hex);
    }

    @Test
    void test5() throws Exception {
        byte b = (byte) 0xaa;
        assertTrue((b & 0xff) == 0xaa);

        UUID uuid1 = new UUID(0L, 0L);
        UUID uuid2 = new UUID(0L, 0L);
        assertTrue(uuid1.equals(uuid2));
    }
}

/* */
