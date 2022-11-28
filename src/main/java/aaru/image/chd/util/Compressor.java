/*
 * license:BSD-3-Clause
 * copyright-holders:Aaron Giles
 */

package aaru.image.chd.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import aaru.commonType.TrackType;
import aaru.image.chd.Chd;


/**
 * Compressor.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-24 nsano initial version <br>
 */
public abstract class Compressor {

    /* AFAIK the theoretical limit */
    static final int CD_MAX_TRACKS = 99;
    static final int CD_MAX_SECTOR_DATA = 2352;
    static final int CD_MAX_SUBCODE_DATA = 96;

    static final int CD_FRAME_SIZE = CD_MAX_SECTOR_DATA + CD_MAX_SUBCODE_DATA;
    static final int CD_FRAMES_PER_HUNK = 8;

    static final int CD_METADATA_WORDS = 1 + (CD_MAX_TRACKS * 6);

    public abstract int read(byte[] dest, long offset, int length) throws IOException;

    static class AviInfo {
        int fps_times_1million;
        int width;
        int height;
        boolean interlaced;
        int channels;
        int rate;
        int max_samples_per_frame;
        int bytes_per_frame;
    }

    static class CdromTrackInfo {
        // fields used by CHDMAN and in MAME

        /** track type */
        TrackType trktype;
        /** subcode data type */
        int subtype;
        /** size of data in each sector of this track */
        int datasize;
        /** size of subchannel data in each sector of this track */
        int subsize;
        /** number of frames in this track */
        int frames;
        /** number of "spillage" frames in this track */
        int extraframes;
        /** number of pregap frames */
        int pregap;
        /** number of postgap frames */
        int postgap;
        /** type of sectors in pregap */
        int pgtype;
        /** type of subchannel data in pregap */
        int pgsub;
        /** size of data in each sector of the pregap */
        int pgdatasize;
        /** size of subchannel data in each sector of the pregap */
        int pgsubsize;

        // fields used in CHDMAN only

        /** number of frames of padding to add to the end of the track; needed for GDI */
        int padframes;
        /** number of frames to read from the next file; needed for Redump split-bin GDI */
        int splitframes;

        // fields used in MAME/MESS only

        /** logical frame of actual track data - offset by pregap size if pregap not physically present */
        int logframeofs;
        /** physical frame of actual track data in CHD data */
        int physframeofs;
        /** frame number this track starts at on the CHD */
        int chdframeofs;
        /** number of frames from logframeofs until end of track data */
        int logframes;

        /** fields used in multi-cue GDI */
        int multicuearea;
    }

    static class CdromToc {
        /** number of tracks */
        int numtrks;
        /** see FLAG_ above */
        int flags;
        CdromTrackInfo[] tracks = new CdromTrackInfo[CD_MAX_TRACKS];
    }

    static class ChdcdTrackInputEntry {
        ChdcdTrackInputEntry() { reset(); }
        void reset() { fname = null; offset = idx0offs = idx1offs = 0; swap = false; }

        /** filename for each track */
        String fname;
        /** offset in the data file for each track */
        int offset;
        /** data needs to be byte swapped */
        boolean swap;
        int idx0offs;
        int idx1offs;
    }

    static class ChdcdTrackInputInfo {
        void reset() { for (ChdcdTrackInputEntry elem : track) elem.reset(); }

        ChdcdTrackInputEntry[] track = new ChdcdTrackInputEntry[CD_MAX_TRACKS];
    }

    public static class ZeroCompressor extends Compressor {

        public ZeroCompressor(long offset/*=0*/, long maxoffset/*=0*/) {
            m_offset = offset;
            m_maxoffset = maxoffset;
        }

        // read interface
        @Override
        public int read(byte[] dest, long offset, int length) {
            offset += m_offset;
            if (offset >= m_maxoffset)
                return 0;
            if (offset + length > m_maxoffset)
                length = (int) (m_maxoffset - offset);
            Arrays.fill(dest, 0, length, (byte) 0);
            return length;
        }

