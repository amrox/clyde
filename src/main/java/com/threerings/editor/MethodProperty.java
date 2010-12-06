//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

package com.threerings.editor;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.samskivert.util.StringUtil;

import static com.threerings.editor.Log.*;

/**
 * A property accessed through a pair of getter/setter methods.
 */
public class MethodProperty extends Property
{
    /**
     * Creates a new method property.
     */
    public MethodProperty (Method getter, Method setter)
    {
        _getter = getter;
        _setter = setter;
        _name = StringUtil.toUSLowerCase(StringUtil.unStudlyName(_setter.getName().substring(3)));
    }

    @Override // documentation inherited
    public Member getMember ()
    {
        return _setter;
    }

    @Override // documentation inherited
    public Class getType ()
    {
        return _getter.getReturnType();
    }

    @Override // documentation inherited
    public Type getGenericType ()
    {
        return _getter.getGenericReturnType();
    }

    @Override // documentation inherited
    public Object get (Object object)
    {
        try {
            return _getter.invoke(object);
        } catch (Exception e) {
            log.warning("Failed to get property [getter=" + _getter + "].", e);
            return null;
        }
    }

    @Override // documentation inherited
    public void set (Object object, Object value)
    {
        try {
            _setter.invoke(object, value);
        } catch (Exception e) {
            log.warning("Failed to set property [setter=" + _setter + "].", e);
        }
    }

    /** The getter and setter methods. */
    protected Method _getter, _setter;
}