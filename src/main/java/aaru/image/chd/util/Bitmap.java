// license:BSD-3-Clause
// copyright-holders:Aaron Giles


package aaru.image.chd.util;

import java.util.Arrays;


/**
 * Core bitmap routines.
 */
class Bitmap {

    /** Format describes the various bitmap formats we use */
    enum Format {
        // invalid forma */
        BITMAP_FORMAT_INVALID,
        // 8bpp indexed */
        BITMAP_FORMAT_IND8,
        // 16bpp indexed */
        BITMAP_FORMAT_IND16,
        // 32bpp indexed */
        BITMAP_FORMAT_IND32,
        // 64bpp indexed */
        BITMAP_FORMAT_IND64,
        // 32bpp 8-8-8 RGB */
        BITMAP_FORMAT_RGB32,
        // 32bpp 8-8-8-8 ARGB */
        BITMAP_FORMAT_ARGB32,
        // 16bpp 8-8 Y/Cb, Y/Cr in sequence */
        BITMAP_FORMAT_YUY16
    }

    // rectangles describe a bitmap portion
    static class Rectangle {

        // construction/destruction
        public Rectangle() {
        }

        public Rectangle(int minx, int maxx, int miny, int maxy) {
            min_x = minx;
            max_x = maxx;
            min_y = miny;
            max_y = maxy;
        }

        // getters
        int left() {
            return min_x;
        }

        int right() {
            return max_x;
        }

        int top() {
            return min_y;
        }

        int bottom() {
            return max_y;
        }

        // compute intersection with another rect
        Rectangle operatorAndEquals(final Rectangle src) {
            if (src.min_x > min_x) min_x = src.min_x;
            if (src.max_x < max_x) max_x = src.max_x;
            if (src.min_y > min_y) min_y = src.min_y;
            if (src.max_y < max_y) max_y = src.max_y;
            return this;
        }

        // compute union with another rect
        Rectangle operatorOrEquals(final Rectangle src) {
            if (src.min_x < min_x) min_x = src.min_x;
            if (src.max_x > max_x) max_x = src.max_x;
            if (src.min_y < min_y) min_y = src.min_y;
            if (src.max_y > max_y) max_y = src.max_y;
            return this;
        }

        Rectangle operatorAnd(final Rectangle b) {
            Rectangle a = this;
            a = a.operatorAndEquals(b);
            return a;
        }

        Rectangle operatorOr(final Rectangle b) {
            Rectangle a = this;
            a = a.operatorOrEquals(b);
            return a;
        }

        // comparisons
        boolean operatorEquals(final Rectangle rhs) {
            return min_x == rhs.min_x && max_x == rhs.max_x && min_y == rhs.min_y && max_y == rhs.max_y;
        }

        boolean operatorNotEquals(final Rectangle rhs) {
            return min_x != rhs.min_x || max_x != rhs.max_x || min_y != rhs.min_y || max_y != rhs.max_y;
        }

        boolean operatorGT(final Rectangle rhs) {
            return min_x < rhs.min_x && min_y < rhs.min_y && max_x > rhs.max_x && max_y > rhs.max_y;
        }

        boolean operatorGE(final Rectangle rhs) {
            return min_x <= rhs.min_x && min_y <= rhs.min_y && max_x >= rhs.max_x && max_y >= rhs.max_y;
        }

        boolean operatorLT(final Rectangle rhs) {
            return min_x >= rhs.min_x || min_y >= rhs.min_y || max_x <= rhs.max_x || max_y <= rhs.max_y;
        }

        boolean operatorLE(final Rectangle rhs) {
            return min_x > rhs.min_x || min_y > rhs.min_y || max_x < rhs.max_x || max_y < rhs.max_y;
        }

        // other helpers
        boolean empty() {
            return (min_x > max_x) || (min_y > max_y);
        }

        boolean contains(int x, int y) {
            return (x >= min_x) && (x <= max_x) && (y >= min_y) && (y <= max_y);
        }

        boolean contains(final Rectangle rect) {
            return (min_x <= rect.min_x) && (max_x >= rect.max_x) && (min_y <= rect.min_y) && (max_y >= rect.max_y);
        }

        int width() {
            return max_x + 1 - min_x;
        }

        int height() {
            return max_y + 1 - min_y;
        }

        int xcenter() {
            return (min_x + max_x + 1) / 2;
        }

