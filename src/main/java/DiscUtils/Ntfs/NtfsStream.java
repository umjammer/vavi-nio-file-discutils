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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Util.Range;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;


public class NtfsStream {
    private final File _file;

    public NtfsStream(File file, NtfsAttribute attr) {
        _file = file;
        __Attribute = attr;
    }

    private NtfsAttribute __Attribute;

    public NtfsAttribute getAttribute() {
        return __Attribute;
    }

    public AttributeType getAttributeType() {
        return getAttribute().getType();
    }

    public String getName() {
        return getAttribute().getName();
    }

    /**
     * Gets the content of a stream. The stream's content structure.
     * @param contentType 
     * @return The content.
     */
    public <T extends IByteArraySerializable & IDiagnosticTraceable> T getContent(Class<T> contentType) {
        assert contentType != null;
        byte[] buffer;

        try (Stream s = open(FileAccess.Read)) {
            buffer = StreamUtilities.readExact(s, (int) s.getLength());

            T value = contentType.newInstance();
            value.readFrom(buffer, 0);
            return value;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Sets the content of a stream. The stream's content structure.
     *
     * @param value The new value for the stream.
     */
    public <T extends IByteArraySerializable & IDiagnosticTraceable> void setContent(T value) {
        try (Stream s = open(FileAccess.Write)) {
            byte[] buffer = new byte[value.size()];
            value.writeTo(buffer, 0);
            s.write(buffer, 0, buffer.length);
            s.setLength(buffer.length);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public SparseStream open(FileAccess access) {
        return getAttribute().open(access);
    }

    public List<Range> getClusters() {
        return getAttribute().getClusters();
    }

    public List<StreamExtent> getAbsoluteExtents() {
        List<StreamExtent> result = new ArrayList<>();
        long clusterSize = _file.getContext().getBiosParameterBlock().getBytesPerCluster();
        if (getAttribute().getIsNonResident()) {
            List<Range> clusters = getAttribute().getClusters();
            for (Range clusterRange : clusters) {
                result.add(new StreamExtent(clusterRange.getOffset() * clusterSize, clusterRange.getCount() * clusterSize));
            }
        } else {
            result.add(new StreamExtent(getAttribute().offsetToAbsolutePos(0), getAttribute().getLength()));
        }
        return result;
    }
}
