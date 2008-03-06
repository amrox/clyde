//
// $Id$

package com.threerings.opengl.gui;

import org.lwjgl.opengl.GL11;

import com.threerings.opengl.renderer.Camera;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.Renderable;

import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.gui.util.Rectangle;

/**
 * Displays 3D geometry inside a normal user interface.
 */
public class GeomView extends Component
{
    /**
     * Creates a view with no configured geometry. Geometry can be set later with {@link
     * #setGeometry}.
     */
    public GeomView ()
    {
        this(null);
    }

    /**
     * Creates a view with the specified {@link Renderable} to be rendered.
     */
    public GeomView (Renderable geom)
    {
        _geom = geom;
    }

    /**
     * Returns the camera used when rendering our geometry.
     */
    public Camera getCamera ()
    {
        if (_camera == null) {
            _camera = createCamera();
        }
        return _camera;
    }

    /**
     * Configures the spatial to be rendered by this view.
     */
    public void setGeometry (Renderable geom)
    {
        _geom = geom;
    }

    /**
     * Returns the geometry rendered by this view.
     */
    public Renderable getGeometry()
    {
    	return _geom;
    }

    /**
     * Called every frame (while we're added to the view hierarchy) by the {@link Root}.
     */
    public void update (float frameTime)
    {
        if (_geom != null) {
            // _geom.updateGeometricState(frameTime, true);
        }
    }

    // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _root = getWindow().getRoot();
        _root.registerGeomView(this);
    }

    // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _root.unregisterGeomView(this);
        _root = null;
    }

    // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);
        if (_geom == null) {
            return;
        }

    }

    /**
     * Called to create and configure the camera that we'll use when rendering our geometry.
     */
    protected Camera createCamera ()
    {
        return null;
    }

    protected Root _root;
    protected Camera _camera;
    protected Renderable _geom;
    protected int _swidth, _sheight;
    protected float _cx, _cy, _cwidth, _cheight;

    protected Rectangle _srect = new Rectangle();
}