        int ycenter() {
            return (min_y + max_y + 1) / 2;
        }

        // setters
        void set(int minx, int maxx, int miny, int maxy) {
            min_x = minx;
            max_x = maxx;
            min_y = miny;
            max_y = maxy;
        }

        void setx(int minx, int maxx) {
            min_x = minx;
            max_x = maxx;
        }

        void sety(int miny, int maxy) {
            min_y = miny;
            max_y = maxy;
        }

        void set_width(int width) {
            max_x = min_x + width - 1;
        }

        void set_height(int height) {
            max_y = min_y + height - 1;
        }

        void set_origin(int x, int y) {
            max_x += x - min_x;
            max_y += y - min_y;
            min_x = x;
            min_y = y;
        }

        void set_size(int width, int height) {
            set_width(width);
            set_height(height);
        }

        // offset helpers
        void offset(int xdelta, int ydelta) {
            min_x += xdelta;
            max_x += xdelta;
            min_y += ydelta;
            max_y += ydelta;
        }

        void offsetx(int delta) {
            min_x += delta;
            max_x += delta;
        }

        void offsety(int delta) {
            min_y += delta;
            max_y += delta;
        }

        // internal state

        /** minimum X, or left coordinate */
        int min_x = 0;
        /** maximum X, or right coordinate (inclusive) */
        int max_x = 0;
        /** minimum Y, or top coordinate */
        int min_y = 0;
        /** maximum Y, or bottom coordinate (inclusive) */
        int max_y = 0;
    }

    // construction/destruction -- subclasses only to ensure type correctness
//        Bitmap(final Bitmap &) = delete;
//        protected Bitmap(Bitmap &&that);
    protected Bitmap(Format format, byte bpp, int width/* = 0*/, int height/* = 0*/, int xslop/* = 0*/, int yslop/* = 0*/) {
        m_alloc = null;
        m_allocbytes = 0;
        m_format = format;
        m_bpp = bpp;
        m_palette = null;
        allocate(width, height, xslop, yslop);
    }

    protected Bitmap(Format format, byte bpp, byte[] base, int width, int height, int rowpixels) {
        m_alloc = null;
        m_allocbytes = 0;
        m_base = base;
        m_rowpixels = rowpixels;
        m_width = width;
        m_height = height;
        m_format = format;
        m_bpp = bpp;
        m_palette = null;
        m_cliprect = new Rectangle(0, width - 1, 0, height - 1);
    }

    protected Bitmap(Format format, byte bpp, Bitmap source, final Rectangle subrect) {
        m_alloc = null;
        m_allocbytes = 0;
        m_base = source.raw_pixptr(subrect.top(), subrect.left());
        m_rowpixels = source.m_rowpixels;
        m_width = subrect.width();
        m_height = subrect.height();
        m_format = format;
        m_bpp = bpp;
        m_palette = null;
        m_cliprect = new Rectangle(0, subrect.width() - 1, 0, subrect.height() - 1);
    }

    // prevent implicit copying
//        Bitmap operatorLet(final Bitmap &) = delete;
//        Bitmap operatorLet(Bitmap &&that);

    // allocation/deallocation
    public void reset() {
        // delete any existing stuff
        set_palette(null);
        m_alloc.reset();
        m_base = null;

        // reset all fields
        m_rowpixels = 0;
        m_width = 0;
        m_height = 0;
        m_cliprect.set(0, -1, 0, -1);
    }

    // getters
    int width() {
        return m_width;
    }

    int height() {
        return m_height;
    }

    int rowpixels() {
        return m_rowpixels;
    }

    int rowbytes() {
        return m_rowpixels * m_bpp / 8;
    }

    byte bpp() {
        return m_bpp;
    }

    Format format() {
        return m_format;
    }

    boolean valid() {
        return (m_base != null);
    }

    Palette palette() {
        return m_palette;
    }

    final Rectangle cliprect() {
        return m_cliprect;
    }

