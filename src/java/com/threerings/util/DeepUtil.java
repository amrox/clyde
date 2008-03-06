//
// $Id$

package com.threerings.util;

import java.io.File;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.google.common.collect.Maps;

import com.samskivert.util.ObjectUtil;

import static java.util.logging.Level.*;
import static com.threerings.ClydeLog.*;

/**
 * Various methods that use reflection to perform "deep" operations: copying, comparison, etc.
 * The usual warnings about circular references apply.
 */
public class DeepUtil
{
    /**
     * Creates and returns a deep copy of an object using reflection.
     */
    public static <T> T copy (T source)
    {
        return copy(source, null);
    }

    /**
     * Creates a deep copy of an object using reflection, storing the result in the object
     * provided if possible.
     *
     * @return the copied object.
     */
    public static <T> T copy (T source, T dest)
    {
        if (source == null) {
            return null;
        }
        Class clazz = source.getClass();
        if (dest != null && dest.getClass() != clazz) {
            dest = null;
        }
        @SuppressWarnings("unchecked") ObjectHandler<T> handler =
            (ObjectHandler<T>)getObjectHandler(clazz);
        try {
            return handler.copy(source, dest);
        } catch (IllegalAccessException e) {
            log.log(WARNING, "Couldn't access fields for deep copy.", e);
            return null;
        }
    }

    /**
     * Compares two objects for deep equality.
     */
    public static <T> boolean equals (T o1, T o2)
    {
        if (o1 == o2) {
            return true;
        }
        Class c1 = (o1 == null) ? null : o1.getClass();
        Class c2 = (o2 == null) ? null : o2.getClass();
        if (c1 != c2) {
            return false;
        }
        @SuppressWarnings("unchecked") ObjectHandler<T> handler =
            (ObjectHandler<T>)getObjectHandler(c1);
        try {
            return handler.equals(o1, o2);
        } catch (IllegalAccessException e) {
            log.log(WARNING, "Couldn't access fields for deep equals.", e);
            return false;
        }
    }

    /**
     * Computes the deep hash code of an object.
     */
    public static int hashCode (Object object)
    {
        if (object == null) {
            return 0;
        }
        @SuppressWarnings("unchecked") ObjectHandler<Object> handler =
            (ObjectHandler<Object>)getObjectHandler(object.getClass());
        try {
            return handler.hashCode(object);
        } catch (IllegalAccessException e) {
            log.log(WARNING, "Couldn't access fields for deep hash code.", e);
            return 0;
        }
    }

    /**
     * Retrieves the handler for the supplied class.
     */
    protected static ObjectHandler getObjectHandler (Class clazz)
    {
        ObjectHandler handler = _objectHandlers.get(clazz);
        if (handler == null) {
            if (Enum.class.isAssignableFrom(clazz)) {
                return IMMUTABLE_OBJECT_HANDLER;
            } else if (clazz.isArray()) {
                return ARRAY_OBJECT_HANDLER;
            }
            _objectHandlers.put(clazz, handler = new ReflectiveObjectHandler(clazz));
        }
        return handler;
    }

    /**
     * Populates the supplied list with the copyable/comparable fields of the given class.
     */
    protected static void getInstanceFields (Class clazz, ArrayList<Field> fields)
    {
        // add those of the superclass, if any
        Class sclazz = clazz.getSuperclass();
        if (sclazz != Object.class) {
            getInstanceFields(sclazz, fields);
        }

        // add any non-static, non-synthetic fields without the DeepOmit annotation
        for (Field field : clazz.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (!(Modifier.isStatic(mods) || field.isSynthetic() ||
                    field.getAnnotation(DeepOmit.class) != null)) {
                field.setAccessible(true);
                fields.add(field);
            }
        }
    }

    /**
     * Performs the actual object operations.
     */
    protected static abstract class ObjectHandler<T>
    {
        /**
         * Performs a deep copy from source to dest.
         */
        public abstract T copy (T source, T dest)
            throws IllegalAccessException;

        /**
         * Compares two objects for equality.
         */
        public abstract boolean equals (T o1, T o2)
            throws IllegalAccessException;

        /**
         * Computes the object's hash code.
         */
        public abstract int hashCode (T object)
            throws IllegalAccessException;
    }

