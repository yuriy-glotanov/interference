/**
 The MIT License (MIT)

 Copyright (c) 2010-2020 head systems, ltd

 Permission is hereby granted, free of charge, to any person obtaining a copy of
 this software and associated documentation files (the "Software"), to deal in
 the Software without restriction, including without limitation the rights to
 use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 the Software, and to permit persons to whom the Software is furnished to do so,
 subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 */

package su.interference.sql;

import su.interference.core.Instance;
import su.interference.core.ValueSet;
import su.interference.exception.InternalException;
import su.interference.persistent.FrameData;
import su.interference.persistent.Session;
import su.interference.persistent.Table;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Yuriy Glotanov
 * @since 1.0
 */

public class SQLIndex implements FrameIterator, Finder {
    private final Table t;
    private final Session s;
    private final int join;
    private final Table parent;
    private final SQLColumn lkey;
    private final SQLColumn rkey;
    private final boolean left;
    private final boolean unique;
    private final boolean merged;
    private final LinkedBlockingQueue<FrameData> frames;
    private final AtomicBoolean returned;
    private final AtomicBoolean terminate;
    private final ValueCondition vc;
    private SQLIndexFrame mframe;

    public SQLIndex(Table t, Table parent, boolean left, SQLColumn lkey, SQLColumn rkey, boolean merged, NestedCondition nc, int join, Session s) throws InternalException, IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (!t.isIndex()) throw new InternalException();
        this.t = t;
        this.s = s;
        this.join = join;
        this.lkey = lkey;
        this.rkey = rkey;
        this.parent = parent;
        this.left = left;
        this.unique = left?lkey.isUnique():rkey.isUnique();
        this.merged = merged;
        this.frames = join == SQLJoinDispatcher.MERGE ? null : left ? t.getFrames(s) : null;
        this.returned = new AtomicBoolean(false);
        this.terminate = new AtomicBoolean(false);
        this.vc = nc.getIndexVC(this, t);
    }

    public List<Object> get(Object key, Session s) throws Exception {
        List<Object> res = new ArrayList<>();
        if (!left&&unique) {
            res.add(t.getObjectByKey(new ValueSet(key), s));
            return res;
        } else if (!left) {
            res.addAll(t.getObjectsByKey(new ValueSet(key), s));
            return res;
        }
        return null;
    }

    //may returns null
    public FrameApi nextFrame() throws Exception {
        if (!left || join == SQLJoinDispatcher.MERGE) {
            if (!returned.get()) {
                returned.compareAndSet(false, true);
                if (merged) {
                    synchronized (this) {
                        if (mframe == null) {
                            mframe = new SQLIndexFrame(t, parent, null, lkey, rkey, vc, left, unique, merged, join);
                        }
                        return mframe;
                    }
                } else {
                    return new SQLIndexFrame(t, parent, null, lkey, rkey, vc, left, unique, merged, join);
                }
            }
        } else {
            if (hasNextFrame()) {
                final FrameData bd = frames.take();
                if (bd.getObjectId() == 0 && bd.getFrameId() == 0) {
                    terminate.compareAndSet(false, true);
                    return null;
                }
                return new SQLIndexFrame(t, parent, bd, lkey, rkey, vc, left, unique, merged, join);
            }
        }
        return null;
    }

    public FrameApi getFrameByAllocId(long allocId) {
        final FrameData bd = Instance.getInstance().getFrameByAllocId(allocId);
        return new SQLIndexFrame(t, parent, bd, lkey, rkey, vc, left, unique, merged, join);
    }

    public void resetIterator() {
        if (left) {
/* optional (possibly will be need)
            try {
                frames = t.getFrames(s);
                terminate.compareAndSet(true, false);
            } catch (Exception e) {
                throw new RuntimeException();
            }
*/
        }
        if (returned.get()) {
            returned.compareAndSet(true, false);
        }
    }

    public boolean hasNextFrame() {
        if (!left || join == SQLJoinDispatcher.MERGE) {
            return !returned.get();
        } else {
            return !terminate.get();
        }
    }

    public int getType() {
        return FrameIterator.TYPE_TABLE;
    }

    public boolean isIndex() {
        return t.isIndex();
    }

    public int getObjectId() {
        return parent.getObjectId();
    }

    public List<Integer> getObjectIds() {
        return Arrays.asList(new Integer[]{parent.getObjectId()});
    }

    @Override
    public boolean isLeftfs() {
        return false;
    }

    @Override
    public void setLeftfs(boolean leftfs) {

    }

    @Override
    public boolean noDistribute() {
        return this.vc != null || this.join == SQLJoinDispatcher.MERGE;
    }

}
