package org.no.ip.bca.util;

import junit.framework.Assert;

import org.junit.Test;

public class QuickTimerTest {
    private static class NullSink<T> implements Sink<T> {
        public void add(final T t) {

        }
    }

    private static final NullSink<QuickTimer> SINK = new NullSink<QuickTimer>();

    @Test
    public void test() {
        final QuickTimer qt = QuickTimer.start("foo");
        qt.stop(SINK);
        Assert.assertTrue(qt.getDurationInNanos() > 0);
        System.out.println(qt.getDurationInNanos());
    }
}