    /**
     * Handles an object according to its reflected fields.
     */
    protected static class ReflectiveObjectHandler extends ObjectHandler<Object>
    {
        public ReflectiveObjectHandler (Class clazz)
        {
            ArrayList<Field> fields = new ArrayList<Field>();
            getInstanceFields(clazz, fields);
            _fields = fields.toArray(new Field[fields.size()]);
            _handlers = new FieldHandler[_fields.length];
            for (int ii = 0; ii < _fields.length; ii++) {
                Field field = _fields[ii];
                Class type = field.getType();
                if (type.isPrimitive()) {
                    _handlers[ii] = PRIMITIVE_FIELD_HANDLERS.get(type);
                } else if (field.getAnnotation(Deep.class) != null) {
                    _handlers[ii] = DEEP_OBJECT_FIELD_HANDLER;
                } else if (field.getAnnotation(Shallow.class) != null) {
                    _handlers[ii] = SHALLOW_OBJECT_FIELD_HANDLER;
                } else {
                    _handlers[ii] = DEFAULT_OBJECT_FIELD_HANDLER;
                }
            }
        }

        @Override // documentation inherited
        public Object copy (Object source, Object dest)
            throws IllegalAccessException
        {
            // create the destination object if it doesn't exist yet
            if (dest == null) {
                if ((dest = ReflectionUtil.newInstance(
                       source.getClass(), ReflectionUtil.getOuter(source))) == null) {
                    return null; // an error will have been logged
                }
            }
            // deep-copy the fields
            for (int ii = 0; ii < _fields.length; ii++) {
                _handlers[ii].copy(_fields[ii], source, dest);
            }
            return dest;
        }

