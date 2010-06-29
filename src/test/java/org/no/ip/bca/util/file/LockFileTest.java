package org.no.ip.bca.util.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LockFileTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private final List<LockFile> lockFiles = new ArrayList<LockFile>();

    @After
    public void closeLockFiles() {
        for (final LockFile file : lockFiles) {
            try {
                file.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    private File getLckFile(final LockFile lockFile) {
        final File file = lockFile.getFile();
        return new File(file.getParentFile(), file.getName() + ".lck");
    }

    private LockFile lockFile(final File file) {
        final LockFile lockFile = new LockFile(file);
        lockFiles.add(lockFile);
        return lockFile;
    }

    @Test
    public void testLckFileExists() throws Exception {
        final File root = folder.getRoot();
        final File name = new File(root, "a");
        final LockFile lockFile = lockFile(name);
        final File lckFile = getLckFile(lockFile);
        Assert.assertTrue(lckFile.exists());
    }

    @Test
    public void testLckFileRemovedAfterClose() throws Exception {
        final File root = folder.getRoot();
        final File name = new File(root, "a");
        final LockFile lockFile = lockFile(name);
        final File lckFile = getLckFile(lockFile);
        lockFile.close();
        Assert.assertFalse(lckFile.exists());
    }

    @Test
    public void testTwoLocksWontLockSameFile() throws Exception {
        final File root = folder.getRoot();
        final File name = new File(root, "a");
        final LockFile lockFile1 = lockFile(name);
        final LockFile lockFile2 = lockFile(name);
        Assert.assertFalse(lockFile1.getFile().equals(lockFile2.getFile()));
    }
}
