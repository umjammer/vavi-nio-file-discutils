// license:BSD-3-Clause
// copyright-holders:Aaron Giles

package aaru.image.chd.util;


import java.awt.Color;
import java.util.Arrays;


/**
 * Core palette routines.
 */
class Palette {

    private static int clamp(float value) {
        return (value < 0) ? 0 : (int) Math.min(value, 255);
    }

    private static Color as_rgb15(Color c) {
        return new Color((c.getRed() >> 3) << 10, (c.getGreen() >> 3) << 5, (c.getBlue() >> 3) << 0);
    }

    // a single palette client
    static class Client {

        // construction/destruction
        public Client(Palette palette) {
            m_palette = palette;
            m_next = null;
            m_live = m_dirty[0];
            m_previous = m_dirty[1];

            // add a reference to the palette
            palette.ref();

            // resize the dirty lists
            int total_colors = palette.num_colors() * palette.num_groups();
            m_dirty[0].resize(total_colors);
            m_dirty[1].resize(total_colors);

            // now add us to the list of clients
            m_next = palette.m_client_list;
            palette.m_client_list = this;
        }

        // getters
        public Client next() {
            return m_next;
        }

        public Palette palette() {
            return m_palette;
        }

        public int[] dirty_list(int mindirty, int maxdirty) {
            // if nothing to report, report nothing and don't swap
            int[] result = m_live.dirty_list(mindirty, maxdirty);
            if (result == null)
                return null;

            // swap the live and previous lists
            DirtyState temp = m_live;
            m_live = m_previous;
            m_previous = temp;

            // reset new live one and return the pointer to the previous
            m_live.reset();
            return result;
        }

        // dirty marking
        public void mark_dirty(int index) {
            m_live.mark_dirty(index);
        }

        // internal object to track dirty states
        private static class DirtyState {

            // construction
            public DirtyState() {
                m_mindirty = 0;
                m_maxdirty = 0;
            }

            // operations
            public int[] dirty_list(int mindirty, int maxdirty) {
                // fill in the mindirty/maxdirty
                mindirty = m_mindirty;
                maxdirty = m_maxdirty;

                // if nothing to report, report nothing
                return (m_mindirty > m_maxdirty) ? null : m_dirty;
            }

            public void resize(int colors) {
                // resize to the correct number of dwords and mark all entries dirty
                int dirty_dwords = (colors + 31) / 32;
                m_dirty = new int[dirty_dwords];

                // mark all entries dirty
                m_dirty[dirty_dwords - 1] &= (1 << (colors % 32)) - 1;

                // set min/max
                m_mindirty = 0;
                m_maxdirty = colors - 1;
            }

            public void mark_dirty(int index) {
                m_dirty[index / 32] |= 1 << (index % 32);
                m_mindirty = Math.min(m_mindirty, index);
                m_maxdirty = Math.max(m_maxdirty, index);
            }


            public void reset() {
                // erase relevant entries in the new live one
                if (m_mindirty <= m_maxdirty)
                    Arrays.fill(m_dirty, m_mindirty / 32, m_maxdirty / 32 + 1, 0);
                m_mindirty = m_dirty.length * 32 - 1;
                m_maxdirty = 0;
            }

            // bitmap of dirty entries */
            private int[] m_dirty;
            // minimum dirty entry */
            private int m_mindirty;
            // minimum dirty entry */
            private int m_maxdirty;
        }

        // reference to the palette */
        Palette m_palette;
        // pointer to next client */
        Client m_next;
        // live dirty state */
        DirtyState m_live;
        // previous dirty state */
        DirtyState m_previous;
        // two dirty states */
        DirtyState[] m_dirty = new DirtyState[2];
    }

    // static constructor: used to ensure same new/delete is used
    public static Palette alloc(int numcolors, int numgroups/* =1*/) {
        return new Palette(numcolors, numgroups);
    }

    // reference counting
    public void ref() {
        m_refcount++;
    }

    public void deref() {
    }

    // getters
    public int num_colors() {
        return m_numcolors;
    }

    public int num_groups() {
        return m_numgroups;
    }

    public int max_index() {
        return m_numcolors * m_numgroups + 2;
    }

    public int black_entry() {
        return m_numcolors * m_numgroups + 0;
    }

    public int white_entry() {
        return m_numcolors * m_numgroups + 1;
    }

