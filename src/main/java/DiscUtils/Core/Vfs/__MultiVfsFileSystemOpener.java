
package DiscUtils.Core.Vfs;

//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import vavi.io.dus.DiscFileSystem;
//
//import DiscUtils.Core.FileSystemParameters;
//import DiscUtils.Core.VolumeInfo;
//import DiscUtils.Core.CoreCompat.ListSupport;


/**
 * Delegate for instantiating a file system.
 * 
 * @param stream The stream containing the file system.
 * @param volumeInfo Optional, information about the volume the file system is
 *            on.
 * @param parameters Parameters for the file system.
 * @return A file system implementation.
 */
//public class __MultiVfsFileSystemOpener implements VfsFileSystemOpener {
//    public DiscFileSystem invoke(Stream stream, VolumeInfo volumeInfo, FileSystemParameters parameters) {
//        List<VfsFileSystemOpener> copy = new ArrayList<>(), members = this.getInvocationList();
//        synchronized (members) {
//            copy = new LinkedList<VfsFileSystemOpener>(members);
//        }
//        VfsFileSystemOpener prev = null;
//        for (Object __dummyForeachVar0 : copy) {
//            VfsFileSystemOpener d = (VfsFileSystemOpener) __dummyForeachVar0;
//            if (prev != null)
//                prev.invoke(stream, volumeInfo, parameters);
//
//            prev = d;
//        }
//        return prev.invoke(stream, volumeInfo, parameters);
//    }
//
//    private List<VfsFileSystemOpener> _invocationList = new ArrayList<>();
//
//    public static VfsFileSystemOpener combine(VfsFileSystemOpener a, VfsFileSystemOpener b) {
//        if (a == null)
//            return b;
//
//        if (b == null)
//            return a;
//
//        __MultiVfsFileSystemOpener ret = new __MultiVfsFileSystemOpener();
//        ret._invocationList = a.getInvocationList();
//        ret._invocationList.addAll(b.getInvocationList());
//        return ret;
//    }
//
//    public static VfsFileSystemOpener remove(VfsFileSystemOpener a, VfsFileSystemOpener b) {
//        if (a == null || b == null)
//            return a;
//
//        List<VfsFileSystemOpener> aInvList = a.getInvocationList();
//        List<VfsFileSystemOpener> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//        if (aInvList == newInvList) {
//            return a;
//        } else {
//            __MultiVfsFileSystemOpener ret = new __MultiVfsFileSystemOpener();
//            ret._invocationList = newInvList;
//            return ret;
//        }
//    }
//
//    public List<VfsFileSystemOpener> getInvocationList() {
//        return _invocationList;
//    }
//
//}
