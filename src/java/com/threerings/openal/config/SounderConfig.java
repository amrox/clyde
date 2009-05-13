//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

package com.threerings.openal.config;

import java.util.HashSet;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;

import com.threerings.openal.Sounder;
import com.threerings.openal.util.AlContext;

import static com.threerings.openal.Log.*;

/**
 * The configuration of a sounder.
 */
public class SounderConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the sounder.
     */
    @EditorTypes({ Clip.class, Stream.class, MetaStream.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Adds the implementation's update references to the provided set.
         */
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            // nothing by default
        }

        /**
         * Adds the implementation's update resources to the provided set.
         */
        public void getUpdateResources (HashSet<String> paths)
        {
            // nothing by default
        }

        /**
         * Creates or updates a sounder implementation for this configuration.
         *
         * @param scope the sounder's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl);
    }

    /**
     * The superclass of the implementations describing an original sounder, as opposed to one
     * derived from another configuration.
     */
    public static abstract class Original extends Implementation
    {
        /** Whether or not the position of the sound is relative to the listener. */
        @Editable(hgroup="s")
        public boolean sourceRelative;

        /** Whether or not the sound is directional. */
        @Editable(hgroup="s")
        public boolean directional;

        /** The base gain (volume). */
        @Editable(min=0, step=0.01, hgroup="g")
        public float gain = 1f;

        /** The pitch multiplier. */
        @Editable(min=0, step=0.01, hgroup="g")
        public float pitch = 1f;

        /** The minimum gain for the source. */
        @Editable(min=0, max=1, step=0.01, hgroup="m")
        public float minGain;

        /** The maximum gain for the source. */
        @Editable(min=0, max=1, step=0.01, hgroup="m")
        public float maxGain = 1f;

        /** The distance at which the volume would normally drop by half (before being influenced
         * by the rolloff factor or the maximum distance). */
        @Editable(min=0, step=0.01, hgroup="r")
        public float referenceDistance = 1f;

        /** The rolloff rate. */
        @Editable(min=0, step=0.01, hgroup="r")
        public float rolloffFactor = 1f;

        /** The distance at which attenuation stops. */
        @Editable(min=0, step=0.01, hgroup="m")
        public float maxDistance = Float.MAX_VALUE;

        /** The gain when outside the oriented cone. */
        @Editable(min=0, max=1, step=0.01, hgroup="m")
        public float coneOuterGain;

        /** The inner angle of the sound cone. */
        @Editable(min=-360, max=+360, hgroup="c")
        public float coneInnerAngle = 360f;

        /** The outer angle of the sound cone. */
        @Editable(min=-360, max=+360, hgroup="c")
        public float coneOuterAngle = 360f;
    }

    /**
     * Plays a sound clip.
     */
    public static class Clip extends Original
    {
        /** The sound resource from which to load the clip. */
        @Editable(editor="resource", nullable=true, weight=-1, hgroup="f")
        @FileConstraints(
            description="m.sound_files_desc",
            extensions={".ogg"},
            directory="sound_dir")
        public String file;

        /** Whether or not the sound loops. */
        @Editable(weight=-1, hgroup="f")
        public boolean loop;

        @Override // documentation inherited
        public void getUpdateResources (HashSet<String> paths)
        {
            if (file != null) {
                paths.add(file);
            }
        }

        @Override // documentation inherited
        public Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl)
        {
            if (file == null) {
                return null;
            }
            if (impl instanceof Sounder.Clip) {
                ((Sounder.Clip)impl).setConfig(this);
            } else {
                impl = new Sounder.Clip(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * Base class for {@link Stream} and {@link MetaStream}.
     */
    public static abstract class BaseStream extends Original
    {
        /** The interval over which to fade in the stream. */
        @Editable(min=0, step=0.01, hgroup="f")
        public float fadeIn;

        /** The interval over which to fade out the stream. */
        @Editable(min=0, step=0.01, hgroup="f")
        public float fadeOut;
    }

    /**
     * Plays a sound stream.
     */
    public static class Stream extends BaseStream
    {
        /** The files to enqueue in the stream. */
        @Editable(weight=-1)
        public QueuedFile[] queue = new QueuedFile[0];

        @Override // documentation inherited
        public void getUpdateResources (HashSet<String> paths)
        {
            for (QueuedFile queued : queue) {
                if (queued.file != null) {
                    paths.add(queued.file);
                }
            }
        }

        @Override // documentation inherited
        public Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl)
        {
            if (queue.length == 0 || queue[0].file == null) {
                return null;
            }
            if (impl instanceof Sounder.Stream) {
                ((Sounder.Stream)impl).setConfig(this);
            } else {
                impl = new Sounder.Stream(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * Represents a file to enqueue in the stream.
     */
    public static class QueuedFile extends DeepObject
        implements Exportable
    {
        /** The file to stream. */
        @Editable(editor="resource", nullable=true, hgroup="f")
        @FileConstraints(
            description="m.sound_files_desc",
            extensions={".ogg"},
            directory="sound_dir")
        public String file;

        /** Whether or not to loop the file. */
        @Editable(hgroup="f")
        public boolean loop;
    }

    /**
     * Selects between a number of substreams.
     */
    public static class MetaStream extends BaseStream
    {
        /** The files from which to choose. */
        @Editable(weight=-1)
        public WeightedFile[] files = new WeightedFile[0];

        /** The cross-fade between tracks. */
        @Editable(min=0, step=0.01, hgroup="f")
        public float crossFade;

        @Override // documentation inherited
        public void getUpdateResources (HashSet<String> paths)
        {
            for (WeightedFile wfile : files) {
                if (wfile.file != null) {
                    paths.add(wfile.file);
                }
            }
        }

        @Override // documentation inherited
        public Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl)
        {
            if (files.length == 0) {
                return null;
            }
            if (impl instanceof Sounder.MetaStream) {
                ((Sounder.MetaStream)impl).setConfig(this);
            } else {
                impl = new Sounder.MetaStream(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * Combines a file to enqueue .
     */
    public static class WeightedFile extends DeepObject
        implements Exportable
    {
        /** The file to stream. */
        @Editable(editor="resource", nullable=true, hgroup="f")
        @FileConstraints(
            description="m.sound_files_desc",
            extensions={".ogg"},
            directory="sound_dir")
        public String file;

        /** The weight of the file. */
        @Editable(min=0.0, step=0.01, hgroup="f")
        public float weight = 1f;
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The sound reference. */
        @Editable(nullable=true)
        public ConfigReference<SounderConfig> sounder;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(SounderConfig.class, sounder);
        }

        @Override // documentation inherited
        public Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl)
        {
            SounderConfig config = ctx.getConfigManager().getConfig(SounderConfig.class, sounder);
            return (config == null) ? null : config.getSounderImplementation(ctx, scope, impl);
        }
    }

    /** The actual sound implementation. */
    @Editable
    public Implementation implementation = new Clip();

    /**
     * Creates or updates sounder implementation for this configuration.
     *
     * @param scope the sounder's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public Sounder.Implementation getSounderImplementation (
        AlContext ctx, Scope scope, Sounder.Implementation impl)
    {
        return implementation.getSounderImplementation(ctx, scope, impl);
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }

    @Override // documentation inherited
    protected void getUpdateResources (HashSet<String> paths)
    {
        implementation.getUpdateResources(paths);
    }
}
