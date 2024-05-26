/*
 * license:BSD-3-Clause
 * copyright-holders:Aaron Giles
 *
 * ported from cdrom.h
 */

package libchdr;


/**
 * Generic MAME cd-rom implementation
 */
public class Cdrom {

    private Cdrom() {}

//#region CONSTANTS

    /** tracks are padded to a multiple of this many frames */
    public static final int CD_TRACK_PADDING = 4;
    /** AFAIK the theoretical limit */
    public static final int CD_MAX_TRACKS = 99;
    public static final int CD_MAX_SECTOR_DATA = 2352;
    public static final int CD_MAX_SUBCODE_DATA = 96;

    public static final int CD_FRAME_SIZE = CD_MAX_SECTOR_DATA + CD_MAX_SUBCODE_DATA;
    public static final int CD_FRAMES_PER_HUNK = 8;

    public static final int CD_METADATA_WORDS = 1 + CD_MAX_TRACKS * 6;

//#endregion
}