    // allocation/sizing
    void allocate(int width, int height, int xslop /*= 0*/, int yslop /*= 0*/) {
        assert (m_format != Format.BITMAP_FORMAT_INVALID);
        assert (m_bpp == 8 || m_bpp == 16 || m_bpp == 32 || m_bpp == 64);

        // delete any existing stuff
        reset();

        // handle empty requests cleanly
        if (width <= 0 || height <= 0)
            return;

        // initialize fields
        m_rowpixels = compute_rowpixels(width, xslop);
        m_width = width;
        m_height = height;
        m_cliprect.set(0, width - 1, 0, height - 1);

        // allocate memory for the bitmap itself
        m_allocbytes = m_rowpixels * (m_height + 2 * yslop) * m_bpp / 8;
        m_alloc = new byte[m_allocbytes];

        // clear to 0 by default
        memset(m_alloc.get(), 0, m_allocbytes);

        // compute the base
        compute_base(xslop, yslop);
    }

    void resize(int width, int height, int xslop /*= 0*/, int yslop /*= 0*/) {
        assert (m_format != Format.BITMAP_FORMAT_INVALID);
        assert (m_bpp == 8 || m_bpp == 16 || m_bpp == 32 || m_bpp == 64);

        // handle empty requests cleanly
        if (width <= 0 || height <= 0)
            width = height = 0;

        // determine how much memory we need for the new bitmap
        int new_rowpixels = compute_rowpixels(width, xslop);
        int new_allocbytes = new_rowpixels * (height + 2 * yslop) * m_bpp / 8;

        if (new_allocbytes > m_allocbytes) {
            // if we need more memory, just realloc
            Palette palette = m_palette;
            allocate(width, height, xslop, yslop);
            set_palette(palette);
        } else {

            // otherwise, reconfigure
            m_rowpixels = new_rowpixels;
            m_width = width;
            m_height = height;
            m_cliprect.set(0, width - 1, 0, height - 1);

            // re-compute the base
            compute_base(xslop, yslop);
        }
    }

    // operations
    void set_palette(Palette palette) {
        // first dereference any existing palette
        if (m_palette != null) {
            m_palette.deref();
            m_palette = null;
        }

        // then reference any new palette
        if (palette != null) {
            palette.ref();
            m_palette = palette;
        }
    }

    void fill(long color) {
        fill(color, m_cliprect);
    }

    void fill(long color, final Rectangle bounds) {
        // if we have a cliprect, intersect with that
        Rectangle fill = bounds;
        fill = fill.operatorAndEquals(m_cliprect);
        if (!fill.empty()) {
            // based on the bpp go from there
            switch (m_bpp) {
            case 8:
                for (int y = fill.top(); y <= fill.bottom(); y++)
                    Arrays.fill(this.<Byte>pixt(y, fill.left()), fill.width(), (byte) color);
                break;

            case 16:
                for (int y = fill.top(); y <= fill.bottom(); ++y)
                    Arrays.fill(this.<Short>pixt(y, fill.left()), fill.width(), (short) color);
                break;

            case 32:
                for (int y = fill.top(); y <= fill.bottom(); ++y)
                    Arrays.fill(this.<Integer>pixt(y, fill.left()), fill.width(), (int) color);
                break;

            case 64:
                for (int y = fill.top(); y <= fill.bottom(); ++y)
                    Arrays.fill(this.<Long>pixt(y, fill.left()), fill.width(), (long) color);
                break;
            }
        }
    }

    void plot_box(int x, int y, int width, int height, long color) {
        fill(color, new Rectangle(x, x + width - 1, y, y + height - 1));
    }

    // pixel access
    byte[] raw_pixptr(int y, int x /*= 0*/) {
        return (byte[]) (m_base) + (y * m_rowpixels + x) * m_bpp / 8;
    }

    // for use by subclasses only to ensure type correctness
    protected <PixelType> PixelType pixt(int y, int x /*= 0*/) {
        return (PixelType) ((m_base) + y * m_rowpixels + x);
    }

    protected void wrap(byte[] base, int width, int height, int rowpixels) {
        // delete any existing stuff
        reset();

        // initialize relevant fields
        m_base = base;
        m_rowpixels = rowpixels;
        m_width = width;
        m_height = height;
        m_cliprect.set(0, m_width - 1, 0, m_height - 1);
    }

    protected void wrap(Bitmap source, final Rectangle subrect) {
        assert (m_format == source.m_format);
        assert (m_bpp == source.m_bpp);
        assert (source.cliprect().contains(subrect));

        // delete any existing stuff
        reset();

        // copy relevant fields
        m_base = source.raw_pixptr(subrect.top(), subrect.left());
        m_rowpixels = source.m_rowpixels;
        m_width = subrect.width();
        m_height = subrect.height();
        set_palette(source.m_palette);
        m_cliprect.set(0, m_width - 1, 0, m_height - 1);
    }

