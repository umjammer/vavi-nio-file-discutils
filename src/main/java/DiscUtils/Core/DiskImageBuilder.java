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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Core.CoreCompat.ReflectionHelper;
import DiscUtils.Core.Internal.VirtualDiskFactory;
import DiscUtils.Core.Internal.VirtualDiskFactoryAttribute;
import DiscUtils.Streams.SparseStream;


/**
 * Base class for all disk image builders.
 */
public abstract class DiskImageBuilder {
    private static Map<String, VirtualDiskFactory> _typeMap = new HashMap<>();

    /**
     * Gets or sets the geometry of this disk, as reported by the BIOS, will be
     * implied from the content stream if not set.
     */
    private Geometry __BiosGeometry;

    public Geometry getBiosGeometry() {
        return __BiosGeometry;
    }

    public void setBiosGeometry(Geometry value) {
        __BiosGeometry = value;
    }

    /**
     * Gets or sets the content for this disk, implying the size of the disk.
     */
    private SparseStream __Content;

    public SparseStream getContent() {
        return __Content;
    }

    public void setContent(SparseStream value) {
        __Content = value;
    }

    /**
     * Gets or sets the adapter type for created virtual disk, for file formats
     * that encode this information.
     */
    private GenericDiskAdapterType __GenericAdapterType = GenericDiskAdapterType.Ide;

    public GenericDiskAdapterType getGenericAdapterType() {
        return __GenericAdapterType;
    }

    public void setGenericAdapterType(GenericDiskAdapterType value) {
        __GenericAdapterType = value;
    }

    /**
     * Gets or sets the geometry of this disk, will be implied from the content
     * stream if not set.
     */
    private Geometry __Geometry;

    public Geometry getGeometry() {
        return __Geometry;
    }

    public void setGeometry(Geometry value) {
        __Geometry = value;
    }

    /**
     * Gets a value indicating whether this file format preserves BIOS geometry
     * information.
     */
    public boolean getPreservesBiosGeometry() {
        return false;
    }

    private static Map<String, VirtualDiskFactory> getTypeMap() {
        if (_typeMap == null) {
            initializeMaps();
        }

        return _typeMap;
    }

    /**
     * Gets an instance that constructs the specified type (and variant) of
     * virtual disk image.
     *
     * @param type The type of image to build (VHD, VMDK, etc).
     * @param variant The variant type (differencing/dynamic, fixed/static,
     *            etc).
     * @return The builder instance.
     */
    public static DiskImageBuilder getBuilder(String type, String variant) {
        if (!getTypeMap().containsKey(type)) {
            throw new IllegalArgumentException(String.format("Unknown disk type '%s'", type));
        }

        return getTypeMap().get(type).getImageBuilder(variant);
    }

    /**
     * Initiates the construction of the disk image.
     *
     * @param baseName The base name for the disk images.
     * @return A set of one or more logical files that constitute the
     *         disk image. The first file is the 'primary' file that is normally
     *         attached to VMs.The supplied
     *         {@code baseName}
     *         is the start of the file name, with no file
     *         extension. The set of file specifications will indicate the
     *         actual name corresponding
     *         to each logical file that comprises the disk image. For example,
     *         given a base name
     *         'foo', the files 'foo.vmdk' and 'foo-flat.vmdk' could be
     *         returned.
     */
    public abstract List<DiskImageFileSpecification> build(String baseName);

    private static void initializeMaps() {
        try {
            Map<String, VirtualDiskFactory> typeMap = new HashMap<>();
            for (Class<?> type : ReflectionHelper.getAssembly(VirtualDisk.class)) {
                VirtualDiskFactoryAttribute attr = ReflectionHelper
                        .getCustomAttribute(type, VirtualDiskFactoryAttribute.class, false);
                if (attr != null) {
                    VirtualDiskFactory factory = (VirtualDiskFactory) type.newInstance();
                    typeMap.put(attr.type(), factory);
                }

            }
            _typeMap = typeMap;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

}
