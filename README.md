# vavi-nio-file-discutils

## Status

| fs       | list | upload | download | copy | move | rm | mkdir | cache | watch | create | comment |
|----------|------|--------|----------|------|------|----|-------|-------|-------|--------|---------|
| ISO      | 🚧   |        |          |      |      |    |       |       |       | ✅     |         |
| UDF      |      |        |          |      |      |    |       |       |       |        |         |
| FAT      |      |        |          |      |      |    |       |       |       |        |         |
| NTFS     | ✅   |        | ✅      |      |      |    |       |       |       |        |         |
| VHD      |      |        |          |      |      |    |       |       |       |        |         |
| VDI      | ✅   |        |          |      |      |     |      |       |       |        |         |
| XVA      |      |        |          |      |      |    |       |       |       |        |         |
| VMDK     |      |        |          |      |      |    |       |       |       |        |         |
| DMG      | ✅   |        |          |      |     |    |        |       |       |        |         |
| HSF+     | ✅ (DMG) |        |          |      |     |    |        |       |       |        | 🚫 (ISO) same error on original |
| EXT     | 🚧 (VDI) |        |          |      |     |    |        |       |       |        |        |
| Registry | ✅   |        |          |      |      |    |       |       |       |        |         |
|  BCD     | ✅   |        |          |      |      |    |       |       |       |        |         |
| iSCSI    |      |        |          |      |      |    |       |       |       |        |         |
| NFS      |      |        |          |      |      |    |       |       |       |        |         |
| Optical Disk Share |      |        |          |      |      |    |       |       |       |        |         |

## Project Description

[![Actions Status](https://github.com/umjammer/vavi-nio-file-discutils/workflows/Java%20CI/badge.svg)](https://github.com/umjammer/vavi-nio-file-discutils/actions)

vavi-nio-file-discutils is a Java library to read and write ISO files and Virtual Machine disk files (VHD, VDI, XVA, VMDK, etc). DiscUtils is developed in Java with no native code.

Implementation of the ISO, UDF, FAT and NTFS file systems is now fairly stable. VHD, XVA, VMDK and VDI disk formats are implemented, as well as read/write Registry support. The library also includes a simple iSCSI initiator, for accessing disks via iSCSI and an NFS client implementation.

Note: this is a fork of https://github.com/DiscUtils/DiscUtils, which itself is a fork of https://github.com/quamotion/DiscUtils, which itself is a fork of https://discutils.codeplex.com/. 

### Wiki (at original site)

See more up to date documentation at the [Wiki](https://github.com/DiscUtils/DiscUtils/wiki)

### Implementation in this repository

This repository has performed a few changes to the core DiscUtils library. For starters, all projects have been converted to Java, and are targeting Java 8.

The vavi-nio-file-discutils library has been split into 25 independent projects, which can function without the others present. This reduces the "cost" of having vavi-nio-file-discutils immensely, as we're down from the 1 MB binary it used to be. 

To work with this, four Meta packages have been created:

* DiscUtils.Complete: Everything, like before
* DiscUtils.Containers: such as VMDK, VHD, VHDX
* DiscUtils.FileSystems: such as NTFS, FAT, EXT
* DiscUtils.Transports: such as NFS

#### Note on detections

vavi-nio-file-discutils has a number of detection helpers. These provide services like "which filesystem is this stream?". For this to work, you must register your filesystem providers with the DiscUtils core. To do this, write:

    META-INF/services/`class name`

Where `class name` is the classes you wish to register. Note that the metapackages have helpers:

    META-INF/services/DiscUtils.Core.Internal.VirtualDiskFactory // From DiscUtils.Complete
    META-INF/services/DiscUtils.Core.Internal.LogicalVolumeFactory // From DiscUtils.Containers
    META-INF/services/DiscUtils.Core.Vfs.VfsFileSystemFactory // From DiscUtils.FileSystems
    META-INF/services/DiscUtils.Core.Internal.VirtualDiskTransport // From DiscUtils.Transports

## How to use the Library

Here's a few really simple examples.

### How to create a new ISO:

```Java
CDBuilder builder = new CDBuilder();
builder.useJoliet = true;
builder.volumeIdentifier = "A_SAMPLE_DISK";
builder.addFile("Folder\\Hello.txt", "Hello World!".getBytes(Charset.forName("ASCII")));
builder.build("C:\\temp\\sample.iso");
```

You can add files as byte arrays (shown above), as files from the Windows filesystem, or as a Stream. By using a different form of Build, you can get a Stream to the ISO file, rather than writing it to the Windows filesystem.


### How to extract a file from an ISO:

```Java
try (FileStream isoStream = File.open("C:\\temp\\sample.iso")) {
  CDReader cd = new CDReader(isoStream, true);
  Stream fileStream = cd.openFile("Folder\\Hello.txt", FileMode.Open);
  // Use fileStream...
}
```

You can also browse through the directory hierarchy, starting at cd.Root.

### How to create a virtual hard disk:

```Java
long diskSize = 30 * 1024 * 1024; //30MB
try (Stream vhdStream = File.create("C:\\TEMP\\mydisk.vhd")) {
    Disk disk = Disk.initializeDynamic(vhdStream, diskSize);
    BiosPartitionTable.initialize(disk, WellKnownPartitionType.WindowsFat);
    try (FatFileSystem fs = FatFileSystem.formatPartition(disk, 0, null)) {
        fs.CreateDirectory(@"TestDir\CHILD");
        // do other things with the file system...
    }
}
```

As with ISOs, you can browse the file system, starting at fs.Root.


### How to create a virtual floppy disk:

```Java
try (FileStream fs = File.create("myfloppy.vfd");
     FatFileSystem floppy = FatFileSystem.formatFloppy(fs, FloppyDiskType.HighDensity, "MY FLOPPY  ");
     Stream s = floppy.openFile("foo.txt", FileMode.Create)) {
    // Use stream...
}
```

Again, start browsing the file system at floppy.Root.

## Development releases

Automated CI builds are available on Github.

## References

 * https://github.com/twiglet/cs2j
    * getter/setter properties naming
    * enum with value
    * injection without constructor
    * lambda expression
    * extended for
    * delegate eliminate listFoo etc.
    * Encoding
    * substring 2nd param is index instead of length
    * Array.Clear 3rd param is index instead of length 
    * Array.copy, Array.clear
    * Guid -> UUID
    * unit test (xUnit -> junit)
    * assembly -> ServiceLoader
    * attribute -> annotation
    * unsigned
    * object equals
    * operator overloads
 * https://github.com/feyris-tan/dotnetIo4j [(vavi patched)](https://github.com/umjammer/dotnet4j)
 * https://github.com/shevek/lzo-java
 * https://github.com/akaigoro/df4j
 * https://github.com/coderforlife/ms-compress
 * https://github.com/Saine87/SDDL-parser