        @Override // documentation inherited
        public boolean equals (Object o1, Object o2)
            throws IllegalAccessException
        {
            // deep-compare the fields
            for (int ii = 0; ii < _fields.length; ii++) {
                if (!_handlers[ii].equals(_fields[ii], o1, o2)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Computes the hash code of an object's fields.
         */
        public int hashCode (Object object)
            throws IllegalAccessException
        {
            // this is the algorithm that, for example, java.util.Arrays uses
            int hash = 1;
            for (int ii = 0; ii < _fields.length; ii++) {
                hash = 31*hash + _handlers[ii].hashCode(_fields[ii], object);
            }
            return hash;
        }

        /** The fields to copy and compare. */
        protected Field[] _fields;

        /** The handlers for each field. */
        protected FieldHandler[] _handlers;
    }

    /**
     * Copies or compares a field of a single type.
     */
    protected static abstract class FieldHandler
    {
        /**
         * Copies the specified field from the first object to the second.
         */
        public abstract void copy (Field field, Object source, Object dest)
            throws IllegalAccessException;

        /**
         * Checks whether the field is equal in both objects.
         */
        public abstract boolean equals (Field field, Object o1, Object o2)
            throws IllegalAccessException;

        /**
         * Computes the hash code of the given field value.
         */
        public abstract int hashCode (Field field, Object object)
            throws IllegalAccessException;
    }

    /** Field handler for immutable fields, which can be handled by reference. */
    protected static final ObjectHandler IMMUTABLE_OBJECT_HANDLER = new ObjectHandler<Object>() {
        public Object copy (Object source, Object dest)
            throws IllegalAccessException {
            return source;
        }
        public boolean equals (Object o1, Object o2)
            throws IllegalAccessException {
            return o1.equals(o2);
        }
        public int hashCode (Object object)
            throws IllegalAccessException {
            return object.hashCode();
        }
    };

    /** Object handlers mapped by class. */
    protected static final HashMap<Class, ObjectHandler> _objectHandlers = Maps.newHashMap();
    static {
        _objectHandlers.put(boolean[].class, new ObjectHandler<boolean[]>() {
            public boolean[] copy (boolean[] source, boolean[] dest)
                    throws IllegalAccessException {
                if (dest != null && dest.length == source.length) {
                    System.arraycopy(source, 0, dest, 0, source.length);
                    return dest;
                } else {
                    return (boolean[])source.clone();
                }
            }
            public boolean equals (boolean[] o1, boolean[] o2)
                    throws IllegalAccessException {
                return Arrays.equals(o1, o2);
            }
            public int hashCode (boolean[] object)
                    throws IllegalAccessException {
                return Arrays.hashCode(object);
            }
        });

        _objectHandlers.put(byte[].class, new ObjectHandler<byte[]>() {
            public byte[] copy (byte[] source, byte[] dest)
                    throws IllegalAccessException {
                if (dest != null && dest.length == source.length) {
                    System.arraycopy(source, 0, dest, 0, source.length);
                    return dest;
                } else {
                    return (byte[])source.clone();
                }
            }
            public boolean equals (byte[] o1, byte[] o2)
                    throws IllegalAccessException {
                return Arrays.equals(o1, o2);
            }
            public int hashCode (byte[] object)
                    throws IllegalAccessException {
                return Arrays.hashCode(object);
            }
        });

        _objectHandlers.put(char[].class, new ObjectHandler<char[]>() {
            public char[] copy (char[] source, char[] dest)
                    throws IllegalAccessException {
                if (dest != null && dest.length == source.length) {
                    System.arraycopy(source, 0, dest, 0, source.length);
                    return dest;
                } else {
                    return (char[])source.clone();
                }
            }
            public boolean equals (char[] o1, char[] o2)
                    throws IllegalAccessException {
                return Arrays.equals(o1, o2);
            }
            public int hashCode (char[] object)
                    throws IllegalAccessException {
                return Arrays.hashCode(object);
            }
        });

        _objectHandlers.put(double[].class, new ObjectHandler<double[]>() {
            public double[] copy (double[] source, double[] dest)
                    throws IllegalAccessException {
                if (dest != null && dest.length == source.length) {
                    System.arraycopy(source, 0, dest, 0, source.length);
                    return dest;
                } else {
                    return (double[])source.clone();
                }
            }
            public boolean equals (double[] o1, double[] o2)
                    throws IllegalAccessException {
                return Arrays.equals(o1, o2);
            }
            public int hashCode (double[] object)
                    throws IllegalAccessException {
                return Arrays.hashCode(object);
            }
        });

        _objectHandlers.put(float[].class, new ObjectHandler<float[]>() {
            public float[] copy (float[] source, float[] dest)
                    throws IllegalAccessException {
                if (dest != null && dest.length == source.length) {
                    System.arraycopy(source, 0, dest, 0, source.length);
                    return dest;
                } else {
                    return (float[])source.clone();
                }
            }
            public boolean equals (float[] o1, float[] o2)
                    throws IllegalAccessException {
                return Arrays.equals(o1, o2);
            }
            public int hashCode (float[] object)
                    throws IllegalAccessException {
                return Arrays.hashCode(object);
            }
        });

        _objectHandlers.put(int[].class, new ObjectHandler<int[]>() {
            public int[] copy (int[] source, int[] dest)
                    throws IllegalAccessException {
                if (dest != null && dest.length == source.length) {
                    System.arraycopy(source, 0, dest, 0, source.length);
                    return dest;
                } else {
                    return (int[])source.clone();
                }
            }
            public boolean equals (int[] o1, int[] o2)
                    throws IllegalAccessException {
                return Arrays.equals(o1, o2);
            }
            public int hashCode (int[] object)
                    throws IllegalAccessException {
                return Arrays.hashCode(object);
            }
        });

        _objectHandlers.put(long[].class, new ObjectHandler<long[]>() {
            public long[] copy (long[] source, long[] dest)
                    throws IllegalAccessException {
                if (dest != null && dest.length == source.length) {
                    System.arraycopy(source, 0, dest, 0, source.length);
                    return dest;
                } else {
                    return (long[])source.clone();
                }
            }
            public boolean equals (long[] o1, long[] o2)
                    throws IllegalAccessException {
                return Arrays.equals(o1, o2);
            }
            public int hashCode (long[] object)
                    throws IllegalAccessException {
                return Arrays.hashCode(object);
            }
        });

        _objectHandlers.put(short[].class, new ObjectHandler<short[]>() {
            public short[] copy (short[] source, short[] dest)
                    throws IllegalAccessException {
                if (dest != null && dest.length == source.length) {
                    System.arraycopy(source, 0, dest, 0, source.length);
                    return dest;
                } else {
                    return (short[])source.clone();
                }
            }
            public boolean equals (short[] o1, short[] o2)
                    throws IllegalAccessException {
                return Arrays.equals(o1, o2);
            }
            public int hashCode (short[] object)
                    throws IllegalAccessException {
                return Arrays.hashCode(object);
            }
        });

        // standard immutables
        _objectHandlers.put(Boolean.class, IMMUTABLE_OBJECT_HANDLER);
        _objectHandlers.put(Byte.class, IMMUTABLE_OBJECT_HANDLER);
        _objectHandlers.put(Character.class, IMMUTABLE_OBJECT_HANDLER);
        _objectHandlers.put(Double.class, IMMUTABLE_OBJECT_HANDLER);
        _objectHandlers.put(Float.class, IMMUTABLE_OBJECT_HANDLER);
        _objectHandlers.put(Integer.class, IMMUTABLE_OBJECT_HANDLER);
        _objectHandlers.put(Long.class, IMMUTABLE_OBJECT_HANDLER);
        _objectHandlers.put(Short.class, IMMUTABLE_OBJECT_HANDLER);
        _objectHandlers.put(String.class, IMMUTABLE_OBJECT_HANDLER);
        _objectHandlers.put(File.class, IMMUTABLE_OBJECT_HANDLER);
    }

    /** Field handler for object arrays. */
    protected static final ObjectHandler ARRAY_OBJECT_HANDLER = new ObjectHandler<Object[]>() {
        public Object[] copy (Object[] source, Object[] dest)
            throws IllegalAccessException {
            if (dest == null || dest.length != source.length) {
                dest = (Object[])Array.newInstance(
                    source.getClass().getComponentType(), source.length);
            }
            for (int ii = 0; ii < source.length; ii++) {
                dest[ii] = DeepUtil.copy(source[ii], dest[ii]);
            }
            return dest;
        }
        public boolean equals (Object[] o1, Object[] o2)
            throws IllegalAccessException {
            if (o1.length != o2.length) {
                return false;
            }
            for (int ii = 0; ii < o1.length; ii++) {
                if (!DeepUtil.equals(o1[ii], o2[ii])) {
                    return false;
                }
            }
            return true;
        }
        public int hashCode (Object[] object)
            throws IllegalAccessException {
            int hash = 1;
            for (Object element : object) {
                hash = 31*hash + DeepUtil.hashCode(element);
            }
            return hash;
        }
    };

    /** Handlers for primitive fields mapped by class. */
    protected static final HashMap<Class, FieldHandler> PRIMITIVE_FIELD_HANDLERS =
        Maps.newHashMap();
    static {
        PRIMITIVE_FIELD_HANDLERS.put(Boolean.TYPE, new FieldHandler() {
            public void copy (Field field, Object source, Object dest)
                    throws IllegalAccessException {
                field.setBoolean(dest, field.getBoolean(source));
            }
            public boolean equals (Field field, Object o1, Object o2)
                    throws IllegalAccessException {
                return field.getBoolean(o1) == field.getBoolean(o2);
            }
            public int hashCode (Field field, Object object)
                    throws IllegalAccessException {
                return field.getBoolean(object) ? 1231 : 1237;
            }
        });

        PRIMITIVE_FIELD_HANDLERS.put(Byte.TYPE, new FieldHandler() {
            public void copy (Field field, Object source, Object dest)
                    throws IllegalAccessException {
                field.setByte(dest, field.getByte(source));
            }
            public boolean equals (Field field, Object o1, Object o2)
                    throws IllegalAccessException {
                return field.getByte(o1) == field.getByte(o2);
            }
            public int hashCode (Field field, Object object)
                    throws IllegalAccessException {
                return field.getByte(object);
            }
        });

        PRIMITIVE_FIELD_HANDLERS.put(Character.TYPE, new FieldHandler() {
            public void copy (Field field, Object source, Object dest)
                    throws IllegalAccessException {
                field.setChar(dest, field.getChar(source));
            }
            public boolean equals (Field field, Object o1, Object o2)
                    throws IllegalAccessException {
                return field.getChar(o1) == field.getChar(o2);
            }
            public int hashCode (Field field, Object object)
                    throws IllegalAccessException {
                return field.getChar(object);
            }
        });

        PRIMITIVE_FIELD_HANDLERS.put(Double.TYPE, new FieldHandler() {
            public void copy (Field field, Object source, Object dest)
                    throws IllegalAccessException {
                field.setDouble(dest, field.getDouble(source));
            }
            public boolean equals (Field field, Object o1, Object o2)
                    throws IllegalAccessException {
                return field.getDouble(o1) == field.getDouble(o2);
            }
            public int hashCode (Field field, Object object)
                    throws IllegalAccessException {
                long bits = Double.doubleToLongBits(field.getDouble(object));
            	return (int)(bits ^ (bits >>> 32));
            }
        });

        PRIMITIVE_FIELD_HANDLERS.put(Float.TYPE, new FieldHandler() {
            public void copy (Field field, Object source, Object dest)
                    throws IllegalAccessException {
                field.setFloat(dest, field.getFloat(source));
            }
            public boolean equals (Field field, Object o1, Object o2)
                    throws IllegalAccessException {
                return field.getFloat(o1) == field.getFloat(o2);
            }
            public int hashCode (Field field, Object object)
                    throws IllegalAccessException {
                return Float.floatToIntBits(field.getFloat(object));
            }
        });

        PRIMITIVE_FIELD_HANDLERS.put(Integer.TYPE, new FieldHandler() {
            public void copy (Field field, Object source, Object dest)
                    throws IllegalAccessException {
                field.setInt(dest, field.getInt(source));
            }
            public boolean equals (Field field, Object o1, Object o2)
                    throws IllegalAccessException {
                return field.getInt(o1) == field.getInt(o2);
            }
            public int hashCode (Field field, Object object)
                    throws IllegalAccessException {
                return field.getInt(object);
            }
        });

        PRIMITIVE_FIELD_HANDLERS.put(Long.TYPE, new FieldHandler() {
            public void copy (Field field, Object source, Object dest)
                    throws IllegalAccessException {
                field.setLong(dest, field.getLong(source));
            }
            public boolean equals (Field field, Object o1, Object o2)
                    throws IllegalAccessException {
                return field.getLong(o1) == field.getLong(o2);
            }
            public int hashCode (Field field, Object object)
                    throws IllegalAccessException {
                long bits = field.getLong(object);
                return (int)(bits ^ (bits >>> 32));
            }
        });

        PRIMITIVE_FIELD_HANDLERS.put(Short.TYPE, new FieldHandler() {
            public void copy (Field field, Object source, Object dest)
                    throws IllegalAccessException {
                field.setShort(dest, field.getShort(source));
            }
            public boolean equals (Field field, Object o1, Object o2)
                    throws IllegalAccessException {
                return field.getShort(o1) == field.getShort(o2);
            }
            public int hashCode (Field field, Object object)
                    throws IllegalAccessException {
                return field.getShort(object);
            }
        });
    }

    /** Default handler for object fields. */
    protected static FieldHandler DEFAULT_OBJECT_FIELD_HANDLER = new FieldHandler() {
        public void copy (Field field, Object source, Object dest)
                throws IllegalAccessException {
            Object v1 = field.get(source), v2 = field.get(dest);
            if (v1 == null) {
                field.set(dest, null);
            } else if (v1 instanceof Copyable) {
                field.set(dest, ((Copyable)v1).copy(v2));
            } else {
                field.set(dest, DeepUtil.copy(v1, v2));
            }
        }
        public boolean equals (Field field, Object o1, Object o2)
                throws IllegalAccessException {
            Object v1 = field.get(o1), v2 = field.get(o2);
            if (v1 == null) {
                return v2 == null;
            } else if (v1.getClass().isArray()) {
                return DeepUtil.equals(v1, v2);
            } else {
                return v1.equals(v2);
            }
        }
        public int hashCode (Field field, Object object)
                throws IllegalAccessException {
            Object value = field.get(object);
            if (value == null) {
                return 0;
            } else if (value.getClass().isArray()) {
                return DeepUtil.hashCode(value);
            } else {
                return value.hashCode();
            }
        }
    };

    /** Field handler for deep object fields. */
    protected static FieldHandler DEEP_OBJECT_FIELD_HANDLER = new FieldHandler() {
        public void copy (Field field, Object source, Object dest)
                throws IllegalAccessException {
            field.set(dest, DeepUtil.copy(field.get(source), field.get(dest)));
        }
        public boolean equals (Field field, Object o1, Object o2)
                throws IllegalAccessException {
            return DeepUtil.equals(field.get(o1), field.get(o2));
        }
        public int hashCode (Field field, Object object)
                throws IllegalAccessException {
            return DeepUtil.hashCode(field.get(object));
        }
    };

    /** Field handler for shallow object fields. */
    protected static FieldHandler SHALLOW_OBJECT_FIELD_HANDLER = new FieldHandler() {
        public void copy (Field field, Object source, Object dest)
                throws IllegalAccessException {
            field.set(dest, field.get(source));
        }
        public boolean equals (Field field, Object o1, Object o2)
                throws IllegalAccessException {
            return field.get(o1) == field.get(o2);
        }
        public int hashCode (Field field, Object object)
                throws IllegalAccessException {
            return System.identityHashCode(field.get(object));
        }
    };
}
