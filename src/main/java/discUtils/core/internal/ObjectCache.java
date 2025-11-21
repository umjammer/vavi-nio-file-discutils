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

package discUtils.core.internal;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dotnet4j.util.compat.Tuple;


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

    private final Map<K, WeakReference<V>> entries;

    private int nextPruneCount;

    private final List<Tuple<K, V>> recent;

    public ObjectCache() {
        entries = new HashMap<>();
        recent = new ArrayList<>();
    }

    public synchronized V get(K key) {
        for (int i = 0; i < recent.size(); ++i) {
            Tuple<K, V> recentEntry = recent.get(i);
            if (recentEntry.getKey().equals(key)) {
                makeMostRecent(i);
                return recentEntry.getValue();
            }
        }
        if (entries.containsKey(key)) {
            V val = entries.get(key).get();
            if (val != null) {
                makeMostRecent(key, val);
            }

            return val;
        }

        return null;
    }

    public synchronized V put(K key, V value) {
        WeakReference<V> v = entries.put(key, new WeakReference<>(value));
        makeMostRecent(key, value);
        pruneEntries();
        return v != null ? v.get() : null;
    }

    public synchronized V remove(Object key) {
        for (int i = 0; i < recent.size(); ++i) {
            if (recent.get(i).getKey().equals(key)) {
                recent.remove(i);
                break;
            }
        }
        return entries.remove((K) key).get();
    }

    private synchronized void pruneEntries() {
        nextPruneCount++;
        if (nextPruneCount > PruneGap) {
            List<K> toPrune = new ArrayList<>();
            for (Map.Entry<K, WeakReference<V>> entry : entries.entrySet()) {
                if (entry.getValue().get() == null) {
                    toPrune.add(entry.getKey());
                }
            }
            for (K key : toPrune) {
                entries.remove(key);
            }
            nextPruneCount = 0;
        }
    }

    private synchronized void makeMostRecent(int i) {
        if (i == 0) {
            return;
        }

        Tuple<K, V> entry = recent.get(i);
        recent.remove(i);
        recent.add(0, entry);
    }

    private synchronized void makeMostRecent(K key, V val) {
        while (recent.size() >= MostRecentListSize) {
            recent.remove(recent.size() - 1);
        }
        recent.add(0, new Tuple<>(key, val));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(entries.size());
        sb.append("]");
        sb.append("{");
        entries.forEach((key, value) -> {
            sb.append(key);
            sb.append('=');
            sb.append(value.get());
            sb.append(", ");
        });
        sb.setLength(sb.length() - 2);
        sb.append("}");
        return sb.toString();
    }
}
