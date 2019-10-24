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

package DiscUtils.Iso9660;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import DiscUtils.Core.Internal.LocalFileLocator;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;


/**
 * Represents a file that will be built into the ISO image.
 */
public final class BuildFileInfo extends BuildDirectoryMember {
    private byte[] _contentData;

    private String _contentPath;

    private long _contentSize;

    private Stream _contentStream;

    public BuildFileInfo(String name, BuildDirectoryInfo parent, byte[] content) {
        super(IsoUtilities.normalizeFileName(name), makeShortFileName(name, parent));
        __Parent = parent;
        _contentData = content;
        _contentSize = content.length;
    }

    public BuildFileInfo(String name, BuildDirectoryInfo parent, String content) {
        super(IsoUtilities.normalizeFileName(name), makeShortFileName(name, parent));
        __Parent = parent;
        _contentPath = content;
        try {
            _contentSize = Files.size(Paths.get(_contentPath));
            setCreationTime(Files.getLastModifiedTime(Paths.get(_contentPath)).toMillis());
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public BuildFileInfo(String name, BuildDirectoryInfo parent, Stream source) {
        super(IsoUtilities.normalizeFileName(name), makeShortFileName(name, parent));
        __Parent = parent;
        _contentStream = source;
        _contentSize = _contentStream.getLength();
    }

    /**
     * The parent directory, or
     * {@code null}
     * if none.
     */
    private BuildDirectoryInfo __Parent;

    public BuildDirectoryInfo getParent() {
        return __Parent;
    }

    public long getDataSize(Charset enc) {
        return _contentSize;
    }

    public Stream openStream() {
        if (_contentData != null) {
            return new MemoryStream(_contentData, false);
        }

        if (_contentPath != null) {
            LocalFileLocator locator = new LocalFileLocator("");
            return locator.open(_contentPath, FileMode.Open, FileAccess.Read, FileShare.Read);
        }

        return _contentStream;
    }

    public void closeStream(Stream s) {
        // Close and dispose the stream, unless it's one we were given to stream in
        // from (we might need it again).
        if (_contentStream != s) {
            try {
                s.close();
            } catch (IOException e) {
                new dotnet4j.io.IOException(e);
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
}
