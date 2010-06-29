package org.no.ip.bca.util;

public class QuickTimer {
    public static QuickTimer start(final String name) {
        final QuickTimer quickTimer = new QuickTimer(name);
        quickTimer.start();
        return quickTimer;
    }

    private final String name;
    private long startTimeM;
    private long startTimeN;
    private long endTimeN;

    public QuickTimer(final String name) {
        this.name = name;
    }

    public long getDurationInNanos() {
        return endTimeN - startTimeN;
    }

    public long getEndTimeNanos() {
        return endTimeN;
    }

    public String getName() {
        return name;
    }

    public long getStartTimeMillis() {
        return startTimeM;
    }

    public long getStartTimeNanos() {
        return startTimeN;
    }

    public void start() {
        startTimeM = System.currentTimeMillis();
        startTimeN = System.nanoTime();
    }

    public void stop(final Sink<QuickTimer> sink) {
        endTimeN = System.nanoTime();
        sink.add(this);
    }
}
