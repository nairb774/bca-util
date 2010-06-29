package org.no.ip.bca.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class EventCollector<T> implements Runnable, Sink<T> {
    public interface EventCollectorHandler<T> {
        void deadThread(Thread thread);

        void newThread(Thread thread);

        void process(Thread thread, List<T> list);

        void roundFinished();

        void roundStarted();
    }

    private static class NewThreads<T> {
        private Map<Thread, ThreadQueue<T>> threads = new HashMap<Thread, ThreadQueue<T>>();

        synchronized void add(final Thread thread, final ThreadQueue<T> threadQueue) {
            threads.put(thread, threadQueue);
        }

        synchronized Map<Thread, ThreadQueue<T>> take() {
            final Map<Thread, ThreadQueue<T>> oldThreads = threads;
            threads = new HashMap<Thread, ThreadQueue<T>>();
            return oldThreads;
        }
    }

    private static class ThreadQueue<T> implements Sink<T> {
        private LinkedList<T> list = new LinkedList<T>();

        public synchronized void add(final T t) {
            list.addLast(t);
        }

        synchronized LinkedList<T> take() {
            final LinkedList<T> oldList = list;
            list = new LinkedList<T>();
            return oldList;
        }
    }

    private static final class TL<T> extends ThreadLocal<Sink<T>> {
        final NewThreads<T> newThreads = new NewThreads<T>();

        @Override
        protected ThreadQueue<T> initialValue() {
            final ThreadQueue<T> threadQueue = new ThreadQueue<T>();
            newThreads.add(Thread.currentThread(), threadQueue);
            return threadQueue;
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

    private final TL<T> threadLocal = new TL<T>();
    private final Map<Thread, ThreadQueue<T>> threads = new HashMap<Thread, ThreadQueue<T>>();
    private final EventCollectorHandler<T> handler;

    public EventCollector(final EventCollectorHandler<T> handler) {
        this.handler = handler;
    }

    public void add(final T t) {
        threadLocal.get().add(t);
    }

    public Sink<T> get() {
        return threadLocal.get();
    }

    public ThreadLocal<Sink<T>> getThreadLocal() {
        return threadLocal;
    }

    public void run() {
        handler.roundStarted();
        final Map<Thread, ThreadQueue<T>> newThreads = threadLocal.newThreads.take();
        for (final Thread thread : newThreads.keySet()) {
            handler.newThread(thread);
        }
        threads.putAll(newThreads);
        for (final Iterator<Entry<Thread, ThreadQueue<T>>> iter = threads.entrySet().iterator(); iter.hasNext();) {
            final Entry<Thread, ThreadQueue<T>> e = iter.next();
            final Thread thread = e.getKey();
            final boolean alive = thread.isAlive();
            final LinkedList<T> take = e.getValue().take();
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
}