        private long m_offset;
        private long m_maxoffset;
    }

    /**  */
    static class RawFileCompressor extends Compressor {

        public RawFileCompressor(Path file, long offset/* = 0*/, long maxoffset/* = Long.MAX_VALUE*/) throws IOException {
            m_file = Files.newByteChannel(file);
            m_offset = offset;

            // TODO: what to do about error getting file size?
            long filelen = Files.size(file);
            if (filelen != 0)
                m_maxoffset = Math.min(maxoffset, filelen);
            else
                m_maxoffset = maxoffset;
        }

        @Override
        public int read(byte[] dest, long offset, int length) throws IOException {
            offset += m_offset;
            if (offset >= m_maxoffset)
                return 0;
            if (offset + length > m_maxoffset)
                length = (int) (m_maxoffset - offset);
            m_file.position(offset);
            int actual = m_file.read(ByteBuffer.wrap(dest));
            return actual;
        }

        private SeekableByteChannel m_file;
        private long m_offset;
        private long m_maxoffset;
    }

    /** chd_chdfile_compressor */
    static class ChdFileCompressor extends Compressor {

        // construction/destruction
        public ChdFileCompressor(Chd file, long offset/* = 0*/, long maxoffset/* = ~0*/) {
            m_toc = null;
            m_file = file;
            m_offset = offset;
            m_maxoffset = Math.min(maxoffset, file.logical_bytes());
        }

        // read interface
        public int read(byte[] dest, long offset, int length) {
            offset += m_offset;
            if (offset >= m_maxoffset)
                return 0;
            if (offset + length > m_maxoffset)
                length = (int) (m_maxoffset - offset);
            m_file.read_bytes(offset, dest, length);

            // if we have TOC - detect audio sectors and swap data
            if (m_toc != null) {
                assert (offset % CD_FRAME_SIZE == 0);
                assert (length % CD_FRAME_SIZE == 0);

                int startlba = (int) (offset / CD_FRAME_SIZE);
                int lenlba = length / CD_FRAME_SIZE;
                byte[] _dest = dest;

                for (int chdlba = 0; chdlba < lenlba; chdlba++) {
                    // find current frame's track number
                    int tracknum = m_toc.numtrks;
                    for (int track = 0; track < m_toc.numtrks; track++)
                        if ((chdlba + startlba) < m_toc.tracks[track + 1].chdframeofs) {
                            tracknum = track;
                            break;
                        }
                    // is it audio ?
                    if (m_toc.tracks[tracknum].trktype != TrackType.Audio)
                        continue;
                    // byteswap if yes
                    int dataoffset = chdlba * CD_FRAME_SIZE;
                    for (int swapindex = dataoffset; swapindex < (dataoffset + CD_MAX_SECTOR_DATA); swapindex += 2) {
                        byte temp = _dest[swapindex];
                        _dest[swapindex] = _dest[swapindex + 1];
                        _dest[swapindex + 1] = temp;
                    }
                }
            }

            return length;
        }

        final CdromToc m_toc;

        // internal state
        private Chd m_file;
        private long m_offset;
        private long m_maxoffset;
    }

    /** */
    static class CdCompressor extends Compressor {

        // construction/destruction
        public CdCompressor(CdromToc toc, ChdcdTrackInputInfo info) {
            m_file = null;
            m_toc = toc;
            m_info = info;
        }

