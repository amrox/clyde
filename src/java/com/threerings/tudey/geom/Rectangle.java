//
// $Id$

package com.threerings.tudey.geom;

/**
 * An axis-aligned rectangle.
 */
public final class Rectangle extends Shape
{
    /**
     * Creates a rectangle with the specified minimum and maximum extents.
     */
    public Rectangle (float minX, float minY, float maxX, float maxY)
    {
        set(minX, minY, maxX, maxY);
    }

    /**
     * Copy constructor.
     */
    public Rectangle (Rectangle other)
    {
        set(other);
    }

    /**
     * Creates an uninitialized rectangle.
     */
    public Rectangle ()
    {
    }

    /**
     * Copies the parameters of another rectangle.
     */
    public void set (Rectangle other)
    {
        set(other.getMinimumX(), other.getMinimumY(), other.getMaximumX(), other.getMaximumY());
    }

    /**
     * Sets the parameters of the rectangle.
     */
    public void set (float minX, float minY, float maxX, float maxY)
    {
        if (_minX == minX && _minY == minY && _maxX == maxX && _maxY == maxY) {
            return;
        }
        willMove();
        _minX = minX;
        _minY = minY;
        _maxX = maxX;
        _maxY = maxY;
        _bounds.set(_minX, _minY, _maxX, _maxY);
        didMove();
    }

    /**
     * Returns the minimum x extent of the rectangle.
     */
    public float getMinimumX ()
    {
        return _minX;
    }

    /**
     * Returns the minimum y extent of the rectangle.
     */
    public float getMinimumY ()
    {
        return _minY;
    }

    /**
     * Returns the maximum x extent of the rectangle.
     */
    public float getMaximumX ()
    {
        return _maxX;
    }

    /**
     * Returns the maximum y extent of the rectangle.
     */
    public float getMaximumY ()
    {
        return _maxY;
    }

    @Override // documentation inherited
    public boolean intersects (Shape other)
    {
        return other.checkIntersects(this);
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Point point)
    {
        float x = point.getX();
        float y = point.getY();
        return x >= _minX && x <= _maxX && y >= _minY && y <= _maxY;
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Line line)
    {
        return false; // TODO
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Circle circle)
    {
        return false; // TODO
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Rectangle rectangle)
    {
        return _minX <= rectangle.getMaximumX() && _minY <= rectangle.getMaximumY() &&
            _maxX >= rectangle.getMinimumX() && _maxY >= rectangle.getMinimumY();
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Capsule capsule)
    {
        return capsule.checkIntersects(this);
    }

    /** The minimum extent of the rectangle. */
    protected float _minX, _minY;

    /** The maximum extent of the rectangle. */
    protected float _maxX, _maxY;
}
