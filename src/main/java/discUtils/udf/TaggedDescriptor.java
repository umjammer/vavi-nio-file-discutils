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

package discUtils.udf;

import java.lang.reflect.InvocationTargetException;

import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


public abstract class TaggedDescriptor<T extends BaseTaggedDescriptor> extends BaseTaggedDescriptor {

    protected TaggedDescriptor(TagIdentifier id) {
        super(id);
    }

    public static <T extends BaseTaggedDescriptor> T fromStream(Stream stream, int sector, int sectorSize, Class<T> clazz) {
        try {
            stream.position(sector * (long) sectorSize);
            byte[] buffer = StreamUtilities.readExact(stream, 512);
            T result = clazz.getDeclaredConstructor().newInstance();
            result.readFrom(buffer, 0);
            if (result.tag.tagIdentifier != result.requiredTagIdentifier || result.tag.tagLocation != sector) {
                throw new IllegalStateException(String.format("Corrupt UDF file system, unable to read %s tag at sector %d",
                                                              result.requiredTagIdentifier,
                                                              sector));
            }

            return result;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public abstract int parse(byte[] buffer, int offset);
}
