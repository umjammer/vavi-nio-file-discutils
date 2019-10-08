//
// Copyright (c) 2008-2011, Kenneth Bell
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.
//

package DiscUtils.Streams.Util;

public class MathUtilities {
    /**
     * Round up a value to a multiple of a unit size.
     *
     * @param value The value to round up.
     * @param unit The unit (the returned value will be a multiple of this
     *            number).
     * @return The rounded-up value.
     */
    public static long roundUp(long value, long unit) {
        return (value + (unit - 1)) / unit * unit;
    }

    /**
     * Round up a value to a multiple of a unit size.
     *
     * @param value The value to round up.
     * @param unit The unit (the returned value will be a multiple of this
     *            number).
     * @return The rounded-up value.
     */
    public static int roundUp(int value, int unit) {
        return (value + (unit - 1)) / unit * unit;
    }

    /**
     * Round down a value to a multiple of a unit size.
     *
     * @param value The value to round down.
     * @param unit The unit (the returned value will be a multiple of this
     *            number).
     * @return The rounded-down value.
     */
    public static long roundDown(long value, long unit) {
        return value / unit * unit;
    }

    /**
     * Calculates the CEIL function.
     *
     * @param numerator The value to divide.
     * @param denominator The value to divide by.
     * @return The value of CEIL(numerator/denominator).
     */
    public static int ceil(int numerator, int denominator) {
        return (numerator + (denominator - 1)) / denominator;
    }

    /**
     * Calculates the CEIL function.
     *
     * @param numerator The value to divide.
     * @param denominator The value to divide by.
     * @return The value of CEIL(numerator/denominator).
     */
    public static long ceil(long numerator, long denominator) {
        return (numerator + (denominator - 1)) / denominator;
    }

    public static int log2(int val) {
        if (val == 0) {
            throw new IllegalArgumentException("Cannot calculate log of Zero");
        }

        int result = 0;
        while ((val & 1) != 1) {
            val >>= 1;
            ++result;
        }
        if (val == 1) {
            return result;
        }

        throw new IllegalArgumentException("Input is not a power of Two");
    }

}
