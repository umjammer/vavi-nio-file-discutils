// license:BSD-3-Clause
// copyright-holders:Aaron Giles

package aaru.image.chd.util;


/**
 * Parse Philips codes and other data from VBI lines.
 */
class VbiParse {

	/** size of packed VBI data */
	public static final int VBI_PACKED_BYTES = 16;

	// these codes are full 24-bit codes with no parameter data

	public static final int VBI_CODE_LEADIN = 0x88ffff;
	public static final int VBI_CODE_LEADOUT = 0x80eeee;
	public static final int VBI_CODE_STOP = 0x82cfff;
	public static final int VBI_CODE_CLV = 0x87ffff;

	// these codes require a mask because some bits are parameters

	public static final int VBI_MASK_CAV_PICTURE = 0xf00000;
	public static final int VBI_CODE_CAV_PICTURE = 0xf00000;
	public static final int VBI_MASK_CHAPTER = 0xf00fff;
	public static final int VBI_CODE_CHAPTER = 0x800ddd;
	public static final int VBI_MASK_CLV_TIME = 0xf0ff00;
	public static final int VBI_CODE_CLV_TIME = 0xf0dd00;
	public static final int VBI_MASK_STATUS_CX_ON = 0xfff000;
	public static final int VBI_CODE_STATUS_CX_ON = 0x8dc000;
	public static final int VBI_MASK_STATUS_CX_OFF = 0xfff000;
	public static final int VBI_CODE_STATUS_CX_OFF = 0x8bc000;
	public static final int VBI_MASK_USER = 0xf0f000;
	public static final int VBI_CODE_USER = 0x80d000;
	public static final int VBI_MASK_CLV_PICTURE = 0xf0f000;
	public static final int VBI_CODE_CLV_PICTURE = 0x80e000;

	static int VBI_CAV_PICTURE(int x) {
		return  (((x >> 16) & 0x07) * 10000) +
				(((x >> 12) & 0x0f) * 1000) +
				(((x >>  8) & 0x0f) * 100) +
				(((x >>  4) & 0x0f) * 10) +
				(((x >>  0) & 0x0f) * 1);
	}

	static int VBI_CHAPTER(int x) {
		return (((x >> 16) & 0x07) * 10) + (((x >> 12) & 0x0f) * 1);
	}

	public static class VbiMetadata {
		/** white flag: on or off */
		byte white;
		/** line 16 code */
		int line16;
		/** line 17 code */
		int line17;
		/** line 18 code */
		int line18;
		/** most plausible value from lines 17/18 */
		int line1718;
	}
}