        // read interface
        public int read(byte[] _dest, long offset, int length) throws IOException {
            // verify assumptions made below
            assert (offset % CD_FRAME_SIZE == 0);
            assert (length % CD_FRAME_SIZE == 0);

            // initialize destination to 0 so that unused areas are filled
            int dest = 0; // _dest;
            Arrays.fill(_dest, 0, length, (byte) 0);

            // find out which track we're starting in
            long startoffs = 0;
            int length_remaining = length;
            for (int tracknum = 0; tracknum < m_toc.numtrks; tracknum++) {
                final CdromTrackInfo trackinfo = m_toc.tracks[tracknum];
                long endoffs = startoffs + (long) (trackinfo.frames + trackinfo.extraframes) * CD_FRAME_SIZE;
                if (offset >= startoffs && offset < endoffs) {
                    // if we don't already have this file open, open it now
                    if (m_file == null || !m_lastfile.equals(m_info.track[tracknum].fname)) {
                        m_file.close();
                        m_lastfile = m_info.track[tracknum].fname;
                        m_file = Files.newByteChannel(Paths.get(m_lastfile), StandardOpenOption.READ);
                    }

                    // iterate over frames
                    long bytesperframe = trackinfo.datasize + trackinfo.subsize;
                    long src_track_start = m_info.track[tracknum].offset;
                    long src_track_end = src_track_start + bytesperframe * (long) trackinfo.frames;
                    long pad_track_start = src_track_end - ((long) m_toc.tracks[tracknum].padframes * bytesperframe);
                    long split_track_start = pad_track_start - ((long) m_toc.tracks[tracknum].splitframes * bytesperframe);

                    // dont split when split-bin read not required
                    if ((long) m_toc.tracks[tracknum].splitframes == 0L)
                        split_track_start = Long.MAX_VALUE;

                    while (length_remaining != 0 && offset < endoffs) {
                        // determine start of current frame
                        long src_frame_start = src_track_start + ((offset - startoffs) / CD_FRAME_SIZE) * bytesperframe;

                        // auto-advance next track for split-bin read
                        if (src_frame_start == split_track_start && m_lastfile.compare(m_info.track[tracknum + 1].fname) != 0) {
                            m_file.close();
                            m_lastfile = m_info.track[tracknum + 1].fname;
                            m_file = Files.newByteChannel(Paths.get(m_lastfile), StandardOpenOption.READ);
                        }

                        if (src_frame_start < src_track_end) {
                            // read it in, or pad if we're into the padframes
                            if (src_frame_start >= pad_track_start) {
                                Arrays.fill(_dest, dest, (int) bytesperframe, (byte) 0);
                            } else {
                                m_file.position(
                                        (src_frame_start >= split_track_start)
                                                ? src_frame_start - split_track_start
                                                : src_frame_start
                                        );
                                int count = 0;
                                count = m_file.read(ByteBuffer.wrap(_dest, dest, (int) bytesperframe));
                                if (count == -1 || (count != bytesperframe))
                                    throw new IOException("Error reading input file: " + m_lastfile);
                            }

                            // swap if appropriate
                            if (m_info.track[tracknum].swap)
                                for (int swapindex = 0; swapindex < 2352; swapindex += 2) {
                                    byte temp = _dest[dest + swapindex];
                                    _dest[dest + swapindex] = _dest[dest + swapindex + 1];
                                    _dest[dest + swapindex + 1] = temp;
                                }
                        }

                        // advance
                        offset += CD_FRAME_SIZE;
                        dest += CD_FRAME_SIZE;
                        length_remaining -= CD_FRAME_SIZE;
                        if (length_remaining == 0)
                            break;
                    }
                }

                // next track starts after the previous one
                startoffs = endoffs;
            }
            return length - length_remaining;
        }

        // internal state
        private String m_lastfile;
        private SeekableByteChannel m_file;
        private CdromToc m_toc;
        private ChdcdTrackInputInfo m_info;
    }

    /** chd_avi_compressor */
    static class AviCompressor extends Compressor {

