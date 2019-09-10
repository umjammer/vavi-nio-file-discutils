
package DiscUtils.Core;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Core.CoreCompat.ReflectionHelper;
import DiscUtils.Core.Internal.VirtualDiskFactory;
import DiscUtils.Core.Internal.VirtualDiskFactoryAttribute;
import DiscUtils.Core.Internal.VirtualDiskTransport;
import DiscUtils.Core.Internal.VirtualDiskTransportAttribute;


/**
 * Helps discover and use VirtualDiskFactory's
 */
public class VirtualDiskManager {
    static {
        __ExtensionMap = new HashMap<>();
        __TypeMap = new HashMap<>();
        __DiskTransports = new HashMap<>();
    }

    private static Map<String, Class<VirtualDiskTransport>> __DiskTransports;

    public static Map<String, Class<VirtualDiskTransport>> getDiskTransports() {
        return __DiskTransports;
    }

    private static Map<String, VirtualDiskFactory> __ExtensionMap;

    public static Map<String, VirtualDiskFactory> getExtensionMap() {
        return __ExtensionMap;
    }

    /**
     * Gets the set of disk formats supported as an array of file extensions.
     */
    public static Collection<String> getSupportedDiskFormats() {
        return getExtensionMap().keySet();
    }

    /**
     * Gets the set of disk types supported, as an array of identifiers.
     */
    public static Collection<String> getSupportedDiskTypes() {
        return getTypeMap().keySet();
    }

    private static Map<String, VirtualDiskFactory> __TypeMap;

    public static Map<String, VirtualDiskFactory> getTypeMap() {
        return __TypeMap;
    }

    /**
     * Locates VirtualDiskFactory factories attributed with
     * VirtualDiskFactoryAttribute, and types marked with
     * VirtualDiskTransportAttribute, that are able to work with Virtual Disk
     * types.
     * 
     * @param assembly An assembly to scan
     */
    public static void registerVirtualDiskTypes(List<Class<VirtualDiskTransport>> assembly) {
        try {
            for (Class<VirtualDiskTransport> type : assembly) {
                VirtualDiskFactoryAttribute diskFactoryAttribute = ReflectionHelper
                        .getCustomAttribute(type, VirtualDiskFactoryAttribute.class, false);
                if (diskFactoryAttribute != null) {
                    VirtualDiskFactory factory = VirtualDiskFactory.class.cast(type.newInstance());
                    getTypeMap().put(diskFactoryAttribute.getType(), factory);
                    for (String extension : diskFactoryAttribute.getFileExtensions()) {
                        getExtensionMap().put(extension.toUpperCase(), factory);
                    }
                }

                if (VirtualDiskTransportAttribute.class
                        .isInstance(ReflectionHelper.getCustomAttribute(type, VirtualDiskTransportAttribute.class, false))) {
                    getDiskTransports().put(VirtualDiskTransportAttribute.class
                            .cast(ReflectionHelper.getCustomAttribute(type, VirtualDiskTransportAttribute.class, false))
                            .getScheme()
                            .toUpperCase(), type);
                }
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
