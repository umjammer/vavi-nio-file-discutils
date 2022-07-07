
package discUtils.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;

import vavi.util.Debug;

import discUtils.core.internal.VirtualDiskFactory;
import discUtils.core.internal.VirtualDiskFactoryAttribute;
import discUtils.core.internal.VirtualDiskTransport;
import discUtils.core.internal.VirtualDiskTransportAttribute;


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

    /**
     * Locates {@link VirtualDiskFactory} factories attributed with
     * {@link VirtualDiskFactoryAttribute}, and types marked with
     * {@link VirtualDiskTransportAttribute}, that are able to work with Virtual Disk
     * types.
     */
    static {
        ServiceLoader<VirtualDiskFactory> factories = ServiceLoader.load(VirtualDiskFactory.class);

        for (VirtualDiskFactory factory : factories) {
            VirtualDiskFactoryAttribute annotation = factory.getClass().getAnnotation(VirtualDiskFactoryAttribute.class);
            if (annotation != null) {
                typeMap.put(annotation.type(), factory);
                for (String extension : annotation.fileExtensions()) {
                    extensionMap.put(extension.replace(".", "").toUpperCase(), factory);
                }
            }
        }
Debug.println(Level.FINE, "typeMap: " + typeMap);
Debug.println(Level.FINE, "extensionMap: " + extensionMap);

        ServiceLoader<VirtualDiskTransport> transports = ServiceLoader.load(VirtualDiskTransport.class);

        for (VirtualDiskTransport transport : transports) {
            VirtualDiskTransportAttribute annotation = transport.getClass().getAnnotation(VirtualDiskTransportAttribute.class);
            if (annotation != null) {
                diskTransports.put(annotation.scheme().toUpperCase(), transport);
            }
        }
Debug.println(Level.FINE, "diskTransports: " + diskTransports);
    }
}
