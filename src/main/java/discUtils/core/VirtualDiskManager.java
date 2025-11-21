
package discUtils.core;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import discUtils.core.internal.VirtualDiskFactory;
import discUtils.core.internal.VirtualDiskFactoryAttribute;
import discUtils.core.internal.VirtualDiskTransport;
import discUtils.core.internal.VirtualDiskTransportAttribute;

import static java.lang.System.getLogger;


/**
 * Helps discover and use VirtualDiskFactory's
 */
public class VirtualDiskManager {

    private static final Logger logger = getLogger(VirtualDiskManager.class.getName());

    static {
        extensionMap = new HashMap<>();
        typeMap = new HashMap<>();
        diskTransports = new HashMap<>();
    }

    private static final Map<String, VirtualDiskTransport> diskTransports;

    public static Map<String, VirtualDiskTransport> getDiskTransports() {
        return diskTransports;
    }

    private static final Map<String, VirtualDiskFactory> extensionMap;

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

    private static final Map<String, VirtualDiskFactory> typeMap;

    public static Map<String, VirtualDiskFactory> getTypeMap() {
        return typeMap;
    }

    /*
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
logger.log(Level.DEBUG, "typeMap: " + typeMap.keySet());
logger.log(Level.DEBUG, "extensionMap: " + extensionMap.keySet());

        ServiceLoader<VirtualDiskTransport> transports = ServiceLoader.load(VirtualDiskTransport.class);

        for (VirtualDiskTransport transport : transports) {
            VirtualDiskTransportAttribute annotation = transport.getClass().getAnnotation(VirtualDiskTransportAttribute.class);
            if (annotation != null) {
                diskTransports.put(annotation.scheme().toUpperCase(), transport);
            }
        }
logger.log(Level.DEBUG, "diskTransports: " + diskTransports.keySet());
    }
}
