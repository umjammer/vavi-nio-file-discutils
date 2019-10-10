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

package LibraryTests;

import org.junit.jupiter.api.Test;

import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Block.BlockCacheSettings;
import DiscUtils.Streams.Block.BlockCacheStream;
import DiscUtils.Streams.Util.Ownership;
import moe.yo3explorer.dotnetio4j.MemoryStream;


public final class BlockCacheStreamTest {

    @Test
    public void bug5203_IncreaseSize() throws Exception {
        MemoryStream ms = new MemoryStream();
        BlockCacheSettings settings = new BlockCacheSettings();
        BlockCacheStream bcs = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                    Ownership.Dispose,
                                                    settings);
        // Pre-load read cache with a 'short' block
        bcs.write(new byte[11], 0, 11);
        bcs.setPosition(0);
        bcs.read(new byte[11], 0, 11);
        for (int i = 0; i < 20; ++i) {
            // Extend stream
            bcs.write(new byte[11], 0, 11);
        }
        // Try to read from first block beyond length of original cached short length
        // Bug was throwing exception here
        bcs.setPosition(60);
        bcs.read(new byte[20], 0, 20);
    }
}
