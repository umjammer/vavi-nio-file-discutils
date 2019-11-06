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

package DiscUtils.Streams.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class BlockCache<T extends Block> {
    private final Map<Long, T> _blocks;

    private int _blocksCreated;

    private final int _blockSize;

    private final List<T> _freeBlocks;

    private final LinkedList<T> _lru;

    private final int _totalBlocks;

    public BlockCache(int blockSize, int blockCount) {
        _blockSize = blockSize;
        _totalBlocks = blockCount;

        _blocks = new HashMap<>();
        _lru = new LinkedList<>();
        _freeBlocks = new ArrayList<>(_totalBlocks);

        _freeBlockCount = _totalBlocks;
    }

    private int _freeBlockCount;

    public int getFreeBlockCount() {
        return _freeBlockCount;
    }

    public void setFreeBlockCount(int value) {
        _freeBlockCount = value;
    }

    public boolean containsBlock(long position) {
        return _blocks.containsKey(position);
    }

    /**
     * @param block {@cs out}
     */
    public boolean tryGetBlock(long position, T[] block) {
        if (_blocks.containsKey(position)) {
            block[0] = _blocks.get(position);
            _lru.remove(block[0]);
            _lru.addFirst(block[0]);
            return true;
        }

        return false;
    }

    public T getBlock(long position, Class<T> c) {
        T result;

        if (containsBlock(position)) {
            result = getBlock(position, c);
            return result;
        }

        result = getFreeBlock(c);
        result.setPosition(position);
        result.setAvailable(-1);
        storeBlock(result);

        return result;
    }

    public void releaseBlock(long position) {
        if (_blocks.containsKey(position)) {
            T block = _blocks.get(position);
            _blocks.remove(position);
            _lru.remove(block);
            _freeBlocks.add(block);
            _freeBlockCount++;
        }
    }

    private void storeBlock(T block) {
        _blocks.put(block.getPosition(), block);
        _lru.addFirst(block);
    }

    private T getFreeBlock(Class<T> c) {
        T block;

        if (_freeBlocks.size() > 0) {
            int idx = _freeBlocks.size() - 1;
            block = _freeBlocks.get(idx);
            _freeBlocks.remove(idx);
            _freeBlockCount--;
        } else if (_blocksCreated < _totalBlocks) {
            try {
                block = c.newInstance();
                block.setData(new byte[_blockSize]);
                _blocksCreated++;
                _freeBlockCount--;
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        } else {
            block = _lru.getLast();
            _lru.removeLast();
            _blocks.remove(block.getPosition());
        }

        return block;
    }
}
