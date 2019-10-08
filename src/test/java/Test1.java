/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.time.Instant;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
        assertEquals(7, NtfsFileAttributes.valueOf(set));
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
            System.err.println(Instant.parse("9999-12-31T23:59:59.999999900Z"));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}

/* */