    /** compute a rowpixels value */
    private int compute_rowpixels(int width, int xslop) {
        return width + 2 * xslop;
    }

    /**
     * compute a bitmap base address with the given slop values
     */
    private void compute_base(int xslop, int yslop) {
        m_base = m_alloc.get() + (m_rowpixels * yslop + xslop) * (m_bpp / 8);
    }

    /**
     * return true if the bitmap format
     * is valid and agrees with the BPP
     */
    private boolean valid_format() {
        switch (m_format) {
        // invalid format
        case BITMAP_FORMAT_INVALID:
            return false;

        // 8bpp formats
        case BITMAP_FORMAT_IND8:
            return m_bpp == 8;

        // 16bpp formats
        case BITMAP_FORMAT_IND16:
        case BITMAP_FORMAT_YUY16:
            return m_bpp == 16;

        // 32bpp formats
        case BITMAP_FORMAT_IND32:
        case BITMAP_FORMAT_RGB32:
        case BITMAP_FORMAT_ARGB32:
            return m_bpp == 32;

        // 64bpp formats
        case BITMAP_FORMAT_IND64:
            return m_bpp == 64;
        }

        return false;
    }

    // internal state

    // pointer to allocated pixel memory
    byte[] m_alloc;
    // size of our allocation
    int m_allocbytes;
    // pointer to pixel (0,0) (adjusted for padding)
    byte[] m_base;
    // pixels per row (including padding)
    int m_rowpixels;
    // width of the bitmap
    int m_width;
    // height of the bitmap
    int m_height;
    // format of the bitmap
    Format m_format;
    // bits per pixel
    byte m_bpp;
    // optional palette
    Palette m_palette;
    // a clipping Rectangle covering the full bitmap
    Rectangle m_cliprect;

    /** bitmap_specific, bitmap8_t, bitmap16_t, bitmap32_t, bitmap64_t */
    static abstract class bitmap_specific<PixelType> extends Bitmap {

        // construction/destruction -- subclasses only
//        protected bitmap_specific(PixelType &&) = default;
        protected bitmap_specific(Format format, int width /*= 0*/, int height /*= 0*/, int xslop /*= 0*/, int yslop /*= 0*/) {
            super(format, PIXEL_BITS, width, height, xslop, yslop);
        }

        protected bitmap_specific(Format format, PixelType base, int width, int height, int rowpixels) {
            super(format, PIXEL_BITS, base, width, height, rowpixels);
        }

        protected bitmap_specific(Format format, PixelType source, final Rectangle subrect) {
            super(format, PIXEL_BITS, source, subrect);
        }

//        protected bitmap_specific<PixelType> operatorLet(bitmap_specific<PixelType> &&) = default;


        public PixelType pixel_t;

        // getters
        abstract byte bpp();

        // pixel accessors
        PixelType pix(int y, int x /*= 0*/) {
            return super.<PixelType>pixt(y, x);
        }

        // operations
        void fill(PixelType color) {
            fill(color, cliprect());
        }

        void fill(PixelType color, final Rectangle bounds) {
            // if we have a cliprect, intersect with that
            Rectangle fill = bounds;
            fill = fill.operatorAndEquals(cliprect());
            if (!fill.empty()) {
                for (int y = fill.top(); y <= fill.bottom(); y++)
                    Arrays.fill(pix(y, fill.left()), fill.width(), color);
            }
        }

        void plot_box(int x, int y, int width, int height, PixelType color) {
            fill(color, new Rectangle(x, x + width - 1, y, y + height - 1));
        }
    }

    // BITMAP_FORMAT_IND8 bitmaps
    static class bitmap_ind8 extends bitmap_specific<Byte> {

        static final Format k_bitmap_format = Format.BITMAP_FORMAT_IND8;

        // construction/destruction
//        public bitmap_ind8(bitmap_ind8 &&) = default;
        public bitmap_ind8(int width /*= 0*/, int height /*= 0*/, int xslop /*= 0*/, int yslop /*= 0*/) {
            super(k_bitmap_format, width, height, xslop, yslop);
        }

        public bitmap_ind8(byte[] base, int width, int height, int rowpixels) {
            super(k_bitmap_format, base, width, height, rowpixels);
        }

