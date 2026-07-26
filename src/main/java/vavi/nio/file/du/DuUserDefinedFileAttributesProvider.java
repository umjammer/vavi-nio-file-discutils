/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.du;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.github.fge.filesystem.attributes.provider.UserDefinedFileAttributesProvider;
import discUtils.core.DiscFileSystemInfo;
import discUtils.iso9660.CDReader;
import discUtils.iso9660.CommonVolumeDescriptor;


/**
 * {@link UserDefinedFileAttributeView} implementation for DiscUtils.
 * <p>
 * Volume level meta data which has no place in {@code basic} attributes is
 * exposed here. Currently that is the ISO 9660
 * {@link CommonVolumeDescriptor} of the active variant, which is the same for
 * every path of an ISO image. All values are UTF-8 encoded text, dates are
 * ISO-8601 instants and unset dates are omitted.
 * <p>
 * For file systems other than ISO 9660 no attribute is defined, i.e.
 * {@link #list()} is empty.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @see CommonVolumeDescriptor
 */
public final class DuUserDefinedFileAttributesProvider extends UserDefinedFileAttributesProvider {

    /** volume level meta data, name to UTF-8 encoded text */
    private final Map<String, byte[]> attributes;

    public DuUserDefinedFileAttributesProvider(DiscFileSystemInfo entry) throws IOException {
        Objects.requireNonNull(entry);
        this.attributes = entry.getFileSystem() instanceof CDReader cd ? toAttributes(cd) : Collections.emptyMap();
    }

    /** Flattens the volume descriptor of the active iso9660 variant. */
    private static Map<String, byte[]> toAttributes(CDReader cd) {
        CommonVolumeDescriptor vd = cd.getVolumeDescriptor();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("activeVariant", cd.getActiveVariant().name());
        values.put("systemIdentifier", vd.systemIdentifier);
        values.put("volumeIdentifier", vd.volumeIdentifier);
        values.put("volumeSetIdentifier", vd.volumeSetIdentifier);
        values.put("publisherIdentifier", vd.publisherIdentifier);
        values.put("dataPreparerIdentifier", vd.dataPreparerIdentifier);
        values.put("applicationIdentifier", vd.applicationIdentifier);
        values.put("copyrightFileIdentifier", vd.copyrightFileIdentifier);
        values.put("abstractFileIdentifier", vd.abstractFileIdentifier);
        values.put("bibliographicFileIdentifier", vd.bibliographicFileIdentifier);
        values.put("volumeSpaceSize", String.valueOf(vd.volumeSpaceSize & 0xffff_ffffL));
        values.put("volumeSetSize", String.valueOf(vd.volumeSetSize & 0xffff));
        values.put("volumeSequenceNumber", String.valueOf(vd.volumeSequenceNumber & 0xffff));
        values.put("logicalBlockSize", String.valueOf(vd.getLogicalBlockSize()));
        values.put("pathTableSize", String.valueOf(vd.pathTableSize & 0xffff_ffffL));
        values.put("fileStructureVersion", String.valueOf(vd.fileStructureVersion & 0xff));
        putTime(values, "creationDateAndTime", vd.creationDateAndTime);
        putTime(values, "modificationDateAndTime", vd.modificationDateAndTime);
        putTime(values, "expirationDateAndTime", vd.expirationDateAndTime);
        putTime(values, "effectiveDateAndTime", vd.effectiveDateAndTime);

        Map<String, byte[]> attributes = new LinkedHashMap<>();
        values.forEach((name, value) -> attributes.put(name, value.getBytes(StandardCharsets.UTF_8)));
        return Collections.unmodifiableMap(attributes);
    }

    /** {@link Long#MIN_VALUE} means the volume descriptor leaves the date unset. */
    private static void putTime(Map<String, String> values, String name, long millis) {
        if (millis != Long.MIN_VALUE) {
            values.put(name, Instant.ofEpochMilli(millis).toString());
        }
    }

    @Override
    public List<String> list() throws IOException {
        return List.copyOf(attributes.keySet());
    }

    @Override
    public int size(String name) throws IOException {
        return value(name).length;
    }

    @Override
    public int read(String name, ByteBuffer dst) throws IOException {
        byte[] value = value(name);
        dst.put(value);
        return value.length;
    }

    @Override
    public Map<String, Object> getAllAttributes() throws IOException {
        return Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    private byte[] value(String name) {
        byte[] value = attributes.get(Objects.requireNonNull(name));
        if (value == null) {
            throw new IllegalArgumentException(name + " is undefined");
        }
        return value;
    }
}
