
package discUtils.complete;

@Deprecated
public class SetupHelper {
    public static void setupComplete() {
        Class[] x = new Class[] {
            discUtils.bootConfig.Store.class,
            discUtils.dmg.Disk.class,
            discUtils.btrfs.BtrfsFileSystem.class,
            discUtils.ext.ExtFileSystem.class,
            discUtils.fat.FatFileSystem.class,
            discUtils.hfsPlus.HfsPlusFileSystem.class,
            discUtils.iscsi.Disk.class,
            discUtils.iso9660.BuildFileInfo.class,
            discUtils.net.dns.DnsClient.class,
            discUtils.nfs.Nfs3Status.class,
            discUtils.ntfs.NtfsFileSystem.class,
            discUtils.opticalDiscSharing.DiscInfo.class,
            discUtils.opticalDisk.Disc.class,
            discUtils.registry.RegistryHive.class,
            discUtils.sdi.SdiFile.class,
            discUtils.squashFs.SquashFileSystemBuilder.class,
            discUtils.swap.SwapFileSystem.class,
            discUtils.udf.UdfReader.class,
            discUtils.vdi.Disk.class,
            discUtils.vhd.Disk.class,
            discUtils.vhdx.Disk.class,
            discUtils.vmdk.Disk.class,
            discUtils.wim.WimFile.class,
            discUtils.xfs.XfsFileSystem.class,
            discUtils.xva.Disk.class,
            discUtils.lvm.LogicalVolumeManager.class,
        };
    }
}
