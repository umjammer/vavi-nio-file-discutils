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

package discUtils.hfsPlus;

import discUtils.core.vfs.VfsContext;
import dotnet4j.io.Stream;


final class Context extends VfsContext {

    private BTree<AttributeKey> attribute;

    public BTree<AttributeKey> getAttributes() {
        return attribute;
    }

    public void setAttributes(BTree<AttributeKey> value) {
        attribute = value;
    }

    private BTree<CatalogKey> catalog;

    public BTree<CatalogKey> getCatalog() {
        return catalog;
    }

    public void setCatalog(BTree<CatalogKey> value) {
        catalog = value;
    }

    private BTree<ExtentKey> extentsOverflow;

    public BTree<ExtentKey> getExtentsOverflow() {
        return extentsOverflow;
    }

    public void setExtentsOverflow(BTree<ExtentKey> value) {
        extentsOverflow = value;
    }

    private VolumeHeader volumeHeader;

    public VolumeHeader getVolumeHeader() {
        return volumeHeader;
    }

    public void setVolumeHeader(VolumeHeader value) {
        volumeHeader = value;
    }

    private Stream volumeStream;

    public Stream getVolumeStream() {
        return volumeStream;
    }

    public void setVolumeStream(Stream value) {
        volumeStream = value;
    }
}