        public bitmap_ind8(bitmap_ind8 source, final Rectangle subrect) {
            super(k_bitmap_format, source, subrect);
        }

        public void wrap(byte[] base, int width, int height, int rowpixels) {
            super.wrap(base, width, height, rowpixels);
        }

        public void wrap(bitmap_ind8 source, final Rectangle subrect) {
            super.wrap((Bitmap) source, subrect);
        }

        // getters
        public Format format() {
            return k_bitmap_format;
        }

        @Override
        byte bpp() {
            return 8 * Byte.BYTES;
        }

//        public bitmap_ind8 operatorLet(bitmap_ind8 &&) = default;
    }

    // BITMAP_FORMAT_IND16 bitmaps
    static class bitmap_ind16 extends bitmap_specific<Short> {

        static final Format k_bitmap_format = Format.BITMAP_FORMAT_IND16;

        // construction/destruction
//        public bitmap_ind16(bitmap_ind16 &&) = default;
        public bitmap_ind16(int width /*= 0*/, int height /*= 0*/, int xslop /*= 0*/, int yslop /*= 0*/) {
            super(k_bitmap_format, width, height, xslop, yslop);
        }

        public bitmap_ind16(short[] base, int width, int height, int rowpixels) {
            super(k_bitmap_format, base, width, height, rowpixels);
        }

        public bitmap_ind16(bitmap_ind16 source, final Rectangle subrect) {
            super(k_bitmap_format, source, subrect);
        }

        public void wrap(short[] base, int width, int height, int rowpixels) {
            super.wrap(base, width, height, rowpixels);
        }

        public void wrap(bitmap_ind8 source, final Rectangle subrect) {
            super.wrap(source, subrect);
        }

        // getters
        Format format() {
            return k_bitmap_format;
        }

        @Override
        byte bpp() {
            return 8 * Short.BYTES;
        }

//        bitmap_ind16 operatorLet(bitmap_ind16 &&) = default;
    }

    // BITMAP_FORMAT_IND32 bitmaps
    static class bitmap_ind32 extends bitmap_specific<Integer> {

        static final Format k_bitmap_format = Format.BITMAP_FORMAT_IND32;

        // construction/destruction
//        public bitmap_ind32(bitmap_ind32 &&) = default;
        public bitmap_ind32(int width /*= 0*/, int height /*= 0*/, int xslop /*= 0*/, int yslop /*= 0*/) {
            super(k_bitmap_format, width, height, xslop, yslop);
        }

        public bitmap_ind32(int[] base, int width, int height, int rowpixels) {
            super(k_bitmap_format, base, width, height, rowpixels);
        }

        public bitmap_ind32(bitmap_ind32 source, final Rectangle subrect) {
            super(k_bitmap_format, source, subrect);
        }

        public void wrap(int[] base, int width, int height, int rowpixels) {
            super.wrap(base, width, height, rowpixels);
        }

        public void wrap(bitmap_ind8 source, final Rectangle subrect) {
            super.wrap(source, subrect);
        }

        // getters
        public Format format() {
            return k_bitmap_format;
        }

        @Override
        byte bpp() {
            return 8 * Integer.BYTES;
        }

//        bitmap_ind32 operatorLet(bitmap_ind32 &&) = default;
    }

    // BITMAP_FORMAT_IND64 bitmaps
    static class bitmap_ind64 extends bitmap_specific<Long> {

        static final Format k_bitmap_format = Format.BITMAP_FORMAT_IND64;

        // construction/destruction
//        public bitmap_ind64(bitmap_ind64 &&) = default;
        public bitmap_ind64(int width/* = 0*/, int height /*= 0*/, int xslop /*= 0*/, int yslop /*= 0*/) {
            super(k_bitmap_format, width, height, xslop, yslop);
        }

        public bitmap_ind64(long[] base, int width, int height, int rowpixels) {
            super(k_bitmap_format, base, width, height, rowpixels);
        }

        public bitmap_ind64(bitmap_ind64 source, final Rectangle subrect) {
            super(k_bitmap_format, source, subrect);
        }

        public void wrap(long[] base, int width, int height, int rowpixels) {
            super.wrap(base, width, height, rowpixels);
        }

        public void wrap(bitmap_ind8 source, final Rectangle subrect) {
            super.wrap((Bitmap) source, subrect);
        }

