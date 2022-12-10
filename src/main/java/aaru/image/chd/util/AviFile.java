// license:BSD-3-Clause
// copyright-holders:Aaron Giles, Vas Crabb

package aaru.image.chd.util;

/**
 * AVI movie format parsing helpers.
 */
class AviFile {

    static int AVI_FOURCC(int a, int b, int c, int d) {
        return a | (b << 8) | (c << 16) | (d << 24);
    }

    static final int FORMAT_UYVY = AVI_FOURCC('U', 'Y', 'V', 'Y');
    static final int FORMAT_VYUY = AVI_FOURCC('V', 'Y', 'U', 'Y');
    static final int FORMAT_YUY2 = AVI_FOURCC('Y', 'U', 'Y', '2');
    static final int FORMAT_HFYU = AVI_FOURCC('H', 'F', 'Y', 'U');
    static final int FORMAT_I420 = AVI_FOURCC('I', '4', '2', '0');
    ;
    static final int FORMAT_DIB = AVI_FOURCC('D', 'I', 'B', ' ');
    static final int FORMAT_RGB = AVI_FOURCC('R', 'G', 'B', ' ');
    static final int FORMAT_RAW = AVI_FOURCC('R', 'A', 'W', ' ');
    static final int FORMAT_UNCOMPRESSED = 0x00000000;

    public enum error {
        NONE,
        END,
        INVALID_DATA,
        NO_MEMORY,
        READ_ERROR,
        WRITE_ERROR,
        STACK_TOO_DEEP,
        UNSUPPORTED_FEATURE,
        CANT_OPEN_FILE,
        INCOMPATIBLE_AUDIO_STREAMS,
        INVALID_SAMPLERATE,
        INVALID_STREAM,
        INVALID_FRAME,
        INVALID_BITMAP,
        UNSUPPORTED_VIDEO_FORMAT,
        UNSUPPORTED_AUDIO_FORMAT,
        EXCEEDED_SOUND_BUFFER
    }

    public enum datatype {
        VIDEO,
        AUDIO_CHAN0,
        AUDIO_CHAN1,
        AUDIO_CHAN2,
        AUDIO_CHAN3,
        AUDIO_CHAN4,
        AUDIO_CHAN5,
        AUDIO_CHAN6,
        AUDIO_CHAN7
    }

    public static class movie_info {
        // format of video data */
        int video_format;
        // timescale for video data */
        int video_timescale;
        // duration of a single video sample (frame) */
        int video_sampletime;
        // total number of video samples */
        int video_numsamples;
        // width of the video */
        int video_width;
        // height of the video */
        int video_height;
        // depth of the video */
        int video_depth;

        // format of audio data */
        int audio_format;
        // timescale for audio data */
        int audio_timescale;
        // duration of a single audio sample */
        int audio_sampletime;
        // total number of audio samples */
        int audio_numsamples;
        // number of audio channels */
        int audio_channels;
        // number of bits per channel */
        int audio_samplebits;
        // sample rate of audio */
        int audio_samplerate;
    }
}