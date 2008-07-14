//
// $Id$

package com.threerings.opengl;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.PixelFormat;

import com.samskivert.swing.util.SwingUtil;

import com.threerings.media.ManagedJFrame;

import com.threerings.util.KeyboardManager;
import com.threerings.util.KeyTranslatorImpl;

import com.threerings.opengl.camera.CameraHandler;
import com.threerings.opengl.camera.OrbitCameraHandler;
import com.threerings.opengl.gui.CanvasRoot;
import com.threerings.opengl.gui.Root;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.Renderable;

import static com.threerings.opengl.Log.*;

/**
 * A base class for applications centered around an OpenGL canvas.
 */
public abstract class GlCanvasApp extends GlApp
{
    public GlCanvasApp ()
    {
        _frame = new JFrame();
        _frame.setSize(1024, 768);
        SwingUtil.centerWindow(_frame);

        // shutdown the application when the window is closed
        _frame.addWindowListener(new WindowAdapter() {
            public void windowClosing (WindowEvent event) {
                shutdown();
            }
        });

        // add the canvas inside a panel so that we can use KeyboardManager
        JPanel panel = new JPanel(new BorderLayout());
        _frame.add(panel, BorderLayout.CENTER);
        try {
            panel.add(_canvas = new GlCanvas(new PixelFormat(8, 8, 8)) {
                public void didInit () {
                    initRenderer();
                }
                public void updateScene () {
                    _now.value = System.currentTimeMillis();
                    GlCanvasApp.this.updateScene();
                }
                public void renderScene () {
                    _camhand.updatePosition();
                    GlCanvasApp.this.renderScene();
                }
            }, BorderLayout.CENTER);

        } catch (LWJGLException e) {
            log.warning("Failed to open window.", e);
            return;
        }

        // create the keyboard manager
        _keymgr = new KeyboardManager();
        _keymgr.setTarget(panel, new KeyTranslatorImpl());
    }

    /**
     * Returns a reference to the containing frame.
     */
    public JFrame getFrame ()
    {
        return _frame;
    }

    /**
     * Returns a reference to the canvas.
     */
    public GlCanvas getCanvas ()
    {
        return _canvas;
    }

    /**
     * Starts up the application.
     */
    public void start ()
    {
        _frame.setVisible(true);
    }

    /**
     * Shuts down the application.
     */
    public void shutdown ()
    {
        willShutdown();
        System.exit(0);
    }

    // documentation inherited from interface RunQueue
    public void postRunnable (Runnable run)
    {
        // queue it on up on the awt thread
        EventQueue.invokeLater(run);
    }

    // documentation inherited from interface RunQueue
    public boolean isDispatchThread ()
    {
        return EventQueue.isDispatchThread();
    }

    @Override // documentation inherited
    public Root createRoot ()
    {
        return new CanvasRoot(this, _canvas);
    }

    /**
     * Initializes the application once the OpenGL context is available.
     */
    protected void initRenderer ()
    {
        _renderer.init(_canvas, _canvas.getWidth(), _canvas.getHeight());
        _compositor.init();
        _camhand = createCameraHandler();
        _camhand.updatePerspective();

        // adjust the viewport on resize
        _canvas.addComponentListener(new ComponentAdapter() {
            public void componentResized (ComponentEvent event) {
                canvasResized(_canvas.getWidth(), _canvas.getHeight());
            }
        });

        // request focus for the canvas
        _canvas.requestFocusInWindow();

        // enable the keyboard manager
        _keymgr.setEnabled(true);

        // add a root to call the enqueueScene method
        _compositor.addRoot(new Renderable() {
            public void enqueue () {
                enqueueScene();
            }
        });

        // give subclasses a chance to init
        didInit();
    }

    /**
     * Creates and returns the camera handler.
     */
    protected CameraHandler createCameraHandler ()
    {
        return new OrbitCameraHandler(this);
    }

    /**
     * Override to perform custom initialization after the render context is valid.
     */
    protected void didInit ()
    {
    }

    /**
     * Override to perform cleanup before the application exits.
     */
    protected void willShutdown ()
    {
    }

    /**
     * Performs any scene updates that are necessary even when not rendering.
     */
    protected void updateScene ()
    {
    }

    /**
     * Renders the entire scene.
     */
    protected void renderScene ()
    {
        _compositor.renderScene();
    }

    /**
     * Gives the application a chance to enqueue anything it might want rendered.
     */
    protected void enqueueScene ()
    {
    }

    /**
     * Called when the canvas has been resized.
     */
    protected void canvasResized (int width, int height)
    {
        _compositor.getCamera().getViewport().set(0, 0, width, height);
        _camhand.updatePerspective();
    }

    /** The frame containing the canvas. */
    protected JFrame _frame;

    /** The render canvas. */
    protected GlCanvas _canvas;

    /** The keyboard manager for the canvas. */
    protected KeyboardManager _keymgr;
}
