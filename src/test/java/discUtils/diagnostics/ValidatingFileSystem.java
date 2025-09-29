//
// Copyright (c) 2008-2011, Kenneth Bell
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.
//

package discUtils.diagnostics;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import discUtils.core.DiscDirectoryInfo;
import discUtils.core.DiscFileInfo;
import discUtils.core.DiscFileSystem;
import discUtils.core.DiscFileSystemChecker;
import discUtils.core.DiscFileSystemInfo;
import discUtils.core.IDiagnosticTraceable;
import discUtils.core.ReportLevels;
import discUtils.streams.SnapshotStream;
import discUtils.streams.SparseStream;
import discUtils.streams.util.Ownership;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.Stream;


@FunctionalInterface
interface Activity<TFileSystem extends DiscFileSystem & IDiagnosticTraceable> {

    /**
     * Delegate that represents an individual (replayable) activity.
     *
     * The {@code context} information is reset (i.e. empty) at the start of a
     * particular replay. It's purpose is to enable multiple activites that
     * operate in sequence to co-ordinate.
     *
     * @param fs The file system instance to perform the activity on
     * @param context Contextual information shared by all activities during a
     *            'run'The concrete type of the file system the action is
     *            performed on.
     * @return A return value that is made available after the activity is run
     */
    Object invoke(TFileSystem fs, Map<String, Object> context);
}

/**
 * Enumeration of stream views that can be requested.
 */
enum StreamView {
    /**
     * The current state of the stream under test.
     */
    Current,
    /**
     * The state of the stream at the last good checkpoint.
     */
    LastCheckpoint
}

/**
 * Class that wraps a {@link DiscFileSystem}, validating file system integrity.
 *
 * @param <TFileSystem> The concrete type of file system to validate.
 * @param <TChecker> The concrete type of the file system checker.
 */
public class ValidatingFileSystem<TFileSystem extends DiscFileSystem & IDiagnosticTraceable, TChecker extends DiscFileSystemChecker> extends DiscFileSystem {

    private static final String FS = java.io.File.separator;

    private Stream baseStream;

    // -------------------------------------
    // CONFIG

    /**
     * How often a check point is run (in number of 'activities').
     */
    private int checkpointPeriod = 1;

    /**
     * Indicates if a read/write trace should run all the time.
     */
    private boolean runGlobalTrace;

    /**
     * Indicates whether to capture full stack traces when doing a global trace.
     */
    private boolean globalTraceCaptureStackTraces;

    // -------------------------------------
    // INITIALIZED STATE

    private SnapshotStream snapStream;

    private TFileSystem liveTarget;

    private boolean initialized;

    private Map<String, Object> activityContext;

    private TracingStream globalTrace;

    /**
     * The random number generator used to generate seeds for
     * checkpoint-specific generators.
     */
    private Random masterRng;

    // -------------------------------------
    // RUNNING STATE

    /**
     * Activities get logged here until a checkpoint is hit, so we can replay
     * between checkpoints.
     */
    private List<Activity<TFileSystem>> checkpointBuffer;

    /**
     * The random number generator seed value (set at checkpoint).
     */
    private int checkpointRngSeed;

    /**
     * The last verification report generated at a scheduled checkpoint.
     */
    private String lastCheckpointReport;

    /**
     * Flag set when a validation failure is observed, preventing further file
     * system activity.
     */
    private boolean lockdown;

    /**
     * The exception (if any) that indicated the file system was corrupt.
     */
    private Exception failureException;

    /**
     * The total number of events carried out before lock-down occured.
     */
    private long totalEventsBeforeLockDown;

    private int numScheduledCheckpoints;

    private Class<TFileSystem> fileSystemClass;

    private Class<TChecker> checkerClass;

