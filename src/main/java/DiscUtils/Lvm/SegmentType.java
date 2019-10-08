
package DiscUtils.Lvm;

public enum SegmentType {
    //$ lvm segtypes, man(8) lvm
    None,
    Striped,
    Zero,
    Error,
    Free,
    Snapshot,
    Mirror,
    Raid1,
    Raid10,
    Raid4,
    Raid5,
    Raid5La,
    Raid5Ra,
    Raid5Ls,
    Raid5Rs,
    Raid6,
    Raid6Zr,
    Raid6Nr,
    Raid6Nc,
    ThinPool,
    Thin
}
