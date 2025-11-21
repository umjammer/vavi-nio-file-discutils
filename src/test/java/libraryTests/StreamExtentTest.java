//
// Copyright (c) 2008-2011, Kenneth Bell
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

package libraryTests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import discUtils.streams.StreamExtent;
import discUtils.streams.util.Range;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


class StreamExtentTest {

    @Test
    void testIntersect1() throws Exception {
        StreamExtent[] s1 = new StreamExtent[] {
            new StreamExtent(0, 4)
        };
        StreamExtent[] s2 = new StreamExtent[] {
            new StreamExtent(4, 8)
        };
        StreamExtent[] r = new StreamExtent[] {};
        compare(r, StreamExtent.intersect(s1, s2));
    }

    @Test
    void testIntersect2() throws Exception {
        StreamExtent[] s1 = new StreamExtent[] {
            new StreamExtent(0, 4)
        };
        StreamExtent[] s2 = new StreamExtent[] {
            new StreamExtent(3, 8)
        };
        StreamExtent[] r = new StreamExtent[] {
            new StreamExtent(3, 1)
        };
        compare(r, StreamExtent.intersect(s1, s2));
    }

    @Test
    void testIntersect3() throws Exception {
        StreamExtent[] s1 = new StreamExtent[] {
            new StreamExtent(0, 4), new StreamExtent(10, 10)
        };
        StreamExtent[] s2 = new StreamExtent[] {
            new StreamExtent(3, 8)
        };
        StreamExtent[] r = new StreamExtent[] {
            new StreamExtent(3, 1), new StreamExtent(10, 1)
        };
        compare(r, StreamExtent.intersect(s1, s2));
    }

    @Test
    void testIntersect4() throws Exception {
        StreamExtent[] s1 = new StreamExtent[] {
            new StreamExtent(0, 4)
        };
        StreamExtent[] s2 = new StreamExtent[] {
            new StreamExtent(3, 8)
        };
        StreamExtent[] s3 = new StreamExtent[] {
            new StreamExtent(10, 10)
        };
        StreamExtent[] r = new StreamExtent[] {};
        compare(r, StreamExtent.intersect(s1, s2, s3));
    }

    @Test
    void testIntersect5() throws Exception {
        StreamExtent[] s1 = new StreamExtent[] {
            new StreamExtent(0, 10)
        };
        StreamExtent[] s2 = new StreamExtent[] {
            new StreamExtent(3, 5)
        };
        StreamExtent[] r = new StreamExtent[] {
            new StreamExtent(3, 5)
        };
        compare(r, StreamExtent.intersect(s1, s2));
    }

    @Test
    void testUnion1() throws Exception {
        StreamExtent[] s1 = new StreamExtent[] {
            new StreamExtent(0, 4)
        };
        StreamExtent[] s2 = new StreamExtent[] {
            new StreamExtent(4, 8)
        };
        StreamExtent[] r = new StreamExtent[] {
            new StreamExtent(0, 12)
        };
        compare(r, StreamExtent.union(s1, s2));
    }

    @Test
    void testUnion2() throws Exception {
        StreamExtent[] s1 = new StreamExtent[] {
            new StreamExtent(0, 4)
        };
        StreamExtent[] s2 = new StreamExtent[] {
            new StreamExtent(5, 8)
        };
        StreamExtent[] r = new StreamExtent[] {
            new StreamExtent(0, 4), new StreamExtent(5, 8)
        };
        compare(r, StreamExtent.union(s1, s2));
    }

    @Test
    void testUnion3() throws Exception {
        StreamExtent[] s1 = new StreamExtent[] {
            new StreamExtent(0, 4)
        };
        StreamExtent[] s2 = new StreamExtent[] {
            new StreamExtent(2, 8)
        };
        StreamExtent[] r = new StreamExtent[] {
            new StreamExtent(0, 10)
        };
        compare(r, StreamExtent.union(s1, s2));
    }

    @Test
    void testUnion4() throws Exception {
        StreamExtent[] s1 = new StreamExtent[] {
            new StreamExtent(0, 4), new StreamExtent(4, 4)
        };
        StreamExtent[] r = new StreamExtent[] {
            new StreamExtent(0, 8)
        };
        compare(r, StreamExtent.union(s1));
    }

    @Test
    void testUnion5() throws Exception {
        StreamExtent[] r = new StreamExtent[] {};
        compare(r, StreamExtent.union());
    }

    @Test
    void testBlockCount() throws Exception {
        StreamExtent[] s = new StreamExtent[] {
            new StreamExtent(0, 8), new StreamExtent(11, 4)
        };
        assertEquals(2, StreamExtent.blockCount(s, 10));
        s = new StreamExtent[] {
            new StreamExtent(0, 8), new StreamExtent(9, 8)
        };
        assertEquals(2, StreamExtent.blockCount(s, 10));
        s = new StreamExtent[] {
            new StreamExtent(3, 4), new StreamExtent(19, 4), new StreamExtent(44, 4)
        };
        assertEquals(4, StreamExtent.blockCount(s, 10));
    }

    @Test
    void testBlocks() throws Exception {
        StreamExtent[] s = new StreamExtent[] {
            new StreamExtent(0, 8), new StreamExtent(11, 4)
        };
        List<Range> ranges = new ArrayList<>(StreamExtent.blocks(s, 10));
        assertEquals(1, ranges.size());
        assertEquals(0, ranges.get(0).getOffset());
        assertEquals(2, ranges.get(0).getCount());
        s = new StreamExtent[] {
            new StreamExtent(0, 8), new StreamExtent(9, 8)
        };
        ranges = new ArrayList<>(StreamExtent.blocks(s, 10));
        assertEquals(1, ranges.size());
        assertEquals(0, ranges.get(0).getOffset());
        assertEquals(2, ranges.get(0).getCount());
        s = new StreamExtent[] {
            new StreamExtent(3, 4), new StreamExtent(19, 4), new StreamExtent(44, 4)
        };
        ranges = new ArrayList<>(StreamExtent.blocks(s, 10));
        assertEquals(2, ranges.size());
        assertEquals(0, ranges.get(0).getOffset());
        assertEquals(3, ranges.get(0).getCount());
        assertEquals(4, ranges.get(1).getOffset());
        assertEquals(1, ranges.get(1).getCount());
    }

    static void compare(StreamExtent[] expected, List<StreamExtent> actual) throws Exception {
        List<StreamExtent> eList = Arrays.asList(expected);
        List<StreamExtent> aList = new ArrayList<>(actual);
        boolean failed = false;
        int failedIndex = -1;
        if (eList.size() == aList.size()) {
            for (int i = 0; i < eList.size(); ++i) {
                if (!eList.get(i).equals(aList.get(i))) {
                    failed = true;
                    failedIndex = i;
                    break;
                }
            }
        } else {
            failed = true;
        }
        if (failed) {
            StringBuilder str = new StringBuilder("Expected " + eList.size() + "(<");
            for (int i = 0; i < Math.min(4, eList.size()); ++i) {
                str.append(eList.get(i).toString()).append(",");
            }
            if (eList.size() > 4) {
                str.append("...");
            }

            str.append(">)");
            str.append(", actual ").append(aList.size()).append("(<");
            for (int i = 0; i < Math.min(4, aList.size()); ++i) {
                str.append(aList.get(i).toString()).append(",");
            }
            if (aList.size() > 4) {
                str.append("...");
            }

            str.append(">)");
            if (failedIndex != -1) {
                str.append(" - different at index ").append(failedIndex);
            }

            fail(str.toString());
        }
    }
}
