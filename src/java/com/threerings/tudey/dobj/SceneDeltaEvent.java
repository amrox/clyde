//
// $Id$

package com.threerings.tudey.dobj;

import com.samskivert.util.StringUtil;

import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.net.Transport;

import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.effect.Effect;

/**
 * Represents an update to the dynamic state of the scene.  Each delta represents difference
 * between the current state and the last state (either the baseline state or the result of
 * applying the last acknowledged delta).  These events are published on the client object,
 * rather than the scene object, because they are targeted at specific clients.
 */
public class SceneDeltaEvent extends DEvent
{
    /**
     * Creates a new delta event.
     */
    public SceneDeltaEvent (
        int targetOid, int sceneOid, long acknowledge, int ping, long reference, long timestamp,
        Actor[] addedActors, ActorDelta[] updatedActorDeltas, int[] removedActorIds,
        Effect[] effectsFired)
    {
        this(targetOid, sceneOid, acknowledge, ping, reference, timestamp, addedActors,
            updatedActorDeltas, removedActorIds, effectsFired, Transport.DEFAULT);
    }

    /**
     * Creates a new delta event.
     */
    public SceneDeltaEvent (
        int targetOid, int sceneOid, long acknowledge, int ping, long reference, long timestamp,
        Actor[] addedActors, ActorDelta[] updatedActorDeltas, int[] removedActorIds,
        Effect[] effectsFired, Transport transport)
    {
        super(targetOid, transport);
        _sceneOid = sceneOid;
        _acknowledge = acknowledge;
        _ping = ping;
        _reference = reference;
        _timestamp = timestamp;
        _addedActors = addedActors;
        _updatedActorDeltas = updatedActorDeltas;
        _removedActorIds = removedActorIds;
        _effectsFired = effectsFired;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public SceneDeltaEvent ()
    {
    }

    /**
     * Returns the oid of the scene to which this delta applies.
     */
    public int getSceneOid ()
    {
        return _sceneOid;
    }

    /**
     * Returns the timestamp of the last input frame received by the server.
     */
    public long getAcknowledge ()
    {
        return _acknowledge;
    }

    /**
     * Returns the ping time estimate.
     */
    public int getPing ()
    {
        return _ping;
    }

    /**
     * Returns the timestamp of the update that serves as a basis of comparison for this delta
     * (either the last delta known to be acknowledged by the client, or 0 for the baseline).
     */
    public long getReference ()
    {
        return _reference;
    }

    /**
     * Returns the timestamp of the delta.
     */
    public long getTimestamp ()
    {
        return _timestamp;
    }

    /**
     * Returns a reference to the array of actors added to the scene since the last delta, or
     * <code>null</code> for none.
     */
    public Actor[] getAddedActors ()
    {
        return _addedActors;
    }

    /**
     * Returns a reference to the array of deltas for actors updated since the last delta, or
     * <code>null</code> for none.
     */
    public ActorDelta[] getUpdatedActorDeltas ()
    {
        return _updatedActorDeltas;
    }

    /**
     * Returns a reference to the array of ids of actors removed from the scene since the last
     * delta, or <code>null</code> for none.
     */
    public int[] getRemovedActorIds ()
    {
        return _removedActorIds;
    }

    /**
     * Returns the array of effects fired since the last delta, or <code>null</code> for none.
     */
    public Effect[] getEffectsFired ()
    {
        return _effectsFired;
    }

    @Override // documentation inherited
    public boolean applyToObject (DObject target)
        throws ObjectAccessException
    {
        return true; // nothing to do here
    }

    @Override // documentation inherited
    protected void notifyListener (Object listener)
    {
        if (listener instanceof SceneDeltaListener) {
            ((SceneDeltaListener)listener).sceneDeltaReceived(this);
        }
    }

    @Override // documentation inherited
    protected void toString (StringBuilder buf)
    {
        buf.append("DELTA:");
        super.toString(buf);
        buf.append(", sceneOid=").append(_sceneOid);
        buf.append(", acknowledge=").append(_acknowledge);
        buf.append(", ping=").append(_ping);
        buf.append(", reference=").append(_reference);
        buf.append(", timestamp=").append(_timestamp);
        buf.append(", addedActors=").append(StringUtil.toString(_addedActors));
        buf.append(", updatedActorDeltas=").append(StringUtil.toString(_updatedActorDeltas));
        buf.append(", removedActorIds=").append(StringUtil.toString(_removedActorIds));
        buf.append(", effectsFired=").append(StringUtil.toString(_effectsFired));
    }

    /** The oid of the scene to which this event applies. */
    protected int _sceneOid;

    /** The timestamp of the latest input frame received by the server. */
    protected long _acknowledge;

    /** The estimated ping time. */
    protected int _ping;

    /** The timestamp of the update that serves as a basis of comparison for this delta (either
     * the last delta known to be acknowledged by the client, or 0 for the baseline). */
    protected long _reference;

    /** The timestamp of the delta. */
    protected long _timestamp;

    /** The actors added to the scene since the referenced update (or <code>null</code>). */
    protected Actor[] _addedActors;

    /** The deltas of the actors updated since the referenced update (or <code>null</code). */
    protected ActorDelta[] _updatedActorDeltas;

    /** The ids of the actors removed since the referenced update (or <code>null</code>). */
    protected int[] _removedActorIds;

    /** The effects fired since the last delta (or <code>null</code>). */
    protected Effect[] _effectsFired;
}
