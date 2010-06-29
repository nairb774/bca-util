package org.no.ip.bca.util;

import java.util.List;

import org.junit.Test;
import org.no.ip.bca.util.EventCollector.EventCollectorHandler;

public class EventCollectorTest {
    public static class ECH implements EventCollectorHandler<Object> {
        public void deadThread(final Thread thread) {
        }

        public void newThread(final Thread thread) {
        }

        public void process(final Thread thread, final List<Object> list) {
        }

        public void roundFinished() {
        }

        public void roundStarted() {
        }
    }

    @Test
    public void testSimple() {
        final EventCollector<Object> eventCollector = new EventCollector<Object>(new ECH());
        eventCollector.add(new Object());
        eventCollector.run();
    }
}
