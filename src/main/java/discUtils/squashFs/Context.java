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

package discUtils.squashFs;

import discUtils.core.vfs.VfsContext;
import dotnet4j.io.Stream;


public class Context extends VfsContext {
    private MetablockReader __DirectoryReader;

    public MetablockReader getDirectoryReader() {
        return __DirectoryReader;
    }

    public void setDirectoryReader(MetablockReader value) {
        __DirectoryReader = value;
    }

    private MetablockReader[] __FragmentTableReaders;

    public MetablockReader[] getFragmentTableReaders() {
        return __FragmentTableReaders;
    }

    public void setFragmentTableReaders(MetablockReader[] value) {
        __FragmentTableReaders = value;
    }

    private MetablockReader __InodeReader;

    public MetablockReader getInodeReader() {
        return __InodeReader;
    }

    public void setInodeReader(MetablockReader value) {
        __InodeReader = value;
    }

    private Stream __RawStream;

    public Stream getRawStream() {
        return __RawStream;
    }

    public void setRawStream(Stream value) {
        __RawStream = value;
    }

    private ReadBlock __ReadBlock;

    public ReadBlock getReadBlock() {
        return __ReadBlock;
    }

    public void setReadBlock(ReadBlock value) {
        __ReadBlock = value;
    }

    private ReadMetaBlock __ReadMetaBlock;

    public ReadMetaBlock getReadMetaBlock() {
        return __ReadMetaBlock;
    }

    public void setReadMetaBlock(ReadMetaBlock value) {
        __ReadMetaBlock = value;
    }

    private SuperBlock __SuperBlock;

    public SuperBlock getSuperBlock() {
        return __SuperBlock;
    }

    public void setSuperBlock(SuperBlock value) {
        __SuperBlock = value;
    }

    private MetablockReader[] __UidGidTableReaders;

    public MetablockReader[] getUidGidTableReaders() {
        return __UidGidTableReaders;
    }

    public void setUidGidTableReaders(MetablockReader[] value) {
        __UidGidTableReaders = value;
    }
}
