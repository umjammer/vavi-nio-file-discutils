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

package DiscUtils.Diagnostics;

import java.util.Map;

import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.IDiagnosticTraceable;


@FunctionalInterface
public interface Activity<TFileSystem extends DiscFileSystem & IDiagnosticTraceable> {

    /**
     * Delegate that represents an individual (replayable) activity.
     *
     * @param fs      The file system instance to perform the activity on
     * @param context Contextual information shared by all activities during a
     *                    'run'The concrete type of the file system the action is
     *                    performed on.
     * @return A return value that is made available after the activity is run
     * The
     *         {@code context} information is reset (i.e. empty) at the start of a
     *         particular replay. It's purpose is to enable multiple activites that
     *         operate in sequence to co-ordinate.
     */
    Object invoke(TFileSystem fs, Map<String, Object> context);
}
