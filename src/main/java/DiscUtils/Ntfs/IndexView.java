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

package DiscUtils.Ntfs;

import java.util.HashMap;
import java.util.Map;

import DiscUtils.Streams.IByteArraySerializable;
import dotnet4j.Tuple;


public class IndexView<K extends IByteArraySerializable, D extends IByteArraySerializable> {
    private Class<K> keyClass;

    private Class<D> valueClass;

    private final Index _index;

    public IndexView(Class<K> keyClass, Class<D> valueClass, Index index) {
        _index = index;
        this.keyClass = keyClass;
        this.valueClass = valueClass;
    }

    public int getCount() {
        return _index.getCount();
    }

    public Map<K, D> getEntries() {
        Map<K, D> result = new HashMap<>();
        for (Tuple<byte[], byte[]> entry : _index.getEntries()) {
            result.put(convert(keyClass, entry.getKey()), convert(valueClass, entry.getValue()));
        }
        return result;
    }

    public D get___idx(K key) {
        return (D) convert(keyClass, _index.get___idx(unconvert(key)));
    }

    public void set___idx(K key, D value) {
        _index.set___idx(unconvert(key), unconvert(value));
    }

    public Map<K, D> findAll_(Comparable<byte[]> query) {
        Map<K, D> result = new HashMap<>();
        for (Tuple<byte[], byte[]> entry : _index.findAll(query)) {
            result.put(convert(keyClass, entry.getKey()), convert(valueClass, entry.getValue()));
        }
        return result;
    }

    public Map.Entry<K, D> findFirst_(Comparable<byte[]> query) {
        for (Map.Entry<K, D> entry : findAll_(query).entrySet()) {
            return entry;
        }
        return null;
    }

    public Map<K, D> findAll(Comparable<K> query) {
        Map<K, D> result = new HashMap<>();
        for (Tuple<byte[], byte[]> entry : _index.findAll(new ComparableConverter(query))) {
            result.put(convert(keyClass, entry.getKey()), convert(valueClass, entry.getValue()));
        }
        return result;
    }

    public Map.Entry<K, D> findFirst(Comparable<K> query) {
        for (Map.Entry<K, D> entry : findAll(query).entrySet()) {
            return entry;
        }
        return null;
    }

    public boolean tryGetValue(K key, D[] data) {
        byte[][] value = new byte[1][];
        if (_index.tryGetValue(unconvert(key), value)) {
            data[0] = convert(valueClass, value[0]);
            return true;
        }

        data[0] = null;
        return false;
    }

    public boolean containsKey(K key) {
        return _index.containsKey(unconvert(key));
    }

    public void remove(K key) {
        _index.remove(unconvert(key));
    }

    private static <T extends IByteArraySerializable> T convert(Class<T> clazz, byte[] data) {
        try {
            T result = clazz.newInstance();
            result.readFrom(data, 0);
            return result;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T extends IByteArraySerializable> byte[] unconvert(T value) {
        byte[] buffer = new byte[value.size()];
        value.writeTo(buffer, 0);
        return buffer;
    }

    private class ComparableConverter implements Comparable<byte[]> {
        private final Comparable<K> _wrapped;

        public ComparableConverter(Comparable<K> toWrap) {
            _wrapped = toWrap;
        }

        public int compareTo(byte[] other) {
            return _wrapped.compareTo(convert(keyClass, other));
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("IndexVIew: {");
        getEntries().entrySet().forEach(e -> {
            sb.append('"');
            sb.append(e.getKey());
            sb.append('"');
            sb.append(": ");
            sb.append('"');
            sb.append(e.getValue());
            sb.append('"');
            sb.append(", ");
        });
        sb.setLength(sb.length() - 2);
        sb.append("}");
        return sb.toString();
    }
}
