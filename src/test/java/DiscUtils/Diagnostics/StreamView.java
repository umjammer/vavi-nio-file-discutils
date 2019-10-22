
package DiscUtils.Diagnostics;

/**
 * Enumeration of stream views that can be requested.
 */
public enum StreamView {
    /**
     * The current state of the stream under test.
     */
    Current,
    /**
     * The state of the stream at the last good checkpoint.
     */
    LastCheckpoint
}
