
package DiscUtils.Diagnostics;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import DiscUtils.Core.DiscDirectoryInfo;
import DiscUtils.Core.DiscFileInfo;
import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.DiscFileSystemChecker;
import DiscUtils.Core.DiscFileSystemInfo;
import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Core.ReportLevels;
import DiscUtils.Streams.SnapshotStream;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Util.Ownership;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.Stream;


/**
 * Class that wraps a {@link DiscFileSystem}, validating file system integrity. The
 * concrete type of file system to validate.The concrete type of the file system
 * checker.
 */
public class ValidatingFileSystem<TFileSystem extends DiscFileSystem & IDiagnosticTraceable, TChecker extends DiscFileSystemChecker>
        extends
        DiscFileSystem {

    private Stream _baseStream;

    // -------------------------------------
    // CONFIG

    /**
     * How often a check point is run (in number of 'activities').
     */
    private int _checkpointPeriod = 1;

    /**
     * Indicates if a read/write trace should run all the time.
     */
    private boolean _runGlobalTrace;

    /**
     * Indicates whether to capture full stack traces when doing a global trace.
     */
    private boolean _globalTraceCaptureStackTraces;

    // -------------------------------------
    // INITIALIZED STATE

    private SnapshotStream _snapStream;

    private TFileSystem _liveTarget;

    private boolean _initialized;

    private Map<String, Object> _activityContext;

    private TracingStream _globalTrace;

    /**
     * The random number generator used to generate seeds for checkpoint-specific
     * generators.
     */
    private Random _masterRng;

    // -------------------------------------
    // RUNNING STATE

    /**
     * Activities get logged here until a checkpoint is hit, so we can replay
     * between checkpoints.
     */
    private List<Activity<TFileSystem>> _checkpointBuffer;

    /**
     * The random number generator seed value (set at checkpoint).
     */
    private int _checkpointRngSeed;

    /**
     * The last verification report generated at a scheduled checkpoint.
     */
    private String _lastCheckpointReport;

    /**
     * Flag set when a validation failure is observed, preventing further file system activity.
     */
    private boolean _lockdown;

    /**
     * The exception (if any) that indicated the file system was corrupt.
     */
    private Exception _failureException;

    /**
     * The total number of events carried out before lock-down occured.
     */
    private long _totalEventsBeforeLockDown;

    private int _numScheduledCheckpoints;

    private Class<TFileSystem> fileSystemClass;

    private Class<TChecker> checkerClass;

    /**
     * Creates a new instance.
     *
     * @param stream A stream containing an existing (valid) file system.The new
     *                   instance does not take ownership of the stream.
     */
    public ValidatingFileSystem(Class<TFileSystem> fileSystemClass, Class<TChecker> checkerClass, Stream stream) {
        _baseStream = stream;
        this.fileSystemClass = fileSystemClass;
        this.checkerClass = checkerClass;
    }

    /**
     * Disposes of this instance, forcing a checkpoint if one is outstanding.
     */
    public void close() throws IOException {
        try {
            checkpointAndThrow();
        } finally {
            if (_globalTrace != null) {
                _globalTrace.close();
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
        return _checkpointPeriod;
    }

    public void setCheckpointInterval(int value) {
        _checkpointPeriod = value;
    }

    /**
     * Gets and sets whether an inter-checkpoint trace should be run (useful for
     * non-reproducible failures).
     */
    public boolean getRunGlobalIOTrace() {
        return _runGlobalTrace;
    }

    public void setRunGlobalIOTrace(boolean value) {
        _runGlobalTrace = value;
    }

    /**
     * Gets and sets whether a global I/O trace should be run (useful for
     * non-reproducible failures).
     */
    public boolean getGlobalIOTraceCapturesStackTraces() {
        return _globalTraceCaptureStackTraces;
    }

    public void setGlobalIOTraceCapturesStackTraces(boolean value) {
        _globalTraceCaptureStackTraces = value;
    }

    /**
     * Gets access to a view of the stream being validated, forcing 'lock-down'.
     *
     * @param view     The view to open.
     * @param readOnly Whether to fail changes to the stream.
     * @return The new stream, the caller must dispose.Always use this method to
     *         access the stream, rather than keeping a reference to the stream
     *         passed to the constructor. This method never lets changes through to
     *         the underlying stream, so ensures the integrity of the underlying
     *         stream. Any changes made to the returned stream are held as a private
     *         delta and discarded when the stream is disposed.
     */
    public Stream openStreamView(StreamView view, boolean readOnly) {
        // Prevent further changes.
        _lockdown = true;
        Stream s;
        // Perversely, the snap stream has the current view (squirrelled away in it's
        // delta). The base stream is actually the stream state back at the last
        // checkpoint.
        if (view == StreamView.Current) {
            s = _snapStream;
        } else {
            s = _baseStream;
        }
        // Return a protective wrapping stream, so the original stream is preserved.
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
     * @param reportOutput The destination for the verification report, or
     *                         {@code null}
     *
     * @param levels       The amount of detail to include in the report (if not
     *                         {@code null} )
     * @return {@code true} if the file system is OK, else {@code false} .This
     *         method may place this object into "lock-down", where no further
     *         changes are permitted (if corruption is detected). Unlike Checkpoint,
     *         this method doesn't cause the snapshot to be re-taken.
     */
    public boolean verify(PrintWriter reportOutput, ReportLevels levels) {
        boolean ok = true;
        _snapStream.freeze();
        // Note the trace stream means that we can guarantee no further stream access
        // after the file system object is disposed - when we dispose it, it forcibly
        // severes the connection to the snapshot stream.
        try (TracingStream traceStream = new TracingStream(_snapStream, Ownership.None)) {
            try {
                if (!doVerify(checkerClass, traceStream, reportOutput, levels)) {
                    ok = false;
                }
            } catch (Exception e) {
                _failureException = e;
                ok = false;
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
        if (ok) {
            _snapStream.thaw();
            return true;
        } else {
            _lockdown = true;
            if (_runGlobalTrace) {
                _globalTrace.stop();
                _globalTrace.writeToFile(null);
            }

            return false;
        }
    }

    /**
     * Verifies the file system integrity (as seen on disk), and resets the disk
     * checkpoint.
     *
     * @param reportOutput The destination for the verification report, or
     *                         {@code null}
     *
     * @param levels       The amount of detail to include in the report (if not
     *                         {@code null} )This method is automatically invoked
     *                         according to the CheckpointInterval property, but can
     *                         be called manually as well.
     */
    public boolean checkpoint(PrintWriter reportOutput, ReportLevels levels) {
        if (!verify(reportOutput, levels)) {
            return false;
        }

        // Since the file system is OK, reset the snapshot (keeping changes).
        _snapStream.forgetSnapshot();
        _snapStream.snapshot();
        _checkpointBuffer.clear();
        // Set the file system's RNG to a known, but unpredictable, state.
        _checkpointRngSeed = _masterRng.nextInt();
        _liveTarget.getOptions().setRandomNumberGenerator(new Random(_checkpointRngSeed));
        // Reset the global trace stream - no longer interested in what it captured.
        if (_runGlobalTrace) {
            _globalTrace.reset(_runGlobalTrace);
        }

        return true;
    }

    /**
     * Generates a diagnostic report by replaying file system activities since the
     * last checkpoint.
     */
    public ReplayReport replayFromLastCheckpoint() {
        if (!doReplayAndVerify(0)) {
            throw new IllegalStateException("Previous checkpoint now shows as invalid, the underlying storage stream may be broken");
        }

        // TODO - do full replay, check for failure - is this reproducible?
        // Binary chop for activity that causes failure
        int lowPoint = 0;
        int highPoint = _checkpointBuffer.size();
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
        try (SnapshotStream replayCapture = new SnapshotStream(_baseStream, Ownership.None)) {
            // Preserve the base stream
            replayCapture.snapshot();
            // Use tracing to capture changes to the stream
            try (TracingStream ts = new TracingStream(replayCapture, Ownership.None)) {
                Exception replayException = null;
                String preVerificationReportString = null;
                try (StringWriter preVerificationReport = new StringWriter()) {
                    TFileSystem replayFs = createFileSystem(fileSystemClass, ts);
                    try {
                        // Re-init the RNG to it's state when the checkpoint started, so we get
                        // reproducibility.
                        replayFs.getOptions().setRandomNumberGenerator(new Random(_checkpointRngSeed));
                        Map<String, Object> replayContext = new HashMap<>();
                        for (int i = 0; i < lowPoint - 1; ++i) {
                            _checkpointBuffer.get(i).invoke(replayFs, replayContext);
                        }
                        doVerify(checkerClass, ts, new PrintWriter(preVerificationReport), ReportLevels.All);
                        ts.setCaptureStackTraces(true);
                        ts.start();
                        _checkpointBuffer.get(lowPoint).invoke(replayFs, replayContext);
                        ts.stop();
                    } finally {
                        if (replayFs != null)
                            replayFs.close();
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
                return new ReplayReport(_failureException,
                                        replayException,
                                        _globalTrace,
                                        ts,
                                        _checkpointBuffer.size(),
                                        lowPoint + 1,
                                        _totalEventsBeforeLockDown,
                                        preVerificationReportString,
                                        failedVerificationOnReplay,
                                        verificationReport.getBuffer().toString(),
                                        _lastCheckpointReport);
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Indicates if we're in lock-down (i.e. corruption has been detected).
     */
    public boolean getInLockdown() {
        return _lockdown;
    }

    /**
     * Replays a specified number of activities.
     * 
     * @param activityCount Number of activities to replay
     */
    private boolean doReplayAndVerify(int activityCount) {
        try (SnapshotStream replayCapture = new SnapshotStream(_baseStream, Ownership.None)) {
            // Preserve the base stream
            replayCapture.snapshot();
            try {
                try (TFileSystem replayFs = createFileSystem(fileSystemClass, replayCapture)) {
                    // Re-init the RNG to it's state when the checkpoint started, so we get
                    // reproducibility.
                    replayFs.getOptions().setRandomNumberGenerator(new Random(_checkpointRngSeed));
                    Map<String, Object> replayContext = new HashMap<>();
                    for (int i = 0; i < activityCount; ++i) {
                        _checkpointBuffer.get(i).invoke(replayFs, replayContext);
                    }
                    return doVerify(checkerClass, replayCapture, null, ReportLevels.None);
                }
            } catch (Exception __dummyCatchVar0) {
                return false;
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Used to perform filesystem activities that are exposed in addition to those
     * in the DiscFileSystem class.
     * 
     * @param activity The activity to perform, as a delegate
     * 
     * @return The value returned from the activity delegateThe supplied activity
     *         may be executed multiple times, against multiple instances of the
     *         file system if a replay is requested. Always drive the file system
     *         object supplied as a parameter and do not persist references to that
     *         object.
     */
    public Object performActivity(Activity<TFileSystem> activity) {
        if (_lockdown) {
            throw new IllegalStateException("Validator in lock-down, file system corruption has been detected.");
        }

        if (!_initialized) {
            initialize();
        }

        _totalEventsBeforeLockDown++;
        _checkpointBuffer.add(activity);
        boolean doCheckpoint = false;
        try {
            Object retVal = activity.invoke(_liveTarget, _activityContext);
            doCheckpoint = true;
            return retVal;
        } finally {
            // If a checkpoint is due...
            if (_checkpointBuffer.size() >= _checkpointPeriod) {
                // Roll over the on-disk trace
                if (_runGlobalTrace) {
                    _globalTrace.writeToFile(String.format("C:\\temp\\working\\trace%3X.log", _numScheduledCheckpoints++));
                }

                // We only do a full checkpoint, if the activity didn't throw an exception.
                // Otherwise,
                // we'll discard all replay info just when the caller might want it. Instead,
                // just do a
                // verify until (and unless), an activity that doesn't throw an exception
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
        if (_initialized) {
            throw new IllegalStateException();
        }

        _snapStream = new SnapshotStream(_baseStream, Ownership.None);
        Stream focusStream = _snapStream;
        _masterRng = new Random(56456456);
        if (_runGlobalTrace) {
            _globalTrace = new TracingStream(_snapStream, Ownership.None);
            _globalTrace.setCaptureStackTraces(_globalTraceCaptureStackTraces);
            _globalTrace.reset(_runGlobalTrace);
            _globalTrace.writeToFile(String.format("C:\\temp\\working\\trace%3X.log", _numScheduledCheckpoints++));
            focusStream = _globalTrace;
        }

        _checkpointRngSeed = _masterRng.nextInt();
        _activityContext = new HashMap<>();
        _checkpointBuffer = new ArrayList<>();
        _liveTarget = createFileSystem(fileSystemClass, focusStream);
        _liveTarget.getOptions().setRandomNumberGenerator(new Random(_checkpointRngSeed));
        // Take a snapshot, to preserve the stream state before we perform
        // an operation (assumption is that merely creating a file system object
        // (above) is not significant...
        _snapStream.snapshot();
        _initialized = true;
        // Preliminary test, lets make sure we think the file system's good before we
        // start...
        verifyAndThrow();
    }

    private static <TChecker extends DiscFileSystemChecker> boolean doVerify(Class<TChecker> clazz,
                                                                             Stream s,
                                                                             PrintWriter w,
                                                                             ReportLevels levels) {
        TChecker checker = createChecker(clazz, s);
        if (w != null) {
            return checker.check(w, levels);
        } else {
            NullTextWriter nullWriter = new NullTextWriter();
            try {
                return checker.check(nullWriter, ReportLevels.None);
            } finally {
                if (nullWriter != null)
                    nullWriter.close();
            }
        }
    }

    private void checkpointAndThrow() {
        try (StringWriter writer = new StringWriter()) {
            boolean passed = checkpoint(new PrintWriter(writer), ReportLevels.Errors);
            _lastCheckpointReport = writer.getBuffer().toString();
            if (!passed) {
                throw new IllegalStateException("File system failed verification ", _failureException);
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    private void verifyAndThrow() {
        try (StringWriter writer = new StringWriter()) {
            boolean passed = verify(new PrintWriter(writer), ReportLevels.Errors);
            _lastCheckpointReport = writer.getBuffer().toString();
            if (!passed) {
                throw new IllegalStateException("File system failed verification ", _failureException);
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    private static <TFileSystem> TFileSystem createFileSystem(Class<TFileSystem> clazz, Stream stream) {
        try {
            return clazz.getConstructor(Stream.class).newInstance(stream);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | IllegalArgumentException
                | NoSuchMethodException | SecurityException tie) {
            try {
                Field remoteStackTraceString = Exception.class.getClass().getField("_remoteStackTraceString");
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
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | IllegalArgumentException
                | NoSuchMethodException | SecurityException tie) {
            try {
                Field remoteStackTraceString = Exception.class.getClass().getField("_remoteStackTraceString");
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
    public String getFriendlyName() {
        return (String) performActivity((fs, context) -> {
            return fs.getFriendlyName();
        });
    }

    /**
     * Indicates whether the file system is read-only or read-write.
     *
     * @return true if the file system is read-write.
     */
    public boolean canWrite() {
        return (Boolean) performActivity((fs, context) -> {
            return fs.canWrite();
        });
    }

    /**
     * Gets the root directory of the file system.
     */
    public DiscDirectoryInfo getRoot() {
        return new DiscDirectoryInfo(this, "");
    }

    /**
     * Copies an existing file to a new file.
     *
     * @param sourceFile      The source file
     * @param destinationFile The destination file
     */
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
     * Copies an existing file to a new file, allowing overwriting of an existing
     * file.
     *
     * @param sourceFile      The source file
     * @param destinationFile The destination file
     * @param overwrite       Whether to permit over-writing of an existing file.
     */
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
     * @return Array of directories.
     */
    public List<String> getDirectories(String path) {
        return List.class.cast(performActivity((fs, context) -> {
            try {
                return fs.getDirectories(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }));
    }

    /**
     * Gets the names of subdirectories in a specified directory matching a
     * specified search pattern.
     *
     * @param path          The path to search.
     * @param searchPattern The search string to match against.
     * @return Array of directories matching the search pattern.
     */
    public List<String> getDirectories(String path, String searchPattern) {
        return List.class.cast(performActivity((fs, context) -> {
            try {
                return fs.getDirectories(path, searchPattern);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }));
    }

    /**
     * Gets the names of subdirectories in a specified directory matching a
     * specified search pattern, using a value to determine whether to search
     * subdirectories.
     *
     * @param path          The path to search.
     * @param searchPattern The search string to match against.
     * @param searchOption  Indicates whether to search subdirectories.
     * @return Array of directories matching the search pattern.
     */
    public List<String> getDirectories(String path, String searchPattern, String searchOption) {
        return List.class.cast(performActivity((fs, context) -> {
            try {
                return fs.getDirectories(path, searchPattern, searchOption);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }));
    }

    /**
     * Gets the names of files in a specified directory.
     *
     * @param path The path to search.
     * @return Array of files.
     */
    public List<String> getFiles(String path) {
        return List.class.cast(performActivity((fs, context) -> {
            try {
                return fs.getFiles(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }));
    }

    /**
     * Gets the names of files in a specified directory.
     *
     * @param path          The path to search.
     * @param searchPattern The search string to match against.
     * @return Array of files matching the search pattern.
     */
    public List<String> getFiles(String path, String searchPattern) {
        return List.class.cast(performActivity((fs, context) -> {
            try {
                return fs.getFiles(path, searchPattern);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }));
    }

    /**
     * Gets the names of files in a specified directory matching a specified search
     * pattern, using a value to determine whether to search subdirectories.
     *
     * @param path          The path to search.
     * @param searchPattern The search string to match against.
     * @param searchOption  Indicates whether to search subdirectories.
     * @return Array of files matching the search pattern.
     */
    public List<String> getFiles(String path, String searchPattern, String searchOption) {
        return List.class.cast(performActivity((fs, context) -> {
            try {
                return fs.getFiles(path, searchPattern, searchOption);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }));
    }

    /**
     * Gets the names of all files and subdirectories in a specified directory.
     *
     * @param path The path to search.
     * @return Array of files and subdirectories matching the search pattern.
     */
    public List<String> getFileSystemEntries(String path) {
        return List.class.cast(performActivity((fs, context) -> {
            try {
                return fs.getFileSystemEntries(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }));
    }

    /**
     * Gets the names of files and subdirectories in a specified directory matching
     * a specified search pattern.
     *
     * @param path          The path to search.
     * @param searchPattern The search string to match against.
     * @return Array of files and subdirectories matching the search pattern.
     */
    public List<String> getFileSystemEntries(String path, String searchPattern) {
        return List.class.cast(performActivity((fs, context) -> {
            try {
                return fs.getFileSystemEntries(path, searchPattern);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }));
    }

    /**
     * Moves a directory.
     *
     * @param sourceDirectoryName      The directory to move.
     * @param destinationDirectoryName The target directory name.
     */
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
     * @param sourceName      The file to move.
     * @param destinationName The target file name.
     */
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
     * @param sourceName      The file to move.
     * @param destinationName The target file name.
     * @param overwrite       Whether to permit a destination file to be overwritten
     */
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
    public SparseStream openFile(String path, FileMode mode) {
        // This delegate can be used at any time the wrapper needs it, if it's in a
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
     * @param path   The full path of the file to open.
     * @param mode   The file mode for the created stream.
     * @param access The access permissions for the created stream.
     * @return The new stream.
     */
    public SparseStream openFile(String path, FileMode mode, FileAccess access) {
        // This delegate can be used at any time the wrapper needs it, if it's in a
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
    public Map<String, Object> getAttributes(String path) {
        return Map.class.cast(performActivity((fs, context) -> {
            try {
                return fs.getAttributes(path);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }));
    }

    /**
     * Sets the attributes of a file or directory.
     *
     * @param path     The file or directory to change
     * @param newValue The new attributes of the file or directory
     */
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
     * @param path    The path of the file or directory.
     * @param newTime The new time to set.
     */
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
     * @param path    The path of the file or directory.
     * @param newTime The new time to set.
     */
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
     * @param path    The path of the file or directory.
     * @param newTime The new time to set.
     */
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
     * @param path    The path of the file or directory.
     * @param newTime The new time to set.
     */
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
     * @param path    The path of the file or directory.
     * @param newTime The new time to set.
     */
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
     * @param path    The path of the file or directory.
     * @param newTime The new time to set.
     */
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
     * @return The representing objectThe file system object does not need to exist
     */
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
    public String getVolumeLabel() {
        return (String) performActivity((fs, context) -> {
            return fs.getVolumeLabel();
        });
    }

    public long getSize() {
        throw new UnsupportedOperationException();
    }

    public long getUsedSpace() {
        throw new UnsupportedOperationException();
    }

    public long getAvailableSpace() {
        throw new UnsupportedOperationException();
    }
}
