
package discUtils.iscsi;

enum Digest {
    @ProtocolKeyValueAttribute(name = "None")
    None,
    @ProtocolKeyValueAttribute(name = "CRC32C")
    Crc32c
}
