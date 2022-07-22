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

package discUtils.xva;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import discUtils.core.archives.TarFileBuilder;
import discUtils.streams.ConcatStream;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.SubStream;
import discUtils.streams.ZeroStream;
import discUtils.streams.builder.BuilderExtent;
import discUtils.streams.builder.StreamBuilder;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Sizes;
import dotnet4j.io.Stream;


/**
 * A class that can be used to create Xen Virtual Appliance (XVA) files. This
 * class is not intended to be a general purpose XVA generator, the options to
 * control the VM properties are strictly limited. The class generates a minimal
 * VM really as a wrapper for one or more disk images, making them easy to
 * import into XenServer.
 */
public final class VirtualMachineBuilder extends StreamBuilder implements Closeable {

    static class DiskRecord {
        String item1;

        SparseStream item2;

        Ownership item3;

        public DiskRecord(String label, SparseStream stream, Ownership ownership) {
            this.item1 = label;
            this.item2 = stream;
            this.item3 = ownership;
        }
    }

    private final List<DiskRecord> disks;

    /**
     * Initializes a new instance of the VirtualMachineBuilder class.
     */
    public VirtualMachineBuilder() {
        disks = new ArrayList<>();
        setDisplayName("VM");
    }

    /**
     * Gets or sets the display name of the VM.
     */
    private String displayName;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String value) {
        displayName = value;
    }

    /**
     * Disposes this instance, including any underlying resources.
     */
    public void close() throws IOException {
        for (DiskRecord r : disks) {
            if (r.item3 == Ownership.Dispose) {
                r.item2.close();
            }
        }
    }

    /**
     * Adds a sparse disk image to the XVA file.
     *
     * @param label The admin-visible name of the disk.
     * @param content The content of the disk.
     * @param ownsContent Indicates if ownership of content is transfered.
     */
    public void addDisk(String label, SparseStream content, Ownership ownsContent) {
        disks.add(new DiskRecord(label, content, ownsContent));
    }

    /**
     * Adds a disk image to the XVA file.
     *
     * @param label The admin-visible name of the disk.
     * @param content The content of the disk.
     * @param ownsContent Indicates if ownership of content is transfered.
     */
    public void addDisk(String label, Stream content, Ownership ownsContent) {
        disks.add(new DiskRecord(label, SparseStream.fromStream(content, ownsContent), Ownership.Dispose));
    }

    /**
     * Creates a new stream that contains the XVA image.
     *
     * @return The new stream.
     */
    public SparseStream build() {
        try {
            TarFileBuilder tarBuilder = new TarFileBuilder();
            int[][] diskIds = new int[1][];
            String ovaFileContent = generateOvaXml(diskIds);
//Debug.println(ovaFileContent);
            tarBuilder.addFile("ova.xml", ovaFileContent.getBytes(StandardCharsets.US_ASCII));
            int diskIdx = 0;
            for (DiskRecord diskRec : disks) {
                SparseStream diskStream = diskRec.item2;
                List<StreamExtent> extents = diskStream.getExtents();
                int lastChunkAdded = -1;
                for (StreamExtent extent : extents) {
                    int firstChunk = (int) (extent.getStart() / Sizes.OneMiB);
                    int lastChunk = (int) ((extent.getStart() + extent.getLength() - 1) / Sizes.OneMiB);
                    for (int i = firstChunk; i <= lastChunk; ++i) {
                        if (i != lastChunkAdded) {
                            Stream chunkStream;
                            long diskBytesLeft = diskStream.getLength() - i * Sizes.OneMiB;
                            if (diskBytesLeft < Sizes.OneMiB) {
                                chunkStream = new ConcatStream(Ownership.Dispose,
                                                               new SubStream(diskStream, i * Sizes.OneMiB, diskBytesLeft),
                                                               new ZeroStream(Sizes.OneMiB - diskBytesLeft));
                            } else {
                                chunkStream = new SubStream(diskStream, i * Sizes.OneMiB, Sizes.OneMiB);
                            }
                            Stream chunkHashStream;
                            MessageDigest hashAlgDotnet = MessageDigest.getInstance("SHA-1");
                            chunkHashStream = new HashStreamDotnet(chunkStream, Ownership.Dispose, hashAlgDotnet);
                            tarBuilder.addFile(String.format("Ref:%s/%8d", diskIds[0][diskIdx], i), chunkHashStream);
                            byte[] hash;
                            hashAlgDotnet.update(new byte[0], 0, 0);
                            hash = hashAlgDotnet.digest();
                            String hashString = BitSet.valueOf(hash).toString().replace("-", "").toLowerCase();
                            byte[] hashStringAscii = hashString.getBytes(StandardCharsets.US_ASCII);
                            tarBuilder.addFile(String.format("Ref:%s/%8d.checksum", diskIds[0][diskIdx], i), hashStringAscii);
                            lastChunkAdded = i;
                        }
                    }
                }
                // Make sure the last chunk is present, filled with zero's if
                // necessary
                int lastActualChunk = (int) ((diskStream.getLength() - 1) / Sizes.OneMiB);
                if (lastChunkAdded < lastActualChunk) {
                    Stream chunkStream = new ZeroStream(Sizes.OneMiB);
                    Stream chunkHashStream;
                    MessageDigest hashAlgDotnet = MessageDigest.getInstance("SHA-1");
                    chunkHashStream = new HashStreamDotnet(chunkStream, Ownership.Dispose, hashAlgDotnet);
                    tarBuilder.addFile(String.format("Ref:%s/%8d", diskIds[0][diskIdx], lastActualChunk), chunkHashStream);
                    byte[] hash;
                    hashAlgDotnet.update(new byte[0], 0, 0);
                    hash = hashAlgDotnet.digest();
                    String hashString = BitSet.valueOf(hash).toString().replace("-", "").toLowerCase();
                    byte[] hashStringAscii = hashString.getBytes(StandardCharsets.US_ASCII);
                    tarBuilder.addFile(String.format("Ref:%s/%8d.checksum", diskIds[0][diskIdx], lastActualChunk),
                                       hashStringAscii);
                }

                ++diskIdx;
            }
            return tarBuilder.build();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    protected List<BuilderExtent> fixExtents(long[] totalLength) {
        // Not required - deferred to TarFileBuilder
        throw new UnsupportedOperationException();
    }

    private static final String[] names = {
        "XVA_ova_base", "XVA_ova_ref", "XVA_ova_vbd", "XVA_ova_vm", "XVA_ova_vdi", "XVA_ova_sr"
    };

    private static Map<String, String> getStaticStrings() {
        Map<String, String> results = new HashMap<>();
        Arrays.stream(names).forEach(s -> {
            Scanner scanner = new Scanner(VirtualMachineBuilder.class.getResourceAsStream("/" + s + ".xml"));
            List<String> lines = new ArrayList<>();
            while (scanner.hasNextLine()) {
                lines.add(scanner.nextLine());
            }
            scanner.close();
            String v = String.join("\n", lines);
            results.put(s, v);
        });
        return results;
    }

    /**
     * @param diskIds {@cs out}
     */
    private String generateOvaXml(int[][] diskIds) {
        int id = 0;
        UUID vmGuid = UUID.randomUUID();
        String vmName = getDisplayName();
        int vmId = id++;
        // Establish per-disk info
        UUID[] vbdGuids = new UUID[disks.size()];
        int[] vbdIds = new int[disks.size()];
        UUID[] vdiGuids = new UUID[disks.size()];
        String[] vdiNames = new String[disks.size()];
        int[] vdiIds = new int[disks.size()];
        long[] vdiSizes = new long[disks.size()];
        int diskIdx = 0;
        for (DiskRecord disk : disks) {
            vbdGuids[diskIdx] = UUID.randomUUID();
            vbdIds[diskIdx] = id++;
            vdiGuids[diskIdx] = UUID.randomUUID();
            vdiIds[diskIdx] = id++;
            vdiNames[diskIdx] = disk.item1;
            vdiSizes[diskIdx] = MathUtilities.roundUp(disk.item2.getLength(), Sizes.OneMiB);
            diskIdx++;
        }
        // Establish SR info
        UUID srGuid = UUID.randomUUID();
        String srName = "SR";
        int srId = id++;
        StringBuilder vbdRefs = new StringBuilder();
        Map<String, String> staticStrings = getStaticStrings();
        for (int i = 0; i < disks.size(); ++i) {
            vbdRefs.append(String.format(staticStrings.get("XVA_ova_ref"), "Ref:" + vbdIds[i]));
        }
        StringBuilder vdiRefs = new StringBuilder();
        for (int i = 0; i < disks.size(); ++i) {
            vdiRefs.append(String.format(staticStrings.get("XVA_ova_ref"), "Ref:" + vdiIds[i]));
        }
        StringBuilder objectsString = new StringBuilder();
        objectsString.append(String.format(staticStrings.get("XVA_ova_vm"), "Ref:" + vmId, vmGuid, vmName, vbdRefs));
        for (int i = 0; i < disks.size(); ++i) {
            objectsString.append(String
                    .format(staticStrings.get("XVA_ova_vbd"), "Ref:" + vbdIds[i], vbdGuids[i], "Ref:" + vmId, "Ref:" + vdiIds[i], i));
        }
        for (int i = 0; i < disks.size(); ++i) {
            objectsString.append(String.format(staticStrings.get("XVA_ova_vdi"),
                                               "Ref:" + vdiIds[i],
                                               vdiGuids[i],
                                               vdiNames[i],
                                               "Ref:" + srId,
                                               "Ref:" + vbdIds[i],
                                               vdiSizes[i]));
        }
        objectsString.append(String.format(staticStrings.get("XVA_ova_sr"), "Ref:" + srId, srGuid, srName, vdiRefs));
        diskIds[0] = vdiIds;
        return String.format(staticStrings.get("XVA_ova_base"), objectsString);
    }
}
