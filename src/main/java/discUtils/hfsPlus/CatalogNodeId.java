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


class CatalogNodeId {

    public static final CatalogNodeId RootParentId = new CatalogNodeId(1);

    public static final CatalogNodeId RootFolderId = new CatalogNodeId(2);

    public static final CatalogNodeId ExtentsFileId = new CatalogNodeId(3);

    public static final CatalogNodeId CatalogFileId = new CatalogNodeId(4);

    public static final CatalogNodeId BadBlockFileId = new CatalogNodeId(5);

    public static final CatalogNodeId AllocationFileId = new CatalogNodeId(6);

    public static final CatalogNodeId StartupFileId = new CatalogNodeId(7);

    public static final CatalogNodeId AttributesFileId = new CatalogNodeId(8);

    public static final CatalogNodeId RepairCatalogFileId = new CatalogNodeId(14);

    public static final CatalogNodeId BogusExtentFileId = new CatalogNodeId(15);

    public static final CatalogNodeId FirstUserCatalogNodeId = new CatalogNodeId(16);

    private final int id;

    public CatalogNodeId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String toString() {
        return String.valueOf(id);
    }

    public boolean equals(Object other) {
        return other instanceof CatalogNodeId ? id == ((CatalogNodeId) other).id : false;
    }
}
