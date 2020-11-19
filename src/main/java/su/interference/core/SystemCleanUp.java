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

package su.interference.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.interference.metrics.Metrics;
import su.interference.persistent.FrameData;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @author Yuriy Glotanov
 * @since 1.0
 */

public class SystemCleanUp implements Runnable, ManagedProcess {
    private volatile boolean f = true;
    CountDownLatch latch;
    private final static Logger logger = LoggerFactory.getLogger(SystemCleanUp.class);
    private static final int CLEANUP_TIMEOUT = 3000;
    public static final int DATA_RETRIEVED_PRIORITY = 6;
    public static final int INDEX_RETRIEVED_PRIORITY = 9;
    private static final int CLEANUP_PROTECTION_THR = 1000;
    private static final int IX_CLEANUP_PROTECTION_THR = 5000;

    public void run () {
        Thread.currentThread().setName("interference-cleanup-thread");
        while (f) {
            latch = new CountDownLatch(1);
            try {
                cleanUpFrames();
            } catch(Exception e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(CLEANUP_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            latch.countDown();
        }
    }

    public void stop() throws InterruptedException{
        f = false;
        if (latch != null) {
            latch.await();
        }
    }

    private void cleanUpFrames() {

        Metrics.get("systemCleanUp").start();
        int i = 0;
        int d = 0;
        int x = 0;
        int u = 0;
        int i_ = 0;
        int d_ = 0;
        int x_ = 0;
        int u_ = 0;
        for (Object entry : Instance.getInstance().getFramesMap().entrySet()) {
            final FrameData f = (FrameData) ((DataChunk) ((Map.Entry) entry).getValue()).getEntity();
            final long frameAmount = f.getDataObject().getFrameAmount();
            if (f.getDataFile().isData()) {
                f.decreasePriority();
                if (f.isSynced() && f.getObjectId() > 999 && f.getPriority() == 0 && frameAmount > CLEANUP_PROTECTION_THR) {
                    if (f.clearFrame()) {
                        d++;
                    }
                }
                if (f.isFrame()) {
                    d_++;
                }
            }
            if (f.getDataFile().isIndex()) {
                f.decreasePriority();
                if (f.isSynced() && f.getObjectId() > 999 && f.getPriority() == 0 && frameAmount > IX_CLEANUP_PROTECTION_THR) {
                    if (f.clearFrame()) {
                        x++;
                    }
                }
                if (f.isFrame()) {
                    x_++;
                }
            }
            if (f.getDataFile().isTemp()) {
                if (f.isSynced() && f.getObjectId() > 999 && frameAmount > CLEANUP_PROTECTION_THR) {
                    if (f.clearFrame()) {
                        i++;
                    }
                }
                if (f.isFrame()) {
                    i_++;
                }
            }
            if (f.getDataFile().isUndo()) {
                if (f.isSynced() && frameAmount > CLEANUP_PROTECTION_THR) {
                    if (f.clearFrame()) {
                        u++;
                    }
                }
                if (f.isFrame()) {
                    u_++;
                }
            }
        }
        Metrics.get("сleanUpDataFrames").put(d);
        Metrics.get("сleanUpIndexFrames").put(x);
        Metrics.get("сleanUpUndoFrames").put(u);
        Metrics.get("imDataFrames").put(d_);
        Metrics.get("imIndexFrames").put(x_);
        Metrics.get("imUndoFrames").put(u_);
        Metrics.get("systemCleanUp").stop();

    }

    public static void forceCleanUp() {
        for (Object entry : Instance.getInstance().getFramesMap().entrySet()) {
            final FrameData f = (FrameData) ((DataChunk) ((Map.Entry) entry).getValue()).getEntity();
            if (f.isSynced()) {
                f.clearFrame();
            }
        }
    }
}
