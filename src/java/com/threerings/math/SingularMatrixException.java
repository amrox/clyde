//
// $Id$

package com.threerings.math;

/**
 * Thrown when inversion is attempted on a singular (non-invertible) matrix.
 */
public class SingularMatrixException extends RuntimeException
{
    /**
     * Creates a new exception.
     */
    public SingularMatrixException ()
    {
    }

    /**
     * Creates a new exception with the provided message.
     */
    public SingularMatrixException (String message)
    {
        super(message);
    }
}
