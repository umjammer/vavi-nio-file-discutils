/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package libraryTests.iso9660;

import java.io.File;
import java.nio.charset.StandardCharsets;

import discUtils.iso9660.CDBuilder;
import discUtils.iso9660.CDReader;
import discUtils.iso9660.CommonVolumeDescriptor;
import discUtils.iso9660.Iso9660Variant;
import discUtils.streams.SparseStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Volume level meta data of an ISO 9660 image, i.e. the
 * {@link CommonVolumeDescriptor} of the variant a {@link CDReader} settled on,
 * plus the space accounting derived from it.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @see CommonVolumeDescriptor
 */
class VolumeDescriptorTest {

    private static final String FS = File.separator;

    /** an image with one file in a sub directory and a volume identifier */
    private static SparseStream build(boolean joliet) {
        CDBuilder builder = new CDBuilder();
        builder.setUseJoliet(joliet);
        builder.setVolumeIdentifier("A_SAMPLE_DISK");
        builder.addFile("FOLDER" + FS + "HELLO.TXT", "Hello World!".getBytes(StandardCharsets.US_ASCII));
        return builder.build();
    }

    @Test
    @DisplayName("the joliet variant is reported as joliet, not as plain iso9660")
    void activeVariantIsJolietWhenSupplementaryDescriptorIsUsed() throws Exception {
        try (CDReader fs = new CDReader(build(true), true)) {
            assertEquals(Iso9660Variant.Joliet, fs.getActiveVariant());
        }
    }

    @Test
    @DisplayName("without joliet the primary descriptor is used")
    void activeVariantIsIso9660WhenPrimaryDescriptorIsUsed() throws Exception {
        try (CDReader fs = new CDReader(build(true), false)) {
            assertEquals(Iso9660Variant.Iso9660, fs.getActiveVariant());
        }
    }

    @Test
    @DisplayName("volume descriptor of the primary descriptor")
    void primaryVolumeDescriptor() throws Exception {
        try (CDReader fs = new CDReader(build(false), false)) {
            CommonVolumeDescriptor vd = fs.getVolumeDescriptor();
            assertEquals("A_SAMPLE_DISK", vd.volumeIdentifier);
            assertEquals("A_SAMPLE_DISK", fs.getVolumeLabel());
            assertEquals(2048, vd.getLogicalBlockSize());
            assertEquals(1, vd.volumeSetSize);
            assertEquals(1, vd.volumeSequenceNumber);
            assertEquals(1, vd.fileStructureVersion);
            assertEquals(StandardCharsets.US_ASCII, vd.characterEncoding);
            assertTrue(vd.volumeSpaceSize > 0, "volumeSpaceSize: " + vd.volumeSpaceSize);
            assertTrue(vd.pathTableSize > 0, "pathTableSize: " + vd.pathTableSize);
        }
    }

    @Test
    @DisplayName("the joliet descriptor carries the same identifier, in UTF-16")
    void supplementaryVolumeDescriptor() throws Exception {
        try (CDReader fs = new CDReader(build(true), true)) {
            CommonVolumeDescriptor vd = fs.getVolumeDescriptor();
            assertEquals("A_SAMPLE_DISK", vd.volumeIdentifier);
            assertEquals(StandardCharsets.UTF_16BE, vd.characterEncoding);
            assertEquals(2048, vd.getLogicalBlockSize());
        }
    }

    @Test
    @DisplayName("space accounting of a read only, fully allocated medium")
    void space() throws Exception {
        try (CDReader fs = new CDReader(build(true), true)) {
            CommonVolumeDescriptor vd = fs.getVolumeDescriptor();
            long expected = (long) vd.volumeSpaceSize * vd.getLogicalBlockSize();
            assertEquals(expected, fs.getSize());
            assertEquals(expected, fs.getUsedSpace());
            assertEquals(0, fs.getAvailableSpace());
            assertEquals(vd.volumeSpaceSize, fs.getTotalClusters());
            assertEquals(vd.getLogicalBlockSize(), fs.getClusterSize());
        }
    }
}
