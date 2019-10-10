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

package LibraryTests;

//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import DiscUtils.Core.DiscFileSystem;
//import DiscUtils.Core.CoreCompat.ListSupport;
//
//
//public class __MultiNewFileSystemDelegate implements NewFileSystemDelegate {
//    public DiscFileSystem invoke() {
//        List<NewFileSystemDelegate> copy = new ArrayList<>(), members = this.getInvocationList();
//        synchronized (members) {
//            copy = new LinkedList<NewFileSystemDelegate>(members);
//        }
//        NewFileSystemDelegate prev = null;
//        for (Object __dummyForeachVar0 : copy) {
//            NewFileSystemDelegate d = (NewFileSystemDelegate) __dummyForeachVar0;
//            if (prev != null)
//                prev.invoke();
//
//            prev = d;
//        }
//        return prev.invoke();
//    }
//
//    private List<NewFileSystemDelegate> _invocationList = new ArrayList<NewFileSystemDelegate>();
//
//    public static NewFileSystemDelegate combine(NewFileSystemDelegate a, NewFileSystemDelegate b) throws Exception {
//        if (a == null)
//            return b;
//
//        if (b == null)
//            return a;
//
//        __MultiNewFileSystemDelegate ret = new __MultiNewFileSystemDelegate();
//        ret._invocationList = a.getInvocationList();
//        ret._invocationList.addAll(b.getInvocationList());
//        return ret;
//    }
//
//    // TODO: When format code complete, format a vanilla partition rather than relying on file on disk
//    public static NewFileSystemDelegate remove(NewFileSystemDelegate a, NewFileSystemDelegate b) throws Exception {
//        if (a == null || b == null)
//            return a;
//
//        List<NewFileSystemDelegate> aInvList = a.getInvocationList();
//        List<NewFileSystemDelegate> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//        if (aInvList == newInvList) {
//            return a;
//        } else {
//            __MultiNewFileSystemDelegate ret = new __MultiNewFileSystemDelegate();
//            ret._invocationList = newInvList;
//            return ret;
//        }
//    }
//
//    public List<NewFileSystemDelegate> getInvocationList() throws Exception {
//        return _invocationList;
//    }
//}