    /**
     * Creates a new instance.
     *
     * @param stream A stream containing an existing (valid) file system.The new
     *            instance does not take ownership of the stream.
     */
    public ValidatingFileSystem(Class<TFileSystem> fileSystemClass, Class<TChecker> checkerClass, Stream stream) {
        baseStream = stream;
        this.fileSystemClass = fileSystemClass;
        this.checkerClass = checkerClass;
    }

    /**
     * Disposes of this instance, forcing a checkpoint if one is outstanding.
     */
    @Override
    public void close() throws IOException {
        try {
            checkpointAndThrow();
        } finally {
            if (globalTrace != null) {
                globalTrace.close();
            }
        }

        super.close();
    }

    /**
     * Gets and sets how often an automatic checkpoint occurs. The number here
     * represents the number of distinct file system operations. Each
     * method/property access on DiscFileSystem or a stream retrieved from
     * DiscFileSystem counts as an operation.
     */
    public int getCheckpointInterval() {
        return checkpointPeriod;
    }

    public void setCheckpointInterval(int value) {
        checkpointPeriod = value;
    }

    /**
     * Gets and sets whether an inter-checkpoint trace should be run (useful for
     * non-reproducible failures).
     */
    public boolean getRunGlobalIOTrace() {
        return runGlobalTrace;
    }

    public void setRunGlobalIOTrace(boolean value) {
        runGlobalTrace = value;
    }

    /**
     * Gets and sets whether a global I/O trace should be run (useful for
     * non-reproducible failures).
     */
    public boolean getGlobalIOTraceCapturesStackTraces() {
        return globalTraceCaptureStackTraces;
    }

    public void setGlobalIOTraceCapturesStackTraces(boolean value) {
        globalTraceCaptureStackTraces = value;
    }

    /**
     * Gets access to a view of the stream being validated, forcing 'lock-down'.
     *
     * This method never lets changes through to the underlying stream, so
     * ensures the integrity of the underlying stream. Any changes made to the
     * returned stream are held as a private delta and discarded when the stream
     * is disposed.
     *
     * @param view The view to open.
     * @param readOnly Whether to fail changes to the stream.
     * @return The new stream, the caller must dispose.Always use this method to
     *         access the stream, rather than keeping a reference to the stream
     *         passed to the constructor.
     */
    public Stream openStreamView(StreamView view, boolean readOnly) {
        // Prevent further changes.
        lockdown = true;
        Stream s;
        // Perversely, the snap stream has the current view (squirrelled away in
        // it's delta). The base stream is actually the stream state back at the
        // last checkpoint.
        if (view == StreamView.Current) {
            s = snapStream;
        } else {
            s = baseStream;
        }
        // Return a protective wrapping stream, so the original stream is
        // preserved.
        SnapshotStream snapStream = new SnapshotStream(s, Ownership.None);
        snapStream.snapshot();
        if (readOnly) {
            snapStream.freeze();
        }

        return snapStream;
    }

