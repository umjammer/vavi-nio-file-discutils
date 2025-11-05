/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.qcow2;

import java.io.IOException;

import discUtils.core.DiskImageBuilder;
import discUtils.core.FileLocator;
import discUtils.core.VirtualDisk;
import discUtils.core.VirtualDiskLayer;
import discUtils.core.VirtualDiskParameters;
import discUtils.core.VirtualDiskTypeInfo;
import discUtils.core.internal.VirtualDiskFactory;
import discUtils.core.internal.VirtualDiskFactoryAttribute;
import dotnet4j.io.FileAccess;


/**
 * DiskFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2025/10/01 umjammer initial version <br>
 */
@VirtualDiskFactoryAttribute(type = "QCOW2", fileExtensions = { ".qcow2" })
public final class DiskFactory implements VirtualDiskFactory {

    @Override
    public String[] getVariants() {
        return new String[] { "qcow2" };
    }

    @Override
    public VirtualDiskTypeInfo getDiskTypeInformation(String variant) {
        return makeDiskTypeInfo(variant);
    }

    @Override
    public DiskImageBuilder getImageBuilder(String variant) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualDisk createDisk(FileLocator locator,
                                  String variant,
                                  String path,
                                  VirtualDiskParameters diskParameters) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualDisk openDisk(String path, FileAccess access) throws IOException {
        return new Disk(path, access);
    }

    @Override
    public VirtualDisk openDisk(FileLocator locator, String path, FileAccess access) throws IOException {
        return new Disk(locator, path, access);
    }

    @Override
    public VirtualDiskLayer openDiskLayer(FileLocator locator, String path, FileAccess access) {
        return new DiskImageFile(locator, path, access);
    }

    public static VirtualDiskTypeInfo makeDiskTypeInfo(String variant) {
        return new VirtualDiskTypeInfo();
    }
}
