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

package discUtils.ntfs;

public class DirectoryEntry {
    private final Directory _directory;

    public DirectoryEntry(Directory directory, FileRecordReference fileReference, FileNameRecord fileDetails) {
        _directory = directory;
        _reference = fileReference;
        _details = fileDetails;
    }

    private FileNameRecord _details;

    public FileNameRecord getDetails() {
        return _details;
    }

    public boolean isDirectory() {
        return getDetails()._flags.contains(FileAttributeFlags.Directory);
    }

    private FileRecordReference _reference;

    public FileRecordReference getReference() {
        return _reference;
    }

    public String getSearchName() {
        String fileName = _details._fileName;
        if (fileName.indexOf('.') == -1) {
            return fileName + ".";
        }

        return fileName;
    }

    public void updateFrom(File file) {
        file.freshenFileName(_details, true);
        _directory.updateEntry(this);
    }

    public String toString() {
        return _details._fileName;
    }
}
