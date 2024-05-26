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

package discUtils.streams.block;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class BlockCache<T extends Block> {

    private final Map<Long, T> blocks;

    private int blocksCreated;

    private final int blockSize;

    private final List<T> freeBlocks;

    private final LinkedList<T> lru;

    private final int totalBlocks;

    public BlockCache(int blockSize, int blockCount) {
        this.blockSize = blockSize;
        totalBlocks = blockCount;

        blocks = new HashMap<>();
        lru = new LinkedList<>();
        freeBlocks = new ArrayList<>(totalBlocks);

        freeBlockCount = totalBlocks;
    }

    private int freeBlockCount;

    public int getFreeBlockCount() {
        return freeBlockCount;
    }

    public void setFreeBlockCount(int value) {
        freeBlockCount = value;
    }

    public boolean containsBlock(long position) {
        return blocks.containsKey(position);
    }

    /**
     * @param block {@cs out}
     */
    public boolean tryGetBlock(long position, T[] block) {
        if (blocks.containsKey(position)) {
            block[0] = blocks.get(position);
            lru.remove(block[0]);
            lru.addFirst(block[0]);
            return true;
        }

        return false;
    }

    public T getBlock(long position, Class<T> c) {
        T result;

        if (containsBlock(position)) {
            result = blocks.get(position);
            return result;
        }

        result = getFreeBlock(c);
        result.setPosition(position);
        result.setAvailable(-1);
        storeBlock(result);

        return result;
    }

    public void releaseBlock(long position) {
        if (blocks.containsKey(position)) {
            T block = blocks.get(position);
            blocks.remove(position);
            lru.remove(block);
            freeBlocks.add(block);
            freeBlockCount++;
        }
    }

    private void storeBlock(T block) {
        blocks.put(block.getPosition(), block);
        lru.addFirst(block);
    }

    private T getFreeBlock(Class<T> c) {
        T block;

        if (!freeBlocks.isEmpty()) {
            int idx = freeBlocks.size() - 1;
            block = freeBlocks.get(idx);
            freeBlocks.remove(idx);
            freeBlockCount--;
        } else if (blocksCreated < totalBlocks) {
            try {
                block = c.getDeclaredConstructor().newInstance();
                block.setData(new byte[blockSize]);
                blocksCreated++;
                freeBlockCount--;
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        } else {
            block = lru.getLast();
            lru.removeLast();
            blocks.remove(block.getPosition());
        }

        return block;
    }
}
