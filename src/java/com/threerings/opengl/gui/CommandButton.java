//
// $Id$

package com.threerings.opengl.gui;

import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.event.CommandEvent;
import com.threerings.opengl.gui.icon.Icon;

/**
 * A button that fires a {@link CommandEvent} when clicked.
 */
public class CommandButton extends Button
{
    /**
     * Creates a command button with the specified textual label.
     */
    public CommandButton (String text)
    {
        this(text, "", null);
    }

    /**
     * Creates a button with the specified label, action, and argument. The action will be
     * dispatched via a {@link CommandEvent} when the button is clicked.
     */
    public CommandButton (String text, String action, Object argument)
    {
        this(text, null, action, null);
    }

    /**
     * Creates a button with the specified label, action, and argument. The action will be
     * dispatched via a {@link CommandEvent} to the specified {@link ActionListener} when the
     * button is clicked.
     */
    public CommandButton (String text, ActionListener listener, String action, Object argument)
    {
        super(text, listener, action);
        _argument = argument;
    }

    /**
     * Creates a button with the specified icon, action, and argument. The action will be
     * dispatched via a {@link CommandEvent} when the button is clicked.
     */
    public CommandButton (Icon icon, String action, Object argument)
    {
        this(icon, null, action, null);
    }

    /**
     * Creates a button with the specified icon, action, and argument. The action will be
     * dispatched via a {@link CommandEvent} to the specified {@link ActionListener} when the
     * button is clicked.
     */
    public CommandButton (Icon icon, ActionListener listener, String action, Object argument)
    {
        super(icon, listener, action);
        _argument = argument;
    }

    /**
     * Configures the argument to be generated when this button is clicked.
     */
    public void setArgument (Object argument)
    {
        _argument = argument;
    }

    /**
     * Returns the argument generated when this button is clicked.
     */
    public Object getArgument ()
    {
        return _argument;
    }

    @Override // documentation inherited
    protected void fireAction (long when, int modifiers)
    {
        emitEvent(new CommandEvent(this, when, modifiers, _action, _argument));
    }

    /** The argument generated when the button is clicked. */
    protected Object _argument;
}
