package org.no.ip.bca.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TriggeredEventCollector<T> implements Runnable, Sink<T> {
    public interface EventCollectorHandler<T> {
        void deadThread(Thread thread);

        void newThread(Thread thread);

        void process(Thread thread, List<T> list);

        void roundFinished();

        void roundStarted();
    }

    private static class NewThreads<T> {
        private Map<Thread, ThreadQueue<T>> threads = new HashMap<Thread, ThreadQueue<T>>();
        private boolean[] trigger;

        public NewThreads(final boolean[] trigger) {
            this.trigger = trigger;
        }

        synchronized ThreadQueue<T> add(final Thread thread) {
            final ThreadQueue<T> threadQueue = new ThreadQueue<T>(trigger);
            threads.put(thread, threadQueue);
            if (threads.size() == 8) {
                fireTrigger(trigger);
            }
            return threadQueue;
        }

        synchronized Map<Thread, ThreadQueue<T>> take(final boolean[] trigger) {
            this.trigger = trigger;
            final Map<Thread, ThreadQueue<T>> oldThreads = threads;
            threads = new HashMap<Thread, ThreadQueue<T>>();
            return oldThreads;
        }
    }

    private static class ThreadQueue<T> implements Sink<T> {
        private LinkedList<T> list = new LinkedList<T>();
        private boolean[] trigger;

        public ThreadQueue(final boolean[] trigger) {
            this.trigger = trigger;
        }

        public synchronized void add(final T t) {
            list.addLast(t);
            if (list.size() == 8) {
                fireTrigger(trigger);
            }
        }

        synchronized LinkedList<T> take(final boolean[] trigger) {
            this.trigger = trigger;
            final LinkedList<T> oldList = list;
            list = new LinkedList<T>();
            return oldList;
        }
    }

    private static final class TL<T> extends ThreadLocal<Sink<T>> {
        final NewThreads<T> newThreads;

        public TL(final NewThreads<T> newThreads) {
            this.newThreads = newThreads;
        }

        @Override
        protected ThreadQueue<T> initialValue() {
            return newThreads.add(Thread.currentThread());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(final Sink<T> value) {
            throw new UnsupportedOperationException();
        }
    }

    private static void fireTrigger(final boolean[] trigger) {
        synchronized (trigger) {
            trigger[0] = true;
            trigger.notify();
        }
    }

    private final TL<T> threadLocal;
    private final NewThreads<T> newThreads;
    private final Map<Thread, ThreadQueue<T>> threads = new HashMap<Thread, ThreadQueue<T>>();
    private final EventCollectorHandler<T> handler;
    private boolean[] trigger = new boolean[1];

    public TriggeredEventCollector(final EventCollectorHandler<T> handler) {
        this.handler = handler;
        newThreads = new NewThreads<T>(trigger);
        threadLocal = new TL<T>(newThreads);
    }

    public void add(final T t) {
        threadLocal.get().add(t);
    }

    public void awaitTrigger() throws InterruptedException {
        synchronized (trigger) {
            if (!trigger[0]) {
                trigger.wait();
            }
        }
        run();
    }

    public boolean awaitTrigger(final long timeout) throws InterruptedException {
        final boolean triggered;
        synchronized (trigger) {
            if (!trigger[0]) {
                trigger.wait(timeout);
                triggered = trigger[0];
            } else {
                triggered = true;
            }
        }
        if (triggered) {
            run();
        }
        return triggered;
    }

    public Sink<T> get() {
        return threadLocal.get();
    }

    public ThreadLocal<Sink<T>> getThreadLocal() {
        return threadLocal;
    }

    public void run() {
        trigger = new boolean[1];
        handler.roundStarted();
        final Map<Thread, ThreadQueue<T>> newThreads = this.newThreads.take(trigger);
        for (final Thread thread : newThreads.keySet()) {
            handler.newThread(thread);
        }
        threads.putAll(newThreads);
        for (final Iterator<Entry<Thread, ThreadQueue<T>>> iter = threads.entrySet().iterator(); iter.hasNext();) {
            final Entry<Thread, ThreadQueue<T>> e = iter.next();
            final Thread thread = e.getKey();
            final boolean alive = thread.isAlive();
            final LinkedList<T> take = e.getValue().take(trigger);
            if (!take.isEmpty()) {
                handler.process(thread, take);
            }
            if (!alive) {
                iter.remove();
                handler.deadThread(thread);
            }
        }
        handler.roundFinished();
    }

    public boolean runIfTriggered() {
        final boolean triggered;
        synchronized (trigger) {
            triggered = trigger[0];
        }
        if (triggered) {
            run();
        }
        return triggered;
    }
}
