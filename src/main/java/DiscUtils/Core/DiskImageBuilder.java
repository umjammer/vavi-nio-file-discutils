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
import java.util.ServiceLoader;

import DiscUtils.Core.CoreCompat.ReflectionHelper;
import DiscUtils.Core.Internal.VirtualDiskFactory;
import DiscUtils.Core.Internal.VirtualDiskFactoryAttribute;
import DiscUtils.Streams.SparseStream;


/**
 * Base class for all disk image builders.
 */
public abstract class DiskImageBuilder {
    private static final Map<String, VirtualDiskFactory> _typeMap;

    /**
     * Gets or sets the geometry of this disk, as reported by the BIOS, will be
     * implied from the content stream if not set.
     */
    private Geometry _biosGeometry;

    public Geometry getBiosGeometry() {
        return _biosGeometry;
    }

    public void setBiosGeometry(Geometry value) {
        _biosGeometry = value;
    }

    /**
     * Gets or sets the content for this disk, implying the size of the disk.
     */
    private SparseStream _content;

    public SparseStream getContent() {
        return _content;
    }

    public void setContent(SparseStream value) {
        _content = value;
    }

    /**
     * Gets or sets the adapter type for created virtual disk, for file formats
     * that encode this information.
     */
    private GenericDiskAdapterType _genericAdapterType = GenericDiskAdapterType.Ide;

    public GenericDiskAdapterType getGenericAdapterType() {
        return _genericAdapterType;
    }

    public void setGenericAdapterType(GenericDiskAdapterType value) {
        _genericAdapterType = value;
    }

    /**
     * Gets or sets the geometry of this disk, will be implied from the content
     * stream if not set.
     */
    private Geometry _geometry;

    public Geometry getGeometry() {
        return _geometry;
    }

    public void setGeometry(Geometry value) {
        _geometry = value;
    }

    /**
     * Gets a value indicating whether this file format preserves BIOS geometry
     * information.
     */
    public boolean getPreservesBiosGeometry() {
        return false;
    }

    private static Map<String, VirtualDiskFactory> getTypeMap() {
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
     * The supplied {@code baseName} is the start of the file name, with no file
     * extension. The set of file specifications will indicate the actual name
     * corresponding to each logical file that comprises the disk image. For
     * example, given a base name 'foo', the files 'foo.vmdk' and
     * 'foo-flat.vmdk' could be returned.
     * 
     * @param baseName The base name for the disk images.
     * @return A set of one or more logical files that constitute the disk
     *         image. The first file is the 'primary' file that is normally
     *         attached to VMs.
     */
    public abstract List<DiskImageFileSpecification> build(String baseName);

    static {
        ServiceLoader<VirtualDiskFactory> loader = ServiceLoader.load(VirtualDiskFactory.class);
        _typeMap = new HashMap<>();
        for (VirtualDiskFactory factory : loader) {
            VirtualDiskFactoryAttribute attr = ReflectionHelper
                    .getCustomAttribute(factory.getClass(), VirtualDiskFactoryAttribute.class, false);
            _typeMap.put(attr.type(), factory);
        }
    }
}
