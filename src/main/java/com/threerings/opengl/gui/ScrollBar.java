//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2011 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.opengl.gui;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.event.ChangeEvent;
import com.threerings.opengl.gui.event.ChangeListener;
import com.threerings.opengl.gui.event.MouseAdapter;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.event.MouseListener;
import com.threerings.opengl.gui.event.MouseMotionListener;
import com.threerings.opengl.gui.event.MouseWheelListener;

import com.threerings.opengl.gui.background.Background;
import com.threerings.opengl.gui.layout.BorderLayout;
import com.threerings.opengl.gui.util.Insets;

/**
 * Displays a scroll bar for all your horizontal and vertical scrolling
 * needs.
 */
public class ScrollBar extends Container
    implements UIConstants
{
    /**
     * Creates a vertical scroll bar with the default range, value and
     * extent.
     */
    public ScrollBar (GlContext ctx)
    {
        this(ctx, VERTICAL);
    }

    /**
     * Creates a scroll bar with the default range, value and extent.
     */
    public ScrollBar (GlContext ctx, int orientation)
    {
        this(ctx, orientation, 0, 100, 0, 10);
    }

    /**
     * Creates a scroll bar with the specified orientation, range, value
     * and extent.
     */
    public ScrollBar (GlContext ctx, int orientation, int min, int value, int extent, int max)
    {
        this(ctx, orientation, new BoundedRangeModel(min, value, extent, max));
    }

    /**
     * Creates a scroll bar with the specified orientation which will
     * interact with the supplied model.
     */
    public ScrollBar (GlContext ctx, int orientation, BoundedRangeModel model)
    {
        super(ctx, new BorderLayout());
        _orient = orientation;
        _model = model;
        _model.addChangeListener(_updater);
    }

    /**
     * Returns a reference to the scrollbar's range model.
     */
    public BoundedRangeModel getModel ()
    {
        return _model;
    }

    // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();

        // listen for mouse wheel events
        addListener(_wheelListener = _model.createWheelListener());

        // create our buttons and backgrounds
        String oprefix = "Default/ScrollBar" + ((_orient == HORIZONTAL) ? "H" : "V");
        _well = new Component(_ctx);
        _well.setStyleConfig(oprefix + "Well");
        add(_well, BorderLayout.CENTER);
        _well.addListener(_wellListener);

        _thumb = new Component(_ctx);
        _thumb.setStyleConfig(oprefix + "Thumb");
        add(_thumb, BorderLayout.IGNORE);
        _thumb.addListener(_thumbListener);

        _less = new Button(_ctx, "");
        _less.setStyleConfig(oprefix + "Less");
        add(_less, _orient == HORIZONTAL ?
            BorderLayout.WEST : BorderLayout.NORTH);
        _less.addListener(_buttoner);
        _less.setAction("less");

        _more = new Button(_ctx, "");
        _more.setStyleConfig(oprefix + "More");
        add(_more, _orient == HORIZONTAL ?
            BorderLayout.EAST : BorderLayout.SOUTH);
        _more.addListener(_buttoner);
        _more.setAction("more");
    }

    // documentation inherited
    public void wasRemoved ()
    {
        super.wasRemoved();

        if (_wheelListener != null) {
            removeListener(_wheelListener);
            _wheelListener = null;
        }
        if (_well != null) {
            remove(_well);
            _well = null;
        }
        if (_thumb != null) {
            remove(_thumb);
            _thumb = null;
        }
        if (_less != null) {
            remove(_less);
            _less = null;
        }
        if (_more != null) {
            remove(_more);
            _more = null;
        }
    }

    // documentation inherited
    public Component getHitComponent (int mx, int my)
    {
        // we do special processing for the thumb
        if (_thumb.getHitComponent(mx - _x, my - _y) != null) {
            return _thumb;
        }
        return super.getHitComponent(mx, my);
    }

    /**
     * Recomputes and repositions the scroll bar thumb to reflect the
     * current configuration of the model.
     */
    protected void update ()
    {
        if (!isAdded()) {
            return;
        }
        Insets winsets = _well.getInsets();
        int tx = 0, ty = 0;
        int twidth = _well.getWidth() - winsets.getHorizontal();
        int theight = _well.getHeight() - winsets.getVertical();
        int range = Math.max(_model.getRange(), 1); // avoid div0
        int extent = Math.max(_model.getExtent(), 1); // avoid div0
        if (_orient == HORIZONTAL) {
            int wellSize = twidth;
            tx = _model.getValue() * wellSize / range;
            twidth = extent * wellSize / range;
        } else {
            int wellSize = theight;
            ty = (range-extent-_model.getValue()) * wellSize / range;
            theight = extent * wellSize / range;
        }
        _thumb.setBounds(_well.getX() + winsets.left + tx,
                         _well.getY() + winsets.bottom + ty, twidth, theight);
    }

    @Override // documentation inherited
    protected String getDefaultStyleConfig ()
    {
        return "Default/ScrollBar";
    }

    // documentation inherited
    protected void layout ()
    {
        super.layout();

        // reposition our thumb
        update();
    }

    protected ChangeListener _updater = new ChangeListener() {
        public void stateChanged (ChangeEvent event) {
            update();
        }
    };

    protected MouseListener _wellListener = new MouseAdapter() {
        public void mousePressed (MouseEvent event) {
            // if we're above the thumb, scroll up by a page, if we're
            // below, scroll down a page
            int mx = event.getX() - getAbsoluteX(),
                my = event.getY() - getAbsoluteY(), dv = 0;
            if (_orient == HORIZONTAL) {
                if (mx < _thumb.getX()) {
                    dv = -1;
                } else if (mx > _thumb.getX() + _thumb.getWidth()) {
                    dv = 1;
                }
            } else {
                if (my < _thumb.getY()) {
                    dv = 1;
                } else if (my > _thumb.getY() + _thumb.getHeight()) {
                    dv = -1;
                }
            }
            if (dv != 0) {
                dv *= Math.max(1, _model.getExtent());
                _model.setValue(_model.getValue() + dv);
            }
        }
    };

    protected MouseAdapter _thumbListener = new MouseAdapter() {
        public void mousePressed (MouseEvent event) {
            _sv = _model.getValue();
            _sx = event.getX() - getAbsoluteX();
            _sy = event.getY() - getAbsoluteY();
        }

        public void mouseDragged (MouseEvent event) {
            int dv = 0;
            if (_orient == HORIZONTAL) {
                int mx = event.getX() - getAbsoluteX();
                dv = (mx - _sx) * _model.getRange() /
                    (_well.getWidth() - _well.getInsets().getHorizontal());
            } else {
                int my = event.getY() - getAbsoluteY();
                dv = (_sy - my) * _model.getRange() /
                    (_well.getHeight() - _well.getInsets().getVertical());
            }

            if (dv != 0) {
                _model.setValue(_sv + dv);
            }
        }

        protected int _sx, _sy, _sv;
    };

    protected ActionListener _buttoner = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            int delta = _model.getScrollIncrement();
            if (event.getAction().equals("less")) {
                _model.setValue(_model.getValue() - delta);
            } else {
                _model.setValue(_model.getValue() + delta);
            }
        }
    };

    protected BoundedRangeModel _model;
    protected int _orient;

    protected Button _less, _more;
    protected Component _well, _thumb;

    protected MouseWheelListener _wheelListener;
}