        // getters
        Format format() {
            return k_bitmap_format;
        }

        @Override
        byte bpp() {
            return 8 * Long.BYTES;
        }

//      bitmap_ind64 operatorLet(bitmap_ind64 &&) = default;
    }

    // ======================> bitmap_yuy16, bitmap_rgb32, bitmap_argb32

    // BITMAP_FORMAT_YUY16 bitmaps
    static class bitmap_yuy16 extends bitmap_specific<Short> {

        static final Format k_bitmap_format = Format.BITMAP_FORMAT_YUY16;

        // construction/destruction
//        public bitmap_yuy16(bitmap_yuy16 &&) = default;
        public bitmap_yuy16(int width /*= 0*/, int height /*= 0*/, int xslop /*= 0*/, int yslop /*= 0*/) {
            super(k_bitmap_format, width, height, xslop, yslop);
        }

        public bitmap_yuy16(short[] base, int width, int height, int rowpixels) {
            super(k_bitmap_format, base, width, height, rowpixels);
        }

        public bitmap_yuy16(bitmap_yuy16 source, final Rectangle subrect) {
            super(k_bitmap_format, source, subrect);
        }

        public void wrap(short[] base, int width, int height, int rowpixels) {
            super.wrap(base, width, height, rowpixels);
        }

        public void wrap(bitmap_yuy16 source, final Rectangle subrect) {
            super.wrap((Bitmap) source, subrect);
        }

        // getters
        public Format format() {
            return k_bitmap_format;
        }

        @Override
        byte bpp() {
            return 8 * Short.BYTES;
        }

//        public  bitmap_yuy16 operatorLet(bitmap_yuy16 &&) = default;
    }

    // BITMAP_FORMAT_RGB32 bitmaps
    static class bitmap_rgb32 extends bitmap_specific<Integer> {

        static final Format k_bitmap_format = Format.BITMAP_FORMAT_RGB32;

        // construction/destruction
//        public  bitmap_rgb32(bitmap_rgb32 &&) = default;
        public bitmap_rgb32(int width /*= 0*/, int height /*= 0*/, int xslop /*= 0*/, int yslop /*= 0*/) {
            super(k_bitmap_format, width, height, xslop, yslop);
        }

        public bitmap_rgb32(int[] base, int width, int height, int rowpixels) {
            super(k_bitmap_format, base, width, height, rowpixels);
        }

        public bitmap_rgb32(bitmap_rgb32 source, final Rectangle subrect) {
            super(k_bitmap_format, source, subrect);
        }

        public void wrap(int[] base, int width, int height, int rowpixels) {
            super.wrap(base, width, height, rowpixels);
        }

        public void wrap(bitmap_rgb32 source, final Rectangle subrect) {
            super.wrap((Bitmap) source, subrect);
        }

        // getters
        public Format format() {
            return k_bitmap_format;
        }

        @Override
        byte bpp() {
            return 8 * Integer.BYTES;
        }

//        public bitmap_rgb32 operatorLet(bitmap_rgb32 &&) = default;
    }

    // BITMAP_FORMAT_ARGB32 bitmaps
    static class bitmap_argb32 extends bitmap_specific<Integer> {

        static final Format k_bitmap_format = Format.BITMAP_FORMAT_ARGB32;

        // construction/destruction
//        public bitmap_argb32(bitmap_argb32 &&) = default;
        public bitmap_argb32(int width/* = 0*/, int height/* = 0*/, int xslop /*= 0*/, int yslop/* = 0*/) {
            super(k_bitmap_format, width, height, xslop, yslop);
        }

        public bitmap_argb32(int[] base, int width, int height, int rowpixels) {
            super(k_bitmap_format, base, width, height, rowpixels);
        }

        public bitmap_argb32(bitmap_argb32 source, final Rectangle subrect) {
            super(k_bitmap_format, source, subrect);
        }

        public void wrap(int[] base, int width, int height, int rowpixels) {
            super.wrap(base, width, height, rowpixels);
        }

        public void wrap(bitmap_argb32 source, final Rectangle subrect) {
            super.wrap((Bitmap) source, subrect);
        }

        // getters
        public Format format() {
            return k_bitmap_format;
        }

        @Override
        byte bpp() {
            return 8 * Integer.BYTES;
        }

//        public bitmap_argb32 operatorLet(bitmap_argb32 &&) = default;
    }
}