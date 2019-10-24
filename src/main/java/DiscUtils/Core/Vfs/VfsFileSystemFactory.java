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

package DiscUtils.Core.Vfs;

import java.io.IOException;

import DiscUtils.Core.FileSystemInfo;
import DiscUtils.Core.VolumeInfo;
import dotnet4j.io.Stream;


/**
 * Base class for logic to detect file systems.
 */
public abstract class VfsFileSystemFactory {
    /**
     * Detects if a stream contains any known file systems.
     *
     * @param stream The stream to inspect.
     * @return A list of file systems (may be empty).
     */
    public FileSystemInfo[] detect(Stream stream) {
        return detect(stream, null);
    }

    /**
     * Detects if a volume contains any known file systems.
     *
     * @param volume The volume to inspect.
     * @return A list of file systems (may be empty).
     */
    public FileSystemInfo[] detect(VolumeInfo volume) {
        try (Stream stream = volume.open()) {
            return detect(stream, volume);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * The logic for detecting file systems.
     *
     * @param stream The stream to inspect.
     * @param volumeInfo Optionally, information about the volume.
     * @return A list of file systems detected (may be empty).
     */
    public abstract FileSystemInfo[] detect(Stream stream, VolumeInfo volumeInfo);
}
