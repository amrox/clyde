//
// $Id$

package com.threerings.tudey.server;

import java.util.ArrayList;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.RunAnywhere;

import com.threerings.presents.data.ClientObject;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.server.SceneManager;

import com.threerings.math.Rect;

import com.threerings.tudey.data.InputFrame;
import com.threerings.tudey.data.TudeyOccupantInfo;
import com.threerings.tudey.data.TudeySceneObject;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.effect.Effect;
import com.threerings.tudey.server.logic.ActorLogic;
import com.threerings.tudey.space.HashSpace;
import com.threerings.tudey.space.SpaceElement;

import static com.threerings.tudey.Log.*;

/**
 * Manager for Tudey scenes.
 */
public class TudeySceneManager extends SceneManager
    implements TudeySceneProvider
{
    /**
     * Returns a reference to the actor influence space.
     */
    public HashSpace getInfluenceSpace ()
    {
        return _influenceSpace;
    }

    /**
     * Returns a map containing all actors whose influence regions intersect the provided bounds.
     */
    public HashIntMap<Actor> getActors (Rect bounds)
    {
        _influenceSpace.getElements(bounds, _elements);
        HashIntMap<Actor> map = new HashIntMap<Actor>();
        for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
            Actor actor = ((ActorLogic)_elements.get(ii).getUserObject()).getActor();
            map.put(actor.getId(), actor);
        }
        _elements.clear();
        return map;
    }

    /**
     * Returns an array containing all effects fired on the current tick whose influence regions
     * intersect the provided bounds.
     */
    public Effect[] getEffectsFired (Rect bounds)
    {
        for (int ii = 0, nn = _effectsFired.size(); ii < nn; ii++) {
            Effect effect = _effectsFired.get(ii);
            if (effect.getInfluence().intersects(bounds)) {
                _effects.add(effect);
            }
        }
        Effect[] array = _effects.toArray(new Effect[_effects.size()]);
        _effects.clear();
        return array;
    }

    /**
     * Fires an effect in the current tick.
     */
    public void fireEffect (Effect effect)
    {
        _effectsFired.add(effect);
    }

    // documentation inherited from interface TudeySceneProvider
    public void enqueueInput (
        ClientObject caller, long acknowledge, long smoothedTime, InputFrame[] frames)
    {
        // forward to client liaison
        ClientLiaison client = _clients.get(caller.getOid());
        if (client != null) {
            // ping is current time minus client's smoothed time estimate
            long currentTime = _tsobj.timestamp + (RunAnywhere.currentTimeMillis() - _lastTick);
            client.enqueueInput(acknowledge, currentTime - smoothedTime, frames);
        } else {
            log.warning("Received input from unknown client.",
                "who", caller.who(), "where", where());
        }
    }

    @Override // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return (_tsobj = new TudeySceneObject());
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        // register and fill in our tudey scene service
        _tsobj.setTudeySceneService(_invmgr.registerDispatcher(new TudeySceneDispatcher(this)));

        // initialize the last tick timestamp
        _lastTick = RunAnywhere.currentTimeMillis();

        // start the ticker
        _ticker = new Interval(_omgr) {
            public void expired () {
                tick();
            }
        };
        _ticker.schedule(getTickInterval(), true);
    }

    @Override // documentation inherited
    protected void didShutdown ()
    {
        super.didShutdown();

        // stop the ticker
        _ticker.cancel();
        _ticker = null;

        // clear out the scene service
        _invmgr.clearDispatcher(_tsobj.tudeySceneService);
    }

    @Override // documentation inherited
    protected void insertOccupantInfo (OccupantInfo info, BodyObject body)
    {
        // add the actor and fill in its id
        ((TudeyOccupantInfo)info).actorId = 0;

        super.insertOccupantInfo(info, body);
    }

    @Override // documentation inherited
    protected void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);

        // create and map the client liaison
        _clients.put(bodyOid, new ClientLiaison(this, (BodyObject)_omgr.getObject(bodyOid)));
    }

    @Override // documentation inherited
    protected void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);

        // remove the client liaison
        _clients.remove(bodyOid);
    }

    /**
     * Returns the interval at which we call the {@link #tick} method.
     */
    protected long getTickInterval ()
    {
        return 50L;
    }

    /**
     * Updates the scene.
     */
    protected void tick ()
    {
        // update the scene timestamp
        long now = RunAnywhere.currentTimeMillis();
        _tsobj.timestamp += (now - _lastTick);
        _lastTick = now;

        // post deltas for all clients
        for (ClientLiaison client : _clients.values()) {
            client.postDelta();
        }

        // clear the effect list
        _effectsFired.clear();
    }

    /** A casted reference to the Tudey scene object. */
    protected TudeySceneObject _tsobj;

    /** The tick interval. */
    protected Interval _ticker;

    /** The system time of the last tick. */
    protected long _lastTick;

    /** Maps body oids to client liaisons. */
    protected HashIntMap<ClientLiaison> _clients = new HashIntMap<ClientLiaison>();

    /** The actor influence space.  Used to find the actors within a client's area of interest. */
    protected HashSpace _influenceSpace = new HashSpace(64f, 6);

    /** The effects fired on the current tick. */
    protected ArrayList<Effect> _effectsFired = new ArrayList<Effect>();

    /** Holds collected elements during queries. */
    protected ArrayList<SpaceElement> _elements = new ArrayList<SpaceElement>();

    /** Holds collected effects during queries. */
    protected ArrayList<Effect> _effects = new ArrayList<Effect>();
}