        // construction/destruction
        public AviCompressor(AviFile file, AviInfo info, int first_frame, int num_frames) {
            m_file = file;
            m_info = info;
            m_bitmap = new Bitmap.bitmap_yuy16(info.width, info.height * (info.interlaced ? 2 : 1));
            m_start_frame = first_frame;
            m_frame_count = num_frames;
            m_ldframedata = new byte[num_frames * VbiParse.VBI_PACKED_BYTES];
            m_rawdata = new byte[info.bytes_per_frame];
        }

        // getters
        public byte[] ldframedata() {
            return m_ldframedata;
        }

        // read interface
        public int read(byte[] _dest, long offset, int length) {
            int dest = 0; // _dest;
            byte interlace_factor = (byte) (m_info.interlaced ? 2 : 1);
            int length_remaining = length;

            // iterate over frames
            int start_frame = (int) (offset / m_info.bytes_per_frame);
            int end_frame = (int) ((offset + length - 1) / m_info.bytes_per_frame);
            for (int framenum = start_frame; framenum <= end_frame; framenum++)
                if (framenum < m_frame_count) {
                    // determine effective frame number and first/last samples
                    int effframe = m_start_frame + framenum;
                    int first_sample = (m_info.rate * effframe * 1000000 + m_info.fps_times_1million - 1) / m_info.fps_times_1million;
                    int samples = (m_info.rate * (effframe + 1) * 1000000 + m_info.fps_times_1million - 1) / m_info.fps_times_1million - first_sample;

                    // loop over channels and read the samples
                    int channels = Math.min(m_info.channels, m_audio.length);
                    short[] samplesptr = new short[m_audio.length];
                    for (int chnum = 0; chnum < channels; chnum++) {
                        // read the sound samples
                        m_audio[chnum] = new short[samples];
                        samplesptr[chnum] = m_audio[chnum][0];
                        m_file.read_sound_samples(chnum, first_sample, samples, m_audio[chnum][0]);
                    }

                    // read the video data
                    m_file.read_video_frame(effframe / interlace_factor, m_bitmap);
                    Bitmap.bitmap_yuy16 subbitmap = new Bitmap.bitmap_yuy16(
                            m_bitmap.pix(effframe % interlace_factor, 0),
                            m_bitmap.width(),
                            m_bitmap.height() / interlace_factor, m_bitmap.rowpixels() * interlace_factor);

                    // update metadata for this frame
                    if (m_info.height == 524 / 2 || m_info.height == 624 / 2) {
                        VbiParse.VbiMetadata vbi;
                        vbi_parse_all(subbitmap.pix(0, 0), subbitmap.rowpixels(), subbitmap.width(), 8, vbi);
                        vbi_metadata_pack(m_ldframedata[framenum * VbiParse.VBI_PACKED_BYTES], framenum, vbi);
                    }

                    // assemble the data into final form
                    avhuff_encoder.assemble_data(m_rawdata, subbitmap, channels, samples, samplesptr);
                    if (m_rawdata.length < m_info.bytes_per_frame) {
                        int old_size = m_rawdata.length;
                        m_rawdata = new byte[m_info.bytes_per_frame];
                        Arrays.fill(m_rawdata, old_size, m_info.bytes_per_frame - old_size, (byte) 0);
                    }

                    // copy to the destination
                    long start_offset = (long) framenum * m_info.bytes_per_frame;
                    long end_offset = start_offset + m_info.bytes_per_frame;
                    int bytes_to_copy = (int) Math.min(length_remaining, end_offset - offset);
                    System.arraycopy(m_rawdata, (int) (offset - start_offset), _dest, dest, bytes_to_copy);

                    // advance
                    offset += bytes_to_copy;
                    dest += bytes_to_copy;
                    length_remaining -= bytes_to_copy;
                }

            return length;
        }

        // internal state
        private AviFile m_file;
        private AviInfo m_info;
        private Bitmap.bitmap_yuy16 m_bitmap;
        private int m_start_frame;
        private int m_frame_count;
        private short[][] m_audio = new short[8][];
        private byte[] m_ldframedata;
        private byte[] m_rawdata;
    }
}
