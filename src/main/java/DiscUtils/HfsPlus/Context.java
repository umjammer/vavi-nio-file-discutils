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

package DiscUtils.HfsPlus;

import DiscUtils.Core.Vfs.VfsContext;
import dotnet4j.io.Stream;


final class Context extends VfsContext {
    private BTree<AttributeKey> _attribute;

    public BTree<AttributeKey> getAttributes() {
        return _attribute;
    }

    public void setAttributes(BTree<AttributeKey> value) {
        _attribute = value;
    }

    private BTree<CatalogKey> _catalog;

    public BTree<CatalogKey> getCatalog() {
        return _catalog;
    }

    public void setCatalog(BTree<CatalogKey> value) {
        _catalog = value;
    }

    private BTree<ExtentKey> _extentsOverflow;

    public BTree<ExtentKey> getExtentsOverflow() {
        return _extentsOverflow;
    }

    public void setExtentsOverflow(BTree<ExtentKey> value) {
        _extentsOverflow = value;
    }

    private VolumeHeader _volumeHeader;

    public VolumeHeader getVolumeHeader() {
        return _volumeHeader;
    }

    public void setVolumeHeader(VolumeHeader value) {
        _volumeHeader = value;
    }

    private Stream _volumeStream;

    public Stream getVolumeStream() {
        return _volumeStream;
    }

    public void setVolumeStream(Stream value) {
        _volumeStream = value;
    }
}