    // overall adjustments
    public void set_brightness(float brightness) {
        // convert incoming value to normalized result
        brightness = (brightness - 1.0f) * 256.0f;

        // set the global brightness if changed
        if (m_brightness == brightness)
            return;
        m_brightness = brightness;

        // update across all indices in all groups
        for (int groupnum = 0; groupnum < m_numgroups; groupnum++)
            for (int index = 0; index < m_numcolors; index++)
                update_adjusted_color(groupnum, index);
    }

    public void set_contrast(float contrast) {
        // set the global contrast if changed
        if (m_contrast == contrast)
            return;
        m_contrast = contrast;

        // update across all indices in all groups
        for (int groupnum = 0; groupnum < m_numgroups; groupnum++)
            for (int index = 0; index < m_numcolors; index++)
                update_adjusted_color(groupnum, index);
    }

    public void set_gamma(float gamma) {
        // set the global gamma if changed
        if (m_gamma == gamma)
            return;
        m_gamma = gamma;

        // recompute the gamma map
        gamma = 1.0f / gamma;
        for (int index = 0; index < 256; index++) {
            float fval = (float) index * (1.0f / 255.0f);
            float fresult = (float) Math.pow(fval, gamma);
            m_gamma_map[index] = (byte) clamp(255.0f * fresult);
        }

        // update across all indices in all groups
        for (int groupnum = 0; groupnum < m_numgroups; groupnum++)
            for (int index = 0; index < m_numcolors; index++)
                update_adjusted_color(groupnum, index);
    }

    // entry getters
    public Color entry_color(int index) {
        return (index < m_numcolors) ? m_entry_color[index] : Color.black;
    }

    public Color entry_adjusted_color(int index) {
        return (index < m_numcolors * m_numgroups) ? m_adjusted_color[index] : Color.black;
    }

    public float entry_contrast(int index) {
        return (index < m_numcolors) ? m_entry_contrast[index] : 1.0f;
    }

    // entry setters
    public void entry_set_color(int index, Color rgb) {
        // if unchanged, ignore
        if (m_entry_color[index] == rgb)
            return;

        assert (index < m_numcolors);

        // set the color
        m_entry_color[index] = rgb;

        // update across all groups
        for (int groupnum = 0; groupnum < m_numgroups; groupnum++)
            update_adjusted_color(groupnum, index);
    }

    public void entry_set_red_level(int index, byte level) {
        // if unchanged, ignore
        if (m_entry_color[index].getRed() == level)
            return;

        assert (index < m_numcolors);

        // set the level
        m_entry_color[index] = new Color(level, m_entry_color[index].getGreen(), m_entry_color[index].getBlue());

        // update across all groups
        for (int groupnum = 0; groupnum < m_numgroups; groupnum++)
            update_adjusted_color(groupnum, index);
    }

    public void entry_set_green_level(int index, byte level) {
        // if unchanged, ignore
        if (m_entry_color[index].getGreen() == level)
            return;

        assert (index < m_numcolors);

        // set the level
        m_entry_color[index] = new Color(m_entry_color[index].getRed(), level, m_entry_color[index].getBlue());

        // update across all groups
        for (int groupnum = 0; groupnum < m_numgroups; groupnum++)
            update_adjusted_color(groupnum, index);
    }

    public void entry_set_blue_level(int index, byte level) {
        // if unchanged, ignore
        if (m_entry_color[index].getBlue() == level)
            return;

        assert (index < m_numcolors);

        // set the level
        m_entry_color[index] = new Color(m_entry_color[index].getRed(), m_entry_color[index].getGreen(), level);

        // update across all groups
        for (int groupnum = 0; groupnum < m_numgroups; groupnum++)
            update_adjusted_color(groupnum, index);
    }

    public void entry_set_contrast(int index, float contrast) {
        // if unchanged, ignore
        if (m_entry_contrast[index] == contrast)
            return;

        assert (index < m_numcolors);

        // set the contrast
        m_entry_contrast[index] = contrast;

        // update across all groups
        for (int groupnum = 0; groupnum < m_numgroups; groupnum++)
            update_adjusted_color(groupnum, index);
    }

    // entry list getters
    public Color entry_list_raw() {
        return m_entry_color[0];
    }

    public Color entry_list_adjusted() {
        return m_adjusted_color[0];
    }

    public Color entry_list_adjusted_rgb15() {
        return m_adjusted_rgb15[0];
    }

    // group adjustments
    public void group_set_brightness(int group, float brightness) {
        // convert incoming value to normalized result
        brightness = (brightness - 1.0f) * 256.0f;

        assert (group < m_numgroups);

        // if unchanged, ignore
        if (m_group_bright[group] == brightness)
            return;

        // set the contrast
        m_group_bright[group] = brightness;

        // update across all colors
        for (int index = 0; index < m_numcolors; index++)
            update_adjusted_color(group, index);
    }