    /**
     * Verifies the file system integrity.
     *
     * Unlike Checkpoint, this method doesn't cause the snapshot to be re-taken.
     *
     * @param reportOutput The destination for the verification report, or
     *            {@code null}
     * @param levels The amount of detail to include in the report (if not
     *            {@code null} )
     * @return {@code true} if the file system is OK, else {@code false} .This
     *         method may place this object into "lock-down", where no further
     *         changes are permitted (if corruption is detected).
     */
    public boolean verify(PrintWriter reportOutput, EnumSet<ReportLevels> levels) {
        boolean ok = true;
        snapStream.freeze();
        // Note the trace stream means that we can guarantee no further stream
        // access after the file system object is disposed - when we dispose it,
        // it forcibly severes the connection to the snapshot stream.
        try (TracingStream traceStream = new TracingStream(snapStream, Ownership.None)) {
            try {
//Debug.println(checkerClass);
                if (!doVerify(checkerClass, traceStream, reportOutput, levels)) {
                    ok = false;
                }
            } catch (Exception e) {
                failureException = e;
                ok = false;
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
        if (ok) {
            snapStream.thaw();
            return true;
        } else {
            lockdown = true;
            if (runGlobalTrace) {
                globalTrace.stop();
                globalTrace.writeToFile(null);
            }

            return false;
        }
    }

    /**
     * Verifies the file system integrity (as seen on disk), and resets the disk
     * checkpoint.
     *
     * This method is automatically invoked according to the CheckpointInterval
     * property, but can be called manually as well.
     *
     * @param reportOutput The destination for the verification report, or
     *            {@code null}
     * @param levels The amount of detail to include in the report (if not
     *            {@code null} )
     */
    public boolean checkpoint(PrintWriter reportOutput, EnumSet<ReportLevels> levels) {
        if (!verify(reportOutput, levels)) {
            return false;
        }

        // Since the file system is OK, reset the snapshot (keeping changes).
        snapStream.forgetSnapshot();
        snapStream.snapshot();
        checkpointBuffer.clear();
        // Set the file system's RNG to a known, but unpredictable, state.
        checkpointRngSeed = masterRng.nextInt();
        liveTarget.getOptions().setRandomNumberGenerator(new Random(checkpointRngSeed));
        // Reset the global trace stream - no longer interested in what it
        // captured.
        if (runGlobalTrace) {
            globalTrace.reset(runGlobalTrace);
        }

        return true;
    }

    /**
     * Generates a diagnostic report by replaying file system activities since
     * the last checkpoint.
     */
    public ReplayReport replayFromLastCheckpoint() {
        if (!doReplayAndVerify(0)) {
            throw new IllegalStateException("Previous checkpoint now shows as invalid, the underlying storage stream may be broken");
        }

        // TODO: do full replay, check for failure - is this reproducible?
        // Binary chop for activity that causes failure
        int lowPoint = 0;
        int highPoint = checkpointBuffer.size();
        int midPoint = highPoint / 2;
        while (highPoint - lowPoint > 1) {
            if (doReplayAndVerify(midPoint)) {
                // This was OK, so must be mid-point or higher
                lowPoint = midPoint;
            } else {
                // Failed, so must be below mid-point
                highPoint = midPoint;
            }
            midPoint = lowPoint + ((highPoint - lowPoint) / 2);
        }
        // Replay again, up to lowPoint - capturing all info desired
        try (SnapshotStream replayCapture = new SnapshotStream(baseStream, Ownership.None)) {
            // Preserve the base stream
            replayCapture.snapshot();
            // Use tracing to capture changes to the stream
            try (TracingStream ts = new TracingStream(replayCapture, Ownership.None)) {
                Exception replayException = null;
                String preVerificationReportString = null;
                try (StringWriter preVerificationReport = new StringWriter()) {
                    try (TFileSystem replayFs = createFileSystem(fileSystemClass, ts)) {
                        // Re-init the RNG to it's state when the checkpoint
                        // started, so we get
                        // reproducibility.
                        replayFs.getOptions().setRandomNumberGenerator(new Random(checkpointRngSeed));
                        Map<String, Object> replayContext = new HashMap<>();
                        for (int i = 0; i < lowPoint - 1; ++i) {
                            checkpointBuffer.get(i).invoke(replayFs, replayContext);
                        }
                        doVerify(checkerClass, ts, new PrintWriter(preVerificationReport), ReportLevels.All);
                        ts.setCaptureStackTraces(true);
                        ts.start();
                        checkpointBuffer.get(lowPoint).invoke(replayFs, replayContext);
                        ts.stop();
                    }
                    preVerificationReportString = preVerificationReport.getBuffer().toString();
                } catch (Exception e) {
                    replayException = e;
                }

                StringWriter verificationReport = new StringWriter();
                boolean failedVerificationOnReplay = doVerify(checkerClass,
                                                              ts,
                                                              new PrintWriter(verificationReport),
                                                              ReportLevels.All);
                return new ReplayReport(failureException,
                                        replayException,
                        globalTrace,
                                        ts,
                                        checkpointBuffer.size(),
                                        lowPoint + 1,
                        totalEventsBeforeLockDown,
                                        preVerificationReportString,
                                        failedVerificationOnReplay,
                                        verificationReport.getBuffer().toString(),
                        lastCheckpointReport);
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Indicates if we're in lock-down (i.e. corruption has been detected).
     */
    boolean getInLockdown() {
        return lockdown;
    }

    /**
     * Replays a specified number of activities.
     *
     * @param activityCount Number of activities to replay
     */
    private boolean doReplayAndVerify(int activityCount) {
        try (SnapshotStream replayCapture = new SnapshotStream(baseStream, Ownership.None)) {
            // Preserve the base stream
            replayCapture.snapshot();
            try {
                try (TFileSystem replayFs = createFileSystem(fileSystemClass, replayCapture)) {
                    // Re-init the RNG to it's state when the checkpoint
                    // started, so we get
                    // reproducibility.
                    replayFs.getOptions().setRandomNumberGenerator(new Random(checkpointRngSeed));
                    Map<String, Object> replayContext = new HashMap<>();
                    for (int i = 0; i < activityCount; ++i) {
                        checkpointBuffer.get(i).invoke(replayFs, replayContext);
                    }
                    return doVerify(checkerClass, replayCapture, null, EnumSet.of(ReportLevels.None));
                }
            } catch (Exception e) {
                return false;
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Used to perform filesystem activities that are exposed in addition to
     * those in the DiscFileSystem class.
     * 
     * The supplied activity may be executed multiple times, against multiple
     * instances of the file system if a replay is requested. Always drive the
     * file system object supplied as a parameter and do not persist references
     * to that object.
     *
     * @param activity The activity to perform, as a delegate
     * @return The value returned from the activity delegate
     */
    public Object performActivity(Activity<TFileSystem> activity) {
        if (lockdown) {
            throw new IllegalStateException("Validator in lock-down, file system corruption has been detected.");
        }

        if (!initialized) {
            initialize();
        }

        totalEventsBeforeLockDown++;
        checkpointBuffer.add(activity);
        boolean doCheckpoint = false;
        try {
            Object retVal = activity.invoke(liveTarget, activityContext);
            doCheckpoint = true;
            return retVal;
        } finally {
            // If a checkpoint is due...
            if (checkpointBuffer.size() >= checkpointPeriod) {
                // Roll over the on-disk trace
                if (runGlobalTrace) {
                    globalTrace.writeToFile("C:" + FS + "temp" + FS + "working" + FS + "trace%3X.log".formatted(numScheduledCheckpoints++));
                }

                // We only do a full checkpoint, if the activity didn't throw an
                // exception. Otherwise, we'll discard all replay info just when
                // the caller might want it. Instead, just do a verify until
                // (and unless), an activity that doesn't throw an exception
                // happens.
                if (doCheckpoint) {
                    checkpointAndThrow();
                } else {
                    verifyAndThrow();
                }
            }
        }
    }

    private void initialize() {
        if (initialized) {
            throw new IllegalStateException();
        }

        snapStream = new SnapshotStream(baseStream, Ownership.None);
        Stream focusStream = snapStream;
        masterRng = new Random(56456456);
        if (runGlobalTrace) {
            globalTrace = new TracingStream(snapStream, Ownership.None);
            globalTrace.setCaptureStackTraces(globalTraceCaptureStackTraces);
            globalTrace.reset(runGlobalTrace);
            globalTrace.writeToFile("C:" + FS + "temp" + FS + "working" + FS + "trace%3X.log".formatted(numScheduledCheckpoints++));
            focusStream = globalTrace;
        }

        checkpointRngSeed = masterRng.nextInt();
        activityContext = new HashMap<>();
        checkpointBuffer = new ArrayList<>();
        liveTarget = createFileSystem(fileSystemClass, focusStream);
        liveTarget.getOptions().setRandomNumberGenerator(new Random(checkpointRngSeed));
        // Take a snapshot, to preserve the stream state before we perform
        // an operation (assumption is that merely creating a file system object
        // (above) is not significant...
        snapStream.snapshot();
        initialized = true;
        // Preliminary test, lets make sure we think the file system's good
        // before we start...
        verifyAndThrow();
    }

    private static <TChecker extends DiscFileSystemChecker> boolean doVerify(Class<TChecker> clazz,
                                                                             Stream s,
                                                                             PrintWriter w,
                                                                             EnumSet<ReportLevels> levels) {
        TChecker checker = createChecker(clazz, s);
        if (w != null) {
            return checker.check(w, levels);
        } else {
            try (NullTextWriter nullWriter = new NullTextWriter()) {
                return checker.check(nullWriter, EnumSet.of(ReportLevels.None));
            }
        }
    }

    private void checkpointAndThrow() {
        try (StringWriter writer = new StringWriter()) {
            boolean passed = checkpoint(new PrintWriter(writer), EnumSet.of(ReportLevels.Errors));
            lastCheckpointReport = writer.getBuffer().toString();
            if (!passed) {
                throw new IllegalStateException("File system failed verification:\n" + lastCheckpointReport,
                        failureException);
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    private void verifyAndThrow() {
        try (StringWriter writer = new StringWriter()) {
            boolean passed = verify(new PrintWriter(writer), EnumSet.of(ReportLevels.Errors));
            lastCheckpointReport = writer.getBuffer().toString();
            if (!passed) {
                throw new IllegalStateException("File system failed verification ", failureException);
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    private static <TFileSystem> TFileSystem createFileSystem(Class<TFileSystem> clazz, Stream stream) {
        try {
            return clazz.getConstructor(Stream.class).newInstance(stream);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | IllegalArgumentException |
                NoSuchMethodException | SecurityException tie) {
            try {
                Field remoteStackTraceString = tie.getClass().getField("setStackTrace");
                remoteStackTraceString.set(tie.getCause(), tie.getCause().getStackTrace());
                throw new dotnet4j.io.IOException(tie.getCause());
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static <TChecker> TChecker createChecker(Class<TChecker> clazz, Stream stream) {
        try {
            return clazz.getConstructor(Stream.class).newInstance(stream);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | IllegalArgumentException |
                NoSuchMethodException | SecurityException tie) {
            try {
                Field remoteStackTraceString = tie.getClass().getField("setStackTrace");
                remoteStackTraceString.set(tie.getCause(), tie.getCause().getStackTrace());
                throw new dotnet4j.io.IOException(tie.getCause());
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Provides a friendly description of the file system type.
     */
    @Override
    public String getFriendlyName() {
        return (String) performActivity((fs, context) -> fs.getFriendlyName());
    }

    /**
     * Indicates whether the file system is read-only or read-write.
     *
     * @return true if the file system is read-write.
     */
    @Override
    public boolean canWrite() {
        return (Boolean) performActivity((fs, context) -> fs.canWrite());
    }

    /**
     * Gets the root directory of the file system.
     */
    @Override
    public DiscDirectoryInfo getRoot() {
        return new DiscDirectoryInfo(this, "");
    }

    /**
     * Copies an existing file to a new file.
     *
     * @param sourceFile The source file
     * @param destinationFile The destination file
     */
    @Override
    public void copyFile(String sourceFile, String destinationFile) {
        performActivity((fs, context) -> {
            try {
                fs.copyFile(sourceFile, destinationFile);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            return null;
        });
    }

    /**
     * Copies an existing file to a new file, allowing overwriting of an
     * existing file.
     *
     * @param sourceFile The source file
     * @param destinationFile The destination file
     * @param overwrite Whether to permit over-writing of an existing file.
     */
    @Override
    public void copyFile(String sourceFile, String destinationFile, boolean overwrite) {
        performActivity((fs, context) -> {
            try {
                fs.copyFile(sourceFile, destinationFile, overwrite);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            return null;
        });
    }

    /**
     * Creates a directory.
     *
     * @param path The path of the new directory
     */
    @Override
    public void createDirectory(String path) {
        performActivity((fs, context) -> {
            try {
                fs.createDirectory(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            return 0;
        });
    }

    /**
     * Deletes a directory.
     *
     * @param path The path of the directory to delete.
     */
    @Override
    public void deleteDirectory(String path) {
        performActivity((fs, context) -> {
            try {
                fs.deleteDirectory(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            return null;
        });
    }

    /**
     * Deletes a file.
     *
     * @param path The path of the file to delete.
     */
    @Override
    public void deleteFile(String path) {
        performActivity((fs, context) -> {
            try {
                fs.deleteFile(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            return null;
        });
    }

    /**
     * Indicates if a directory exists.
     *
     * @param path The path to test
     * @return true if the directory exists
     */
    @Override
    public boolean directoryExists(String path) {
        return (boolean) performActivity((fs, context) -> {
            try {
                return fs.directoryExists(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Indicates if a file exists.
     *
     * @param path The path to test
     * @return true if the file exists
     */
    @Override
    public boolean fileExists(String path) {
        return (boolean) performActivity((fs, context) -> {
            try {
                return fs.fileExists(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Indicates if a file or directory exists.
     *
     * @param path The path to test
     * @return true if the file or directory exists
     */
    @Override
    public boolean exists(String path) {
        return (Boolean) performActivity((fs, context) -> {
            try {
                return fs.exists(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Gets the names of subdirectories in a specified directory.
     *
     * @param path The path to search.
     * @return list of directories.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<String> getDirectories(String path) {
        return (List) performActivity((fs, context) -> {
            try {
                return fs.getDirectories(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Gets the names of subdirectories in a specified directory matching a
     * specified search pattern.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return list of directories matching the search pattern.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<String> getDirectories(String path, String searchPattern) {
        return (List) performActivity((fs, context) -> {
            try {
                return fs.getDirectories(path, searchPattern);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Gets the names of subdirectories in a specified directory matching a
     * specified search pattern, using a value to determine whether to search
     * subdirectories.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @param searchOption Indicates whether to search subdirectories.
     * @return list of directories matching the search pattern.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<String> getDirectories(String path, String searchPattern, String searchOption) {
        return (List) performActivity((fs, context) -> {
            try {
                return fs.getDirectories(path, searchPattern, searchOption);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Gets the names of files in a specified directory.
     *
     * @param path The path to search.
     * @return list of files.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<String> getFiles(String path) {
        return (List) performActivity((fs, context) -> {
            try {
                return fs.getFiles(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Gets the names of files in a specified directory.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return list of files matching the search pattern.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<String> getFiles(String path, String searchPattern) {
        return (List) performActivity((fs, context) -> {
            try {
                return fs.getFiles(path, searchPattern);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Gets the names of files in a specified directory matching a specified
     * search pattern, using a value to determine whether to search
     * subdirectories.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @param searchOption Indicates whether to search subdirectories.
     * @return list of files matching the search pattern.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<String> getFiles(String path, String searchPattern, String searchOption) {
        return (List) performActivity((fs, context) -> {
            try {
                return fs.getFiles(path, searchPattern, searchOption);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Gets the names of all files and subdirectories in a specified directory.
     *
     * @param path The path to search.
     * @return list of files and subdirectories matching the search pattern.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<String> getFileSystemEntries(String path) {
        return (List) performActivity((fs, context) -> {
            try {
                return fs.getFileSystemEntries(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Gets the names of files and subdirectories in a specified directory
     * matching a specified search pattern.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return list of files and subdirectories matching the search pattern.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<String> getFileSystemEntries(String path, String searchPattern) {
        return (List) performActivity((fs, context) -> {
            try {
                return fs.getFileSystemEntries(path, searchPattern);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Moves a directory.
     *
     * @param sourceDirectoryName The directory to move.
     * @param destinationDirectoryName The target directory name.
     */
    @Override
    public void moveDirectory(String sourceDirectoryName, String destinationDirectoryName) {
        performActivity((fs, context) -> {
            try {
                fs.moveDirectory(sourceDirectoryName, destinationDirectoryName);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            return null;
        });
    }

    /**
     * Moves a file.
     *
     * @param sourceName The file to move.
     * @param destinationName The target file name.
     */
    @Override
    public void moveFile(String sourceName, String destinationName) {
        performActivity((fs, context) -> {
            try {
                fs.moveFile(sourceName, destinationName);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            return null;
        });
    }

    /**
     * Moves a file, allowing an existing file to be overwritten.
     *
     * @param sourceName The file to move.
     * @param destinationName The target file name.
     * @param overwrite Whether to permit a destination file to be overwritten
     */
    @Override
    public void moveFile(String sourceName, String destinationName, boolean overwrite) {
        performActivity((fs, context) -> {
            try {
                fs.moveFile(sourceName, destinationName, overwrite);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            return null;
        });
    }

    // delegate SparseStream StreamOpenFn(TFileSystem fs);

    /**
     * Opens the specified file.
     *
     * @param path The full path of the file to open.
     * @param mode The file mode for the created stream.
     * @return The new stream.
     */
    @Override
    public SparseStream openFile(String path, FileMode mode) {
        // This delegate can be used at any time the wrapper needs it, if it's
        // in a
        // 'replay' but the real file open isn't.
        ValidatingFileSystemWrapperStream<TFileSystem, TChecker> wrapper = new ValidatingFileSystemWrapperStream<>(this, fs -> {
            try {
                return fs.openFile(path, mode);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
        performActivity((fs, context) -> {
            try {
                SparseStream s = fs.openFile(path, mode);
                wrapper.setNativeStream(context, s);
                return s;
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
        return wrapper;
    }

    /**
     * Opens the specified file.
     *
     * @param path The full path of the file to open.
     * @param mode The file mode for the created stream.
     * @param access The access permissions for the created stream.
     * @return The new stream.
     */
    @Override
    public SparseStream openFile(String path, FileMode mode, FileAccess access) {
        // This delegate can be used at any time the wrapper needs it, if it's
        // in a
        // 'replay' but the real file open isn't.
        ValidatingFileSystemWrapperStream<TFileSystem, TChecker> wrapper = new ValidatingFileSystemWrapperStream<>(this, fs -> {
            try {
                return fs.openFile(path, mode, access);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
        performActivity((fs, context) -> {
            try {
                Stream s = fs.openFile(path, mode, access);
                wrapper.setNativeStream(context, s);
                return s;
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
        return wrapper;
    }

    /**
     * Gets the attributes of a file or directory.
     *
     * @param path The file or directory to inspect
     * @return The attributes of the file or directory
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map<String, Object> getAttributes(String path) {
        return (Map) performActivity((fs, context) -> {
            try {
                return fs.getAttributes(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Sets the attributes of a file or directory.
     *
     * @param path The file or directory to change
     * @param newValue The new attributes of the file or directory
     */
    @Override
    public void setAttributes(String path, Map<String, Object> newValue) {
        performActivity((fs, context) -> {
            try {
                fs.setAttributes(path, newValue);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            return null;
        });
    }

    /**
     * Gets the creation time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory
     * @return The creation time.
     */
    @Override
    public long getCreationTime(String path) {
        return (long) performActivity((fs, context) -> {
            try {
                return fs.getCreationTime(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Sets the creation time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override
    public void setCreationTime(String path, long newTime) throws IOException {
        performActivity((fs, context) -> {
            try {
                fs.setCreationTime(path, newTime);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            return null;
        });
    }

    /**
     * Gets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The creation time.
     */
    @Override
    public long getCreationTimeUtc(String path) {
        return (long) performActivity((fs, context) -> {
            try {
                return fs.getCreationTimeUtc(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Sets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override
    public void setCreationTimeUtc(String path, long newTime) {
        performActivity((fs, context) -> {
            try {
                fs.setCreationTimeUtc(path, newTime);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            return null;
        });
    }

    /**
     * Gets the last access time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory
     * @return The last access time
     */
    @Override
    public long getLastAccessTime(String path) {
        return (long) performActivity((fs, context) -> {
            try {
                return fs.getLastAccessTime(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Sets the last access time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override
    public void setLastAccessTime(String path, long newTime) {
        performActivity((fs, context) -> {
            try {
                fs.setLastAccessTime(path, newTime);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            return null;
        });
    }

    /**
     * Gets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory
     * @return The last access time
     */
    @Override
    public long getLastAccessTimeUtc(String path) {
        return (long) performActivity((fs, context) -> {
            try {
                return fs.getLastAccessTimeUtc(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Sets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override
    public void setLastAccessTimeUtc(String path, long newTime) {
        performActivity((fs, context) -> {
            try {
                fs.setLastAccessTimeUtc(path, newTime);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            return null;
        });
    }

    /**
     * Gets the last modification time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory
     * @return The last write time
     */
    @Override
    public long getLastWriteTime(String path) {
        return (long) performActivity((fs, context) -> {
            try {
                return fs.getLastWriteTime(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Sets the last modification time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override
    public void setLastWriteTime(String path, long newTime) {
        performActivity((fs, context) -> {
            try {
                fs.setLastWriteTime(path, newTime);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            return null;
        });
    }

    /**
     * Gets the last modification time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory
     * @return The last write time
     */
    @Override
    public long getLastWriteTimeUtc(String path) {
        return (long) performActivity((fs, context) -> {
            try {
                return fs.getLastWriteTime(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Sets the last modification time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override
    public void setLastWriteTimeUtc(String path, long newTime) {
        performActivity((fs, context) -> {
            try {
                fs.setLastWriteTimeUtc(path, newTime);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            return null;
        });
    }

    /**
     * Gets the length of a file.
     *
     * @param path The path to the file
     * @return The length in bytes
     */
    @Override
    public long getFileLength(String path) {
        return (long) performActivity((fs, context) -> {
            try {
                return fs.getFileLength(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Gets an object representing a possible file.
     *
     * @param path The file path
     * @return The representing objectThe file does not need to exist
     */
    @Override
    public DiscFileInfo getFileInfo(String path) {
        return (DiscFileInfo) performActivity((fs, context) -> {
            try {
                return fs.getFileInfo(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Gets an object representing a possible directory.
     *
     * @param path The directory path
     * @return The representing objectThe directory does not need to exist
     */
    @Override
    public DiscDirectoryInfo getDirectoryInfo(String path) {
        return (DiscDirectoryInfo) performActivity((fs, context) -> {
            try {
                return fs.getDirectoryInfo(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Gets an object representing a possible file system object (file or
     * directory).
     *
     * @param path The file system path
     * @return The representing objectThe file system object does not need to
     *         exist
     */
    @Override
    public DiscFileSystemInfo getFileSystemInfo(String path) {
        return (DiscFileSystemInfo) performActivity((fs, context) -> {
            try {
                return fs.getFileSystemInfo(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
    }

    /**
     * Gets the Volume Label.
     */
    @Override
    public String getVolumeLabel() {
        return (String) performActivity((fs, context) -> fs.getVolumeLabel());
    }

    @Override
    public long getSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getUsedSpace() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getAvailableSpace() {
        throw new UnsupportedOperationException();
    }
}
