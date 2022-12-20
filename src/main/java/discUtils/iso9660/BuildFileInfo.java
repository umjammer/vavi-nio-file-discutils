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

package discUtils.iso9660;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import discUtils.core.internal.LocalFileLocator;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;


/**
 * Represents a file that will be built into the ISO image.
 */
public final class BuildFileInfo extends BuildDirectoryMember {

    private byte[] contentData;

    private String contentPath;

    private long contentSize;

    private Stream contentStream;

    BuildFileInfo(String name, BuildDirectoryInfo parent, byte[] content) {
        super(IsoUtilities.normalizeFileName(name), makeShortFileName(name, parent));
        this.parent = parent;
        contentData = content;
        contentSize = content.length;
    }

    BuildFileInfo(String name, BuildDirectoryInfo parent, String content) {
        super(IsoUtilities.normalizeFileName(name), makeShortFileName(name, parent));
        this.parent = parent;
        contentPath = content;
        try {
            Path path = Paths.get(contentPath);
            contentSize = Files.size(path);
            creationTime = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    BuildFileInfo(String name, BuildDirectoryInfo parent, Stream source) {
        super(IsoUtilities.normalizeFileName(name), makeShortFileName(name, parent));
        this.parent = parent;
        contentStream = source;
        contentSize = contentStream.getLength();
    }

    /**
     * The parent directory, or {@code null} if none.
     */
    private BuildDirectoryInfo parent;

    @Override public BuildDirectoryInfo getParent() {
        return parent;
    }

    @Override long getDataSize(Charset enc) {
        return contentSize;
    }

    Stream openStream() {
        if (contentData != null) {
            return new MemoryStream(contentData, false);
        }

        if (contentPath != null) {
            LocalFileLocator locator = new LocalFileLocator("");
            return locator.open(contentPath, FileMode.Open, FileAccess.Read, FileShare.Read);
        }

        return contentStream;
    }

    void closeStream(Stream s) {
        // Close and dispose the stream, unless it's one we were given to stream
        // in from (we might need it again).
        if (contentStream != s) { // TODO object compare
            try {
                s.close();
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }
    }

    private static String makeShortFileName(String longName, BuildDirectoryInfo dir) {
        if (IsoUtilities.isValidFileName(longName)) {
            return longName;
        }

        char[] shortNameChars = longName.toUpperCase().toCharArray();
        for (int i = 0; i < shortNameChars.length; ++i) {
            if (!IsoUtilities.isValidDChar(shortNameChars[i]) && shortNameChars[i] != '.' && shortNameChars[i] != ';') {
                shortNameChars[i] = '_';
            }
        }

        String[] parts = IsoUtilities.splitFileName(new String(shortNameChars));

        if (parts[0].length() + parts[1].length() > 30) {
            parts[1] = parts[1].substring(0, Math.min(parts[1].length(), 3));
        }

        if (parts[0].length() + parts[1].length() > 30) {
            parts[0] = parts[0].substring(0, 30 - parts[1].length());
        }

        String candidate = parts[0] + '.' + parts[1] + ';' + parts[2];

        // TODO: Make unique
        return candidate;
    }

    @Override public String toString() {
        return getName();
    }
}