    public void group_set_contrast(int group, float contrast) {
        // if unchanged, ignore
        if (m_group_contrast[group] == contrast)
            return;

        assert (group < m_numgroups);

        // set the contrast
        m_group_contrast[group] = contrast;

        // update across all colors
        for (int index = 0; index < m_numcolors; index++)
            update_adjusted_color(group, index);
    }

    // utilities
    public void normalize_range(int start, int end, int lum_min /*= 0*/, int lum_max/* = 255*/) {
        // clamp within range
        // start = Math.max(start, 0U); ==> reduces to start = start
        end = Math.min(end, m_numcolors - 1);

        // find the minimum and maximum brightness of all the colors in the range
        int ymin = 1000 * 255, ymax = 0;
        for (int index = start; index <= end; index++) {
            Color rgb = m_entry_color[index];
            int y = 299 * rgb.getRed() + 587 * rgb.getGreen() + 114 * rgb.getBlue();
            ymin = Math.min(ymin, y);
            ymax = Math.max(ymax, y);
        }

        // determine target minimum/maximum
        int tmin = (lum_min < 0) ? ((ymin + 500) / 1000) : lum_min;
        int tmax = (lum_max < 0) ? ((ymax + 500) / 1000) : lum_max;

        // now normalize the palette
        for (int index = start; index <= end; index++) {
            Color rgb = m_entry_color[index];
            int y = 299 * rgb.getRed() + 587 * rgb.getGreen() + 114 * rgb.getBlue();
            int u = (rgb.getBlue() - y / 1000) * 492 / 1000;
            int v = (rgb.getRed() - y / 1000) * 877 / 1000;
            int target = tmin + ((y - ymin) * (tmax - tmin + 1)) / (ymax - ymin);
            int r = clamp(target + 1140 * v / 1000f);
            int g = clamp(target - 395 * u / 1000f - 581 * v / 1000f);
            int b = clamp(target + 2032 * u / 1000f);
            entry_set_color(index, new Color(r, g, b));
        }
    }


    // construction/destruction
    private Palette(int numcolors, int numgroups /*= 1*/) {
        m_refcount = 1;
        m_numcolors = numcolors;
        m_numgroups = numgroups;
        m_brightness = 0.0f;
        m_contrast = 1.0f;
        m_gamma = 1.0f;
        m_entry_color = new Color[numcolors];
        m_entry_contrast = new float[numcolors];
        m_adjusted_color = new Color[numcolors * numgroups + 2];
        m_adjusted_rgb15 = new Color[numcolors * numgroups + 2];
        m_group_bright = new float[numgroups];
        m_group_contrast = new float[numgroups];
        m_client_list = null;

        // initialize gamma map
        for (int index = 0; index < 256; index++)
            m_gamma_map[index] = (byte) index;

        // initialize the per-entry data
        for (int index = 0; index < numcolors; index++) {
            m_entry_color[index] = Color.black;
            m_entry_contrast[index] = 1.0f;
        }

        // initialize the per-group data
        for (int index = 0; index < numgroups; index++) {
            m_group_bright[index] = 0.0f;
            m_group_contrast[index] = 1.0f;
        }

        // initialize the expanded data
        for (int index = 0; index < numcolors * numgroups; index++) {
            m_adjusted_color[index] = Color.black;
            m_adjusted_rgb15[index] = as_rgb15(Color.black);
        }

        // add black and white as the last two colors
        m_adjusted_color[numcolors * numgroups + 0] = Color.black;
        m_adjusted_rgb15[numcolors * numgroups + 0] = as_rgb15(Color.black);
        m_adjusted_color[numcolors * numgroups + 1] = Color.white;
        m_adjusted_rgb15[numcolors * numgroups + 1] = as_rgb15(Color.white);
    }

    // internal helpers
    private Color adjust_palette_entry(Color entry, float brightness, float contrast, byte[] gamma_map) {
        int r = clamp(gamma_map[entry.getRed()] * contrast + brightness);
        int g = clamp(gamma_map[entry.getGreen()] * contrast + brightness);
        int b = clamp(gamma_map[entry.getBlue()] * contrast + brightness);
        int a = entry.getAlpha();
        return new Color(a, r, g, b);
    }

