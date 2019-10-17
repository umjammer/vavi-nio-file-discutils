
package DiscUtils.Core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

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
        extensionMap = new HashMap<>();
        typeMap = new HashMap<>();
        diskTransports = new HashMap<>();
    }

    private static Map<String, VirtualDiskTransport> diskTransports;

    public static Map<String, VirtualDiskTransport> getDiskTransports() {
        return diskTransports;
    }

    private static Map<String, VirtualDiskFactory> extensionMap;

    public static Map<String, VirtualDiskFactory> getExtensionMap() {
        return extensionMap;
    }

    /**
     * Gets the set of disk formats supported as an array of file extensions.
     */
    public static Collection<String> getSupportedDiskFormats() {
        return extensionMap.keySet();
    }

    /**
     * Gets the set of disk types supported, as an array of identifiers.
     */
    public static Collection<String> getSupportedDiskTypes() {
        return typeMap.keySet();
    }

    private static Map<String, VirtualDiskFactory> typeMap;

    public static Map<String, VirtualDiskFactory> getTypeMap() {
        return typeMap;
    }

    // Locates VirtualDiskFactory factories attributed with
    // VirtualDiskFactoryAttribute, and types marked with
    // VirtualDiskTransportAttribute, that are able to work with Virtual Disk
    // types.
    static {
        ServiceLoader<VirtualDiskFactory> factories = ServiceLoader.load(VirtualDiskFactory.class);

        for (VirtualDiskFactory factory : factories) {
            VirtualDiskFactoryAttribute annotation = ReflectionHelper
                    .getCustomAttribute(factory.getClass(), VirtualDiskFactoryAttribute.class, false);
            if (annotation != null) {
                typeMap.put(annotation.type(), factory);
                for (String extension : annotation.fileExtensions()) {
                    extensionMap.put(extension.toUpperCase(), factory);
                }
            }
        }

        ServiceLoader<VirtualDiskTransport> transports = ServiceLoader.load(VirtualDiskTransport.class);

        for (VirtualDiskTransport transport : transports) {
            VirtualDiskTransportAttribute annotation = ReflectionHelper
                    .getCustomAttribute(transport.getClass(), VirtualDiskTransportAttribute.class, false);
            if (annotation != null) {
                diskTransports.put(annotation.scheme().toUpperCase(), transport);
            }
        }
    }
}
