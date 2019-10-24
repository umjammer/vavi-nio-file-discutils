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

package DiscUtils.Ext;

public enum CompatibleFeatures {
    /**
     * Feature flags for backwards compatible features.
     *
     * Indicates pre-allocation hints are present.
     */
    __dummyEnum__0,
    DirectoryPreallocation,
    /**
     * AFS support in inodex.
     */
    IMagicInodes,
    __dummyEnum__1,
    /**
     * Indicates an EXT3-style journal is present.
     */
    HasJournal,
    __dummyEnum__2,
    __dummyEnum__3,
    __dummyEnum__4,
    /**
     * Indicates extended attributes (e.g. FileACLs) are present.
     */
    ExtendedAttributes,
    __dummyEnum__5,
    __dummyEnum__6,
    __dummyEnum__7,
    __dummyEnum__8,
    __dummyEnum__9,
    __dummyEnum__10,
    __dummyEnum__11,
    /**
     * Indicates space is reserved through a special inode to enable the file
     * system to be resized dynamically.
     */
    ResizeInode,
    __dummyEnum__12,
    __dummyEnum__13,
    __dummyEnum__14,
    __dummyEnum__15,
    __dummyEnum__16,
    __dummyEnum__17,
    __dummyEnum__18,
    __dummyEnum__19,
    __dummyEnum__20,
    __dummyEnum__21,
    __dummyEnum__22,
    __dummyEnum__23,
    __dummyEnum__24,
    __dummyEnum__25,
    __dummyEnum__26,
    /**
     * Indicates that directory indexes are present (not used in mainline?).
     */
    DirectoryIndex;

    public static CompatibleFeatures valueOf(int value) {
        return values()[value];
    }
}
