
package DiscUtils.HfsPlus;

enum FileTypeFlags {
    None(0x0),
    /** 'slnk' */
    SymLinkFileType(0x736C6E6B),
    /** 'rhap' */
    SymLinkCreator(0x72686170),
    /** 'hlnk' */
    HardLinkFileType(0x686C6E6B),
    /** 'hfs+' */
    HFSPlusCreator(0x6866732B);

    private int value;

    public int getValue() {
        return value;
    }

    private FileTypeFlags(int value) {
        this.value = value;
    }
}
