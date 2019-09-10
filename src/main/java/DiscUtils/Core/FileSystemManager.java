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

package DiscUtils.Core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import DiscUtils.Core.CoreCompat.ReflectionHelper;
import DiscUtils.Core.Vfs.VfsFileSystemFactory;
import DiscUtils.Core.Vfs.VfsFileSystemFactoryAttribute;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * FileSystemManager determines which file systems are present on a volume.
 * 
 * The static detection methods detect default file systems. To plug in
 * additional
 * file systems, create an instance of this class and call RegisterFileSystems.
 */
public class FileSystemManager {
    private static final List<VfsFileSystemFactory> _factories;
    static {
        /**
         * Initializes a new instance of the FileSystemManager class.
         */
        _factories = new ArrayList<>();
    }

    /**
     * Registers new file systems with an instance of this class.
     * 
     * @param factory The detector for the new file systems.
     */
    public static void registerFileSystems(VfsFileSystemFactory factory) {
        _factories.add(factory);
    }

    /**
     * Registers new file systems detected in an assembly.
     * 
     * @param assembly The assembly to inspect.
     *            To be detected, the
     *            {@code VfsFileSystemFactory}
     *            instances must be marked with the
     * 
     *            {@code VfsFileSystemFactoryAttribute}
     *            > attribute.
     */
    public static void registerFileSystems(List<Class<VfsFileSystemFactory>> assembly) {
        _factories.addAll(detectFactories(assembly));
    }

    /**
     * Detect which file systems are present on a volume.
     * 
     * @param volume The volume to inspect.
     * @return The list of file systems detected.
     */
    public static List<FileSystemInfo> detectFileSystems(VolumeInfo volume) {
        try (Stream s = volume.open()) {
            return doDetect(s, volume);
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Detect which file systems are present in a stream.
     * 
     * @param stream The stream to inspect.
     * @return The list of file systems detected.
     */
    public static List<FileSystemInfo> detectFileSystems(Stream stream) {
        return doDetect(stream, null);
    }

    private static List<VfsFileSystemFactory> detectFactories(List<Class<VfsFileSystemFactory>> assembly) {
        List<VfsFileSystemFactory> result = new ArrayList<>();
        for (Class<?> type : assembly) {
            VfsFileSystemFactoryAttribute attrib = ReflectionHelper.getCustomAttribute(type, VfsFileSystemFactoryAttribute.class, false);
            if (attrib == null)
                continue;
// TODO result.add();
        }
        return result;
    }

    private static List<FileSystemInfo> doDetect(Stream stream, VolumeInfo volume) {
        BufferedStream detectStream = new BufferedStream(stream);
        List<FileSystemInfo> detected = new ArrayList<>();
        for (VfsFileSystemFactory factory : _factories) {
            detected.AddRange(factory.Detect(detectStream, volume));
        }
        return detected;
    }

}
