package org.no.ip.bca.util.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;

public class LockFile {
    private FileOutputStream stream;
    private final File file;

    public LockFile(final File file) {
        int i = -1;
        while (i < 100) {
            i++;
            try {
                stream = new FileOutputStream(new File(file.getParentFile(), file.getName() + "-" + i + ".lck"));
                if (stream.getChannel().tryLock() != null) {
                    break;
                }
                stream.close();
            } catch (final IOException e) {
            } catch (final OverlappingFileLockException e) {
            }
        }
        if (i == 100) {
            throw new RuntimeException();
        }
        this.file = new File(file.getParentFile(), file.getName() + "-" + i);
    }

    public void close() throws IOException {
        stream.close();
        final File lockFile = new File(file.getParentFile(), file.getName() + ".lck");
        if (lockFile.exists() && !lockFile.delete()) {
            lockFile.deleteOnExit();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    public File getFile() {
        return file;
    }
}
