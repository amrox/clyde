//
// $Id$

package com.threerings.tudey.geom;

/**
 * A simple point shape.
 */
public final class Point extends Shape
{
    /**
     * Creates a point with the specified parameters.
     */
    public Point (float x, float y)
    {
        set(x, y);
    }

    /**
     * Copy constructor.
     */
    public Point (Point other)
    {
        set(other);
    }

    /**
     * Creates an uninitialized point.
     */
    public Point ()
    {
    }

    /**
     * Copies the parameters of another point.
     */
    public void set (Point other)
    {
        set(other.getX(), other.getY());
    }

    /**
     * Sets the parameters of the capsule.
     */
    public void set (float x, float y)
    {
        if (_x == x && _y == y) {
            return;
        }
        willMove();
        _x = x;
        _y = y;
        _bounds.set(_x, _y, _x, _y);
        didMove();
    }

    /**
     * Returns the x coordinate of the point.
     */
    public float getX ()
    {
        return _x;
    }

    /**
     * Returns the y coordinate of the point.
     */
    public float getY ()
    {
        return _y;
    }

    @Override // documentation inherited
    public boolean intersects (Shape other)
    {
        return other.checkIntersects(this);
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Circle circle)
    {
        return circle.checkIntersects(this);
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Rectangle rectangle)
    {
        return rectangle.checkIntersects(this);
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Capsule capsule)
    {
        return capsule.checkIntersects(this);
    }

    /** The location of the point. */
    protected float _x, _y;
}
