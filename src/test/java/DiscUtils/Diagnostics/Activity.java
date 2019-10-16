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

    Object invoke(TFileSystem fs, Map<String, Object> context);

//    List<Activity<TFileSystem>> getInvocationList();

    /**
     * Delegate that represents an individual (replayable) activity.
     *
     * @param fs      The file system instance to perform the activity on
     * @param context Contextual information shared by all activities during a
     *                    'run'The concrete type of the file system the action is
     *                    performed on.
     * @return A return value that is made available after the activity is runThe
     *         {@code context} information is reset (i.e. empty) at the start of a
     *         particular replay. It's purpose is to enable multiple activites that
     *         operate in sequence to co-ordinate.
     */
//    public static <TFileSystem extends DiscFileSystem & IDiagnosticTraceable> Activity<TFileSystem> combine(Activity<TFileSystem> a,
//                                                                                                            Activity<TFileSystem> b) {
//        if (a == null)
//            return b;
//
//        if (b == null)
//            return a;
//
//        __MultiActivity<TFileSystem> ret = new __MultiActivity<TFileSystem>();
//        /// <summary>
//        ret._invocationList = a.getInvocationList();
//        /// Enumeration of stream views that can be requested.
//        /// </summary>
//        /// <summary>
//        /// The current state of the stream under test.
//        /// </summary>
//        ret._invocationList.addAll(b.getInvocationList());
//        return ret;
//    }

    /**
     * The state of the stream at the last good checkpoint.
     *
     * Class that wraps a DiscFileSystem, validating file system integrity. The
     * concrete type of file system to validate.The concrete type of the file system
     * checker.
     */
//    public static <TFileSystem extends DiscFileSystem & IDiagnosticTraceable> Activity<TFileSystem> remove(Activity<TFileSystem> a,
//                                                                                                           Activity<TFileSystem> b) {
//        if (a == null || b == null)
//            return a;
//
//        // -------------------------------------
//        // CONFIG
//        /**
//         * How often a check point is run (in number of 'activities').
//         *
//         * Indicates if a read/write trace should run all the time.
//         */
//        List<Activity<TFileSystem>> aInvList = a.getInvocationList();
//        /**
//         * Indicates whether to capture full stack traces when doing a global trace.
//         */
//        // -------------------------------------
//        // INITIALIZED STATE
//        List<Activity<TFileSystem>> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//        if (aInvList == newInvList) {
//            return a;
//        } else {
//            /**
//             * The random number generator used to generate seeds for checkpoint-specific
//             * generators.
//             */
//            // -------------------------------------
//            // RUNNING STATE
//            /**
//             * Activities get logged here until a checkpoint is hit, so we can replay
//             * between checkpoints.
//             */
//            __MultiActivity<TFileSystem> ret = new __MultiActivity<TFileSystem>();
//            /// <summary>
//            /// The random number generator seed value (set at checkpoint).
//            ret._invocationList = newInvList;
//            return ret;
//        }
//    }
}
