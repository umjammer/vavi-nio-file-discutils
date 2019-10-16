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

public class DirectoryEntry {
    private final Directory _directory;

    public DirectoryEntry(Directory directory, FileRecordReference fileReference, FileNameRecord fileDetails) {
        _directory = directory;
        __Reference = fileReference;
        __Details = fileDetails;
    }

    private FileNameRecord __Details;

    public FileNameRecord getDetails() {
        return __Details;
    }

    public boolean getIsDirectory() {
        return getDetails().Flags.contains(FileAttributeFlags.Directory);
    }

    private FileRecordReference __Reference;

    public FileRecordReference getReference() {
        return __Reference;
    }

    public String getSearchName() {
        String fileName = getDetails().FileName;
        if (fileName.indexOf('.') == -1) {
            return fileName + ".";
        }

        return fileName;
    }

    public void updateFrom(File file) {
        file.freshenFileName(getDetails(), true);
        _directory.updateEntry(this);
    }
}
