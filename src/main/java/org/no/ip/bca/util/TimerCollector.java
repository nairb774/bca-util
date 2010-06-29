package org.no.ip.bca.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.no.ip.bca.util.EventCollector.EventCollectorHandler;
import org.no.ip.bca.util.file.LockFile;

public class TimerCollector implements EventCollectorHandler<QuickTimer> {
    private static final String DEAD_THREAD = "DT\t";
    private static final String MILLIS_NANO_SYNC = "MN\t";
    private static final String NEW_THREAD = "NT\t";
    private static final String QUICK_TIMER = "QT\t";
    private static final String THREAD = "TH\t";

    private final int count;
    private final File file;
    private final LockFile lockFile;
    private PrintWriter printWriter;
    private final Map<Thread, String> threadIds = new HashMap<Thread, String>();
    private final long wrapSize;

    public TimerCollector(final File file, final long wrapSize, final int count) throws IOException {
        lockFile = new LockFile(file);
        this.file = lockFile.getFile();
        this.wrapSize = wrapSize;
        this.count = count;
        shiftFiles();
    }

    public void close() throws IOException {
        lockFile.close();
        printWriter.close();
    }

    public void deadThread(final Thread thread) {
        printWriter.println(DEAD_THREAD + threadIds.remove(thread));
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    public void newThread(final Thread thread) {
        final UUID uuid = UUID.randomUUID();
        final String id = Long.toHexString(uuid.getMostSignificantBits())
                + Long.toHexString(uuid.getLeastSignificantBits());
        threadIds.put(thread, id);
        printWriter.println(NEW_THREAD + id);
    }

    public void process(final Thread thread, final List<QuickTimer> list) {
        printWriter.println(THREAD + threadIds.get(thread));
        for (final QuickTimer qt : list) {
            printWriter.println(QUICK_TIMER + qt.getStartTimeNanos() + '\t' + qt.getEndTimeNanos() + '\t'
                    + qt.getDurationInNanos() + '\t' + qt.getName());
        }
    }

    public void roundFinished() {
        if (file.length() > wrapSize) {
            shiftFiles();
        }
    }

    public void roundStarted() {
        final long millis = System.currentTimeMillis();
        final long nanos = System.nanoTime();
        printWriter.println(MILLIS_NANO_SYNC + millis + "\t" + nanos);
    }

    private void shiftFiles() {
        if (printWriter != null) {
            printWriter.close();
        }
        if (count > 1) {
            int i = count - 1;
            File dest = new File(file.getParentFile(), file.getName() + "." + i);
            if (dest.exists()) {
                dest.delete();
            }
            while (i > 1) {
                i--;
                final File src = new File(file.getParentFile(), file.getName() + "." + i);
                if (src.exists()) {
                    src.renameTo(dest);
                }
                dest = src;
            }
            file.renameTo(dest);
        } else {
            file.delete();
        }
        final FileOutputStream fos;
        try {
            fos = new FileOutputStream(file, true);
        } catch (final FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        final OutputStreamWriter osw = new OutputStreamWriter(fos, Charset.forName("UTF-8"));
        printWriter = new PrintWriter(osw, true);
    }
}
