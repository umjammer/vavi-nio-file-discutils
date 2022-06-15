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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

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

    public List<Tuple<K, D>> getEntries() {
        List<Tuple<K, D>> result = new ArrayList<>();
        for (Tuple<byte[], byte[]> entry : _index.getEntries()) {
            result.add(new Tuple<>(convert(keyClass, entry.getKey()), convert(valueClass, entry.getValue())));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public D get(K key) {
        return (D) convert(keyClass, _index.get(unconvert(key)));
    }

    public void put(K key, D value) {
        _index.put(unconvert(key), unconvert(value));
    }

    public List<Tuple<K, D>> findAll_(Comparable<byte[]> query) {
        List<Tuple<K, D>> result = new ArrayList<>();
        for (Tuple<byte[], byte[]> entry : _index.findAll(query)) {
            result.add(new Tuple<>(convert(keyClass, entry.getKey()), convert(valueClass, entry.getValue())));
        }
        return result;
    }

    public Tuple<K, D> findFirst_(Comparable<byte[]> query) {
        for (Tuple<K, D> entry : findAll_(query)) {
            return entry;
        }
        return null;
    }

    public List<Tuple<K, D>> findAll(Comparable<K> query) {
        List<Tuple<K, D>> result = new ArrayList<>();
        for (Tuple<byte[], byte[]> entry : _index.findAll(new ComparableConverter(query))) {
            result.add(new Tuple<>(convert(keyClass, entry.getKey()), convert(valueClass, entry.getValue())));
        }
        return result;
    }

    public Tuple<K, D> findFirst(Comparable<K> query) {
        for (Tuple<K, D> entry : findAll(query)) {
            return entry;
        }

        return null;
    }

    /**
     * @param data {@cs out}
     */
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
            T result = clazz.getDeclaredConstructor().newInstance();
            result.readFrom(data, 0);
            return result;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
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
        getEntries().forEach(e -> {
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
