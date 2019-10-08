//
// Translated by CS2J (http://www.cs2j.com): 2019/07/11 18:18:07
//

package DiscUtils.Core;

import java.util.Random;

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
/**
* Common file system options.
* Not all options are honoured by all file systems.
*/
public class DiscFileSystemOptions
{
    /**
    * Gets or sets the random number generator the file system should use.
    * This option is normally
    *  {@code null}
    * , which is fine for most purposes.
    * Use this option when you need to finely control the filesystem for
    * reproducibility of behaviour (for example in a test harness).
    */
    private Random __RandomNumberGenerator = new Random();
    public Random getRandomNumberGenerator() {
        return __RandomNumberGenerator;
    }

    public void setRandomNumberGenerator(Random value) {
        __RandomNumberGenerator = value;
    }

}


