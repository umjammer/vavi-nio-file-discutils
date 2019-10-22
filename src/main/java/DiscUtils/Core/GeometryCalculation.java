
package DiscUtils.Core;

/**
 * Delegate for calculating a disk geometry from a capacity.
 */
@FunctionalInterface
public interface GeometryCalculation {
    /**
     * @param capacity The disk capacity to convert.
     * @return The appropriate geometry for the disk.
     */
    Geometry invoke(long capacity);
}
