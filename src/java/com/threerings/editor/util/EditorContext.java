//
// $Id$

package com.threerings.editor.util;

import com.threerings.config.ConfigManager;
import com.threerings.media.image.ColorPository;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;

/**
 * Provides access to the services required by the editor.
 */
public interface EditorContext
{
    /**
     * Returns a reference to the resource manager.
     */
    public ResourceManager getResourceManager ();

    /**
     * Returns a reference to the message manager.
     */
    public MessageManager getMessageManager ();

    /**
     * Returns a reference to the configuration manager.
     */
    public ConfigManager getConfigManager ();

    /**
     * Returns a reference to the color pository.
     */
    public ColorPository getColorPository ();
}
