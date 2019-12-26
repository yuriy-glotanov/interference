/**
 The MIT License (MIT)

 Copyright (c) 2010-2019 head systems, ltd

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

import su.interference.core.Chunk;
import su.interference.core.DataChunk;
import su.interference.exception.InternalException;
import su.interference.persistent.Session;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Yuriy Glotanov
 * @since 1.0
 */

public class StreamQueue implements ResultSet {
    //private final PriorityBlockingQueue<Comparable> q = new PriorityBlockingQueue();
    private final ConcurrentLinkedQueue<Object> q = new ConcurrentLinkedQueue();

    public DataChunk persist(Object o, Session s) throws Exception {
        q.add(o);
        return null;
    }

    public Object poll() {
        return q.poll();
    }

    public List<Chunk> getAll(Session s) throws Exception {
        return null;
    }

    public ArrayList<Object> getAll(Session s, int ptr) throws Exception {
        return null;
    }

    public int getObjectId() {
        return 0;
    }

    public boolean isIndex() throws ClassNotFoundException, MalformedURLException {
        return false;
    }

    public Class getTableClass() throws ClassNotFoundException, MalformedURLException {
        return null;
    }

    public java.lang.reflect.Field[] getFields() throws ClassNotFoundException, InternalException, MalformedURLException {
        return null;
    }

    public void deallocate(Session s) throws Exception {

    }
}