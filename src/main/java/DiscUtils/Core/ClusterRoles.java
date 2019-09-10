
package DiscUtils.Core;

public enum ClusterRoles {
    /**
     * Enumeration of possible cluster roles.
     * A cluster may be in more than one role.
     * Unknown, or unspecified role.
     */
    None,
    /**
     * Cluster is free.
     */
    Free,
    /**
     * Cluster is in use by a normal file.
     */
    DataFile,
    __dummyEnum__0,
    /**
     * Cluster is in use by a system file.
     * This isn't a file marked with the 'system' attribute,
     * rather files that form part of the file system namespace but also
     * form part of the file system meta-data.
     */
    SystemFile,
    __dummyEnum__1,
    __dummyEnum__2,
    __dummyEnum__3,
    /**
     * Cluster is in use for meta-data.
     */
    Metadata,
    __dummyEnum__4,
    __dummyEnum__5,
    __dummyEnum__6,
    __dummyEnum__7,
    __dummyEnum__8,
    __dummyEnum__9,
    __dummyEnum__10,
    /**
     * Cluster contains the boot region.
     */
    BootArea,
    __dummyEnum__11,
    __dummyEnum__12,
    __dummyEnum__13,
    __dummyEnum__14,
    __dummyEnum__15,
    __dummyEnum__16,
    __dummyEnum__17,
    __dummyEnum__18,
    __dummyEnum__19,
    __dummyEnum__20,
    __dummyEnum__21,
    __dummyEnum__22,
    __dummyEnum__23,
    __dummyEnum__24,
    __dummyEnum__25,
    /**
     * Cluster is marked bad.
     */
    Bad
}