    private void update_adjusted_color(int group, int index) {
        // compute the adjusted value
        Color adjusted = adjust_palette_entry(m_entry_color[index],
                m_group_bright[group] + m_brightness,
                m_group_contrast[group] * m_entry_contrast[index] * m_contrast,
                m_gamma_map);

        // if not different, ignore
        int finalindex = group * m_numcolors + index;
        if (m_adjusted_color[finalindex] == adjusted)
            return;

        // otherwise, modify the adjusted color array
        m_adjusted_color[finalindex] = adjusted;
        m_adjusted_rgb15[finalindex] = as_rgb15(adjusted);

        // mark dirty in all clients
        for (Client client = m_client_list; client != null; client = client.next())
            client.mark_dirty(finalindex);
    }

    // reference count on the palette */
    private int m_refcount;
    // number of colors in the palette */
    private int m_numcolors;
    // number of groups in the palette */
    private int m_numgroups;

    // overall brightness value */
    private float m_brightness;
    // overall contrast value */
    private float m_contrast;
    // overall gamma value */
    private float m_gamma;
    // gamma map */
    private byte[] m_gamma_map = new byte[256];

    // array of raw colors */
    private Color[] m_entry_color;
    // contrast value for each entry */
    private float[] m_entry_contrast;
    // array of adjusted colors */
    private Color[] m_adjusted_color;
    // array of adjusted colors as RGB15 */
    private Color[] m_adjusted_rgb15;

    // brightness value for each group */
    private float[] m_group_bright;
    // contrast value for each group */
    private float[] m_group_contrast;

    // list of clients for this palette */
    private Client m_client_list;

    // expand a palette value to 8 bits

    int palexpand(int numBits, int bits) {
        if (numBits == 1) {
            return (bits & 1) != 0 ? 0xff : 0x00;
        }
        if (numBits == 2) {
            bits &= 3;
            return (bits << 6) | (bits << 4) | (bits << 2) | bits;
        }
        if (numBits == 3) {
            bits &= 7;
            return (bits << 5) | (bits << 2) | (bits >> 1);
        }
        if (numBits == 4) {
            bits &= 0xf;
            return (bits << 4) | bits;
        }
        if (numBits == 5) {
            bits &= 0x1f;
            return (bits << 3) | (bits >> 2);
        }
        if (numBits == 6) {
            bits &= 0x3f;
            return (bits << 2) | (bits >> 4);
        }
        if (numBits == 7) {
            bits &= 0x7f;
            return (bits << 1) | (bits >> 6);
        }
        return bits;
    }

    // convert an x-bit value to 8 bits

    int pal1bit(int bits) {
        return palexpand(1, bits);
    }

    int pal2bit(int bits) {
        return palexpand(2, bits);
    }

    int pal3bit(int bits) {
        return palexpand(3, bits);
    }

    int pal4bit(int bits) {
        return palexpand(4, bits);
    }

    int pal5bit(int bits) {
        return palexpand(5, bits);
    }

    int pal6bit(int bits) {
        return palexpand(6, bits);
    }

    int pal7bit(int bits) {
        return palexpand(7, bits);
    }

    // expand a 32-bit raw data to 8-bit RGB

    Color rgbexpand(int rBits, int gBits, int bBits, int data, byte rshift, byte gshift, byte bshift) {
        return new Color(palexpand(rBits, data >> rshift), palexpand(gBits, data >> gshift), palexpand(bBits, data >> bshift));
    }

    Color argbexpand(int aBits, int rBits, int gBits, int bBits, int data, byte ashift, byte rshift, byte gshift, byte bshift) {
        return new Color(palexpand(aBits, data >> ashift), palexpand(rBits, data >> rshift), palexpand(gBits, data >> gshift), palexpand(bBits, data >> bshift));
    }

    // create an x-x-x color by extracting bits from a int

    Color pal332(int data, byte rshift, byte gshift, byte bshift) {
        return rgbexpand(3, 3, 2, data, rshift, gshift, bshift);
    }

    Color pal444(int data, byte rshift, byte gshift, byte bshift) {
        return rgbexpand(4, 4, 4, data, rshift, gshift, bshift);
    }

    Color pal555(int data, byte rshift, byte gshift, byte bshift) {
        return rgbexpand(5, 5, 5, data, rshift, gshift, bshift);
    }

    Color pal565(int data, byte rshift, byte gshift, byte bshift) {
        return rgbexpand(5, 6, 5, data, rshift, gshift, bshift);
    }

    Color pal888(int data, byte rshift, byte gshift, byte bshift) {
        return rgbexpand(8, 8, 8, data, rshift, gshift, bshift);
    }
}