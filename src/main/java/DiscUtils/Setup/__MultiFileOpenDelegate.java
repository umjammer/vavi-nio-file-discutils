//
// Copyright (c) 2017, Bianco Veigel
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

package DiscUtils.Setup;

//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import DiscUtils.Core.CoreCompat.ListSupport;
//import DiscUtils.Core.CoreCompat.Stream;
//
//
//public class __MultiFileOpenDelegate implements FileOpenDelegate {
//    public Stream invoke(String fileName, String mode, String access, String share) {
//        List<FileOpenDelegate> copy = new ArrayList<>(), members = this.getInvocationList();
//        synchronized (members) {
//            copy = new LinkedList<>(members);
//        }
//        FileOpenDelegate prev = null;
//        for (FileOpenDelegate d : copy) {
//             = (FileOpenDelegate) __dummyForeachVar0;
//            if (prev != null)
//                prev.invoke(fileName, mode, access, share);
//
//            prev = d;
//        }
//        return prev.invoke(fileName, mode, access, share);
//    }
//
//    private List<FileOpenDelegate> _invocationList = new ArrayList<>();
//
//    public static FileOpenDelegate combine(FileOpenDelegate a, FileOpenDelegate b) {
//        if (a == null)
//            return b;
//
//        if (b == null)
//            return a;
//
//        /**
//         * Event arguments for opening a file
//         */
//        __MultiFileOpenDelegate ret = new __MultiFileOpenDelegate();
//        ret._invocationList = a.getInvocationList();
//        ret._invocationList.addAll(b.getInvocationList());
//        return ret;
//    }
//
//    public static FileOpenDelegate remove(FileOpenDelegate a, FileOpenDelegate b) {
//        /**
//         * Gets or sets the filename to open
//         */
//        if (a == null || b == null)
//            return a;
//
//        /// <summary>
//        /// Gets or sets the <see cref="FileMode"/>
//        /// </summary>
//        /// <summary>
//        List<FileOpenDelegate> aInvList = a.getInvocationList();
//        /// Gets or sets the <see cref="FileAccess"/>
//        /// </summary>
//        /// <summary>
//        /// Gets or sets the <see cref="FileShare"/>
//        /// </summary>
//        /// <summary>
//        List<FileOpenDelegate> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//        /// The resulting stream.
//        /// </summary>
//        /// <remarks>
//        /// If this is set to a non null value, this stream is used instead of opening the supplied <see cref="FileName"/>
//        /// </remarks>
//        if (aInvList == newInvList) {
//            return a;
//        } else {
//            /**
//             * returns the result from the builtin FileLocator
//             *
//             * @return
//             */
//            __MultiFileOpenDelegate ret = new __MultiFileOpenDelegate();
//            ret._invocationList = newInvList;
//            return ret;
//        }
//    }
//
//    public List<FileOpenDelegate> getInvocationList() {
//        return _invocationList;
//    }
//
//}
