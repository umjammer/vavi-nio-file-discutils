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

package DiscUtils.Core.Internal;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dotnet4j.Tuple;


/**
 * Caches objects.
 *
 * Can be use for two purposes - to ensure there is only one instance of a given
 * object, and to prevent the need to recreate objects that are expensive to
 * create.
 *
 * @param <K> The type of the object key.
 * @param <V> The type of the objects to cache.
 */
public class ObjectCache<K, V> {
    private static final int MostRecentListSize = 20;

    private static final int PruneGap = 500;

    private final Map<K, WeakReference<V>> _entries;

    private int _nextPruneCount;

    private final List<Tuple<K, V>> _recent;

    public ObjectCache() {
        _entries = new HashMap<>();
        _recent = new ArrayList<>();
    }

    public synchronized V get(K key) {
        for (int i = 0; i < _recent.size(); ++i) {
            Tuple<K, V> recentEntry = _recent.get(i);
            if (recentEntry.getKey().equals(key)) {
                makeMostRecent(i);
                return recentEntry.getValue();
            }
        }
        if (_entries.containsKey(key)) {
            V val = _entries.get(key).get();
            if (val != null) {
                makeMostRecent(key, val);
            }

            return val;
        }

        return null;
    }

    public synchronized V put(K key, V value) {
        WeakReference<V> v = _entries.put(key, new WeakReference<>(value));
        makeMostRecent(key, value);
        pruneEntries();
        return v != null ? v.get() : null;
    }

    public synchronized V remove(Object key) {
        for (int i = 0; i < _recent.size(); ++i) {
            if (_recent.get(i).getKey().equals(key)) {
                _recent.remove(i);
                break;
            }
        }
        return _entries.remove(key).get();
    }

    private synchronized void pruneEntries() {
        _nextPruneCount++;
        if (_nextPruneCount > PruneGap) {
            List<K> toPrune = new ArrayList<>();
            for (Map.Entry<K, WeakReference<V>> entry : _entries.entrySet()) {
                if (entry.getValue().get() == null) {
                    toPrune.add(entry.getKey());
                }
            }
            for (K key : toPrune) {
                _entries.remove(key);
            }
            _nextPruneCount = 0;
        }
    }

    private synchronized void makeMostRecent(int i) {
        if (i == 0) {
            return;
        }

        Tuple<K, V> entry = _recent.get(i);
        _recent.remove(i);
        _recent.add(0, entry);
    }

    private synchronized void makeMostRecent(K key, V val) {
        while (_recent.size() >= MostRecentListSize) {
            _recent.remove(_recent.size() - 1);
        }
        _recent.add(0, new Tuple<>(key, val));
    }

    @Override
	public String toString() {
	    StringBuilder sb = new StringBuilder();
	    sb.append("[");
	    sb.append(_entries.size());
	    sb.append("]");
	    sb.append("{");
	    _entries.entrySet().forEach(e -> {
	        sb.append(e.getKey());
	        sb.append('=');
	        sb.append(e.getValue().get());
	        sb.append(", ");
	    });
	    sb.setLength(sb.length() - 2);
	    sb.append("}");
	    return sb.toString();
	}
}
